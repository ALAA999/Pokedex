/*
 * Designed and developed by 2020 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.repository

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.skydoves.pokedex.model.Pokemon
import com.skydoves.pokedex.network.PokedexClient
import com.skydoves.pokedex.persistence.PokemonDao
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import com.skydoves.sandwich.onSuccess
import com.skydoves.whatif.whatIfNotNull
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository @Inject constructor(
  private val pokedexClient: PokedexClient,
  private val pokemonDao: PokemonDao
) : Repository {

  override var isLoading: ObservableBoolean = ObservableBoolean(false)

  suspend fun fetchPokemonList(page: Int, error: (String) -> Unit) = withContext(Dispatchers.IO) {
    val liveData = MutableLiveData<List<Pokemon>>()
    var pokemonList = pokemonDao.getPokemonList(page)
    if (pokemonList.isEmpty()) {
      isLoading.set(true)
      pokedexClient.fetchPokemonList(page = page) {
        isLoading.set(false)
        // handle the case when the API request gets a success response.
        it.onSuccess {
          data.whatIfNotNull { response ->
            pokemonList = response.results
            pokemonList.forEach { pokemon -> pokemon.page = page }
            liveData.postValue(pokemonList)
            pokemonDao.insertPokemonList(pokemonList)
          }
        }
          // handle the case when the API request gets a error response.
          // e.g. internal server error.
          .onError {
            error(message())
          }
          // handle the case when the API request gets a exception response.
          // e.g. network connection error.
          .onException {
            error(message())
          }
      }
    }
    liveData.apply { postValue(pokemonList) }
  }
}

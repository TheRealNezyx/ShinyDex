package com.espinosa.shinydex.data.remote

import com.espinosa.shinydex.data.remote.dto.PokemonDetailResponse
import com.espinosa.shinydex.data.remote.dto.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** The slice of https://pokeapi.co/api/v2/ that ShinyDex consumes. */
interface PokeApiService {

    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0,
    ): PokemonListResponse

    @GET("pokemon/{id}")
    suspend fun getPokemon(@Path("id") id: Int): PokemonDetailResponse
}

package com.espinosa.shinydex.data.model

/** Extra data pulled from PokeAPI when a Pokedex entry is opened. */
data class PokemonDetail(
    val id: Int,
    val types: List<String>,
    val heightMetres: Double,
    val weightKilograms: Double,
    val stats: List<Pair<String, Int>>,
)

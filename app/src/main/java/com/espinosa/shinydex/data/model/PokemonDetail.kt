package com.espinosa.shinydex.data.model

import com.espinosa.shinydex.util.DexEntry

/** Extra data pulled from PokeAPI when a Pokedex entry is opened. */
data class PokemonDetail(
    val id: Int,
    val types: List<String>,
    val heightMetres: Double,
    val weightKilograms: Double,
    val stats: List<Pair<String, Int>>,
    /** The entry from the species' own games; null when it could not be loaded. */
    val dexEntry: DexEntry? = null,
)

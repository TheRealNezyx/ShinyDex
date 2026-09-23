package com.espinosa.shinydex.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `GET /pokemon?limit=&offset=` */
data class PokemonListResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("results") val results: List<NamedResource> = emptyList(),
)

data class NamedResource(
    @SerializedName("name") val name: String = "",
    @SerializedName("url") val url: String = "",
) {
    /** PokeAPI resource URLs end in `/<id>/`; pull the National Dex number back out. */
    val id: Int
        get() = url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0
}

/** `GET /pokemon/{id}` -- only the fields the Pokedex screen actually renders. */
data class PokemonDetailResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("height") val height: Int = 0,
    @SerializedName("weight") val weight: Int = 0,
    @SerializedName("types") val types: List<TypeSlot> = emptyList(),
    @SerializedName("stats") val stats: List<StatSlot> = emptyList(),
)

data class TypeSlot(
    @SerializedName("slot") val slot: Int = 0,
    @SerializedName("type") val type: NamedResource = NamedResource(),
)

data class StatSlot(
    @SerializedName("base_stat") val baseStat: Int = 0,
    @SerializedName("stat") val stat: NamedResource = NamedResource(),
)

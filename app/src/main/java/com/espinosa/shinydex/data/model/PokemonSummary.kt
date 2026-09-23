package com.espinosa.shinydex.data.model

import com.espinosa.shinydex.data.remote.Sprites

/**
 * A Pokedex row: National Dex number, name and the sprites for the era it belongs to.
 *
 * [generation] is derived from the dex number rather than stored, so a hunt loaded back
 * from the database always resolves to the right sprite set.
 */
data class PokemonSummary(
    val id: Int,
    val name: String,
) {
    val generation: Generation = Generation.ofDexNumber(id)

    val displayName: String = name.replaceFirstChar { it.uppercase() }.replace('-', ' ')

    val dexNumber: String = "#" + id.toString().padStart(DEX_DIGITS, '0')

    val spriteUrl: String = Sprites.era(generation, id, shiny = false)

    val shinySpriteUrl: String = Sprites.era(generation, id, shiny = true)

    val artworkUrl: String = Sprites.artwork(id, shiny = false)

    val shinyArtworkUrl: String = Sprites.artwork(id, shiny = true)

    fun sprite(shiny: Boolean): String = if (shiny) shinySpriteUrl else spriteUrl

    fun artwork(shiny: Boolean): String = if (shiny) shinyArtworkUrl else artworkUrl

    private companion object {
        const val DEX_DIGITS = 3
    }
}

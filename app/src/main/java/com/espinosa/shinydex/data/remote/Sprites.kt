package com.espinosa.shinydex.data.remote

import com.espinosa.shinydex.data.model.Generation

/**
 * Sprite URLs served by the PokeAPI sprite CDN.
 *
 * A hunt deserves the sprites of the era it belongs to, so the Pokedex asks each
 * [Generation] for its own sprite folder: Crystal for Gen II, Emerald for Gen III and
 * Platinum for Gen IV.
 */
object Sprites {

    private const val BASE =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    /** The period-correct sprite for [dexNumber] in [generation]. */
    fun era(generation: Generation, dexNumber: Int, shiny: Boolean): String {
        val folder = generation.spriteVersion
        return if (shiny) {
            "$BASE/versions/$folder/shiny/$dexNumber.png"
        } else {
            "$BASE/versions/$folder/$dexNumber.png"
        }
    }

    /** Modern official artwork, used on the detail screens where size matters. */
    fun artwork(dexNumber: Int, shiny: Boolean): String =
        if (shiny) {
            "$BASE/other/official-artwork/shiny/$dexNumber.png"
        } else {
            "$BASE/other/official-artwork/$dexNumber.png"
        }
}

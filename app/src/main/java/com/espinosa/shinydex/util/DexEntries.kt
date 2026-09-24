package com.espinosa.shinydex.util

import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation

/** One game's Pokedex text for a species, as PokeAPI lists it. */
data class RawDexEntry(val text: String, val language: String, val version: String)

/** The Pokedex entry the app shows, and the game it comes from. */
data class DexEntry(val text: String, val game: GameVersion)

/**
 * Picks the Pokedex entry that matches the era a species belongs to.
 *
 * PokeAPI returns every entry from every game and language. ShinyDex shows the one from
 * the same game its sprites come from (Crystal, Emerald or Platinum), and falls back to the
 * other games of that generation when that game has none.
 */
object DexEntries {

    private const val LANGUAGE = "en"

    /** Null when no game of [generation] has an English entry for this species. */
    fun pick(entries: List<RawDexEntry>, generation: Generation): DexEntry? {
        val english = entries.filter { it.language == LANGUAGE }
        return preferredGames(generation).firstNotNullOfOrNull { game ->
            english.firstOrNull { it.version == apiName(game) }
                ?.let { DexEntry(clean(it.text), game) }
                ?.takeIf { it.text.isNotEmpty() }
        }
    }

    /** The sprite game first, then the rest of the generation. */
    fun preferredGames(generation: Generation): List<GameVersion> =
        generation.games.reversed()

    /**
     * The old games wrap text with line breaks, page breaks and soft hyphens, and write
     * "POKéMON" in small caps. This turns all of that into one readable paragraph.
     */
    fun clean(raw: String): String =
        raw.replace("­\n", "")
            .replace("­", "")
            .replace(Regex("[\\n\\f\\r]"), " ")
            .replace(Regex("POK[eé]MON", RegexOption.IGNORE_CASE), "Pokémon")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** PokeAPI names versions in lower case: `crystal`, `emerald`, `platinum`. */
    private fun apiName(game: GameVersion): String = game.name.lowercase()
}

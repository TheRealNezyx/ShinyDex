package com.espinosa.shinydex.data.model

/**
 * A specific game a hunt can belong to.
 *
 * Only the games whose Pokedex matches their generation's own region are listed. FireRed,
 * LeafGreen, HeartGold and SoulSilver are remakes that carry an earlier region's dex, so
 * they would break the "one generation, one regional dex" rule the app is built on.
 */
enum class GameVersion(
    val displayName: String,
    val shortName: String,
    val generation: Generation,
) {
    GOLD("Gold", "GLD", Generation.GEN_II),
    SILVER("Silver", "SLV", Generation.GEN_II),
    CRYSTAL("Crystal", "CRY", Generation.GEN_II),

    RUBY("Ruby", "RBY", Generation.GEN_III),
    SAPPHIRE("Sapphire", "SAP", Generation.GEN_III),
    EMERALD("Emerald", "EMR", Generation.GEN_III),

    DIAMOND("Diamond", "DIA", Generation.GEN_IV),
    PEARL("Pearl", "PRL", Generation.GEN_IV),
    PLATINUM("Platinum", "PLT", Generation.GEN_IV),
    ;

    companion object {
        fun fromNameOrNull(value: String?): GameVersion? =
            entries.firstOrNull { it.name == value }

        /** The game a new hunt starts on for [generation]. */
        fun defaultFor(generation: Generation): GameVersion =
            generation.games.lastOrNull() ?: CRYSTAL

        fun fromNameOrDefault(value: String?): GameVersion =
            fromNameOrNull(value) ?: CRYSTAL
    }
}

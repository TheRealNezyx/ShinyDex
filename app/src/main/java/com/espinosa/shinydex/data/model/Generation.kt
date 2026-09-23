package com.espinosa.shinydex.data.model

/**
 * A playable generation.
 *
 * Shiny Pokemon were introduced in Generation II (Gold / Silver / Crystal); Generation I
 * (Red / Blue / Yellow) has no shiny mechanic at all, so it stays locked.
 *
 * [dexRange] holds only the species *introduced* in that generation, not the cumulative
 * National Dex. Picking a generation therefore scopes the whole app -- Pokedex, hunt
 * picker and sprites -- to that region's own Pokemon.
 */
enum class Generation(
    val romanNumeral: String,
    val gamesLabel: String,
    val region: String,
    val dexRange: IntRange,
    /** Folder under the PokeAPI sprite CDN that holds this era's sprites. */
    val spriteVersion: String,
    val unlocked: Boolean,
) {
    GEN_I(
        romanNumeral = "I",
        gamesLabel = "Red · Blue · Yellow",
        region = "Kanto",
        dexRange = 1..151,
        spriteVersion = "generation-i/yellow/transparent",
        unlocked = false,
    ),
    GEN_II(
        romanNumeral = "II",
        gamesLabel = "Gold · Silver · Crystal",
        region = "Johto",
        dexRange = 152..251,
        spriteVersion = "generation-ii/crystal/transparent",
        unlocked = true,
    ),
    GEN_III(
        romanNumeral = "III",
        gamesLabel = "Ruby · Sapphire · Emerald",
        region = "Hoenn",
        dexRange = 252..386,
        spriteVersion = "generation-iii/emerald",
        unlocked = true,
    ),
    GEN_IV(
        romanNumeral = "IV",
        gamesLabel = "Diamond · Pearl · Platinum",
        region = "Sinnoh",
        dexRange = 387..493,
        spriteVersion = "generation-iv/platinum",
        unlocked = true,
    ),
    ;

    val title: String get() = "Generation $romanNumeral"

    /** 1 for Gen I, 2 for Gen II... the form the face-off server speaks. */
    val number: Int get() = ordinal + 1

    val subtitle: String
        get() = if (this == GEN_I) "No shiny mechanic in Gen I" else gamesLabel

    val speciesCount: Int get() = dexRange.count()

    /** How many entries to ask PokeAPI for, and where to start. */
    val apiOffset: Int get() = dexRange.first - 1
    val apiLimit: Int get() = speciesCount

    /** Games that belong to this generation. */
    val games: List<GameVersion>
        get() = GameVersion.entries.filter { it.generation == this }

    /** Every hunting method this generation supports. */
    val methods: List<HuntMethod>
        get() = HuntMethod.entries.filter { it.generation == this }

    /** Methods that are legal in [game]. */
    fun methodsFor(game: GameVersion): List<HuntMethod> =
        methods.filter { it.isAvailableIn(game) }

    companion object {
        fun fromNameOrDefault(value: String?): Generation =
            entries.firstOrNull { it.name == value } ?: GEN_II

        /** Which generation introduced this National Dex number. */
        fun ofDexNumber(dexNumber: Int): Generation =
            entries.firstOrNull { dexNumber in it.dexRange } ?: GEN_II

        val unlockedEntries: List<Generation> get() = entries.filter { it.unlocked }

        fun fromNumber(number: Int): Generation? = entries.getOrNull(number - 1)
    }
}

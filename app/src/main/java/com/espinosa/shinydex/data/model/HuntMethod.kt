package com.espinosa.shinydex.data.model

/**
 * A way of hunting for a shiny, tied to the generation it exists in.
 *
 * Base odds are 1 in 8192 from Generation II through Generation IV; what changes between
 * generations is which methods can beat those odds. Gen II has the Odd Egg and DV-inheritance
 * breeding, Gen III has nothing at all, and Gen IV introduces the Masuda Method and the
 * Poke Radar.
 *
 * [onlyIn] narrows a method to specific games; an empty list means every game of the
 * generation supports it.
 */
enum class HuntMethod(
    val label: String,
    val generation: Generation,
    val oddsDenominator: Int,
    val oddsLabel: String,
    val description: String,
    val onlyIn: List<GameVersion> = emptyList(),
    val guaranteed: Boolean = false,
) {

    /* ------------------------------------------------ Generation II */

    G2_RANDOM_ENCOUNTER(
        label = "Random Encounter",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Walk in grass or caves and check every encounter, " +
            "then run away and repeat. The classic way to lose your afternoon.",
    ),
    G2_SURFING(
        label = "Surfing",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds on water encounters. Surf back and forth on any water " +
            "route and check each Tentacool that shows up.",
    ),
    G2_FISHING(
        label = "Fishing",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Cast with the Old, Good or Super Rod. The rod tier " +
            "changes which species you meet, never the shiny rate.",
    ),
    G2_HEADBUTT(
        label = "Headbutt Trees",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Headbutt trees for Heracross, Pineco and friends. " +
            "Which trees hold Pokemon depends on your Trainer ID, so scout first.",
    ),
    G2_SOFT_RESET(
        label = "Soft Reset",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. For starters, legendaries, gifts and statics: save in " +
            "front of it, encounter it, then soft reset with A + B + Start + Select.",
    ),
    G2_ROAMING(
        label = "Roaming Beast",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Raikou, Entei and Suicune lock their DVs the moment they are " +
            "released, so soft reset at the Burned Tower rather than chasing them.",
    ),
    G2_BUG_CONTEST(
        label = "Bug-Catching Contest",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds inside the National Park contest. Tuesday, Thursday " +
            "and Saturday only, twenty encounters per run.",
    ),
    G2_GAME_CORNER(
        label = "Game Corner Prize",
        generation = Generation.GEN_II,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Buy the prize Pokemon with coins, check it, reset if " +
            "it is not shiny. Bring a lot of coins.",
    ),
    G2_BREEDING(
        label = "Breeding (Shiny Parent)",
        generation = Generation.GEN_II,
        oddsDenominator = 64,
        oddsLabel = "1 / 64",
        description = "Gen II exclusive. Shininess comes from DVs, and DVs are inherited, " +
            "so a shiny parent passes the trait down far more often than full odds.",
    ),
    G2_ODD_EGG(
        label = "Odd Egg",
        generation = Generation.GEN_II,
        oddsDenominator = 7,
        oddsLabel = "~14%",
        description = "Crystal only. The Day-Care Man on Route 34 hands you an Odd Egg " +
            "with a 14% shiny chance in international versions. Save before taking it.",
        onlyIn = listOf(GameVersion.CRYSTAL),
    ),
    G2_RED_GYARADOS(
        label = "Red Gyarados",
        generation = Generation.GEN_II,
        oddsDenominator = 1,
        oddsLabel = "Guaranteed",
        description = "The Lake of Rage Gyarados is scripted to be shiny. Not a hunt so " +
            "much as a free entry in the dex -- do not miss the catch.",
        guaranteed = true,
    ),

    /* ------------------------------------------------ Generation III */

    G3_RANDOM_ENCOUNTER(
        label = "Random Encounter",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds in grass and caves. Gen III has no method that improves " +
            "the rate, so this is exactly as slow as it sounds.",
    ),
    G3_SURFING(
        label = "Surfing",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds on water. Surf along a route and check every Wingull " +
            "and Tentacool that interrupts you.",
    ),
    G3_FISHING(
        label = "Fishing",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Also the only sane way to find Feebas, though the tile " +
            "hunt for it is a separate problem from the shiny hunt.",
    ),
    G3_ROCK_SMASH(
        label = "Rock Smash",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Smash rocks in caves for Geodude and Nosepass. Slow " +
            "encounters, but they come to you.",
    ),
    G3_SAFARI_ZONE(
        label = "Safari Zone",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Limited steps per entry, so budget your run and expect " +
            "to pay the entry fee many times.",
    ),
    G3_SOFT_RESET(
        label = "Soft Reset",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. For starters, Rayquaza, Groudon, Kyogre and the Regis: " +
            "save in front of it and reset with A + B + Start + Select.",
    ),
    G3_ROAMING(
        label = "Roaming Latias / Latios",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds, and the roamer flees every turn. Bring a Pokemon with " +
            "Mean Look and a lot of patience.",
    ),
    G3_BREEDING(
        label = "Breeding",
        generation = Generation.GEN_III,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Still full odds. Gen III dropped DV inheritance and the Masuda " +
            "Method did not exist yet, so eggs are no better than the grass.",
    ),

    /* ------------------------------------------------ Generation IV */

    G4_RANDOM_ENCOUNTER(
        label = "Random Encounter",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds in grass and caves. Fine for a species the Poke Radar " +
            "cannot reach, otherwise use the Radar.",
    ),
    G4_SURFING(
        label = "Surfing",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds on water. The Poke Radar does not work while surfing, " +
            "so water species are a full odds hunt.",
    ),
    G4_FISHING(
        label = "Fishing",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds with the Old, Good and Super Rod. The rod changes the " +
            "species pool, never the shiny rate.",
    ),
    G4_SOFT_RESET(
        label = "Soft Reset",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. For starters, Dialga, Palkia, Giratina and the lake " +
            "trio: save in front of it and reset with L + R + Start + Select.",
    ),
    G4_POKE_RADAR(
        label = "Poke Radar Chain",
        generation = Generation.GEN_IV,
        oddsDenominator = 200,
        oddsLabel = "~1 / 200",
        description = "The best odds in Gen IV. Chain the same species in shaking grass; " +
            "at a chain of 40 the rate caps near 1 in 200. Break the chain and you start " +
            "over, so count carefully.",
    ),
    G4_MASUDA(
        label = "Masuda Method",
        generation = Generation.GEN_IV,
        oddsDenominator = 1638,
        oddsLabel = "1 / 1638",
        description = "Breed two Pokemon from games of different languages and the egg " +
            "gets extra shiny rolls. Introduced in Gen IV and still worth it today.",
    ),
    G4_HONEY_TREE(
        label = "Honey Tree",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds. Slather a tree with Honey, wait six hours and check " +
            "what turned up. The only way to find Munchlax and Heracross.",
    ),
    G4_GREAT_MARSH(
        label = "Great Marsh",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds inside the Pastoria safari. The species rotate daily, " +
            "so check the board before paying the entry fee.",
    ),
    G4_ROAMING(
        label = "Roaming Pokemon",
        generation = Generation.GEN_IV,
        oddsDenominator = FULL_ODDS,
        oddsLabel = FULL_ODDS_LABEL,
        description = "Full odds for Mesprit, Cresselia and the roaming beasts. Their " +
            "data is fixed when released, so reset before that first encounter.",
    ),
    ;

    /** True when [game] can actually use this method. */
    fun isAvailableIn(game: GameVersion): Boolean =
        game.generation == generation && (onlyIn.isEmpty() || game in onlyIn)

    companion object {
        fun fromNameOrDefault(value: String?): HuntMethod =
            entries.firstOrNull { it.name == value } ?: G2_RANDOM_ENCOUNTER

        /** Every method [game] supports. */
        fun availableFor(game: GameVersion): List<HuntMethod> =
            entries.filter { it.isAvailableIn(game) }

        /** The method a new hunt starts on for [game]. */
        fun defaultFor(game: GameVersion): HuntMethod =
            availableFor(game).firstOrNull() ?: G2_RANDOM_ENCOUNTER
    }
}

/** Base shiny rate from Generation II through Generation IV. */
const val FULL_ODDS = 8192
private const val FULL_ODDS_LABEL = "1 / 8192"

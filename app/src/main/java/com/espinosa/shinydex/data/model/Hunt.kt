package com.espinosa.shinydex.data.model

import com.espinosa.shinydex.data.local.HuntEntity
import com.espinosa.shinydex.util.Odds

/** A hunt as the UI sees it: entity fields resolved into real enums. */
data class Hunt(
    val id: Long,
    val pokemon: PokemonSummary,
    val generation: Generation,
    val game: GameVersion,
    val method: HuntMethod,
    val encounters: Int,
    val isFound: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val oddsLabel: String get() = method.oddsLabel

    val probabilityLabel: String
        get() = Odds.cumulativeProbabilityLabel(encounters, method.oddsDenominator)

    val progress: Float
        get() = Odds.progressTowardsOdds(encounters, method.oddsDenominator)
}

fun HuntEntity.toHunt(): Hunt {
    val resolvedGeneration = Generation.fromNameOrDefault(generation)
    return Hunt(
        id = id,
        pokemon = PokemonSummary(id = pokemonId, name = pokemonName),
        generation = resolvedGeneration,
        game = GameVersion.fromNameOrNull(game)
            ?: GameVersion.defaultFor(resolvedGeneration),
        method = HuntMethod.fromNameOrDefault(method),
        encounters = encounters,
        isFound = isFound,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

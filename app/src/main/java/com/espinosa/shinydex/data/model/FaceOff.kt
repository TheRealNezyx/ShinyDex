package com.espinosa.shinydex.data.model

import com.espinosa.shinydex.data.remote.faceoff.PlayerDto
import com.espinosa.shinydex.data.remote.faceoff.RoomDto
import com.espinosa.shinydex.util.Odds

/** How a face-off room plays. */
enum class FaceOffMode(val label: String, val tagline: String) {
    BATTLE("Battle", "Same Pokemon for everyone. First shiny wins."),
    LOUNGE("Lounge", "Everyone hunts their own. Nobody loses."),
}

data class FaceOffPlayer(
    val id: String,
    val name: String,
    val pokemon: PokemonSummary,
    val method: String,
    val oddsDenominator: Int,
    val encounters: Int,
    val found: Boolean,
    val online: Boolean,
) {
    val probabilityLabel: String
        get() = Odds.cumulativeProbabilityLabel(encounters, oddsDenominator)
}

data class FaceOffRoom(
    val code: String,
    val mode: FaceOffMode,
    val generation: Generation,
    val finished: Boolean,
    val hostId: String,
    val winnerId: String?,
    val winnerName: String?,
    /** Battle only: what everybody is hunting. */
    val target: PokemonSummary?,
    val targetMethod: String?,
    val targetOdds: Int,
    val players: List<FaceOffPlayer>,
    val totalEncounters: Int,
    val maxPlayers: Int,
) {
    val foundCount: Int get() = players.count { it.found }

    /**
     * Battle: the chance that *somebody* in the room has hit the shiny by now. Every
     * encounter from every player is an independent roll at the same odds, so the room's
     * encounters simply add up: P = 1 - (1 - 1/d)^(n1 + n2 + ...).
     */
    val roomProbabilityLabel: String
        get() = Odds.cumulativeProbabilityLabel(totalEncounters, targetOdds)

    fun player(id: String?): FaceOffPlayer? = players.firstOrNull { it.id == id }
}

fun RoomDto.toDomain(): FaceOffRoom? {
    val resolvedMode = FaceOffMode.entries.firstOrNull { it.name == mode }
    val resolvedGeneration = Generation.fromNumber(generation)?.takeIf { it.unlocked }
    if (resolvedMode == null || resolvedGeneration == null) return null
    return FaceOffRoom(
        code = code,
        mode = resolvedMode,
        generation = resolvedGeneration,
        finished = status == "FINISHED",
        hostId = hostId,
        winnerId = winnerId,
        winnerName = winnerName,
        target = target?.let { PokemonSummary(id = it.pokemonId, name = it.pokemonName) },
        targetMethod = target?.method,
        targetOdds = target?.oddsDenominator ?: FULL_ODDS,
        players = players.map { it.toDomain() },
        totalEncounters = totalEncounters,
        maxPlayers = maxPlayers,
    )
}

private fun PlayerDto.toDomain() = FaceOffPlayer(
    id = id,
    name = name,
    pokemon = PokemonSummary(id = pokemonId, name = pokemonName),
    method = method,
    oddsDenominator = oddsDenominator,
    encounters = encounters,
    found = found,
    online = online,
)

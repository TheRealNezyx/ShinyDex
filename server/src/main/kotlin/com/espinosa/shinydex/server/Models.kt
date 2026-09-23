package com.espinosa.shinydex.server

/** How a face-off room plays. */
enum class Mode {
    /** Everyone hunts the same Pokemon; the first shiny wins and the room closes. */
    BATTLE,

    /** Everyone hunts their own Pokemon side by side; nobody wins, everybody celebrates. */
    LOUNGE,
}

enum class RoomStatus { OPEN, FINISHED }

/** Server-side player record. [token] is the player's secret and never leaves this process. */
class Player(
    val id: String,
    val token: String,
    val name: String,
    val pokemonId: Int,
    val pokemonName: String,
    val method: String,
    val oddsDenominator: Int,
    val joinedAt: Long,
) {
    var encounters: Int = 0
    var foundAt: Long? = null
    var lastSeen: Long = joinedAt

    val found: Boolean get() = foundAt != null
}

/** Server-side room record. Mutated only while holding the [RoomStore] lock. */
class Room(
    val code: String,
    val mode: Mode,
    val generation: Int,
    val createdAt: Long,
    /** Battle only: the single Pokemon everybody is hunting. */
    val target: Target?,
) {
    val players = mutableListOf<Player>()
    var hostId: String = ""
    var status: RoomStatus = RoomStatus.OPEN
    var winnerId: String? = null
    var winnerName: String? = null
    var lastActivity: Long = createdAt
}

data class Target(
    val pokemonId: Int,
    val pokemonName: String,
    val method: String,
    val oddsDenominator: Int,
)

/* ------------------------------------------------ wire format ------------ */

data class CreateRoomRequest(
    val mode: String? = null,
    val generation: Int? = null,
    val name: String? = null,
    val pokemonId: Int? = null,
    val pokemonName: String? = null,
    val method: String? = null,
    val oddsDenominator: Int? = null,
)

data class JoinRoomRequest(
    val name: String? = null,
    val pokemonId: Int? = null,
    val pokemonName: String? = null,
    val method: String? = null,
    val oddsDenominator: Int? = null,
)

data class EncounterRequest(val delta: Int? = null)

/** Returned once, on create or join: the only time a client ever sees its own token. */
data class JoinResponse(
    val playerId: String,
    val token: String,
    val room: RoomView,
)

/** What any member of the room may see. Deliberately has no tokens in it. */
data class RoomView(
    val code: String,
    val mode: Mode,
    val generation: Int,
    val status: RoomStatus,
    val hostId: String,
    val winnerId: String?,
    val winnerName: String?,
    val target: Target?,
    val players: List<PlayerView>,
    val totalEncounters: Int,
    val maxPlayers: Int,
)

data class PlayerView(
    val id: String,
    val name: String,
    val pokemonId: Int,
    val pokemonName: String,
    val method: String,
    val oddsDenominator: Int,
    val encounters: Int,
    val found: Boolean,
    val foundAt: Long?,
    val online: Boolean,
)

data class ErrorResponse(val error: String)

/** A rule violation, carrying the HTTP status it should be reported with. */
class FaceOffException(val status: Int, override val message: String) : RuntimeException(message)

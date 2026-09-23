package com.espinosa.shinydex.server

import java.security.SecureRandom
import java.util.UUID

/**
 * All face-off rules live here, independent of HTTP, so they can be unit-tested directly.
 *
 * State is in memory only: rooms are short-lived by nature, and a restart simply ends the
 * face-offs in progress. Every public method takes the same lock, which keeps the rules
 * (one winner per battle, no updates after the finish) race-free without a database.
 */
class RoomStore(
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
) {

    private val lock = Any()
    private val rooms = HashMap<String, Room>()

    /* ------------------------------------------------ commands ---------- */

    fun create(request: CreateRoomRequest): JoinResponse = synchronized(lock) {
        purgeExpired()
        if (rooms.size >= MAX_ROOMS) fail(SERVICE_UNAVAILABLE, "The server is full, try again later.")

        val mode = parseMode(request.mode)
        val generation = parseGeneration(request.generation)
        val name = cleanName(request.name)
        val pick = cleanPick(generation, request.pokemonId, request.pokemonName, request.method,
            request.oddsDenominator)

        val now = clock()
        val room = Room(
            code = newCode(),
            mode = mode,
            generation = generation,
            createdAt = now,
            target = if (mode == Mode.BATTLE) pick else null,
        )
        val host = newPlayer(name, pick, now)
        room.players += host
        room.hostId = host.id
        rooms[room.code] = room

        JoinResponse(playerId = host.id, token = host.token, room = view(room))
    }

    fun join(rawCode: String?, request: JoinRoomRequest): JoinResponse = synchronized(lock) {
        purgeExpired()
        val room = roomOrFail(rawCode)
        if (room.status == RoomStatus.FINISHED) fail(CONFLICT, "This face-off is already over.")
        if (room.players.size >= MAX_PLAYERS) fail(CONFLICT, "This room is full.")

        val name = cleanName(request.name)
        if (room.players.any { it.name.equals(name, ignoreCase = true) }) {
            fail(CONFLICT, "Someone in this room is already called $name.")
        }

        // In a battle everyone hunts the host's target; in the lounge each player brings one.
        val pick = room.target ?: cleanPick(room.generation, request.pokemonId,
            request.pokemonName, request.method, request.oddsDenominator)

        val now = clock()
        val player = newPlayer(name, pick, now)
        room.players += player
        room.lastActivity = now

        JoinResponse(playerId = player.id, token = player.token, room = view(room))
    }

    /** Read-only view. A valid [token] also marks that player as online. */
    fun get(rawCode: String?, token: String?): RoomView = synchronized(lock) {
        val room = roomOrFail(rawCode)
        room.players.firstOrNull { it.token == token }?.lastSeen = clock()
        view(room)
    }

    fun addEncounters(rawCode: String?, token: String?, delta: Int?): RoomView = synchronized(lock) {
        val room = roomOrFail(rawCode)
        val player = playerOrFail(room, token)
        val step = delta ?: fail(BAD_REQUEST, "Missing delta.")
        if (step == 0 || step !in -MAX_STEP..MAX_STEP) {
            fail(BAD_REQUEST, "Encounters change by 1 to $MAX_STEP at a time.")
        }
        if (room.status == RoomStatus.FINISHED) fail(CONFLICT, "This face-off is already over.")
        if (player.found) fail(CONFLICT, "You already found your shiny.")

        val now = clock()
        player.encounters = (player.encounters + step).coerceIn(0, MAX_ENCOUNTERS)
        player.lastSeen = now
        room.lastActivity = now
        view(room)
    }

    fun markFound(rawCode: String?, token: String?): RoomView = synchronized(lock) {
        val room = roomOrFail(rawCode)
        val player = playerOrFail(room, token)
        if (room.status == RoomStatus.FINISHED) {
            fail(CONFLICT, "${room.winnerName ?: "Someone"} already won this face-off.")
        }
        if (player.found) fail(CONFLICT, "You already found your shiny.")

        val now = clock()
        player.foundAt = now
        player.lastSeen = now
        room.lastActivity = now

        when (room.mode) {
            // First shiny ends the battle. The lock guarantees nobody else can also win.
            Mode.BATTLE -> {
                room.winnerId = player.id
                room.winnerName = player.name
                room.status = RoomStatus.FINISHED
            }
            // The lounge only closes once every hunter has their shiny.
            Mode.LOUNGE -> if (room.players.all { it.found }) room.status = RoomStatus.FINISHED
        }
        view(room)
    }

    fun leave(rawCode: String?, token: String?): Unit = synchronized(lock) {
        val room = roomOrFail(rawCode)
        val player = playerOrFail(room, token)
        room.players.remove(player)
        room.lastActivity = clock()
        when {
            room.players.isEmpty() -> rooms.remove(room.code)
            room.hostId == player.id -> room.hostId = room.players.minBy { it.joinedAt }.id
        }
    }

    /** Test and monitoring hook. */
    fun roomCount(): Int = synchronized(lock) { rooms.size }

    /* ------------------------------------------------ views ------------- */

    private fun view(room: Room): RoomView {
        val now = clock()
        val ordered = when (room.mode) {
            Mode.BATTLE -> room.players.sortedWith(
                compareByDescending<Player> { it.id == room.winnerId }
                    .thenByDescending { it.encounters }
                    .thenBy { it.joinedAt },
            )
            Mode.LOUNGE -> room.players.sortedWith(
                compareBy<Player> { it.foundAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.encounters }
                    .thenBy { it.joinedAt },
            )
        }
        return RoomView(
            code = room.code,
            mode = room.mode,
            generation = room.generation,
            status = room.status,
            hostId = room.hostId,
            winnerId = room.winnerId,
            winnerName = room.winnerName,
            target = room.target,
            players = ordered.map { p ->
                PlayerView(
                    id = p.id,
                    name = p.name,
                    pokemonId = p.pokemonId,
                    pokemonName = p.pokemonName,
                    method = p.method,
                    oddsDenominator = p.oddsDenominator,
                    encounters = p.encounters,
                    found = p.found,
                    foundAt = p.foundAt,
                    online = now - p.lastSeen <= ONLINE_WINDOW_MS,
                )
            },
            totalEncounters = room.players.sumOf { it.encounters },
            maxPlayers = MAX_PLAYERS,
        )
    }

    /* ------------------------------------------------ validation -------- */

    private fun roomOrFail(rawCode: String?): Room {
        val code = normalizeCode(rawCode) ?: fail(BAD_REQUEST, "That is not a valid room code.")
        return rooms[code] ?: fail(NOT_FOUND, "No room with code $code.")
    }

    private fun playerOrFail(room: Room, token: String?): Player =
        room.players.firstOrNull { token != null && it.token == token }
            ?: fail(FORBIDDEN, "You are not in this room.")

    private fun parseMode(raw: String?): Mode =
        Mode.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
            ?: fail(BAD_REQUEST, "Mode must be BATTLE or LOUNGE.")

    private fun parseGeneration(raw: Int?): Int =
        raw?.takeIf { it in DEX_RANGES } ?: fail(BAD_REQUEST, "Generation must be 2, 3 or 4.")

    private fun cleanName(raw: String?): String {
        val cleaned = raw.orEmpty()
            .filter { it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-' }
            .trim()
            .replace(Regex(" +"), " ")
            .take(MAX_NAME)
        if (cleaned.isEmpty()) fail(BAD_REQUEST, "Pick a name first.")
        return cleaned
    }

    private fun cleanPick(
        generation: Int,
        pokemonId: Int?,
        pokemonName: String?,
        method: String?,
        oddsDenominator: Int?,
    ): Target {
        val range = DEX_RANGES.getValue(generation)
        val id = pokemonId?.takeIf { it in range }
            ?: fail(BAD_REQUEST, "That Pokemon is not part of Generation $generation.")
        val species = pokemonName.orEmpty().trim().lowercase()
            .takeIf { it.isNotEmpty() && it.length <= MAX_SPECIES && SPECIES_PATTERN.matches(it) }
            ?: fail(BAD_REQUEST, "Invalid Pokemon name.")
        val methodLabel = method.orEmpty()
            .filter { !it.isISOControl() }
            .trim()
            .take(MAX_METHOD)
            .ifEmpty { fail(BAD_REQUEST, "Pick a hunting method.") }
        val odds = oddsDenominator?.takeIf { it in 1..MAX_ODDS }
            ?: fail(BAD_REQUEST, "Odds must be between 1 and $MAX_ODDS.")
        return Target(id, species, methodLabel, odds)
    }

    /* ------------------------------------------------ helpers ----------- */

    private fun newPlayer(name: String, pick: Target, now: Long) = Player(
        id = UUID.randomUUID().toString().take(PLAYER_ID_LENGTH),
        token = UUID.randomUUID().toString() + UUID.randomUUID().toString(),
        name = name,
        pokemonId = pick.pokemonId,
        pokemonName = pick.pokemonName,
        method = pick.method,
        oddsDenominator = pick.oddsDenominator,
        joinedAt = now,
    )

    private fun newCode(): String {
        repeat(CODE_ATTEMPTS) {
            val code = buildString {
                repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
            }
            if (code !in rooms) return code
        }
        fail(SERVICE_UNAVAILABLE, "Could not allocate a room code.")
    }

    private fun purgeExpired() {
        val cutoff = clock() - ROOM_TTL_MS
        rooms.values.removeAll { it.lastActivity < cutoff }
    }

    private fun fail(status: Int, message: String): Nothing = throw FaceOffException(status, message)

    companion object {
        /** No 0/O or 1/I, so a code read aloud over a call cannot be misheard. */
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val CODE_LENGTH = 6
        const val MAX_PLAYERS = 8
        const val MAX_STEP = 10

        private const val MAX_ROOMS = 500
        private const val MAX_NAME = 20
        private const val MAX_SPECIES = 30
        private const val MAX_METHOD = 40
        private const val MAX_ODDS = 8192
        private const val MAX_ENCOUNTERS = 999_999
        private const val CODE_ATTEMPTS = 20
        private const val PLAYER_ID_LENGTH = 8
        private const val ONLINE_WINDOW_MS = 20_000L
        private const val ROOM_TTL_MS = 12 * 60 * 60 * 1000L

        private const val BAD_REQUEST = 400
        private const val FORBIDDEN = 403
        private const val NOT_FOUND = 404
        private const val CONFLICT = 409
        private const val SERVICE_UNAVAILABLE = 503

        private val SPECIES_PATTERN = Regex("[a-z0-9-]+")

        /** Each generation's own dex, matching the app's Generation enum. */
        val DEX_RANGES: Map<Int, IntRange> = mapOf(
            2 to 152..251,
            3 to 252..386,
            4 to 387..493,
        )

        /** Upper-cases, strips separators, and rejects anything outside the code alphabet. */
        fun normalizeCode(raw: String?): String? {
            val code = raw.orEmpty().uppercase().filter { it.isLetterOrDigit() }
            return code.takeIf { it.length == CODE_LENGTH && it.all { c -> c in CODE_ALPHABET } }
        }
    }
}

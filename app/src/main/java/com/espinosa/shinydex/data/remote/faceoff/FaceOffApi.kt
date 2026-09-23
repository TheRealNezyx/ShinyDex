package com.espinosa.shinydex.data.remote.faceoff

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** The ShinyDex face-off server (see the :server module). */
interface FaceOffApi {

    @POST("rooms")
    suspend fun create(@Body body: CreateRoomBody): JoinResult

    @GET("rooms/{code}")
    suspend fun room(
        @Path("code") code: String,
        @Header(TOKEN_HEADER) token: String?,
    ): RoomDto

    @POST("rooms/{code}/join")
    suspend fun join(
        @Path("code") code: String,
        @Body body: JoinRoomBody,
    ): JoinResult

    @POST("rooms/{code}/encounters")
    suspend fun encounters(
        @Path("code") code: String,
        @Header(TOKEN_HEADER) token: String,
        @Body body: EncounterBody,
    ): RoomDto

    @POST("rooms/{code}/found")
    suspend fun found(
        @Path("code") code: String,
        @Header(TOKEN_HEADER) token: String,
    ): RoomDto

    @POST("rooms/{code}/leave")
    suspend fun leave(
        @Path("code") code: String,
        @Header(TOKEN_HEADER) token: String,
    ): Response<Unit>

    companion object {
        const val TOKEN_HEADER = "X-Player-Token"
    }
}

/* Wire format. Mirrors server/src/main/kotlin/.../Models.kt; defaults keep Gson null-safe. */

data class CreateRoomBody(
    val mode: String,
    val generation: Int,
    val name: String,
    val pokemonId: Int,
    val pokemonName: String,
    val method: String,
    val oddsDenominator: Int,
)

data class JoinRoomBody(
    val name: String,
    val pokemonId: Int? = null,
    val pokemonName: String? = null,
    val method: String? = null,
    val oddsDenominator: Int? = null,
)

data class EncounterBody(val delta: Int)

data class JoinResult(
    val playerId: String = "",
    val token: String = "",
    val room: RoomDto = RoomDto(),
)

data class RoomDto(
    val code: String = "",
    val mode: String = "",
    val generation: Int = 0,
    val status: String = "",
    val hostId: String = "",
    val winnerId: String? = null,
    val winnerName: String? = null,
    val target: TargetDto? = null,
    val players: List<PlayerDto> = emptyList(),
    val totalEncounters: Int = 0,
    val maxPlayers: Int = 0,
)

data class TargetDto(
    val pokemonId: Int = 0,
    val pokemonName: String = "",
    val method: String = "",
    val oddsDenominator: Int = 1,
)

data class PlayerDto(
    val id: String = "",
    val name: String = "",
    val pokemonId: Int = 0,
    val pokemonName: String = "",
    val method: String = "",
    val oddsDenominator: Int = 1,
    val encounters: Int = 0,
    val found: Boolean = false,
    val foundAt: Long? = null,
    val online: Boolean = false,
)

data class ErrorDto(val error: String? = null)

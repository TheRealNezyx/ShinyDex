package com.espinosa.shinydex.data.repo

import com.espinosa.shinydex.data.local.FaceOffSession
import com.espinosa.shinydex.data.model.FaceOffMode
import com.espinosa.shinydex.data.model.FaceOffRoom
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import com.espinosa.shinydex.data.model.PokemonSummary
import com.espinosa.shinydex.data.model.toDomain
import com.espinosa.shinydex.data.remote.ApiClient
import com.espinosa.shinydex.data.remote.faceoff.CreateRoomBody
import com.espinosa.shinydex.data.remote.faceoff.EncounterBody
import com.espinosa.shinydex.data.remote.faceoff.ErrorDto
import com.espinosa.shinydex.data.remote.faceoff.FaceOffApi
import com.espinosa.shinydex.data.remote.faceoff.JoinResult
import com.espinosa.shinydex.data.remote.faceoff.JoinRoomBody
import com.espinosa.shinydex.data.remote.faceoff.RoomDto
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/** A face-off call that failed, with a message fit to show the user. */
class FaceOffError(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** What joining or creating hands back: the room plus this device's identity in it. */
data class FaceOffSeat(val playerId: String, val token: String, val room: FaceOffRoom)

/**
 * Talks to the face-off server. The address is user-configurable (emulator, LAN laptop or a
 * deployed HTTPS host), so the Retrofit instance is rebuilt whenever it changes. It reuses
 * the app's single hardened OkHttp client, so timeouts and debug-only logging still apply.
 */
class FaceOffRepository {

    private val gson = Gson()
    private var baseUrl: String? = null
    private var api: FaceOffApi? = null

    private fun api(serverUrl: String): FaceOffApi {
        val normalized = normalizeServer(serverUrl)
        val cached = api
        if (cached != null && normalized == baseUrl) return cached
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(ApiClient.okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FaceOffApi::class.java)
            .also {
                baseUrl = normalized
                api = it
            }
    }

    suspend fun create(
        serverUrl: String,
        mode: FaceOffMode,
        generation: Generation,
        name: String,
        pokemon: PokemonSummary,
        method: HuntMethod,
    ): Result<FaceOffSeat> = call(serverUrl) {
        api(it).create(
            CreateRoomBody(
                mode = mode.name,
                generation = generation.number,
                name = name,
                pokemonId = pokemon.id,
                pokemonName = pokemon.name,
                method = method.label,
                oddsDenominator = method.oddsDenominator,
            ),
        ).toSeat()
    }

    /** Looks at a room before joining, so the app knows whether it needs a Pokemon first. */
    suspend fun peek(serverUrl: String, code: String): Result<FaceOffRoom> =
        room(serverUrl, code, token = null)

    suspend fun join(
        serverUrl: String,
        code: String,
        name: String,
        pokemon: PokemonSummary?,
        method: HuntMethod?,
    ): Result<FaceOffSeat> = call(serverUrl) {
        api(it).join(
            code,
            JoinRoomBody(
                name = name,
                pokemonId = pokemon?.id,
                pokemonName = pokemon?.name,
                method = method?.label,
                oddsDenominator = method?.oddsDenominator,
            ),
        ).toSeat()
    }

    suspend fun room(serverUrl: String, code: String, token: String?): Result<FaceOffRoom> =
        call(serverUrl) { api(it).room(code, token).toRoom() }

    suspend fun addEncounters(serverUrl: String, code: String, token: String, delta: Int) =
        call(serverUrl) { api(it).encounters(code, token, EncounterBody(delta)).toRoom() }

    suspend fun markFound(serverUrl: String, code: String, token: String) =
        call(serverUrl) { api(it).found(code, token).toRoom() }

    suspend fun leave(serverUrl: String, code: String, token: String): Result<Unit> =
        call(serverUrl) { api(it).leave(code, token); Unit }

    /* ------------------------------------------------------------------ */

    private inline fun <T> call(serverUrl: String, block: (String) -> T): Result<T> =
        try {
            Result.success(block(serverUrl))
        } catch (e: FaceOffError) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(FaceOffError(serverMessage(e), e.code()))
        } catch (e: IOException) {
            val where = normalizeServer(serverUrl)
            Result.failure(FaceOffError("Can't reach the face-off server at $where", cause = e))
        } catch (e: IllegalArgumentException) {
            Result.failure(FaceOffError("That server address is not valid.", cause = e))
        }

    private fun serverMessage(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = runCatching { gson.fromJson(body, ErrorDto::class.java)?.error }.getOrNull()
        return parsed ?: "The server said no (HTTP ${e.code()})."
    }

    private fun JoinResult.toSeat(): FaceOffSeat =
        FaceOffSeat(playerId = playerId, token = token, room = room.toRoom())

    private fun RoomDto.toRoom(): FaceOffRoom =
        toDomain() ?: throw FaceOffError("The server sent a room this app does not understand.")

    companion object {
        /** Adds a scheme if missing and the trailing slash Retrofit insists on. */
        fun normalizeServer(raw: String): String {
            val trimmed = raw.trim().ifEmpty { FaceOffSession.DEFAULT_SERVER }
            val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}

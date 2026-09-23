package com.espinosa.shinydex.server

import com.google.gson.Gson
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-to-end over HTTP: the same calls the Android app makes. */
class ApplicationTest {

    private val gson = Gson()

    @Test
    fun `a full battle round trip over HTTP`() = testApplication {
        application { faceOffModule(RoomStore()) }

        val created = client.post("/rooms") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"mode":"BATTLE","generation":2,"name":"Gold","pokemonId":155,
                   "pokemonName":"cyndaquil","method":"Soft Reset","oddsDenominator":8192}""",
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val host = gson.fromJson(created.bodyAsText(), JoinResponse::class.java)

        val joined = client.post("/rooms/${host.room.code}/join") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Silver"}""")
        }
        val guest = gson.fromJson(joined.bodyAsText(), JoinResponse::class.java)

        val bumped = client.post("/rooms/${host.room.code}/encounters") {
            header("X-Player-Token", guest.token)
            contentType(ContentType.Application.Json)
            setBody("""{"delta":3}""")
        }
        assertEquals(HttpStatusCode.OK, bumped.status)

        val won = client.post("/rooms/${host.room.code}/found") { header("X-Player-Token", host.token) }
        val room = gson.fromJson(won.bodyAsText(), RoomView::class.java)
        assertEquals(RoomStatus.FINISHED, room.status)
        assertEquals("Gold", room.winnerName)
        assertEquals(3, room.totalEncounters)
    }

    @Test
    fun `mutations without a token are refused`() = testApplication {
        application { faceOffModule(RoomStore()) }
        val created = client.post("/rooms") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"mode":"LOUNGE","generation":3,"name":"Ruby","pokemonId":255,
                   "pokemonName":"torchic","method":"Soft Reset","oddsDenominator":8192}""",
            )
        }
        val host = gson.fromJson(created.bodyAsText(), JoinResponse::class.java)

        val response = client.post("/rooms/${host.room.code}/encounters") {
            contentType(ContentType.Application.Json)
            setBody("""{"delta":1}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("error"))
    }

    @Test
    fun `malformed JSON is a clean 400 with no internals`() = testApplication {
        application { faceOffModule(RoomStore()) }
        val response = client.post("/rooms") {
            contentType(ContentType.Application.Json)
            setBody("{ this is not json")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertFalse(body.contains("Exception"))
        assertFalse(body.contains("at com."))
    }

    @Test
    fun `health check answers`() = testApplication {
        application { faceOffModule(RoomStore()) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ok"))
    }
}

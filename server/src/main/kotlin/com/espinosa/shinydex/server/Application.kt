package com.espinosa.shinydex.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080
private const val TOKEN_HEADER = "X-Player-Token"

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    println("ShinyDex face-off server listening on http://0.0.0.0:$port")
    embeddedServer(CIO, port = port, host = "0.0.0.0") {
        faceOffModule(RoomStore())
    }.start(wait = true)
}

/**
 * HTTP surface of the face-off service. Every rule is in [RoomStore]; this layer only
 * parses requests, pulls the player token from its header and maps errors to status codes.
 */
fun Application.faceOffModule(store: RoomStore) {
    install(ContentNegotiation) { gson() }

    install(StatusPages) {
        exception<FaceOffException> { call, cause ->
            call.respond(HttpStatusCode.fromValue(cause.status), ErrorResponse(cause.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Malformed request."))
        }
        // Never leak stack traces or internals to clients.
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Something went wrong."))
        }
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "rooms" to store.roomCount()))
        }

        route("/rooms") {
            post {
                val body = call.receiveOrEmpty(CreateRoomRequest())
                call.respond(HttpStatusCode.Created, store.create(body))
            }

            route("/{code}") {
                get {
                    call.respond(store.get(call.parameters["code"], call.token()))
                }
                post("/join") {
                    val body = call.receiveOrEmpty(JoinRoomRequest())
                    call.respond(HttpStatusCode.Created, store.join(call.parameters["code"], body))
                }
                post("/encounters") {
                    val body = call.receiveOrEmpty(EncounterRequest())
                    call.respond(store.addEncounters(call.parameters["code"], call.token(), body.delta))
                }
                post("/found") {
                    call.respond(store.markFound(call.parameters["code"], call.token()))
                }
                post("/leave") {
                    store.leave(call.parameters["code"], call.token())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

private fun ApplicationCall.token(): String? = request.headers[TOKEN_HEADER]

/** An empty body means "all defaults"; anything unparsable is a clean 400. */
private suspend inline fun <reified T : Any> ApplicationCall.receiveOrEmpty(empty: T): T {
    val parsed: T? = runCatching { receive<T>() }
        .getOrElse { throw BadRequestException("Malformed request body.", it) }
    return parsed ?: empty
}

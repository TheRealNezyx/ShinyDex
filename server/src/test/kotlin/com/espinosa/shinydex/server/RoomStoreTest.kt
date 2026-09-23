package com.espinosa.shinydex.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RoomStoreTest {

    private var now = 1_000_000L
    private val store = RoomStore(clock = { now })

    private fun battle(name: String = "Ash") = store.create(
        CreateRoomRequest(
            mode = "BATTLE", generation = 3, name = name,
            pokemonId = 258, pokemonName = "mudkip", method = "Soft Reset", oddsDenominator = 8192,
        ),
    )

    private fun lounge(name: String = "Ash") = store.create(
        CreateRoomRequest(
            mode = "LOUNGE", generation = 4, name = name,
            pokemonId = 387, pokemonName = "turtwig", method = "Masuda Method", oddsDenominator = 1638,
        ),
    )

    private fun expectStatus(status: Int, block: () -> Unit) {
        try {
            block()
            fail("expected HTTP $status")
        } catch (e: FaceOffException) {
            assertEquals(e.message, status, e.status)
        }
    }

    @Test
    fun `room codes use the unambiguous alphabet`() {
        val code = battle().room.code
        assertEquals(RoomStore.CODE_LENGTH, code.length)
        assertTrue(code.all { it in RoomStore.CODE_ALPHABET })
    }

    @Test
    fun `codes are normalised and bad codes rejected`() {
        val code = battle().room.code
        assertEquals(code, RoomStore.normalizeCode(code.lowercase().chunked(3).joinToString("-")))
        assertNull(RoomStore.normalizeCode("ABC"))
        assertNull(RoomStore.normalizeCode("ABCDE0"))
    }

    @Test
    fun `battle joiners all hunt the host's target`() {
        val host = battle()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Misty", pokemonId = 300,
            pokemonName = "skitty", method = "x", oddsDenominator = 1))

        val guestView = guest.room.players.first { it.id == guest.playerId }
        assertEquals(258, guestView.pokemonId)
        assertEquals(8192, guestView.oddsDenominator)
    }

    @Test
    fun `the first shiny wins the battle and closes it`() {
        val host = battle()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Misty"))

        val after = store.markFound(host.room.code, guest.token)
        assertEquals(RoomStatus.FINISHED, after.status)
        assertEquals(guest.playerId, after.winnerId)
        assertEquals("Misty", after.winnerName)
        assertEquals(guest.playerId, after.players.first().id)

        expectStatus(409) { store.markFound(host.room.code, host.token) }
        expectStatus(409) { store.addEncounters(host.room.code, host.token, 1) }
    }

    @Test
    fun `nobody can join a finished battle`() {
        val host = battle()
        store.markFound(host.room.code, host.token)
        expectStatus(409) { store.join(host.room.code, JoinRoomRequest(name = "Brock")) }
    }

    @Test
    fun `lounge players each bring their own Pokemon from the room's generation`() {
        val host = lounge()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Dawn", pokemonId = 393,
            pokemonName = "piplup", method = "Poke Radar", oddsDenominator = 200))

        val guestView = guest.room.players.first { it.id == guest.playerId }
        assertEquals(393, guestView.pokemonId)
        assertEquals(200, guestView.oddsDenominator)

        // Mudkip is Gen III, so it cannot be brought into a Gen IV lounge.
        expectStatus(400) {
            store.join(host.room.code, JoinRoomRequest(name = "Max", pokemonId = 258,
                pokemonName = "mudkip", method = "x", oddsDenominator = 8192))
        }
    }

    @Test
    fun `the lounge stays open until everyone has found theirs`() {
        val host = lounge()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Dawn", pokemonId = 393,
            pokemonName = "piplup", method = "Poke Radar", oddsDenominator = 200))

        assertEquals(RoomStatus.OPEN, store.markFound(host.room.code, host.token).status)
        // The host is done, the guest is still hunting.
        expectStatus(409) { store.addEncounters(host.room.code, host.token, 1) }
        assertEquals(1, store.addEncounters(host.room.code, guest.token, 1).totalEncounters)

        assertEquals(RoomStatus.FINISHED, store.markFound(host.room.code, guest.token).status)
    }

    @Test
    fun `a player can only change their own counter`() {
        val host = battle()
        store.join(host.room.code, JoinRoomRequest(name = "Misty"))

        expectStatus(403) { store.addEncounters(host.room.code, "not-a-real-token", 1) }
        expectStatus(403) { store.addEncounters(host.room.code, null, 1) }

        val view = store.addEncounters(host.room.code, host.token, 5)
        assertEquals(5, view.players.first { it.id == host.playerId }.encounters)
        assertEquals(0, view.players.first { it.id != host.playerId }.encounters)
    }

    @Test
    fun `encounter steps are bounded and the counter never goes negative`() {
        val host = battle()
        expectStatus(400) { store.addEncounters(host.room.code, host.token, 0) }
        expectStatus(400) { store.addEncounters(host.room.code, host.token, 11) }
        expectStatus(400) { store.addEncounters(host.room.code, host.token, null) }

        val view = store.addEncounters(host.room.code, host.token, -10)
        assertEquals(0, view.players.single().encounters)
    }

    @Test
    fun `names are sanitised and must be unique in a room`() {
        val host = battle(name = "  <b>Ash</b>  ")
        assertEquals("bAshb", host.room.players.single().name)

        expectStatus(400) { store.join(host.room.code, JoinRoomRequest(name = "<>!!")) }
        expectStatus(409) { store.join(host.room.code, JoinRoomRequest(name = "bashb")) }
    }

    @Test
    fun `rooms cap at eight players`() {
        val host = battle()
        repeat(RoomStore.MAX_PLAYERS - 1) { store.join(host.room.code, JoinRoomRequest(name = "P$it")) }
        expectStatus(409) { store.join(host.room.code, JoinRoomRequest(name = "Late")) }
    }

    @Test
    fun `views never contain tokens`() {
        val host = battle()
        val serialized = com.google.gson.Gson().toJson(store.get(host.room.code, null))
        assertFalse(serialized.contains(host.token))
    }

    @Test
    fun `players drop offline after going quiet`() {
        val host = battle()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Misty"))

        now += 30_000
        store.get(host.room.code, host.token)
        val view = store.get(host.room.code, null)
        assertTrue(view.players.first { it.id == host.playerId }.online)
        assertFalse(view.players.first { it.id == guest.playerId }.online)
    }

    @Test
    fun `host leaving hands the room to the next player and the last one closes it`() {
        val host = battle()
        val guest = store.join(host.room.code, JoinRoomRequest(name = "Misty"))

        store.leave(host.room.code, host.token)
        assertEquals(guest.playerId, store.get(host.room.code, null).hostId)

        store.leave(host.room.code, guest.token)
        assertEquals(0, store.roomCount())
    }

    @Test
    fun `idle rooms expire`() {
        battle()
        now += 13 * 60 * 60 * 1000L
        lounge()
        assertEquals(1, store.roomCount())
    }

    @Test
    fun `invalid creation input is rejected`() {
        expectStatus(400) { store.create(CreateRoomRequest(mode = "RAID", generation = 3, name = "A")) }
        expectStatus(400) { store.create(CreateRoomRequest(mode = "BATTLE", generation = 1, name = "A")) }
        expectStatus(400) {
            store.create(CreateRoomRequest(mode = "BATTLE", generation = 2, name = "A",
                pokemonId = 25, pokemonName = "pikachu", method = "m", oddsDenominator = 8192))
        }
        expectStatus(400) {
            store.create(CreateRoomRequest(mode = "BATTLE", generation = 2, name = "A",
                pokemonId = 152, pokemonName = "Chikorita!", method = "m", oddsDenominator = 8192))
        }
        expectStatus(400) {
            store.create(CreateRoomRequest(mode = "BATTLE", generation = 2, name = "A",
                pokemonId = 152, pokemonName = "chikorita", method = "m", oddsDenominator = 0))
        }
        expectStatus(404) { store.get("ABCDEF", null) }
        assertNotNull(battle())
    }
}

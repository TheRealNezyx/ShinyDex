package com.espinosa.shinydex

import com.espinosa.shinydex.data.model.FaceOffMode
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.toDomain
import com.espinosa.shinydex.data.remote.faceoff.PlayerDto
import com.espinosa.shinydex.data.remote.faceoff.RoomDto
import com.espinosa.shinydex.data.remote.faceoff.TargetDto
import com.espinosa.shinydex.data.repo.FaceOffRepository
import com.espinosa.shinydex.util.FaceOffCodes
import com.espinosa.shinydex.util.Odds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceOffTest {

    @Test
    fun `room codes are normalised the same way the server does`() {
        assertEquals("ABC234", FaceOffCodes.normalize(" abc-234 "))
        assertNull(FaceOffCodes.normalize("ABC23"))
        assertNull(FaceOffCodes.normalize("ABCD10")) // 1 and 0 are never used
    }

    @Test
    fun `the join field only keeps code characters`() {
        assertEquals("ABC234", FaceOffCodes.sanitizeInput("ab c-2 34xyz"))
        assertEquals("", FaceOffCodes.sanitizeInput("0O1I!"))
    }

    @Test
    fun `invite links round trip and foreign links are ignored`() {
        val link = FaceOffCodes.inviteLink("QWE789")
        assertEquals("shinydex://join/QWE789", link)
        assertEquals("QWE789", FaceOffCodes.codeFromLink(link))
        assertEquals("QWE789", FaceOffCodes.codeFromLink("shinydex://join/qwe789?ref=chat"))
        assertNull(FaceOffCodes.codeFromLink("https://evil.example/join/QWE789"))
        assertNull(FaceOffCodes.codeFromLink("shinydex://join/../../etc"))
        assertNull(FaceOffCodes.codeFromLink(null))
    }

    @Test
    fun `server addresses get a scheme and trailing slash`() {
        assertEquals("http://10.0.2.2:8080/", FaceOffRepository.normalizeServer("10.0.2.2:8080"))
        assertEquals("https://hunt.example.com/", FaceOffRepository.normalizeServer("https://hunt.example.com"))
        assertEquals("http://10.0.2.2:8080/", FaceOffRepository.normalizeServer("   "))
    }

    @Test
    fun `battle probability pools every player's encounters`() {
        val room = battleRoom(encounters = listOf(3000, 2000, 3192)).toDomain()!!
        assertEquals(8192, room.totalEncounters)
        // Three players at 8192 combined is exactly one full-odds cycle, about 63%.
        assertEquals(Odds.cumulativeProbabilityLabel(8192, 8192), room.roomProbabilityLabel)
        assertEquals("63%", room.roomProbabilityLabel)
    }

    @Test
    fun `server rooms map onto the app's own types`() {
        val room = battleRoom(encounters = listOf(10)).toDomain()!!
        assertEquals(FaceOffMode.BATTLE, room.mode)
        assertEquals(Generation.GEN_III, room.generation)
        assertEquals("Mudkip", room.target?.displayName)
        assertTrue(room.target!!.shinySpriteUrl.contains("generation-iii"))
    }

    @Test
    fun `rooms the app does not understand are rejected`() {
        assertNull(battleRoom(listOf(0)).copy(mode = "RAID").toDomain())
        assertNull(battleRoom(listOf(0)).copy(generation = 9).toDomain())
    }

    private fun battleRoom(encounters: List<Int>) = RoomDto(
        code = "ABC234",
        mode = "BATTLE",
        generation = 3,
        status = "OPEN",
        hostId = "p0",
        target = TargetDto(258, "mudkip", "Soft Reset", 8192),
        players = encounters.mapIndexed { i, n ->
            PlayerDto(id = "p$i", name = "P$i", pokemonId = 258, pokemonName = "mudkip",
                method = "Soft Reset", oddsDenominator = 8192, encounters = n)
        },
        totalEncounters = encounters.sum(),
        maxPlayers = 8,
    )
}

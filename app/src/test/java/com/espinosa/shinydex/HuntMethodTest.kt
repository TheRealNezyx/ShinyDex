package com.espinosa.shinydex

import com.espinosa.shinydex.data.model.FULL_ODDS
import com.espinosa.shinydex.data.model.GameVersion
import com.espinosa.shinydex.data.model.Generation
import com.espinosa.shinydex.data.model.HuntMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HuntMethodTest {

    @Test
    fun `the odd egg is Crystal only`() {
        assertFalse(HuntMethod.G2_ODD_EGG.isAvailableIn(GameVersion.GOLD))
        assertFalse(HuntMethod.G2_ODD_EGG.isAvailableIn(GameVersion.SILVER))
        assertTrue(HuntMethod.G2_ODD_EGG.isAvailableIn(GameVersion.CRYSTAL))
    }

    @Test
    fun `a method never leaks into another generation`() {
        HuntMethod.entries.forEach { method ->
            GameVersion.entries.filter { it.generation != method.generation }.forEach { game ->
                assertFalse(
                    "${method.name} should not be offered in ${game.name}",
                    method.isAvailableIn(game),
                )
            }
        }
    }

    @Test
    fun `every game can hunt at full odds`() {
        GameVersion.entries.forEach { game ->
            val methods = HuntMethod.availableFor(game)
            assertTrue(
                "${game.name} has no method",
                methods.isNotEmpty(),
            )
            assertTrue(
                "${game.name} has no full odds method",
                methods.any { it.oddsDenominator == FULL_ODDS },
            )
        }
    }

    @Test
    fun `Gen III has nothing that beats full odds`() {
        val best = Generation.GEN_III.methods.minOf { it.oddsDenominator }
        assertEquals(FULL_ODDS, best)
    }

    @Test
    fun `Gen II breeding and Gen IV Masuda beat full odds`() {
        assertTrue(HuntMethod.G2_BREEDING.oddsDenominator < FULL_ODDS)
        assertTrue(HuntMethod.G4_MASUDA.oddsDenominator < FULL_ODDS)
        assertTrue(HuntMethod.G4_POKE_RADAR.oddsDenominator < HuntMethod.G4_MASUDA.oddsDenominator)
    }

    @Test
    fun `the default method is always one the game can use`() {
        GameVersion.entries.forEach { game ->
            assertTrue(
                "default for ${game.name} is not available there",
                HuntMethod.defaultFor(game).isAvailableIn(game),
            )
        }
    }

    @Test
    fun `unknown names fall back to a safe default`() {
        assertEquals(HuntMethod.G2_RANDOM_ENCOUNTER, HuntMethod.fromNameOrDefault("NOT_A_METHOD"))
        assertEquals(Generation.GEN_II, Generation.fromNameOrDefault("GEN_IX"))
        assertEquals(null, GameVersion.fromNameOrNull("NOT_A_GAME"))
    }
}

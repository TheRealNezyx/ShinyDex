package com.espinosa.shinydex

import com.espinosa.shinydex.util.Odds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OddsTest {

    @Test
    fun `zero encounters means zero chance`() {
        assertEquals(0.0, Odds.cumulativeProbability(0, FULL_ODDS), TOLERANCE)
    }

    @Test
    fun `one full odds cycle is about sixty three percent`() {
        val probability = Odds.cumulativeProbability(FULL_ODDS, FULL_ODDS)
        assertEquals(0.6321, probability, 0.001)
    }

    @Test
    fun `probability rises monotonically`() {
        val early = Odds.cumulativeProbability(100, FULL_ODDS)
        val later = Odds.cumulativeProbability(200, FULL_ODDS)
        assertTrue(later > early)
    }

    @Test
    fun `guaranteed methods are always certain`() {
        assertEquals(1.0, Odds.cumulativeProbability(1, 1), TOLERANCE)
    }

    @Test
    fun `progress is clamped to one`() {
        assertEquals(1f, Odds.progressTowardsOdds(FULL_ODDS * 2, FULL_ODDS), 0.0001f)
    }

    @Test
    fun `labels are formatted for the counter`() {
        assertEquals("1.2%", Odds.cumulativeProbabilityLabel(100, FULL_ODDS))
        assertEquals("63%", Odds.cumulativeProbabilityLabel(FULL_ODDS, FULL_ODDS))
    }

    private companion object {
        const val FULL_ODDS = 8192
        const val TOLERANCE = 0.0001
    }
}

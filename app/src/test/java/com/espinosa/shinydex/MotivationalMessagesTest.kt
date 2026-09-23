package com.espinosa.shinydex

import com.espinosa.shinydex.util.MotivationalMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotivationalMessagesTest {

    @Test
    fun `every hundredth encounter is a milestone`() {
        assertTrue(MotivationalMessages.isMilestone(100))
        assertTrue(MotivationalMessages.isMilestone(2500))
    }

    @Test
    fun `other counts are not milestones`() {
        assertFalse(MotivationalMessages.isMilestone(0))
        assertFalse(MotivationalMessages.isMilestone(99))
        assertFalse(MotivationalMessages.isMilestone(101))
    }

    @Test
    fun `milestones always produce a non-empty message`() {
        var count = MotivationalMessages.MILESTONE_INTERVAL
        while (count <= UPPER_BOUND) {
            assertTrue(
                "no message for $count",
                MotivationalMessages.forCount(count).isNotEmpty(),
            )
            count += MotivationalMessages.MILESTONE_INTERVAL
        }
    }

    @Test
    fun `full odds gets its own message`() {
        assertTrue(MotivationalMessages.forCount(FULL_ODDS_ROUNDED).contains("8,192"))
    }

    @Test
    fun `the same milestone always gives the same message`() {
        assertEquals(
            MotivationalMessages.forCount(1300),
            MotivationalMessages.forCount(1300),
        )
    }

    private companion object {
        const val UPPER_BOUND = 25_000
        const val FULL_ODDS_ROUNDED = 8192
    }
}

package com.espinosa.shinydex.util

/**
 * Encouragement shown to the trainer every [MILESTONE_INTERVAL] encounters.
 *
 * Round numbers that mean something to a Gen II hunter (1000, 4096, 8192, ...) get their
 * own line; every other milestone draws from the general pool, seeded by the count so the
 * same milestone always shows the same message.
 */
object MotivationalMessages {

    const val MILESTONE_INTERVAL = 100

    /**
     * True when [count] lands exactly on a milestone worth celebrating: every hundredth
     * encounter, plus the odds-flavoured numbers such as 4096 and 8192 that do not divide
     * evenly by a hundred.
     */
    fun isMilestone(count: Int): Boolean =
        count > 0 && (count % MILESTONE_INTERVAL == 0 || count in SPECIAL)

    /** The message for [count]; call only when [isMilestone] is true. */
    fun forCount(count: Int): String =
        SPECIAL[count] ?: GENERAL[(count / MILESTONE_INTERVAL) % GENERAL.size]

    private val SPECIAL: Map<Int, String> = mapOf(
        100 to "100 encounters. The hunt is officially real now.",
        500 to "500 down. Most people quit before here. You did not.",
        1000 to "1,000 encounters! That is 12% of the way to full odds. Breathe.",
        2000 to "2,000. Your thumb has developed a personality of its own.",
        4096 to "4,096 -- the halfway mark of full odds. Statistically, this is the coin flip.",
        6000 to "6,000. Every encounter from here is one you will tell people about.",
        8192 to "8,192 ENCOUNTERS. You have hit full odds exactly. The odds owe you nothing, " +
            "but the story is already legendary.",
        10000 to "10,000 encounters. Five digits. You are not hunting a Pokemon anymore, " +
            "you are proving a point.",
        15000 to "15,000. At this point the Pokemon is hunting YOU.",
        20000 to "20,000 encounters. Genuinely: take a break, drink water, come back. " +
            "It will still be here.",
    )

    private val GENERAL: List<String> = listOf(
        "Every reset is a fresh 1-in-8192. The counter does not remember, but you do.",
        "Shiny odds have no memory. Your patience does.",
        "The sparkle is coming. It does not know when either.",
        "Keep going. Nobody ever found a shiny by closing the game.",
        "One more encounter. That is the whole strategy.",
        "Gen II hunters had no shiny charm, no chains, no mercy. Just like you.",
        "Somewhere in that grass is a Pokemon with the wrong palette and your name on it.",
        "Progress is not the counter going up. Progress is you still being here.",
        "Slow is fine. Stopped is the only failure.",
        "Stretch your hands. Then find that shiny.",
        "The best hunters are just the ones who did not stop.",
        "Statistically unremarkable. Personally heroic.",
        "This is the boring part. The boring part is where shinies live.",
        "You are closer than you were a hundred encounters ago. That is real.",
        "Trust the grind. The RNG has to blink eventually.",
    )
}

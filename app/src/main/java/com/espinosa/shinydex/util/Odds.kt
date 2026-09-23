package com.espinosa.shinydex.util

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/** Probability helpers for a binomial "at least one success in n tries" shiny hunt. */
object Odds {

    /** Chance of having seen at least one shiny after [encounters] tries at 1-in-[denominator]. */
    fun cumulativeProbability(encounters: Int, denominator: Int): Double = when {
        denominator <= 1 -> 1.0
        encounters <= 0 -> 0.0
        else -> {
            val perTryFailure = 1.0 - (1.0 / denominator)
            1.0 - perTryFailure.pow(encounters.toDouble())
        }
    }

    /** The same figure formatted for the UI, e.g. `23.4%`. */
    fun cumulativeProbabilityLabel(encounters: Int, denominator: Int): String {
        val percent = cumulativeProbability(encounters, denominator) * PERCENT
        return when {
            percent >= WHOLE_NUMBER_THRESHOLD -> "${percent.roundToInt()}%"
            percent >= ONE_DECIMAL_THRESHOLD -> String.format(Locale.US, "%.1f%%", percent)
            else -> String.format(Locale.US, "%.2f%%", percent)
        }
    }

    /** How far through a single "expected" run of [denominator] encounters the hunt is, 0f..1f. */
    fun progressTowardsOdds(encounters: Int, denominator: Int): Float {
        if (denominator <= 0) return 1f
        return (encounters.toFloat() / denominator.toFloat()).coerceIn(0f, 1f)
    }

    private const val PERCENT = 100.0
    private const val WHOLE_NUMBER_THRESHOLD = 10.0
    private const val ONE_DECIMAL_THRESHOLD = 1.0
}

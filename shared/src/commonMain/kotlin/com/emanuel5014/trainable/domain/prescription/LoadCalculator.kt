package com.emanuel5014.trainable.domain.prescription

import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlin.math.floor

object LoadCalculator {
    const val DEFAULT_INCREMENT_KG = 2.5f
    const val DEFAULT_INCREMENT_LB = 5f
    val INCREMENTS_KG = listOf(1f, 1.25f, 2.5f, 5f)
    val INCREMENTS_LB = listOf(2.5f, 5f, 10f)

    fun defaultIncrement(unit: String): Float = if (unit == "lb") DEFAULT_INCREMENT_LB else DEFAULT_INCREMENT_KG

    /** Rounds half-up to the nearest [increment] (e.g. 149.5 → 150 with 2.5). */
    fun roundTo(value: Float, increment: Float): Float {
        if (increment <= 0f) return value
        val steps = floor(value.toDouble() / increment + 0.5 + 1e-9)
        return (steps * increment).toFloat()
    }

    /**
     * Weight in kg for [percent] of [oneRepMaxKg], rounded in the user's display [unit] so that
     * the loaded bar always lands on a real plate increment.
     */
    fun weightForPercent(oneRepMaxKg: Float, percent: Float, unit: String, increment: Float): Float {
        val display = WeightUnitConverter.convertDisplay(oneRepMaxKg, unit) * percent / 100f
        val rounded = roundTo(display, increment)
        return WeightUnitConverter.convertStorage(rounded, unit)
    }

    /** Planned weight for a set, or null when it must come from history (RPE / none). */
    fun targetWeightKg(set: PlannedSet, oneRepMaxKg: Float?, unit: String, increment: Float): Float? =
        when (set.intensityType) {
            IntensityType.PERCENT -> {
                val percent = set.intensityValue
                if (percent != null && oneRepMaxKg != null && oneRepMaxKg > 0f) {
                    weightForPercent(oneRepMaxKg, percent, unit, increment)
                } else null
            }
            IntensityType.WEIGHT -> set.intensityValue
            IntensityType.BODYWEIGHT -> set.intensityValue ?: 0f
            IntensityType.RPE, IntensityType.NONE -> null
        }

    /** Epley estimated 1RM. A single is its own 1RM. */
    fun epley(weight: Float, reps: Int): Float = when {
        reps <= 0 || weight <= 0f -> 0f
        reps == 1 -> weight
        else -> weight * (1f + reps / 30f)
    }

    fun percentOf(weightKg: Float, oneRepMaxKg: Float?): Float? =
        if (oneRepMaxKg == null || oneRepMaxKg <= 0f) null else weightKg / oneRepMaxKg * 100f

    /** Percentage table (60–100% by default) like the one at the bottom of a PL notebook page. */
    fun percentTable(oneRepMaxKg: Float, unit: String, increment: Float, percents: List<Int> = (60..100 step 5).toList()): List<Pair<Int, Float>> =
        percents.map { it to weightForPercent(oneRepMaxKg, it.toFloat(), unit, increment) }
}

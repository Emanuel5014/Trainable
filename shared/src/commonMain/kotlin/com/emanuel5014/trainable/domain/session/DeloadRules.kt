package com.emanuel5014.trainable.domain.session

import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * A deload workout (workout_sessions.is_deload) stays in the history but is left out of statistics, records and
 * charts, and the next workout starts from the last normal one. Whether it counts as a day trained (weekly goal,
 * widgets, training days) is the user's setting, on by default.
 */
object DeloadRules {
    /** The weight a deload workout proposes, as a share of the last normal workout's. */
    const val DEFAULT_PERCENT = 60
    const val MIN_PERCENT = 40
    const val MAX_PERCENT = 100
    const val PERCENT_STEP = 5

    fun clampPercent(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    /**
     * [percent] of [weightKg], rounded in the display [unit] to the plate [increment] so the bar can be loaded.
     * 0 stays 0 (bodyweight), and 100% gives the weight back untouched.
     */
    fun scaledWeightKg(weightKg: Float, percent: Int, unit: String, increment: Float): Float {
        if (weightKg <= 0f || percent >= MAX_PERCENT) return weightKg
        val display = WeightUnitConverter.convertDisplay(weightKg, unit) * clampPercent(percent) / 100f
        val rounded = LoadCalculator.roundTo(display, increment).coerceAtLeast(0f)
        return WeightUnitConverter.convertStorage(rounded, unit)
    }

    /** The workouts that count as days trained this week (or any span), with the setting [deloadCounts]. */
    fun countsAsTrained(session: WorkoutSessionEntity, deloadCounts: Boolean): Boolean = deloadCounts || !session.isDeload
}

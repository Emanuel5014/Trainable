package com.emanuel5014.trainable.domain.prescription

/**
 * The weight a set starts from when the routine has no workout to copy it from yet (the first sessions of a
 * new routine). Only with the "starting weight" setting on: the weight set on the exercise wins, then the
 * last weight used for that exercise in any routine, then nothing.
 */
object StartingWeight {
    fun resolve(enabled: Boolean, presetKg: Float?, lastUsedKg: Float?): Float {
        if (!enabled) return 0f
        return presetKg?.takeIf { it > 0f } ?: lastUsedKg?.takeIf { it > 0f } ?: 0f
    }

    /** A typed weight ("82,5" or "82.5") in the user's unit, or null when it is blank, not a number or not above zero. */
    fun parse(text: String): Float? =
        text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it > 0f && it.isFinite() && it <= MAX_DISPLAY_WEIGHT }

    /** Heaviest weight the field accepts, in the unit typed. */
    const val MAX_DISPLAY_WEIGHT = 1000f
}

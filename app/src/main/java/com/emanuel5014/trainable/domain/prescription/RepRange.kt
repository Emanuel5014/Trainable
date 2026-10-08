package com.emanuel5014.trainable.domain.prescription

/**
 * A rep range such as "10–15": every set starts at [min] and the lifter works up towards [max] with the same
 * weight, adding load only once [max] is reached on every set (double progression).
 *
 * Written with an en dash on purpose: the hyphen is already taken by the per-set list ("8-6-4-2").
 */
data class RepRange(val min: Int, val max: Int) {
    val text: String get() = "$min$SEPARATOR$max"

    companion object {
        const val SEPARATOR = '–'
        const val MAX_REPS = 100

        /** The range of a plan exercise, or null when [min] / [max] do not describe one (fixed reps, per-set list). */
        fun of(min: Int?, max: Int?): RepRange? =
            if (min != null && max != null && min >= 1 && max > min && max <= MAX_REPS) RepRange(min, max) else null
    }
}

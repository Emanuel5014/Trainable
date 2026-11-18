package com.emanuel5014.trainable.util

import kotlin.math.abs

/** The steps of the rest and warmup sliders, and what a remembered value becomes when it is not one of them. */
object RestSeconds {
    const val DEFAULT = 120

    /** The rest of an exercise: 0 means no rest after its sets. */
    val EXERCISE_STEPS = listOf(0, 30, 60, 90, 120, 180, 240, 300)

    /** A warmup of 0 seconds is no warmup, so its slider starts from the shortest one. */
    val WARMUP_STEPS = EXERCISE_STEPS.filter { it > 0 }

    /** The step nearest to [seconds] (the shorter one when halfway), or the default when nothing is stored. */
    fun snap(seconds: Int?, steps: List<Int>): Int {
        if (seconds == null) return DEFAULT
        return steps.minByOrNull { abs(it - seconds) } ?: DEFAULT
    }
}

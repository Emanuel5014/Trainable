package com.emanuel5014.trainable.domain.prescription

/**
 * A Time & Reps exercise is prescribed as sets of a timed interval with a reps goal ("3 × 45s · 20 reps"): the time is
 * fixed by the routine, the reps are what the sets are measured by. The plan stores the goal in `repsTarget` and the
 * interval in `durataTargetSecondi`.
 */
object TimeAndReps {
    const val MAX_REPS = 999
    const val DEFAULT_SECONDS = 45
    const val DEFAULT_REPS_GOAL = 10

    /** The reps goal typed in an editor, null unless it is a whole number from 1 to [MAX_REPS]. */
    fun parseGoal(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..MAX_REPS }

    /** The goal stored on a plan exercise; older or hand-made rows without a number fall back to [DEFAULT_REPS_GOAL]. */
    fun goalOf(repsTarget: String?): Int = repsTarget?.trim()?.toIntOrNull()?.takeIf { it in 1..MAX_REPS } ?: DEFAULT_REPS_GOAL
}

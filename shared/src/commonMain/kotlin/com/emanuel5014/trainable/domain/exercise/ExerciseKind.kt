package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.isTimeAndReps

/** How an exercise is measured, which decides what is compared and how its sets are written. */
enum class ExerciseKind {
    /** Weight × reps. */
    STRENGTH,

    /** Reps with no added load (pull-ups, push-ups). */
    BODYWEIGHT,

    /** Time & Weight: how long a set was held, with an optional load (planks, carries). */
    TIMED,

    /** Time & Reps: the reps done in a timed interval, no load (burpees for 60 s). */
    TIME_AND_REPS;

    companion object {
        /** The kind of an exercise from the sets logged for it: Time & Reps beats a hold, a hold beats a load. */
        fun of(sets: List<SetLogEntity>): ExerciseKind = when {
            sets.any { it.isTimeAndReps } -> TIME_AND_REPS
            sets.any { it.durataSecondi != null } -> TIMED
            sets.any { it.pesoSollevato > 0f } -> STRENGTH
            else -> BODYWEIGHT
        }
    }
}

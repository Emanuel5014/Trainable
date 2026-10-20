package com.emanuel5014.trainable.data.local.entity

/**
 * `exercise_type` of a Time & Reps exercise (plan exercises and set rows): a timed interval and the reps done in it,
 * with no weight (Burpees for 60 s, Battle Ropes intervals).
 */
const val TIME_AND_REPS = "time_and_reps"

/** `exercise_type` of a Time & Weight exercise: a hold timed against a target, with an optional weight. */
const val TIME_AND_WEIGHT = "time_and_weight"

// The enum below has entries with the same names
private const val TIME_AND_REPS_TYPE = TIME_AND_REPS
private const val TIME_AND_WEIGHT_TYPE = TIME_AND_WEIGHT

/** How a logged set is measured, which decides how it is written and edited. */
enum class SetLogKind {
    /** Weight × reps. */
    STRENGTH,

    /** A timed hold with an optional weight; its reps mean nothing. */
    TIME_AND_WEIGHT,

    /** A timed interval and the reps done in it; its weight is always 0. */
    TIME_AND_REPS;

    val isTimed: Boolean get() = this != STRENGTH

    /** The `exercise_type` a row of this kind stores (only Time & Reps needs one). */
    val rowType: String? get() = if (this == TIME_AND_REPS) TIME_AND_REPS_TYPE else null

    companion object {
        /** The kind of an exercise from its `exercise_type` ("strength", "time_and_weight", ...). */
        fun ofExerciseType(type: String?): SetLogKind = when (type) {
            TIME_AND_REPS_TYPE -> TIME_AND_REPS
            TIME_AND_WEIGHT_TYPE -> TIME_AND_WEIGHT
            else -> STRENGTH
        }
    }
}

/** Rows written before Time & Reps existed carry no type: a duration then means Time & Weight. */
val SetLogEntity.kind: SetLogKind
    get() = when {
        exerciseType == TIME_AND_REPS -> SetLogKind.TIME_AND_REPS
        durataSecondi != null -> SetLogKind.TIME_AND_WEIGHT
        else -> SetLogKind.STRENGTH
    }

val SetLogEntity.isTimeAndReps: Boolean get() = kind == SetLogKind.TIME_AND_REPS

/** The kind of an exercise of a session from its rows: one Time & Reps row makes the whole exercise so. */
val List<SetLogEntity>.kind: SetLogKind
    get() = when {
        any { it.isTimeAndReps } -> SetLogKind.TIME_AND_REPS
        any { it.durataSecondi != null } -> SetLogKind.TIME_AND_WEIGHT
        else -> SetLogKind.STRENGTH
    }

/**
 * Turns the row into a set of [kind]: a timed one keeps its duration (or takes [defaultSeconds]), a Time & Reps one
 * loses its weight, a plain one its duration. Its RPE stays in every kind.
 */
fun SetLogEntity.asKind(kind: SetLogKind, defaultSeconds: Int = 45): SetLogEntity = when (kind) {
    SetLogKind.STRENGTH -> copy(durataSecondi = null, exerciseType = null)
    SetLogKind.TIME_AND_WEIGHT -> copy(durataSecondi = durataSecondi ?: defaultSeconds, exerciseType = null)
    SetLogKind.TIME_AND_REPS -> copy(durataSecondi = durataSecondi ?: defaultSeconds, exerciseType = TIME_AND_REPS, pesoSollevato = 0f)
}

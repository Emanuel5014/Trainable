package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.ExerciseLastSet
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.session.WorkoutTitle

/**
 * What an exercise added to a quick workout with one tap starts with: the sets, reps, rest and type of the last time it
 * was done (in any workout), or 3 × 8 when it never was. The weights come from the same sets when the rows are made.
 */
data class QuickAddPlan(
    val sets: Int,
    /** "8", or one value per set ("10-8-6") when they differed. */
    val repsTarget: String,
    val restSeconds: Int?,
    val exerciseType: String,
    /** Target seconds of a timed set; null for weight × reps. */
    val seconds: Int?
) {
    companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = "8"
        private const val MAX_SETS = 10

        /**
         * [lastSets]: the sets of the exercise in its last finished workout. [fallbackType] (the type it was last added
         * with) and [fallbackRest] (the rest last confirmed in the picker) stand in for what those sets cannot tell.
         */
        fun from(lastSets: List<SetLogEntity>, fallbackType: String, fallbackRest: Int?): QuickAddPlan {
            val working = workingSets(lastSets)
            if (working.isEmpty()) return QuickAddPlan(DEFAULT_SETS, DEFAULT_REPS, fallbackRest, fallbackType, null)
            val kind = working.kind
            val reps = working.map { it.repsEffettive.coerceAtLeast(0) }
            return QuickAddPlan(
                sets = working.size.coerceIn(1, MAX_SETS),
                repsTarget = repsText(reps.take(MAX_SETS)),
                restSeconds = working.firstNotNullOfOrNull { it.restTimerSeconds } ?: fallbackRest,
                exerciseType = typeOf(kind),
                seconds = if (kind.isTimed) working.firstNotNullOfOrNull { it.durataSecondi } else null
            )
        }

        /** The sets that count: the done ones that are not warm-ups (all of the non-warm-ups when none is marked done). */
        fun workingSets(sets: List<SetLogEntity>): List<SetLogEntity> {
            val notWarmup = sets.filterNot { it.isWarmup }.sortedBy { it.numeroSerie }
            return notWarmup.filter { it.isCompleted }.ifEmpty { notWarmup }
        }

        fun typeOf(kind: SetLogKind): String = when (kind) {
            SetLogKind.TIME_AND_REPS -> TIME_AND_REPS
            SetLogKind.TIME_AND_WEIGHT -> TIME_AND_WEIGHT
            SetLogKind.STRENGTH -> "strength"
        }

        /** "8" when every set had the same reps, else "10-8-6" (the format the rows are built from). */
        fun repsText(reps: List<Int>): String =
            if (reps.isEmpty()) DEFAULT_REPS else if (reps.distinct().size == 1) reps.first().toString() else reps.joinToString("-")
    }
}

/**
 * The last time an exercise was done: its best set (the note under it in the quick workout picker) and how many sets,
 * reps and rest it had (where the ⚙ of the picker opens).
 */
data class ExerciseLastTime(
    val weightKg: Float,
    val reps: Int,
    /** Seconds of a timed set (then the note shows them instead of the reps). */
    val seconds: Int?,
    val timestamp: Long,
    val sets: Int = 1,
    /** "8" or "10-8-6", see [QuickAddPlan.repsText]. */
    val repsTarget: String = QuickAddPlan.DEFAULT_REPS,
    val restSeconds: Int? = null
) {
    companion object {
        /** Exercise id -> its best set (the heaviest, then the most reps or the longest). */
        fun of(rows: List<ExerciseLastSet>): Map<Int, ExerciseLastTime> = rows.groupBy { it.exerciseId }.mapValues { (_, sets) ->
            val timed = sets.any { it.exerciseType == TIME_AND_REPS || it.seconds != null }
            val best = if (timed) {
                sets.maxWith(compareBy<ExerciseLastSet> { it.weightKg }.thenBy { it.seconds ?: 0 }.thenBy { it.reps })
            } else {
                sets.maxWith(compareBy<ExerciseLastSet> { it.weightKg }.thenBy { it.reps })
            }
            // Two workouts with the same start would both be here: the sets of one of them
            val lastWorkout = sets.filter { it.timestamp == sets.maxOf { s -> s.timestamp } }.distinctBy { it.setNumber }.sortedBy { it.setNumber }
            ExerciseLastTime(
                weightKg = best.weightKg,
                reps = best.reps,
                seconds = best.seconds.takeIf { timed },
                timestamp = sets.maxOf { it.timestamp },
                sets = lastWorkout.size,
                repsTarget = QuickAddPlan.repsText(lastWorkout.map { it.reps }),
                restSeconds = lastWorkout.firstNotNullOfOrNull { it.restSeconds }
            )
        }
    }
}

/** One exercise of a past workout, in the order it was done. */
data class RepeatableExercise(
    val exerciseId: Int,
    /** The exercise's stored name (translated where shown). */
    val name: String,
    val isCardio: Boolean,
    /** How many sets were done (cardio: how many intervals). */
    val sets: Int
)

/** A finished workout a quick workout can start from: its exercises are copied with the sets and weights done then. */
data class RepeatableWorkout(
    val sessionId: Int,
    val timestamp: Long,
    val title: WorkoutTitle,
    val exercises: List<RepeatableExercise>
) {
    companion object {
        /** The workouts with something to copy, newest first; the exercises of each in the order they were done. */
        fun of(sessions: List<SessionWithDetails>, exerciseNames: Map<Int, String>): List<RepeatableWorkout> = sessions.mapNotNull { s ->
            val strength = s.sets.groupBy { it.setLog.ordineEsercizio to it.setLog.exerciseId }.map { (key, rows) ->
                key.first to RepeatableExercise(
                    exerciseId = key.second,
                    name = rows.first().exercise.nome,
                    isCardio = false,
                    sets = QuickAddPlan.workingSets(rows.map { it.setLog }).size
                )
            }.filter { it.second.sets > 0 }
            val cardio = s.cardio.filter { it.isCompleted && it.exerciseId != null }
                .groupBy { it.ordineEsercizio to it.exerciseId!! }
                .mapNotNull { (key, rows) ->
                    val name = exerciseNames[key.second] ?: return@mapNotNull null
                    key.first to RepeatableExercise(key.second, name, isCardio = true, sets = rows.size)
                }
            val exercises = (strength + cardio).sortedBy { it.first }.map { it.second }
            if (exercises.isEmpty()) return@mapNotNull null
            RepeatableWorkout(
                sessionId = s.session.id,
                timestamp = s.session.timestamp,
                title = WorkoutTitle.of(
                    note = s.session.noteSessione,
                    planName = s.plan.nome,
                    isSystemPlan = s.plan.isSystemPlan,
                    hasStrength = strength.isNotEmpty(),
                    hasCardio = cardio.isNotEmpty()
                ),
                exercises = exercises
            )
        }
    }
}

package com.emanuel5014.trainable.domain.session

import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.hasPrescription
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.data.local.entity.toPrescriptionBlocks
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.TimeAndReps

/** One exercise of a routine made from a workout, before the routine exists. */
data class RoutineExerciseDraft(
    val exerciseId: Int,
    /** `exercise_type` of the plan exercise: "strength", [TIME_AND_WEIGHT], [TIME_AND_REPS] or "cardio". */
    val exerciseType: String,
    val sets: Int,
    val repsTarget: String,
    /** Top of a rep range the sets were done against; null for fixed reps and per-set lists. */
    val repsMax: Int? = null,
    val restSeconds: Int,
    val durationSeconds: Int? = null,
    val distanceKm: Float? = null,
    /** The workout's superset id; null when the exercise is on its own. The routine gets new ids. */
    val supersetKey: String? = null,
    /** The advanced prescription the sets were done against, as week 1 (empty for a plain exercise). */
    val blocks: List<PrescriptionBlock> = emptyList(),
    /** Warm-up sets done before it, which the routine leaves out. */
    val warmupsLeftOut: Int = 0
) {
    val isCardio: Boolean get() = exerciseType == CARDIO
    val isSuperset: Boolean get() = supersetKey != null

    companion object {
        const val STRENGTH = "strength"
        const val CARDIO = "cardio"
    }
}

/** A routine made from a workout, with what it takes to undo it. */
data class RoutineFromWorkoutResult(
    val planId: Int,
    val sessionId: Int,
    /** Where the workout was, and its own name there. */
    val previousPlanId: Int,
    val previousName: String?,
    /** False when the workout was of a real routine and stayed there. */
    val movedWorkout: Boolean
)

/**
 * What a workout becomes as a routine: one exercise per exercise done, in the order it was done (the same exercise
 * done twice is there twice). Each takes the sets done (warm-up and extra sets left out), the reps as they were done
 * (a number when every set had the same, else a per-set list, or the rep range the sets were done against), the rest
 * of its first set, its type and, for cardio, the time and distance of its first set. Weights, RPE and notes are how
 * the workout went, not the programme: they stay in the history.
 */
object RoutineFromWorkout {
    /** Rest of an exercise whose sets carry none (the same default as a routine made by the history import). */
    const val DEFAULT_REST_SECONDS = 120

    fun drafts(sets: List<SetLogEntity>, cardio: List<CardioLogEntity>): List<RoutineExerciseDraft> {
        val strength = strengthRuns(sets).mapNotNull { (order, rows) -> strengthDraft(rows)?.let { Ordered(order, it) } }
        val cardioDrafts = cardio.groupedByExercise().mapNotNull { group ->
            val exerciseId = group.exerciseId ?: return@mapNotNull null
            val done = group.sets.filter { it.isCompleted }.ifEmpty { group.sets }
            val first = done.first()
            Ordered(
                Order(group.order, first.id.toLong() + CARDIO_TIE_BREAK),
                RoutineExerciseDraft(
                    exerciseId = exerciseId,
                    exerciseType = RoutineExerciseDraft.CARDIO,
                    sets = done.size,
                    repsTarget = "1",
                    restSeconds = 0,
                    durationSeconds = (first.durataTargetSecondi ?: first.durataSecondi).takeIf { it > 0 },
                    distanceKm = first.distanza.takeIf { it > 0f }
                )
            )
        }
        val ordered = (strength + cardioDrafts).sortedWith(compareBy({ it.order.ordine }, { it.order.tieBreak })).map { it.draft }
        // A superset stays one only while two of its exercises are in the routine
        val supersets = ordered.mapNotNull { it.supersetKey }.groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
        return ordered.map { if (it.supersetKey != null && it.supersetKey !in supersets) it.copy(supersetKey = null) else it }
    }

    /** Number of sets the routine gets, the way the sheet counts them (cardio sets included). */
    fun setCount(drafts: List<RoutineExerciseDraft>): Int = drafts.sumOf { it.sets }

    private data class Order(val ordine: Int, val tieBreak: Long)
    private data class Ordered(val order: Order, val draft: RoutineExerciseDraft)

    /** Cardio rows have ids of their own: at the same order they come after the strength ones. */
    private const val CARDIO_TIE_BREAK = 1L shl 40

    /**
     * The exercises done, each with its rows. Rows written before exercises had an order all carry 0: those are told
     * apart by exercise and ordered by their first row.
     */
    private fun strengthRuns(sets: List<SetLogEntity>): List<Pair<Order, List<SetLogEntity>>> {
        val instances = sets.map { it.ordineEsercizio to it.exerciseId }.distinct().size
        val byOrder = sets.any { it.ordineEsercizio > 0 } || instances <= 1
        val groups = if (byOrder) sets.groupBy { it.ordineEsercizio to it.exerciseId } else sets.groupBy { 0 to it.exerciseId }
        return groups.values.map { rows ->
            val sorted = rows.sortedWith(compareBy({ it.numeroSerie }, { it.id }))
            Order(sorted.first().ordineEsercizio, sorted.minOf { it.id }.toLong()) to sorted
        }
    }

    private fun strengthDraft(rows: List<SetLogEntity>): RoutineExerciseDraft? {
        val working = rows.filter { !it.isWarmup && !it.isExtra }
        val done = working.filter { it.isCompleted }.ifEmpty { working }
        if (done.isEmpty()) return null
        val kind = done.kind
        val rest = done.firstNotNullOfOrNull { it.restTimerSeconds } ?: DEFAULT_REST_SECONDS
        val superset = rows.firstNotNullOfOrNull { it.supersetId }
        val warmups = rows.count { it.isWarmup }
        val base = RoutineExerciseDraft(
            exerciseId = rows.first().exerciseId,
            exerciseType = RoutineExerciseDraft.STRENGTH,
            sets = done.size,
            repsTarget = "",
            restSeconds = rest,
            supersetKey = superset,
            warmupsLeftOut = warmups
        )
        val reps = done.map { it.repsEffettive.coerceAtLeast(0) }
        return when (kind) {
            SetLogKind.STRENGTH -> {
                val blocks = if (done.any { it.hasPrescription }) done.toPrescriptionBlocks() else emptyList()
                val range = done.map { it.repsRange }.distinct().singleOrNull()
                when {
                    blocks.isNotEmpty() -> {
                        val (count, target) = PrescriptionExpander.legacyTargets(blocks)
                        base.copy(sets = count, repsTarget = target, blocks = blocks)
                    }
                    range != null -> base.copy(repsTarget = range.min.toString(), repsMax = range.max)
                    else -> base.copy(repsTarget = perSet(reps))
                }
            }
            SetLogKind.TIME_AND_WEIGHT -> {
                val seconds = done.firstNotNullOfOrNull { it.durataSecondi } ?: TimeAndReps.DEFAULT_SECONDS
                base.copy(exerciseType = TIME_AND_WEIGHT, repsTarget = "${seconds}s", durationSeconds = seconds)
            }
            SetLogKind.TIME_AND_REPS -> base.copy(
                exerciseType = TIME_AND_REPS,
                repsTarget = TimeAndReps.goalOf(reps.first().toString()).toString(),
                durationSeconds = done.firstNotNullOfOrNull { it.durataSecondi } ?: TimeAndReps.DEFAULT_SECONDS
            )
        }
    }

    /** "10" when every set had the same reps, else the list as done ("10-8-7-6"). */
    private fun perSet(reps: List<Int>): String =
        if (reps.distinct().size == 1) reps.first().toString() else reps.joinToString("-")
}

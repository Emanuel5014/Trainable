package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.isInParts
import com.emanuel5014.trainable.data.local.entity.isTimeAndReps
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.data.local.relation.ExerciseSetWithSession
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsChartPoint

/**
 * The estimated 1RM the way Analytics computes it: a single is its own 1RM, otherwise Epley, with no cap on the reps.
 * A warm-up, a timed set, a set done in parts and a set with no reps say nothing about it (0).
 */
fun SetLogEntity.estimatedOneRepMaxKg(): Float =
    if (isWarmup || kind != SetLogKind.STRENGTH || repsEffettive <= 0 || isInParts) 0f
    else LoadCalculator.epley(pesoSollevato, repsEffettive)

/**
 * What one workout did for one exercise. Warm-ups are kept in [sets] but never count in the figures. An exercise done
 * more than once in the workout (warm-ups first, heavy sets at the end) is still one performance: [sets] lists the
 * runs one after the other, in the order they were done, and [groups] tells them apart.
 */
data class ExercisePerformance(
    val sessionId: Int,
    val timestamp: Long,
    val sessionNote: String?,
    val planName: String,
    val sets: List<SetLogEntity>
) {
    /** The sets of each run of the exercise, every run numbered from its own first set. */
    val groups: List<List<SetLogEntity>> = sets.groupBy { it.ordineEsercizio }.values.toList()

    val workingSets: List<SetLogEntity> = sets.filter { !it.isWarmup }

    val topWeightKg: Float = workingSets.maxOfOrNull { it.pesoSollevato } ?: 0f

    val bestE1rmKg: Float = workingSets.maxOfOrNull { it.estimatedOneRepMaxKg() } ?: 0f

    val volumeKg: Float = workingSets.filter { it.durataSecondi == null }.sumOf { (it.pesoSollevato * it.repsEffettive).toDouble() }.toFloat()

    val totalReps: Int = workingSets.filter { it.durataSecondi == null || it.isTimeAndReps }.sumOf { it.repsEffettive }

    val longestHoldSeconds: Int = workingSets.maxOfOrNull { it.durataSecondi ?: 0 } ?: 0
}

data class RecordSet(val set: SetLogEntity, val timestamp: Long)

data class RecordValue(val value: Float, val timestamp: Long)

/** The personal records of an exercise; which ones are filled depends on its [ExerciseKind], the others are null. */
data class ExerciseRecords(
    val sessionCount: Int,
    val workingSetCount: Int,
    val firstTimestamp: Long?,
    val lastTimestamp: Long?,
    val heaviest: RecordSet?,
    val bestE1rm: RecordValue?,
    val bestVolume: RecordValue?,
    val mostRepsInSet: RecordSet?,
    val mostRepsInSession: RecordValue?,
    val longestHold: RecordSet?
)

/** Every finished performance of one exercise, its records and the points of its progress chart (weights in kg). */
data class ExerciseHistory(
    val kind: ExerciseKind,
    /** Newest first. */
    val performances: List<ExercisePerformance>,
    val records: ExerciseRecords,
    /** Best estimated 1RM of each session, oldest first; only for [ExerciseKind.STRENGTH]. */
    val chartPoints: List<AnalyticsChartPoint>
) {
    companion object {
        /** [rows] are the sets of the exercise, the newest workout first; within a workout they are put in the order done. */
        fun of(rows: List<ExerciseSetWithSession>): ExerciseHistory {
            val bySession = LinkedHashMap<Int, MutableList<ExerciseSetWithSession>>()
            rows.forEach { bySession.getOrPut(it.setLog.sessionId) { mutableListOf() }.add(it) }
            val performances = bySession.values.map { group ->
                val first = group.first()
                ExercisePerformance(
                    sessionId = first.setLog.sessionId,
                    timestamp = first.sessionTimestamp,
                    sessionNote = first.sessionNote,
                    planName = first.planName,
                    sets = group.map { it.setLog }.sortedWith(compareBy({ it.ordineEsercizio }, { it.numeroSerie }))
                )
            }
            val allSets = rows.map { it.setLog }
            val kind = ExerciseKind.of(allSets.filter { !it.isWarmup }.ifEmpty { allSets })
            return ExerciseHistory(kind, performances, recordsOf(kind, performances), chartPointsOf(kind, performances))
        }

        private fun recordsOf(kind: ExerciseKind, performances: List<ExercisePerformance>): ExerciseRecords {
            val timestamps = performances.map { it.timestamp }
            val strength = kind == ExerciseKind.STRENGTH
            return ExerciseRecords(
                sessionCount = performances.size,
                workingSetCount = performances.sumOf { it.workingSets.size },
                firstTimestamp = timestamps.minOrNull(),
                lastTimestamp = timestamps.maxOrNull(),
                heaviest = if (strength) {
                    performances.bestSet(
                        include = { it.durataSecondi == null && it.pesoSollevato > 0f },
                        order = compareBy({ it.set.pesoSollevato }, { it.set.repsEffettive }, { it.timestamp })
                    )
                } else null,
                bestE1rm = if (strength) performances.bestValue { it.bestE1rmKg } else null,
                bestVolume = if (strength) performances.bestValue { it.volumeKg } else null,
                mostRepsInSet = when (kind) {
                    ExerciseKind.BODYWEIGHT -> performances.bestSet(
                        include = { it.durataSecondi == null && it.repsEffettive > 0 },
                        order = compareBy({ it.set.repsEffettive }, { it.timestamp })
                    )
                    ExerciseKind.TIME_AND_REPS -> performances.bestSet(
                        include = { it.isTimeAndReps },
                        order = compareBy({ it.set.repsEffettive }, { it.timestamp })
                    )
                    else -> null
                },
                mostRepsInSession = if (kind == ExerciseKind.BODYWEIGHT) performances.bestValue { it.totalReps.toFloat() } else null,
                longestHold = if (kind == ExerciseKind.TIMED) {
                    performances.bestSet(
                        include = { it.durataSecondi != null },
                        order = compareBy({ it.set.durataSecondi }, { it.set.pesoSollevato }, { it.timestamp })
                    )
                } else null
            )
        }

        private fun chartPointsOf(kind: ExerciseKind, performances: List<ExercisePerformance>): List<AnalyticsChartPoint> =
            if (kind != ExerciseKind.STRENGTH) emptyList()
            else performances
                .filter { it.bestE1rmKg > 0f }
                .sortedWith(compareBy({ it.timestamp }, { it.sessionId }))
                .map { AnalyticsChartPoint(timestamp = it.timestamp, value = it.bestE1rmKg, id = it.sessionId) }

        /** The best working set that passes [include], the more recent one winning a tie of [order]. */
        private fun List<ExercisePerformance>.bestSet(include: (SetLogEntity) -> Boolean, order: Comparator<RecordSet>): RecordSet? =
            flatMap { performance -> performance.workingSets.filter(include).map { RecordSet(it, performance.timestamp) } }
                .maxWithOrNull(order)

        /** The session with the highest [score], when above zero; the more recent one wins a tie. */
        private fun List<ExercisePerformance>.bestValue(score: (ExercisePerformance) -> Float): RecordValue? =
            filter { score(it) > 0f }
                .maxWithOrNull(compareBy({ score(it) }, { it.timestamp }))
                ?.let { RecordValue(score(it), it.timestamp) }
    }
}

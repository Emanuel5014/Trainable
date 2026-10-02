package com.emanuel5014.trainable.domain.compare

import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import java.util.Locale
import kotlin.math.abs

/** Epley is only trustworthy for low-rep sets; a 20-rep burn-out says little about a 1RM. */
private const val MAX_REPS_FOR_E1RM = 12

private fun SetLogEntity.e1rm(): Float? =
    if (durataSecondi == null && repsEffettive in 1..MAX_REPS_FOR_E1RM) LoadCalculator.epley(pesoSollevato, repsEffettive) else null

enum class Trend { UP, DOWN, SAME }

enum class Outcome { BETTER, WORSE, NEUTRAL }

/**
 * What a summary row measures. [higherIsBetter] decides how a difference is judged: more tonnage is an
 * improvement, a slower pace is not, and a longer session is neither (null → shown without good/bad colour).
 */
enum class MetricKind(val higherIsBetter: Boolean?) {
    DURATION(null),
    VOLUME(true),
    WORKING_SETS(true),
    TOTAL_REPS(true),
    EXERCISES(null),
    MAX_WEIGHT(true),
    BEST_E1RM(true),
    AVG_RPE(null),
    TIME_UNDER_TENSION(true),
    CARDIO_DISTANCE(true),
    CARDIO_DURATION(null),
    /** Seconds per km: lower is faster. */
    CARDIO_PACE(false)
}

/** One quantity measured in session A and session B ("A compared to B": positive delta = A is higher). */
data class ComparedMetric(val kind: MetricKind, val a: Float?, val b: Float?) {
    val isComparable: Boolean get() = a != null && b != null

    val delta: Float? get() = if (a != null && b != null) a - b else null

    val percent: Float? get() = if (a != null && b != null && b != 0f) (a - b) / abs(b) * 100f else null

    val trend: Trend
        get() {
            val d = delta ?: return Trend.SAME
            val p = percent
            return when {
                p != null && abs(p) < 0.1f -> Trend.SAME
                p == null && d == 0f -> Trend.SAME
                d > 0f -> Trend.UP
                else -> Trend.DOWN
            }
        }

    val outcome: Outcome
        get() {
            val higherIsBetter = kind.higherIsBetter ?: return Outcome.NEUTRAL
            return when (trend) {
                Trend.SAME -> Outcome.NEUTRAL
                Trend.UP -> if (higherIsBetter) Outcome.BETTER else Outcome.WORSE
                Trend.DOWN -> if (higherIsBetter) Outcome.WORSE else Outcome.BETTER
            }
        }
}

/** How an exercise is measured, which decides what is compared and how its sets are written. */
enum class ExerciseKind {
    /** Weight × reps. */
    STRENGTH,

    /** Reps with no added load (pull-ups, push-ups). */
    BODYWEIGHT,

    /** Time & Weight: how long a set was held, with an optional load (planks, carries). */
    TIMED
}

/** A logged set reduced to what the comparison needs; [log] keeps the prescription snapshot and RPE for display. */
data class ComparedSet(
    val number: Int,
    val weightKg: Float,
    val reps: Int,
    val durationSeconds: Int?,
    val isWarmup: Boolean,
    val log: SetLogEntity
)

/** Everything one session did for one exercise. */
data class ExerciseSide(val sets: List<ComparedSet>) {
    val workingSets: List<ComparedSet> = sets.filter { !it.isWarmup }

    val volumeKg: Float = workingSets.filter { it.durationSeconds == null }.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()

    val totalReps: Int = workingSets.filter { it.durationSeconds == null }.sumOf { it.reps }

    val totalSeconds: Int = workingSets.sumOf { it.durationSeconds ?: 0 }

    val topWeightKg: Float = workingSets.maxOfOrNull { it.weightKg } ?: 0f

    val bestE1rmKg: Float = workingSets.mapNotNull { it.log.e1rm() }.maxOrNull() ?: 0f
}

data class ExerciseComparison(
    val exerciseId: Int,
    /** Name as stored (not translated); the screen translates it. */
    val exerciseName: String,
    val category: String,
    val kind: ExerciseKind,
    val a: ExerciseSide?,
    val b: ExerciseSide?
) {
    /** The single figure that says whether the exercise went better, chosen by [kind]. */
    val headline: ComparedMetric?
        get() {
            if (a == null || b == null) return null
            return when (kind) {
                // Without a low-rep set on both sides there is no 1RM to compare: fall back to tonnage
                ExerciseKind.STRENGTH ->
                    if (a.bestE1rmKg > 0f && b.bestE1rmKg > 0f) ComparedMetric(MetricKind.BEST_E1RM, a.bestE1rmKg, b.bestE1rmKg)
                    else ComparedMetric(MetricKind.VOLUME, a.volumeKg, b.volumeKg)
                ExerciseKind.BODYWEIGHT -> ComparedMetric(MetricKind.TOTAL_REPS, a.totalReps.toFloat(), b.totalReps.toFloat())
                ExerciseKind.TIMED -> ComparedMetric(MetricKind.TIME_UNDER_TENSION, a.totalSeconds.toFloat(), b.totalSeconds.toFloat())
            }
        }

    /**
     * The rows of the side-by-side table. Warm-ups are paired with warm-ups and working sets with working
     * sets, so a session that warmed up more doesn't shift every row of the other one.
     */
    val rows: List<SetRow>
        get() {
            fun pairs(isWarmup: Boolean): List<SetRow> {
                val setsA = a?.sets.orEmpty().filter { it.isWarmup == isWarmup }
                val setsB = b?.sets.orEmpty().filter { it.isWarmup == isWarmup }
                return (0 until maxOf(setsA.size, setsB.size)).map { SetRow(isWarmup, it + 1, setsA.getOrNull(it), setsB.getOrNull(it)) }
            }
            return pairs(true) + pairs(false)
        }
}

/** One line of the table: set [index] (1-based) of a kind, as session A and session B did it. */
data class SetRow(val isWarmup: Boolean, val index: Int, val a: ComparedSet?, val b: ComparedSet?)

data class CardioSide(val distanceKm: Float, val durationSeconds: Int, val targetDurationSeconds: Int?) {
    /** Seconds per km, when a distance was logged. */
    val paceSecondsPerKm: Float? get() = if (distanceKm >= MIN_DISTANCE_FOR_PACE_KM && durationSeconds > 0) durationSeconds / distanceKm else null

    private companion object {
        const val MIN_DISTANCE_FOR_PACE_KM = 0.05f
    }
}

data class CardioComparison(val label: String, val a: CardioSide?, val b: CardioSide?) {
    val distance: ComparedMetric? get() = if (a != null && b != null) ComparedMetric(MetricKind.CARDIO_DISTANCE, a.distanceKm, b.distanceKm) else null
    val duration: ComparedMetric? get() = if (a != null && b != null) ComparedMetric(MetricKind.CARDIO_DURATION, a.durationSeconds.toFloat(), b.durationSeconds.toFloat()) else null
    val pace: ComparedMetric?
        get() {
            val pa = a?.paceSecondsPerKm
            val pb = b?.paceSecondsPerKm
            return if (pa != null && pb != null) ComparedMetric(MetricKind.CARDIO_PACE, pa, pb) else null
        }
}

data class SessionComparison(
    val summary: List<ComparedMetric>,
    val exercises: List<ExerciseComparison>,
    val cardio: List<CardioComparison>
) {
    companion object {
        fun of(a: SessionWithDetails, b: SessionWithDetails): SessionComparison = SessionComparison(
            summary = summary(a, b),
            exercises = exercises(a, b),
            cardio = cardio(a, b)
        )

        // ---- summary ------------------------------------------------------------------------------

        private fun summary(a: SessionWithDetails, b: SessionWithDetails): List<ComparedMetric> {
            val sa = SessionFigures.of(a)
            val sb = SessionFigures.of(b)
            val metrics = listOf(
                ComparedMetric(MetricKind.DURATION, a.session.durationMs?.let { it / 1000f }, b.session.durationMs?.let { it / 1000f }),
                ComparedMetric(MetricKind.VOLUME, sa.volumeKg, sb.volumeKg),
                ComparedMetric(MetricKind.WORKING_SETS, sa.workingSets.toFloat(), sb.workingSets.toFloat()),
                ComparedMetric(MetricKind.TOTAL_REPS, sa.totalReps.toFloat(), sb.totalReps.toFloat()),
                ComparedMetric(MetricKind.EXERCISES, sa.exerciseCount.toFloat(), sb.exerciseCount.toFloat()),
                ComparedMetric(MetricKind.MAX_WEIGHT, sa.maxWeightKg, sb.maxWeightKg),
                ComparedMetric(MetricKind.BEST_E1RM, sa.bestE1rmKg, sb.bestE1rmKg),
                ComparedMetric(MetricKind.AVG_RPE, sa.avgRpe, sb.avgRpe),
                ComparedMetric(MetricKind.TIME_UNDER_TENSION, sa.timeUnderTension.toFloat(), sb.timeUnderTension.toFloat()),
                ComparedMetric(MetricKind.CARDIO_DISTANCE, sa.cardioKm, sb.cardioKm),
                ComparedMetric(MetricKind.CARDIO_DURATION, sa.cardioSeconds.toFloat(), sb.cardioSeconds.toFloat()),
                ComparedMetric(MetricKind.CARDIO_PACE, sa.cardioPace, sb.cardioPace)
            )
            // Zero means "nothing measured" (no cardio, no estimate...): show it as missing instead of "0" and
            // a meaningless -100%. A row appears only when at least one session has something to show.
            return metrics
                .map { it.copy(a = it.a?.takeIf { v -> v > 0f }, b = it.b?.takeIf { v -> v > 0f }) }
                .filter { it.a != null || it.b != null }
        }

        // ---- exercises ----------------------------------------------------------------------------

        private fun exercises(a: SessionWithDetails, b: SessionWithDetails): List<ExerciseComparison> {
            val groupsA = a.sets.groupBy { it.exercise.id }
            val groupsB = b.sets.groupBy { it.exercise.id }
            // Session A's exercise order first, then whatever only session B did
            val ids = (orderedExerciseIds(a) + orderedExerciseIds(b)).distinct()
            return ids.map { id ->
                val setsA = groupsA[id].orEmpty()
                val setsB = groupsB[id].orEmpty()
                val exercise = (setsA.firstOrNull() ?: setsB.first()).exercise
                val sideA = setsA.takeIf { it.isNotEmpty() }?.let { side(it.map { s -> s.setLog }) }
                val sideB = setsB.takeIf { it.isNotEmpty() }?.let { side(it.map { s -> s.setLog }) }
                ExerciseComparison(
                    exerciseId = id,
                    exerciseName = exercise.nome,
                    category = exercise.categoria,
                    kind = kindOf(listOfNotNull(sideA, sideB)),
                    a = sideA,
                    b = sideB
                )
            }
        }

        /** Exercises in the order they were first performed in the session. */
        private fun orderedExerciseIds(session: SessionWithDetails): List<Int> =
            session.sets.sortedWith(compareBy({ it.setLog.ordineEsercizio }, { it.setLog.numeroSerie }))
                .map { it.exercise.id }
                .distinct()

        private fun side(logs: List<SetLogEntity>) = ExerciseSide(
            logs.sortedBy { it.numeroSerie }.map {
                ComparedSet(
                    number = it.numeroSerie,
                    weightKg = it.pesoSollevato,
                    reps = it.repsEffettive,
                    durationSeconds = it.durataSecondi,
                    isWarmup = it.isWarmup,
                    log = it
                )
            }
        )

        private fun kindOf(sides: List<ExerciseSide>): ExerciseKind {
            val sets = sides.flatMap { it.workingSets.ifEmpty { it.sets } }
            return when {
                sets.any { it.durationSeconds != null } -> ExerciseKind.TIMED
                sets.any { it.weightKg > 0f } -> ExerciseKind.STRENGTH
                else -> ExerciseKind.BODYWEIGHT
            }
        }

        // ---- cardio -------------------------------------------------------------------------------

        /** Logs of the same activity are paired in order ("Treadmill" with "Treadmill"); the rest stand alone. */
        private fun cardio(a: SessionWithDetails, b: SessionWithDetails): List<CardioComparison> {
            fun sorted(session: SessionWithDetails) = session.cardio.sortedBy { it.ordineEsercizio }
            val queueA = sorted(a).groupBy { normalize(it.categoria) }.mapValues { it.value.toMutableList() }
            val queueB = sorted(b).groupBy { normalize(it.categoria) }.mapValues { it.value.toMutableList() }
            val keys = (sorted(a).map { normalize(it.categoria) } + sorted(b).map { normalize(it.categoria) }).distinct()

            return keys.flatMap { key ->
                val listA = queueA[key].orEmpty()
                val listB = queueB[key].orEmpty()
                (0 until maxOf(listA.size, listB.size)).map { index ->
                    val ca = listA.getOrNull(index)
                    val cb = listB.getOrNull(index)
                    CardioComparison(
                        label = (ca ?: cb)!!.categoria.trim(),
                        a = ca?.toSide(),
                        b = cb?.toSide()
                    )
                }
            }
        }

        private fun CardioLogEntity.toSide() = CardioSide(distanza, durataSecondi, durataTargetSecondi)

        private fun normalize(label: String) = label.trim().lowercase(Locale.ROOT)
    }
}

/** The totals shown in the summary for one session. */
private class SessionFigures(session: SessionWithDetails) {
    private val working = session.sets.map { it.setLog }.filter { !it.isWarmup }
    private val loaded = working.filter { it.durataSecondi == null }

    val volumeKg: Float = loaded.sumOf { (it.pesoSollevato * it.repsEffettive).toDouble() }.toFloat()
    val workingSets: Int = working.size
    val totalReps: Int = loaded.sumOf { it.repsEffettive }
    val exerciseCount: Int = session.sets.map { it.exercise.id }.distinct().size
    val maxWeightKg: Float = working.maxOfOrNull { it.pesoSollevato } ?: 0f
    val bestE1rmKg: Float = working.mapNotNull { it.e1rm() }.maxOrNull() ?: 0f
    val avgRpe: Float? = working.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val timeUnderTension: Int = working.sumOf { it.durataSecondi ?: 0 }
    val cardioKm: Float = session.cardio.sumOf { it.distanza.toDouble() }.toFloat()
    val cardioSeconds: Int = session.cardio.sumOf { it.durataSecondi }
    val cardioPace: Float? = if (cardioKm >= 0.05f && cardioSeconds > 0) cardioSeconds / cardioKm else null

    companion object {
        fun of(session: SessionWithDetails) = SessionFigures(session)
    }
}

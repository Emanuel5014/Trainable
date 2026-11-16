package com.emanuel5014.trainable.domain.heart

import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import kotlin.math.max
import kotlin.math.roundToInt

/** One heart-rate reading of the watch or band. */
data class HeartSample(val timeMs: Long, val bpm: Int)

/** One point of a saved curve: seconds from the start of the workout and the average bpm around it. */
data class HeartPoint(val offsetSeconds: Int, val bpm: Int)

/** What was read for a workout, before it is saved. */
data class HeartSummary(
    val avgBpm: Int?,
    val minBpm: Int?,
    val maxBpm: Int?,
    val points: List<HeartPoint>
)

/**
 * The heart-rate maths of a workout: the curve kept in the database, its average and its zones. Readings come at
 * uneven intervals (about one a second while the band records a workout, one every 1-30 minutes otherwise), so
 * every reading counts for the time until the next one, capped at [MAX_GAP_SECONDS] so a long silence does not
 * stretch one reading over it.
 */
object HeartMath {
    /** A reading stands for at most this long. */
    const val MAX_GAP_SECONDS = 60

    /** The saved curve has at most about this many points. */
    const val MAX_POINTS = 600

    /** Readings no heart makes are dropped (sensor glitches). */
    private const val MIN_VALID_BPM = 25
    private const val MAX_VALID_BPM = 250

    /** The readings in [startMs, endMs), cleaned and averaged into buckets. */
    fun summarize(samples: List<HeartSample>, startMs: Long, endMs: Long): HeartSummary {
        val inside = samples
            .filter { it.timeMs in startMs until endMs && it.bpm in MIN_VALID_BPM..MAX_VALID_BPM }
            .sortedBy { it.timeMs }
            .distinctBy { it.timeMs }
        if (inside.isEmpty()) return HeartSummary(null, null, null, emptyList())

        val points = inside.map { HeartPoint(((it.timeMs - startMs) / 1000L).toInt(), it.bpm) }
        val durationSeconds = ((endMs - startMs) / 1000L).toInt().coerceAtLeast(1)
        return HeartSummary(
            avgBpm = weightedAverage(points, durationSeconds),
            minBpm = inside.minOf { it.bpm },
            maxBpm = inside.maxOf { it.bpm },
            points = bucket(points, durationSeconds)
        )
    }

    /** How long each point counts for: until the next one, capped; the last one until the end, capped. */
    fun weights(points: List<HeartPoint>, durationSeconds: Int): List<Int> = points.mapIndexed { index, point ->
        val next = points.getOrNull(index + 1)?.offsetSeconds ?: durationSeconds
        (next - point.offsetSeconds).coerceIn(1, MAX_GAP_SECONDS)
    }

    fun weightedAverage(points: List<HeartPoint>, durationSeconds: Int): Int? {
        if (points.isEmpty()) return null
        val weights = weights(points, durationSeconds)
        val total = weights.sum().toDouble()
        return (points.indices.sumOf { points[it].bpm.toDouble() * weights[it] } / total).roundToInt()
    }

    /** Readings averaged into buckets long enough to keep at most [MAX_POINTS] (never shorter than 5 s). */
    fun bucket(points: List<HeartPoint>, durationSeconds: Int): List<HeartPoint> {
        val size = max(5, (durationSeconds + MAX_POINTS - 1) / MAX_POINTS)
        return points
            .groupBy { it.offsetSeconds / size }
            .entries
            .sortedBy { it.key }
            .map { (index, group) ->
                HeartPoint(
                    offsetSeconds = index * size + size / 2,
                    bpm = (group.sumOf { it.bpm }.toDouble() / group.size).roundToInt()
                )
            }
    }

    /** A gap longer than this breaks the drawn curve: 2 minutes, or 3 times the usual spacing of sparse data. */
    fun breakGapSeconds(points: List<HeartPoint>): Int {
        val gaps = points.zipWithNext { a, b -> b.offsetSeconds - a.offsetSeconds }.filter { it > 0 }.sorted()
        val median = gaps.getOrNull(gaps.size / 2) ?: 0
        return max(120, median * 3)
    }

    fun encode(points: List<HeartPoint>): String = points.joinToString(",") { "${it.offsetSeconds}:${it.bpm}" }

    /** The saved curve; anything unreadable is skipped. */
    fun decode(text: String): List<HeartPoint> = text.split(',').mapNotNull { pair ->
        val parts = pair.split(':')
        if (parts.size != 2) return@mapNotNull null
        val offset = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
        val bpm = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
        HeartPoint(offset, bpm)
    }

    /** Lower bounds of zones 2..5 as a share of the maximum heart rate; zone 1 is everything below 60 %. */
    private val ZONE_FLOORS = listOf(0.6, 0.7, 0.8, 0.9)

    /** The zone (1..5) of [bpm] for a maximum heart rate of [maxHeartRate]. */
    fun zoneOf(bpm: Int, maxHeartRate: Int): Int = 1 + ZONE_FLOORS.count { bpm >= it * maxHeartRate }

    /** Seconds spent in each of the 5 zones, from the saved curve. */
    fun zoneSeconds(points: List<HeartPoint>, durationSeconds: Int, maxHeartRate: Int): List<Int> {
        val seconds = MutableList(5) { 0 }
        if (maxHeartRate <= 0) return seconds
        val weights = weights(points, durationSeconds)
        points.forEachIndexed { index, point -> seconds[zoneOf(point.bpm, maxHeartRate) - 1] += weights[index] }
        return seconds
    }

    /**
     * The part of an amount recorded over [recordStart, recordEnd) that falls inside [startMs, endMs): a band
     * often writes calories in long blocks, so a block half inside the workout counts for half.
     */
    fun overlapShare(recordStart: Long, recordEnd: Long, startMs: Long, endMs: Long): Double {
        if (recordEnd <= recordStart) return if (recordStart in startMs until endMs) 1.0 else 0.0
        val overlap = minOf(recordEnd, endMs) - maxOf(recordStart, startMs)
        return if (overlap <= 0) 0.0 else overlap.toDouble() / (recordEnd - recordStart)
    }
}

/** The saved curve covers less than half of its span (the band read every few minutes). */
fun HeartMath.isSparse(data: SessionHeartDataEntity): Boolean {
    val duration = ((data.windowEnd - data.windowStart) / 1000L).toInt().coerceAtLeast(1)
    val points = decode(data.samples)
    return points.isEmpty() || weights(points, duration).sum() * 2 < duration
}

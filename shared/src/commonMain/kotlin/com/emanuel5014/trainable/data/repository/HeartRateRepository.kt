package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.HeartDataDao
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.domain.heart.HeartSample
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/** What the platform's health store holds for a span of time, already narrowed to one app (see [HeartRateSource]). */
data class HeartReading(
    val samples: List<HeartSample>,
    val activeKcal: Double?,
    val totalKcal: Double?,
    /** The app that wrote it, as the user knows it (e.g. "Mi Fitness"). */
    val source: String?
)

/** A workout the watch or band recorded, in epoch millis. */
data class RecordedExercise(val startMs: Long, val endMs: Long)

/** The platform's health store: Health Connect on Android; none on iOS yet. */
interface HeartRateSource {
    /** Whether the user gave every read permission the app asks for. */
    suspend fun hasPermissions(): Boolean

    /** Heart rate and calories inside [startMs, endMs); null when the store cannot be read. */
    suspend fun read(startMs: Long, endMs: Long): HeartReading?

    /** Workouts recorded by a watch or band that overlap [startMs, endMs). */
    suspend fun recordedExercises(startMs: Long, endMs: Long): List<RecordedExercise>
}

/** How reading a workout's heart rate went. */
enum class HeartSyncResult { SAVED, NO_DATA, NO_DURATION, NO_PERMISSION, UNAVAILABLE }

/**
 * Heart rate and calories of each workout: read once from the health store and kept in the database, so they stay
 * after the store's 30-day reading window and travel with the backups. [source] is null where there is no store
 * (iOS for now): saved data is still shown.
 */
class HeartRateRepository(
    private val dao: HeartDataDao,
    private val source: HeartRateSource?
) {
    val canSync: Boolean get() = source != null

    fun observe(sessionId: Int): Flow<SessionHeartDataEntity?> = dao.observe(sessionId)

    suspend fun saved(sessionId: Int): SessionHeartDataEntity? = dao.get(sessionId)

    /**
     * The span to read for [session]: from its start to start + duration. A workout without a duration (the
     * workout clock was off) borrows the end of the band's own workout that overlaps its start; without one either,
     * it is taken to last [typicalDurationMs] (how long the lifter's timed workouts usually last).
     */
    suspend fun windowFor(session: WorkoutSessionEntity, typicalDurationMs: Long? = null): Pair<Long, Long>? {
        val start = session.timestamp
        session.durationMs?.takeIf { it > 0 }?.let { return start to start + it }
        val recorded = source?.recordedExercises(start - BAND_START_SLACK_MS, start + LONGEST_WORKOUT_MS).orEmpty()
        val match = recorded
            .filter { it.endMs > start }
            .minByOrNull { kotlin.math.abs(it.startMs - start) }
        if (match != null) return start to match.endMs
        return typicalDurationMs?.takeIf { it > 0 }?.let { start to start + it }
    }

    /** Whether the saved data was read for a different span than the workout has now (e.g. its duration was edited). */
    fun isStale(saved: SessionHeartDataEntity, session: WorkoutSessionEntity): Boolean {
        val duration = session.durationMs?.takeIf { it > 0 } ?: return saved.windowStart != session.timestamp
        return saved.windowStart != session.timestamp || saved.windowEnd != session.timestamp + duration
    }

    /**
     * Reads [session]'s span from the store and saves it. Nothing is saved without heart-rate readings; data
     * already saved is kept when the store has none, except a row without heart rate (left by an earlier version).
     */
    suspend fun sync(session: WorkoutSessionEntity, typicalDurationMs: Long? = null): HeartSyncResult {
        val store = source ?: return HeartSyncResult.UNAVAILABLE
        if (!store.hasPermissions()) return HeartSyncResult.NO_PERMISSION
        val (start, end) = windowFor(session, typicalDurationMs) ?: return HeartSyncResult.NO_DURATION
        val reading = store.read(start, end) ?: return HeartSyncResult.UNAVAILABLE
        val summary = HeartMath.summarize(reading.samples, start, end)
        if (summary.points.isEmpty()) {
            dao.get(session.id)?.takeIf { it.avgBpm == null }?.let { dao.delete(session.id) }
            return HeartSyncResult.NO_DATA
        }
        dao.upsert(
            SessionHeartDataEntity(
                sessionId = session.id,
                source = reading.source,
                avgBpm = summary.avgBpm,
                minBpm = summary.minBpm,
                maxBpm = summary.maxBpm,
                activeKcal = reading.activeKcal,
                totalKcal = reading.totalKcal,
                samples = HeartMath.encode(summary.points),
                windowStart = start,
                windowEnd = end,
                syncedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        return HeartSyncResult.SAVED
    }

    private companion object {
        /** The band's workout may have been started a little before the one in the app. */
        const val BAND_START_SLACK_MS = 30 * 60_000L
        const val LONGEST_WORKOUT_MS = 5 * 3_600_000L
    }
}

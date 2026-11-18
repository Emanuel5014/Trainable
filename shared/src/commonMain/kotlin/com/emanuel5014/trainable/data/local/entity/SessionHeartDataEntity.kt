package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Heart rate and calories of a workout, read from the lifter's watch or band (Health Connect on Android) and kept
 * here, because Health Connect only lets an app read about 30 days back and the data may be deleted at the source.
 * One row per workout; it goes away with the workout.
 */
@Entity(
    tableName = "session_heart_data",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionHeartDataEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    /** The app that wrote the data, as the user knows it (e.g. "Mi Fitness"). */
    @ColumnInfo(name = "source")
    val source: String? = null,
    @ColumnInfo(name = "avg_bpm")
    val avgBpm: Int? = null,
    @ColumnInfo(name = "min_bpm")
    val minBpm: Int? = null,
    @ColumnInfo(name = "max_bpm")
    val maxBpm: Int? = null,
    @ColumnInfo(name = "active_kcal")
    val activeKcal: Double? = null,
    @ColumnInfo(name = "total_kcal")
    val totalKcal: Double? = null,
    /** The curve, averaged into short buckets: "secondsFromStart:bpm" pairs joined by commas (see HeartSeries). */
    @ColumnInfo(name = "samples")
    val samples: String = "",
    /** The time span that was read, in epoch millis. */
    @ColumnInfo(name = "window_start")
    val windowStart: Long,
    @ColumnInfo(name = "window_end")
    val windowEnd: Long,
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long
)

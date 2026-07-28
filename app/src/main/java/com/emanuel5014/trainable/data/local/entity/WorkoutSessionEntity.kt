package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plan_id"), Index("timestamp")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "plan_id")
    val planId: Int,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "note_sessione")
    val noteSessione: String? = null,
    @ColumnInfo(name = "is_finished")
    val isFinished: Boolean = false,
    @ColumnInfo(name = "rest_timer_end_time")
    val restTimerEndTime: Long? = null,
    @ColumnInfo(name = "total_rest_seconds")
    val totalRestSeconds: Int? = null,
    @ColumnInfo(name = "warmup_timer_end_time")
    val warmupTimerEndTime: Long? = null,
    @ColumnInfo(name = "total_warmup_seconds")
    val totalWarmupSeconds: Int? = null,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,
    @ColumnInfo(name = "cardio_timer_seconds")
    val cardioTimerSeconds: Int = 0,
    @ColumnInfo(name = "cardio_timer_running")
    val cardioTimerRunning: Boolean = false,
    @ColumnInfo(name = "cardio_timer_paused")
    val cardioTimerPaused: Boolean = false,
    @ColumnInfo(name = "cardio_timer_started_at")
    val cardioTimerStartedAt: Long? = null
)

package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id")]
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "nome")
    val nome: String,
    @ColumnInfo(name = "data_inizio")
    val dataInizio: Long,
    @ColumnInfo(name = "data_fine")
    val dataFine: Long? = null,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "sessioni_target_settimana")
    val sessioniTargetSettimana: Int = 4,
    @ColumnInfo(name = "ordine")
    val ordine: Int = 0,
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,
    @ColumnInfo(name = "giorni_settimana")
    val giorniSettimana: String? = null,
    /** Number of weeks of a periodized plan (1 = a plain routine). */
    @ColumnInfo(name = "weeks_count")
    val weeksCount: Int = 1,
    @ColumnInfo(name = "current_week")
    val currentWeek: Int = 1,
    @ColumnInfo(name = "auto_advance_week")
    val autoAdvanceWeek: Boolean = true
)

/** Note that tags the plans the app creates for itself (Cardio, Quick/Custom Workout): they hold sessions but are not routines. */
const val SYSTEM_PLAN_NOTE = "SYSTEM_PLAN"

val WorkoutPlanEntity.isSystemPlan: Boolean
    get() = note == SYSTEM_PLAN_NOTE

/** A routine the user archived. System plans are stored as inactive too, but they are not archived routines. */
val WorkoutPlanEntity.isArchived: Boolean
    get() = !isActive && !isSystemPlan

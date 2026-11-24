package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.SetLogEntity

/** A set of one exercise with what is needed to place it in the history: when the workout was, what it was called. */
data class ExerciseSetWithSession(
    @Embedded val setLog: SetLogEntity,
    @ColumnInfo(name = "session_timestamp") val sessionTimestamp: Long,
    @ColumnInfo(name = "session_note") val sessionNote: String?,
    @ColumnInfo(name = "plan_name") val planName: String,
    /** The plan is one of the app's own: the workout has no routine. */
    @ColumnInfo(name = "plan_is_system") val planIsSystem: Boolean = false,
    /** The workout is a deload: shown, but left out of records and charts. */
    @ColumnInfo(name = "session_is_deload") val sessionIsDeload: Boolean = false
)

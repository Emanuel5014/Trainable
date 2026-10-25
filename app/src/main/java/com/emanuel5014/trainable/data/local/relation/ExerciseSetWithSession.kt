package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.SetLogEntity

/** A set of one exercise with what is needed to place it in the history: when the workout was, what it was called. */
data class ExerciseSetWithSession(
    @Embedded val setLog: SetLogEntity,
    @ColumnInfo(name = "session_timestamp") val sessionTimestamp: Long,
    @ColumnInfo(name = "session_note") val sessionNote: String?,
    @ColumnInfo(name = "plan_name") val planName: String
)

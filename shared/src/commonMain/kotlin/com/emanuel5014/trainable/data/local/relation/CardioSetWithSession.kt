package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity

/** A cardio set with what is needed to place it in the history of its activity: when the workout was, what it was called. */
data class CardioSetWithSession(
    @Embedded val cardio: CardioLogEntity,
    @ColumnInfo(name = "session_timestamp") val sessionTimestamp: Long,
    @ColumnInfo(name = "session_note") val sessionNote: String?,
    @ColumnInfo(name = "plan_name") val planName: String,
    /** The plan is one of the app's own: the workout has no routine. */
    @ColumnInfo(name = "plan_is_system") val planIsSystem: Boolean = false,
    /** The workout has other sets than cardio: it is not a cardio workout. */
    @ColumnInfo(name = "session_has_strength") val sessionHasStrength: Boolean = false,
    /** The workout is a deload: shown, but left out of records and charts. */
    @ColumnInfo(name = "session_is_deload") val sessionIsDeload: Boolean = false
)

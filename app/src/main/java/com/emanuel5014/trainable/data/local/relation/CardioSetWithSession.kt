package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity

/** A cardio set with what is needed to place it in the history of its activity: when the workout was, what it was called. */
data class CardioSetWithSession(
    @Embedded val cardio: CardioLogEntity,
    @ColumnInfo(name = "session_timestamp") val sessionTimestamp: Long,
    @ColumnInfo(name = "session_note") val sessionNote: String?,
    @ColumnInfo(name = "plan_name") val planName: String
)

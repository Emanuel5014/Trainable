package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo

/** A cardio exercise and the time of its newest log, for the cardio picker's suggestions. */
data class RecentCardio(
    @ColumnInfo(name = "exerciseId") val exerciseId: Int,
    @ColumnInfo(name = "lastTimestamp") val lastTimestamp: Long
)

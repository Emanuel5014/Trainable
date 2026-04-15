package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity

data class SessionWithPlanName(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "plan_nome")
    val planNome: String
)

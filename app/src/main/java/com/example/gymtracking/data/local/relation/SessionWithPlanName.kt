package com.example.gymtracking.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.example.gymtracking.data.local.entity.WorkoutSessionEntity

data class SessionWithPlanName(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "plan_nome")
    val planNome: String
)

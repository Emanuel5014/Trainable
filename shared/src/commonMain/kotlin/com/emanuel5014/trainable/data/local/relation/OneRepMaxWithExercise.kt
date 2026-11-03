package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity

data class OneRepMaxWithExercise(
    @Embedded val entry: OneRepMaxEntity,
    @ColumnInfo(name = "exercise_nome") val exerciseName: String,
    @ColumnInfo(name = "exercise_categoria") val exerciseCategory: String
)

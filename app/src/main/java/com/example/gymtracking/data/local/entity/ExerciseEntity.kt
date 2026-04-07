package com.example.gymtracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "nome")
    val nome: String,
    @ColumnInfo(name = "categoria")
    val categoria: String,
    @ColumnInfo(name = "descrizione")
    val descrizione: String? = null,
    @ColumnInfo(name = "video_url")
    val videoUrl: String? = null
)

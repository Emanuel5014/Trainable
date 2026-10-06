package com.emanuel5014.trainable.data.local.entity

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
    val videoUrl: String? = null,
    /** File name (not a path) of the user's own image/GIF inside `ExerciseMediaStorage.dir`. */
    @ColumnInfo(name = "media_path")
    val mediaPath: String? = null
)

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
    val mediaPath: String? = null,
    /** The plate calculator is shown for this exercise (only while the global setting is on). */
    @ColumnInfo(name = "plate_calc", defaultValue = "0")
    val plateCalculator: Boolean = false,
    /** Weight of the bar (or sled) in kg, in storage units; null falls back to the default Olympic bar. */
    @ColumnInfo(name = "plate_bar_kg")
    val plateBarKg: Float? = null,
    /**
     * Deleted by the user while workouts, routines or maxes still refer to it: it is hidden from the exercise lists but
     * the row stays, so that history and routines keep their exercise (shown greyed out until it is restored).
     */
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
) {
    companion object {
        /** Built-in exercises have ids below this, the ones the user creates start from it. */
        const val FIRST_CUSTOM_ID = 1000
    }
}

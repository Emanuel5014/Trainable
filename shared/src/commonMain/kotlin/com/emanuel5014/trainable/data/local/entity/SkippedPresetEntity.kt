package com.emanuel5014.trainable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cardio preset (164-166) whose id was already taken by another exercise when this database learned it: it is left
 * out for good, the library sync never inserts it and no cardio log is linked to it. In the database so that a
 * migration can write it and a backup carries it.
 */
@Entity(tableName = "skipped_presets")
data class SkippedPresetEntity(
    @PrimaryKey
    val id: Int
)

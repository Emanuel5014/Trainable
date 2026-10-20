package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A tag the lifter puts on workouts ("Gym", "Left knee", "With Marco"). Tags are global, not per routine. */
@Entity(
    tableName = "session_tags",
    indices = [Index(value = ["name_key"], unique = true)]
)
data class SessionTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** As the lifter typed it (cleaned by `TagName.clean`). */
    val name: String,
    /** The name without case and accents (`TagName.key`): two names with the same key are the same tag. */
    @ColumnInfo(name = "name_key")
    val nameKey: String,
    /** Position in `TagPalette`; the actual colour comes from the theme. */
    @ColumnInfo(name = "color_index")
    val colorIndex: Int = 0,
    /** A colour the lifter picked freely (opaque ARGB); when set it wins over [colorIndex], which stays as the fallback. */
    @ColumnInfo(name = "custom_color")
    val customColor: Int? = null
)

package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A group of routines ("Winter bulk", "Cardio"): a program the lifter keeps together, also once archived. A routine
 * points to its group with `workout_plans.group_id`; a group no routine points to is removed.
 */
@Entity(tableName = "routine_groups")
data class RoutineGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    /** Place of the group in the routines list. */
    val ordine: Int = 0,
    /** Which theme colour the group's covers use (see `RoutineGroupColor`); the actual colour comes from the theme. */
    @ColumnInfo(name = "color_role")
    val colorRole: Int = 0,
    /** A colour the lifter picked freely (opaque ARGB); when set it wins over [colorRole]. */
    @ColumnInfo(name = "custom_color")
    val customColor: Int? = null
)

package com.emanuel5014.trainable.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity

data class SessionWithPlanName(
    @Embedded val session: WorkoutSessionEntity,
    @ColumnInfo(name = "plan_nome")
    val planNome: String,
    /** The plan is one of the app's own (quick workout, no routine): its name is not shown as is. */
    @ColumnInfo(name = "plan_is_system")
    val planIsSystem: Boolean = false
) {
    val displayName: String
        get() = session.noteSessione?.takeIf { it.isNotBlank() } ?: planNome
}

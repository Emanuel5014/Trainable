package com.emanuel5014.trainable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A tag on a workout. Deleting the workout or the tag deletes the link, never the other side. */
@Entity(
    tableName = "session_tag_refs",
    primaryKeys = ["session_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tag_id")]
)
data class SessionTagRefEntity(
    @ColumnInfo(name = "session_id")
    val sessionId: Int,
    @ColumnInfo(name = "tag_id")
    val tagId: Int
)

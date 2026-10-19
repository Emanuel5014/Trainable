package com.emanuel5014.trainable.data.local.relation

import com.emanuel5014.trainable.data.local.entity.SessionTagEntity

/** A tag with how much it is used: the number of workouts that carry it and the date of the latest one. */
data class TagWithUsage(
    val id: Int,
    val name: String,
    val nameKey: String,
    val colorIndex: Int,
    val customColor: Int?,
    val uses: Int,
    val lastUsed: Long?
) {
    fun toEntity() = SessionTagEntity(id = id, name = name, nameKey = nameKey, colorIndex = colorIndex, customColor = customColor)
}

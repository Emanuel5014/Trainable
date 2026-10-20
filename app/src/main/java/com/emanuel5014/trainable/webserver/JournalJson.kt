package com.emanuel5014.trainable.webserver

import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The workout journal of a workout for the local web API: `{"rating": 4, "tags": ["Gym"], "note": "..."}` with only
 * what is set, tags by name; null when the workout has no journal at all.
 */
fun journalJson(session: SessionWithDetails): JsonObject? {
    val rating = session.session.rating
    val tags = session.tags.map { it.name }.sortedBy { it.lowercase() }
    val note = session.session.journalNote?.takeIf { it.isNotBlank() }
    if (rating == null && tags.isEmpty() && note == null) return null

    return buildJsonObject {
        rating?.let { put("rating", it) }
        if (tags.isNotEmpty()) put("tags", buildJsonArray { tags.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        note?.let { put("note", it) }
    }
}

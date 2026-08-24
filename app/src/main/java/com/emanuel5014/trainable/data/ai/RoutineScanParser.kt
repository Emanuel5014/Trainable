package com.emanuel5014.trainable.data.ai

import kotlinx.serialization.json.Json

object RoutineScanParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): List<ParsedExercise> {
        val jsonText = extractJsonArray(raw) ?: return emptyList()
        return try {
            json.decodeFromString<List<ParsedExercise>>(jsonText).filter { it.name.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractJsonArray(raw: String): String? {
        var text = raw.trim()
        // Strip markdown code fences if present
        if (text.contains("```")) {
            text = text.substringAfter("```")
                .substringAfter("\n")
                .substringBefore("```")
                .trim()
        }
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        return text.substring(start, end + 1)
    }
}

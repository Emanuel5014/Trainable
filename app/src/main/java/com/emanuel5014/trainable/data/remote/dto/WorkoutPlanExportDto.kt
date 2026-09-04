package com.emanuel5014.trainable.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TRAINABLEPLAN_VERSION = 2

@Serializable
data class WorkoutPlanExportDto(
    val nome: String,
    val note: String?,
    val sessioniTargetSettimana: Int,
    val giorniSettimana: String? = null,
    val dataInizio: Long? = null,
    val dataFine: Long? = null,
    val imageUri: String?, // Keep for backward compatibility
    val images: List<String> = emptyList(),
    val imageBlobs: List<String> = emptyList(),
    val exercises: List<PlanExerciseExportDto>
)

@Serializable
data class PlanExerciseExportDto(
    val exerciseId: Int,
    val exerciseName: String? = null,
    val exerciseCategory: String? = null,
    val serieTarget: Int,
    val repsTarget: String,
    val recuperoTarget: Int,
    val ordine: Int,
    val supersetId: String? = null,
    val exerciseType: String = "strength",
    val durataTargetSecondi: Int? = null,
    val distanzaTargetKm: Float? = null,
    val cardioCategoria: String? = null
)

/**
 * Versioned envelope for .trainableplan files (v2+).
 * v1 files were a bare JSON list of [WorkoutPlanExportDto] (or a single object).
 */
@Serializable
data class TrainablePlanFile(
    val version: Int = TRAINABLEPLAN_VERSION,
    val plans: List<WorkoutPlanExportDto> = emptyList()
)

object TrainablePlanParser {
    private val json = Json {
        ignoreUnknownKeys = true
        // Force defaults (incl. "version") into the output: Json.Default omits
        // values equal to their defaults, which previously produced envelopes
        // without the "version" key.
        encodeDefaults = true
    }

    fun encode(plans: List<WorkoutPlanExportDto>): String =
        json.encodeToString(TrainablePlanFile.serializer(), TrainablePlanFile(plans = plans))

    /**
     * Parses v2 envelope (with or without the "version" key), v1 bare list
     * and v1 single object.
     * @throws IllegalArgumentException if no plan can be decoded.
     */
    fun decode(jsonData: String): List<WorkoutPlanExportDto> {
        val trimmed = jsonData.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("Empty file")
        // v2 envelope: object containing a "plans" array. Detected via "plans"
        // (not "version", which may be absent in early v2 files).
        if (trimmed.startsWith("{") && trimmed.contains("\"plans\"")) {
            try {
                return json.decodeFromString<TrainablePlanFile>(trimmed).plans
            } catch (_: Exception) {
                // fall through to legacy formats
            }
        }
        try {
            return json.decodeFromString<List<WorkoutPlanExportDto>>(trimmed)
        } catch (_: Exception) {
            // fall through
        }
        // Legacy single-plan object (throws with a meaningful error if invalid)
        val single = json.decodeFromString<WorkoutPlanExportDto>(trimmed)
        return listOf(single)
    }
}

package com.emanuel5014.trainable.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParsedExercise(
    val name: String = "",
    val sets: Int = 3,
    val reps: String = "8-12",
    @SerialName("rest_seconds") val restSeconds: Int = 120,
    @SerialName("cardio_minutes") val cardioMinutes: Int? = null,
    val category: String? = null,
    @SerialName("exercise_type") val exerciseType: String? = null,
    @SerialName("time_seconds") val timeSeconds: Int? = null,
    /** Literal powerlifting notation for the exercise, e.g. `70% 3x3 STOP 2" 75% 3x2` or multi-week `W1: … W2: …`. */
    val notation: String? = null,
    @SerialName("one_rep_max_kg") val oneRepMaxKg: Float? = null,
    /** The sheet says the prescription lives on a separate program sheet ("PROGRAMMAZIONE"). */
    val programmed: Boolean = false
)

enum class AiModelVariant(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeLabel: String,
    val requiredRamGb: Int
) {
    E2B(
        id = "e2b",
        displayName = "Gemma 4 E2B",
        fileName = "gemma-4-E2B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        sizeLabel = "~2.4 GB",
        requiredRamGb = 6
    ),
    E4B(
        id = "e4b",
        displayName = "Gemma 4 E4B",
        fileName = "gemma-4-E4B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        sizeLabel = "~3.4 GB",
        requiredRamGb = 8
    );

    companion object {
        fun fromId(id: String?): AiModelVariant = entries.firstOrNull { it.id == id } ?: E2B
    }
}

sealed interface AiModelStatus {
    data object NotDownloaded : AiModelStatus
    data class Downloading(val progress: Float) : AiModelStatus
    data object Ready : AiModelStatus
    data class Error(val message: String) : AiModelStatus
}

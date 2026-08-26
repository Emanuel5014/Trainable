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
    val category: String? = null
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

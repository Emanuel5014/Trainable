package com.emanuel5014.trainable.data.ai

import android.net.Uri
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedExerciseEntry(
    val rawName: String,
    val exerciseId: Int?,
    val matchedName: String?,
    val suggestedCategory: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val cardioMinutes: Int?
) {
    val isCardio: Boolean get() = cardioMinutes != null
}

object RoutineScanPrompt {

    fun build(languageCode: String, categories: List<String> = emptyList()): String = """
You are an assistant that reads photos of gym workout programs (training sheets).
Look at the image and extract the list of exercises exactly as written.

Rules:
- For each exercise extract: name, number of sets, reps target (e.g. "8-12" or "12"), rest in seconds (if written like "90s", "1'30\"" or "2 min" convert to seconds; use 120 if not specified).
- If an entry is a cardio activity (treadmill, bike, rowing, etc.) set "cardio_minutes" to the duration in minutes and reps to "1"; otherwise "cardio_minutes" must be null.
- For "category": classify each exercise into the muscle group / type it primarily targets. Use one of these exact values when it fits: ${categories.joinToString(", ") { "\"$it\"" }}. If none fits well, pick the closest muscle group in English (e.g. "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio").
- Do NOT invent exercises that are not visible. If the sheet is unreadable or contains no program, return an empty array [].
- Answer with a valid JSON array ONLY, no explanations, no markdown formatting.

JSON schema:
[{"name": "...", "sets": 3, "reps": "8-12", "rest_seconds": 120, "cardio_minutes": null, "category": "..."}]
""".trim()
}

enum class ScanPhase {
    LOADING_MODEL,
    READING_SHEET,
    PARSING
}

@Singleton
class RoutineScanner @Inject constructor(
    private val engine: LocalLlmEngine,
    private val modelFileManager: ModelFileManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend fun scan(
        imageUri: Uri,
        catalog: List<ExerciseEntity>,
        categories: List<String> = emptyList(),
        languageCode: String,
        onPhase: (ScanPhase) -> Unit = {},
        onStreamUpdate: (partialOutput: String, thinkingOutput: String) -> Unit = { _, _ -> }
    ): List<ScannedExerciseEntry> {
        val variantId = userPreferencesRepository.aiModelVariant.first()
        val variant = AiModelVariant.fromId(variantId)
        val modelFile = modelFileManager.getModelFile(variant)
        check(modelFileManager.isDownloaded(variant)) { "AI model not downloaded" }

        onPhase(ScanPhase.LOADING_MODEL)
        engine.ensureReady(modelFile)

        onPhase(ScanPhase.READING_SHEET)
        val response = engine.scanRoutineSheet(
            imageUri = imageUri,
            prompt = RoutineScanPrompt.build(languageCode, categories),
            onStreamUpdate = onStreamUpdate
        )

        onPhase(ScanPhase.PARSING)
        val parsed = RoutineScanParser.parse(response)

        val matcher = ExerciseMatcher(catalog, languageCode)
        return parsed.map { item ->
            val match = matcher.resolve(item.name)
            // Category priority: catalog match > LLM classification (mapped to a
            // known category) > empty (falls back to "Custom" when applying)
            val suggestedCategory = match?.categoria
                ?: item.category?.let { ExerciseMatcher.mapToKnownCategory(it, categories) }
                ?: ""
            ScannedExerciseEntry(
                rawName = item.name,
                exerciseId = match?.id,
                matchedName = match?.nome,
                suggestedCategory = suggestedCategory,
                sets = item.sets.coerceIn(1, 30),
                reps = item.reps.ifBlank { "8-12" },
                restSeconds = item.restSeconds.coerceAtLeast(0),
                cardioMinutes = item.cardioMinutes?.takeIf { it > 0 }
            )
        }
    }
}

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
    val cardioMinutes: Int?,
    val exerciseType: String = "strength",
    val timeSeconds: Int? = null
) {
    val isCardio: Boolean get() = exerciseType == "cardio" || cardioMinutes != null
    val isTimeAndWeight: Boolean get() = exerciseType == "time_and_weight"
}

object RoutineScanPrompt {

    fun build(languageCode: String, categories: List<String> = emptyList()): String {
        val categoriesStr = if (categories.isNotEmpty()) {
            categories.joinToString(", ") { "\"$it\"" }
        } else {
            "\"Petto\", \"Dorso\", \"Gambe\", \"Spalle\", \"Braccia\", \"Addome\", \"Cardio\""
        }

        return """
You are an expert fitness AI specialized in reading and extracting gym workout routines / training cards from images.
Analyze the provided image and extract ALL exercises, sets, repetitions, recovery rest, and cardio duration.

EXTRACTION INSTRUCTIONS:
- name: The exercise name as visible on the card (e.g. "Panca Piana", "Lat Machine", "Squat", "Leg Press", "Curl Manubri", "Alzate Laterali", "Spinte Manubri", "Pulley", "Croci", "Plank"). If gym shorthand is used (e.g. "P. Piana", "Lat Mach.", "Press 45", "Alz. Lat.", "Trazioni alla sbarra"), expand into the full recognizable exercise name.
- sets: Total number of sets (integer, e.g. 3, 4, 5).
  * If a pyramidal scheme is listed (e.g. "8-6-4-2" or "12-10-8-6"), count the stages (e.g. 4 numbers = 4 sets).
  * If written as "4x 8-6-4-2" or "4x8" or "3x10-12", sets is the first number (e.g. 4 or 3).
  * If unspecified, default to 3.
- reps: Repetitions target as a string. CRITICAL ACCURACY RULES:
  * PYRAMIDAL / MULTI-SET SCHEMES: If an exercise has a descending or ascending series of numbers like "8-6-4-2", "12-10-8-6", "10-8-6-4", "4-6-8-10", "12-10-8", "8-6-4", you MUST extract the COMPLETE string with ALL numbers (e.g. "8-6-4-2"). NEVER truncate or shorten it to only two numbers (do NOT write "8-6" instead of "8-6-4-2").
  * RANGES: e.g. "8-12", "8-10", "10-12", "6-8", "12-15".
  * FIXED REPS: e.g. "10", "12", "8".
  * SPECIAL TECHNIQUES: e.g. "6+6+6" (stripping / drop set), "MAX" (to failure).
  * Format multi-number sequences using dashes (e.g. "8-6-4-2", not slashes or commas).
  * TIME-BASED / ISOMETRIC: For timed or isometric exercises (e.g. Plank, Wall Sit, Hollow Body, Farmer's Walk, or any hold with duration like "3x45s", "30\"", "1 min"), write the duration with 's' (e.g. "45s", "60s", "30s").
- exercise_type: The exercise type:
  * "strength": Standard weight & repetition exercises (default).
  * "time_and_weight": Isometric, timed, or hold exercises where performance is measured by seconds / time (e.g. Plank, Wall Sit, Barbell Hold, Farmer's Walk, or exercises with duration in seconds like "45s", "60\"").
  * "cardio": Aerobic cardio machines (Tapis Roulant, Cyclette, Ellittica, etc.).
- time_seconds: For "time_and_weight" exercises, target duration in seconds as integer (e.g. 45, 60, 30); null for other types.
- rest_seconds: Rest time in seconds (integer). Convert "90s", "1'30\"", "2 min", "90\"", "1 min 30 s", "2'" into total seconds (e.g. 90, 120). Default to 120 if not specified.
- cardio_minutes: If the entry is a cardio activity (Tapis Roulant / Treadmill, Cyclette / Bike, Ellittica / Elliptical, Vogatore / Rower, Stairmaster, Cyclette Recline), duration in minutes (e.g. 20) and set reps to "1"; otherwise null.
- category: The target muscle group category (choose from: $categoriesStr).

OUTPUT FORMAT RULES:
- Output MUST be ONLY a valid JSON array of objects.
- Do NOT output extra conversational text, commentary, or markdown outside the JSON block.

JSON Schema Example:
[
  {
    "name": "Panca Piana",
    "sets": 4,
    "reps": "8-6-4-2",
    "rest_seconds": 90,
    "exercise_type": "strength",
    "time_seconds": null,
    "cardio_minutes": null,
    "category": "Petto"
  },
  {
    "name": "Plank",
    "sets": 3,
    "reps": "60s",
    "rest_seconds": 60,
    "exercise_type": "time_and_weight",
    "time_seconds": 60,
    "cardio_minutes": null,
    "category": "Addome"
  },
  {
    "name": "Lat Machine",
    "sets": 4,
    "reps": "12-10-8-6",
    "rest_seconds": 90,
    "exercise_type": "strength",
    "time_seconds": null,
    "cardio_minutes": null,
    "category": "Dorso"
  },
  {
    "name": "Tapis Roulant",
    "sets": 1,
    "reps": "1",
    "rest_seconds": 0,
    "exercise_type": "cardio",
    "time_seconds": null,
    "cardio_minutes": 20,
    "category": "Cardio"
  }
]
""".trim()
    }
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

        try {
            onPhase(ScanPhase.LOADING_MODEL)
            engine.ensureReady(modelFile)

            onPhase(ScanPhase.READING_SHEET)
            val result = engine.scanRoutineSheet(
                imageUri = imageUri,
                prompt = RoutineScanPrompt.build(languageCode, categories),
                onStreamUpdate = onStreamUpdate
            )

            onPhase(ScanPhase.PARSING)
            // 1. Try parsing from main output
            var parsed = RoutineScanParser.parse(result.output)

            // 2. Fallback: if main output didn't yield exercises, try parsing from thinking channel
            if (parsed.isEmpty() && result.thinking.isNotBlank()) {
                parsed = RoutineScanParser.parse(result.thinking)
            }

            // 3. Fallback: try parsing combined output and thinking
            if (parsed.isEmpty() && (result.output.isNotBlank() || result.thinking.isNotBlank())) {
                parsed = RoutineScanParser.parse("${result.output}\n${result.thinking}")
            }

            val matcher = ExerciseMatcher(catalog, languageCode)
            return parsed.map { item ->
                val match = matcher.resolve(item.name)
                // Category priority: catalog match > LLM classification (mapped to a known category) > heuristic inference
                val suggestedCategory = match?.categoria
                    ?: item.category?.let { ExerciseMatcher.mapToKnownCategory(it, categories) }
                    ?: matcher.suggestCategory(item.name, categories)

                val resolvedType = when {
                    item.cardioMinutes != null || item.exerciseType == "cardio" -> "cardio"
                    item.exerciseType == "time_and_weight" || item.timeSeconds != null || item.reps.endsWith("s", ignoreCase = true) -> "time_and_weight"
                    else -> "strength"
                }
                val resolvedTimeSeconds = if (resolvedType == "time_and_weight") {
                    item.timeSeconds ?: item.reps.filter { it.isDigit() }.toIntOrNull() ?: 45
                } else null

                ScannedExerciseEntry(
                    rawName = item.name,
                    exerciseId = match?.id,
                    matchedName = match?.nome,
                    suggestedCategory = suggestedCategory,
                    sets = item.sets.coerceIn(1, 30),
                    reps = if (resolvedType == "time_and_weight" && !item.reps.endsWith("s")) "${resolvedTimeSeconds ?: 45}s" else item.reps.ifBlank { "8-12" },
                    restSeconds = item.restSeconds.coerceAtLeast(0),
                    cardioMinutes = item.cardioMinutes?.takeIf { it > 0 },
                    exerciseType = resolvedType,
                    timeSeconds = resolvedTimeSeconds
                )
            }
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                engine.release()
            }
        }
    }

    suspend fun release() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            engine.release()
        }
    }
}


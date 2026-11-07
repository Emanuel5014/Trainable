package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.ai.backend.ScanBackendProvider
import com.emanuel5014.trainable.data.ai.backend.ScanBackendRunner
import com.emanuel5014.trainable.data.ai.backend.ScanRequest
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import android.net.Uri
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionNotationParser
import com.emanuel5014.trainable.domain.prescription.Technique
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineScanner @Inject constructor(
    private val backends: ScanBackendProvider,
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
        val advanced = userPreferencesRepository.advancedProgrammingEnabled.first()

        val result = ScanBackendRunner.execute(
            backend = backends.current(),
            request = ScanRequest(
                imageUri = imageUri,
                prompt = RoutineScanPrompt.build(languageCode, categories, advanced),
                imageMode = ScanImageMode.FULL_PAGE,
                transcription = false,
                maxOutputTokens = null
            ),
            onPhase = onPhase,
            onStreamUpdate = onStreamUpdate
        )

        onPhase(ScanPhase.PARSING)
        return RoutineScanResults.entries(result, catalog, categories, languageCode, advanced)
    }

    /**
     * Reads the program page of a single exercise (weeks of %1RM blocks) from a photo.
     * @return null when nothing that looks like a prescription could be read.
     */
    suspend fun scanProgram(
        imageUri: Uri,
        exerciseName: String?,
        onPhase: (ScanPhase) -> Unit = {},
        onStreamUpdate: (partialOutput: String, thinkingOutput: String) -> Unit = { _, _ -> }
    ): ScannedProgram? {
        val result = ScanBackendRunner.execute(
            backend = backends.current(),
            request = ScanRequest(
                imageUri = imageUri,
                prompt = ProgramScanPrompt.build(),
                imageMode = ScanImageMode.HANDWRITING_BLOCK,
                transcription = true,
                maxOutputTokens = PROGRAM_MAX_OUTPUT_TOKENS
            ),
            onPhase = onPhase,
            onStreamUpdate = onStreamUpdate
        )

        onPhase(ScanPhase.PARSING)
        return RoutineScanResults.program(result, exerciseName)
    }

    internal fun splitNameQualifiers(name: String) = RoutineScanResults.splitNameQualifiers(name)

    private companion object {
        /** A whole program page is a few dozen tokens; anything longer is the model running on. */
        const val PROGRAM_MAX_OUTPUT_TOKENS = 1536
    }

    suspend fun release() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            backends.releaseLocal()
        }
    }
}


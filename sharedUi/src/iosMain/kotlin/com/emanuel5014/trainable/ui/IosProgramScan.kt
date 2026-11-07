package com.emanuel5014.trainable.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.data.ai.ProgramScanPrompt
import com.emanuel5014.trainable.data.ai.RoutineScanResults
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedProgram
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_program_scan_failed
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.ProgramScanApplication
import com.emanuel5014.trainable.ui.components.ProgramScanButton
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanBackend
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanModel
import com.emanuel5014.trainable.ui.screens.routines.ProgramScanState
import com.emanuel5014.trainable.ui.screens.settings.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IosProgramScanButton(
    exerciseName: String?,
    singleWeek: Boolean,
    currentOneRepMaxKg: Float?,
    weightUnit: String,
    roundingIncrement: Float,
    onApply: (ProgramScanApplication) -> Unit,
    modifier: Modifier,
    scanKey: String?
) {
    key(scanKey, exerciseName) {
        val scope = rememberCoroutineScope()
        val model = remember { ProgramScanModel<Nothing>(IosProgramScanBackend, scope, Dispatchers.IO) }
        DisposableEffect(model) { onDispose { model.close() } }
        val state by model.state.collectAsState()
        KeepIosScanScreenOn(state is ProgramScanState.Scanning)
        val show = LocalShowMessage.current
        val failureText = stringResource(Res.string.ai_program_scan_failed)
        val capture = rememberIosScanPhotoCapture(
            onPhoto = { model.scan(it, exerciseName) },
            onFailure = { show(failureText) }
        )
        ProgramScanButton(
            model = model,
            exerciseName = exerciseName,
            singleWeek = singleWeek,
            currentOneRepMaxKg = currentOneRepMaxKg,
            weightUnit = weightUnit,
            roundingIncrement = roundingIncrement,
            isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f,
            takePhoto = capture.takePhoto,
            pickPhoto = capture.pickPhotos,
            showError = { failed -> show(failed.failure?.userMessage() ?: failureText) },
            onApply = onApply,
            modifier = modifier,
            progressDialogProperties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                usePlatformInsets = false
            )
        )
    }
}

private object IosProgramScanBackend : ProgramScanBackend {
    override suspend fun consentRequired() = IosRemoteScan.consentRequired()
    override suspend fun acceptConsent(request: ConsentRequest) = IosRemoteScan.acceptConsent(request)

    override suspend fun scan(
        imageUri: String,
        exerciseName: String?,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): ScannedProgram? {
        val result = IosRemoteScan.read(
            imagePath = imageUri,
            prompt = ProgramScanPrompt.build(),
            transcription = true,
            maxOutputTokens = ProgramScanPrompt.MAX_OUTPUT_TOKENS,
            onPhase = onPhase,
            onStreamUpdate = onStreamUpdate
        )
        onPhase(ScanPhase.PARSING)
        return RoutineScanResults.program(result, exerciseName)
    }
}

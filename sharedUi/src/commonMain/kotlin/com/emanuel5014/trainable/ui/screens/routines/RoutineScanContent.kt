package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ai.ScanDraft
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_scan_continue
import com.emanuel5014.trainable.sharedui.resources.ai_scan_hide_thinking
import com.emanuel5014.trainable.sharedui.resources.ai_scan_phase_connecting
import com.emanuel5014.trainable.sharedui.resources.ai_scan_phase_generating
import com.emanuel5014.trainable.sharedui.resources.ai_scan_phase_model
import com.emanuel5014.trainable.sharedui.resources.ai_scan_phase_reading
import com.emanuel5014.trainable.sharedui.resources.ai_scan_show_thinking
import com.emanuel5014.trainable.sharedui.resources.ai_scan_source_title
import com.emanuel5014.trainable.sharedui.resources.ai_scan_stop
import com.emanuel5014.trainable.sharedui.resources.ai_scan_stop_confirm_desc
import com.emanuel5014.trainable.sharedui.resources.ai_scan_stop_confirm_title
import com.emanuel5014.trainable.sharedui.resources.ai_scan_thinking_placeholder
import com.emanuel5014.trainable.sharedui.resources.ai_scanning
import com.emanuel5014.trainable.sharedui.resources.camera
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.gallery
import com.emanuel5014.trainable.sharedui.resources.scan_draft_discard
import com.emanuel5014.trainable.sharedui.resources.scan_draft_discard_desc
import com.emanuel5014.trainable.sharedui.resources.scan_draft_discard_title
import com.emanuel5014.trainable.sharedui.resources.scan_draft_resume
import com.emanuel5014.trainable.sharedui.resources.scan_draft_subtitle
import com.emanuel5014.trainable.sharedui.resources.scan_draft_title
import com.emanuel5014.trainable.ui.components.GlowProgressOverlay
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.shared.ScanConsentDialog
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Shared scan entry, draft banner and review, with the platform's photo sources and error presentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <Metrics> rememberRoutineScanUi(
    model: RoutineScanModel<Metrics>,
    languageCode: String,
    photo: ScanPhotoUi,
    takePhoto: () -> Unit,
    pickPhoto: () -> Unit,
    savePhoto: (String) -> String?,
    showError: suspend (AiScanState.Error) -> Unit,
    resourceAnalytics: @Composable (Metrics, Boolean) -> Unit = { _, _ -> }
): RoutineScanUi {
    val uiState by model.detail.uiState.collectAsState()
    val aiScanAvailable by model.aiScanAvailable.collectAsState()
    val scanConsent by model.consent.collectAsState()
    val aiScanState by model.aiScanState.collectAsState()
    val scanDraft by model.scanDraft.collectAsState()
    var showDiscardDraftDialog by remember { mutableStateOf(false) }
    var showScanSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(aiScanState) {
        val state = aiScanState
        if (state is AiScanState.Error) {
            showError(state)
            model.dismissScanResult()
        }
    }

    scanConsent?.let { request ->
        ScanConsentDialog(request, model::acceptConsent, model::declineConsent)
    }

    val isScanActive = aiScanState is AiScanState.Scanning
    return object : RoutineScanUi {
        override val available: Boolean get() = aiScanAvailable
        override val isScanning: Boolean get() = isScanActive
        override val showDraftBanner: Boolean get() = scanDraft != null && aiScanState is AiScanState.Idle

        @Composable
        override fun DraftBanner(modifier: Modifier) {
            val draft = scanDraft ?: return
            ScanDraftBanner(
                draft = draft,
                onResume = { model.resumeScanDraft() },
                onDiscard = { showDiscardDraftDialog = true },
                modifier = modifier
            )
        }

        override fun openSource() {
            showScanSourceSheet = true
        }

        @Composable
        override fun Overlays(hazeState: HazeState, isDark: Boolean, viewWeek: Int) {
            if (showScanSourceSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showScanSourceSheet = false },
                    containerColor = Surface,
                    contentColor = OnSurface,
                    tonalElevation = 0.dp,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(width = 32.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(OnSurfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.ai_scan_source_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ScanOptionItem(
                                icon = Icons.Rounded.PhotoCamera,
                                label = stringResource(Res.string.camera),
                                onClick = {
                                    showScanSourceSheet = false
                                    takePhoto()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ScanOptionItem(
                                icon = Icons.Rounded.PhotoLibrary,
                                label = stringResource(Res.string.gallery),
                                onClick = {
                                    showScanSourceSheet = false
                                    pickPhoto()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (showDiscardDraftDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDraftDialog = false },
                    containerColor = SurfaceContainerHigh,
                    title = { Text(stringResource(Res.string.scan_draft_discard_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                    text = { Text(stringResource(Res.string.scan_draft_discard_desc), color = OnSurfaceVariant) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscardDraftDialog = false
                            model.discardScanDraft()
                        }) { Text(stringResource(Res.string.scan_draft_discard).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDraftDialog = false }) {
                            Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                        }
                    }
                )
            }

            when (val state = aiScanState) {
                is AiScanState.Scanning -> {
                    val scanStream by model.aiScanStream.collectAsState()
                    ScanProgressOverlay(
                        phase = state.phase,
                        stream = scanStream,
                        isDark = isDark,
                        hazeState = hazeState,
                        onCancel = model::cancelAiScan
                    ) {
                        scanStream.metrics?.let { resourceAnalytics(it, isDark) }
                    }
                }
                is AiScanState.Success -> {
                    AiScanPreviewSheet(
                        imageUri = state.imageUri,
                        photo = photo,
                        entries = state.entries,
                        exercises = uiState.availableExercises,
                        categories = uiState.categories,
                        languageCode = languageCode,
                        onAddCustomExercise = { nome, categoria, onCreated ->
                            model.detail.addCustomExercise(nome, categoria, onCreated)
                        },
                        onConfirm = { entries, saveImage ->
                            if (saveImage) {
                                state.imageUri?.let(savePhoto)?.let(model.detail::addPlanImage)
                            }
                            model.applyScannedExercises(entries, targetWeek = viewWeek)
                        },
                        onDismiss = { model.dismissScanResult() },
                        onEntriesChanged = { model.saveScanDraft(it) }
                    )
                }
                else -> {}
            }
        }
    }
}


@Composable
fun ScanProgressOverlay(
    phase: ScanPhase,
    stream: RoutineScanStream<*>,
    isDark: Boolean,
    /** Null when the overlay lives in its own window (no blurred content behind it): a plain scrim is used. */
    hazeState: HazeState?,
    onCancel: () -> Unit,
    resourceAnalytics: @Composable () -> Unit = {}
) {
    var showCancelConfirmation by remember { mutableStateOf(false) }

    PlatformBackHandler {
        showCancelConfirmation = true
    }

    var showThinking by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    GlowProgressOverlay(isDark = isDark, hazeState = hazeState) {
        GymLoadingIndicator()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(Res.string.ai_scanning),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)
                ),
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = aiPhaseLabel(phase) + " · ${elapsedSeconds}s",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Primary,
            trackColor = SurfaceContainerHighest
        )

        resourceAnalytics()

        TextButton(onClick = { showThinking = !showThinking }) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showThinking) stringResource(Res.string.ai_scan_hide_thinking)
                else stringResource(Res.string.ai_scan_show_thinking),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        AnimatedVisibility(visible = showThinking) {
            val streamText = buildString {
                if (stream.thinking.isNotBlank()) append(stream.thinking)
                if (stream.output.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(stream.output)
                }
            }

            Column {
                HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(com.emanuel5014.trainable.ui.theme.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (streamText.isBlank()) {
                        Text(
                            text = stringResource(Res.string.ai_scan_thinking_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = streamText,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showCancelConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            border = BorderStroke(1.dp, Error.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.ai_scan_stop),
                color = Error,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = {
                Text(
                    text = stringResource(Res.string.ai_scan_stop_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.ai_scan_stop_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        onCancel()
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.ai_scan_stop).uppercase(),
                        color = Error,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(
                        text = stringResource(Res.string.ai_scan_continue).uppercase(),
                        color = Primary
                    )
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = Shapes.extraLarge
        )
    }
}

@Composable
private fun aiPhaseLabel(phase: ScanPhase): String =
    when (phase) {
        ScanPhase.LOADING_MODEL -> stringResource(Res.string.ai_scan_phase_model)
        ScanPhase.CONNECTING -> stringResource(Res.string.ai_scan_phase_connecting)
        ScanPhase.READING_SHEET -> stringResource(Res.string.ai_scan_phase_reading)
        ScanPhase.PARSING -> stringResource(Res.string.ai_scan_phase_generating)
    }

@Composable
fun ScanOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = OnSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** Reminder that a scanned routine is waiting to be reviewed; shown until it is imported or discarded. */
@Composable
private fun ScanDraftBanner(
    draft: ScanDraft,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.large)
            .background(Primary.copy(alpha = 0.12f))
            .clickable(onClick = onResume)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.scan_draft_title),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(Res.string.scan_draft_subtitle, draft.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        TextButton(onClick = onResume) {
            Text(stringResource(Res.string.scan_draft_resume).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
        }
        IconButton(onClick = onDiscard) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.scan_draft_discard), tint = OnSurfaceVariant)
        }
    }
}

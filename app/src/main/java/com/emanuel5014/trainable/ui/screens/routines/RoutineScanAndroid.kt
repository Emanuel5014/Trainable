package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.ui.theme.actionGlass
import com.emanuel5014.trainable.ui.theme.actionGlassContainer
import com.emanuel5014.trainable.ui.theme.actionGlassElevation
import com.emanuel5014.trainable.ui.theme.actionGlassSource
import com.emanuel5014.trainable.ui.theme.rememberActionGlassSource
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseTypeMemory
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.RepsDraft
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.StartingWeight
import com.emanuel5014.trainable.ui.components.NumberStepper
import com.emanuel5014.trainable.ui.components.AdvancedPrescriptionEditor
import com.emanuel5014.trainable.ui.components.WeekEditing
import com.emanuel5014.trainable.domain.prescription.WeekShift
import com.emanuel5014.trainable.ui.components.OneRepMaxCard
import com.emanuel5014.trainable.ui.components.OneRepMaxDialog
import com.emanuel5014.trainable.ui.components.PrescriptionBlocksEditor
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.WeekSelector
import com.emanuel5014.trainable.ui.components.ExerciseTypeSelector
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.ExerciseEntryCard
import com.emanuel5014.trainable.ui.components.ExerciseBadgeSize
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.RestoreExerciseDialog
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.components.GlowProgressOverlay
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.RoutineImagePicker
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.TargetSecondsSlider
import com.emanuel5014.trainable.ui.components.exercise.TimeAndRepsEditor
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.ui.components.exercise.RepsEditor
import com.emanuel5014.trainable.ui.components.exercise.StartingWeightField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.headerGlassOn
import com.emanuel5014.trainable.ui.theme.headerGroupGlass
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * The AI scan of a routine sheet on the shared routine screen (RoutineDetailContent): the camera and gallery, the
 * consent, the scanning overlay, the review sheet and the saved draft, all from [viewModel].
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun rememberAndroidRoutineScanUi(viewModel: RoutineDetailViewModel): RoutineScanUi {
    val context = LocalContext.current
    val uiState by viewModel.state.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val aiScanAvailable by viewModel.aiScanAvailable.collectAsState()
    val scanConsent by viewModel.consent.collectAsState()
    val aiResourceAnalyticsEnabled by viewModel.aiResourceAnalyticsEnabled.collectAsState()
    val aiScanState by viewModel.aiScanState.collectAsState()
    val scanDraft by viewModel.scanDraft.collectAsState()
    var showDiscardDraftDialog by remember { mutableStateOf(false) }
    var showScanSourceSheet by remember { mutableStateOf(false) }

    var scanTempImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val scanCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) scanTempImageUri?.let { viewModel.scanRoutineSheet(it) }
    }
    val scanGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.scanRoutineSheet(it) } }
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createScanTempImageUri(context)
            scanTempImageUri = uri
            scanCameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleScanCameraClick() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createScanTempImageUri(context)
            scanTempImageUri = uri
            scanCameraLauncher.launch(uri)
        } else {
            scanPermissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(aiScanState) {
        val state = aiScanState
        if (state is AiScanState.Error) {
            Toast.makeText(
                context,
                state.failure?.userText(context)
                    ?: if (state.message == null) context.getString(R.string.ai_scan_no_exercises)
                    else context.getString(R.string.ai_scan_failed),
                Toast.LENGTH_LONG
            ).show()
            viewModel.dismissScanResult()
        }
    }

    scanConsent?.let { request ->
        com.emanuel5014.trainable.ui.components.shared.ScanConsentDialog(
            request = request,
            onAccept = viewModel::acceptConsent,
            onDecline = viewModel::declineConsent
        )
    }

    val isScanActive = aiScanState is AiScanState.Scanning
    // AI scan (LLM inference) can take a while: keep the screen on for its whole duration.
    // Always on, no setting. Released when scanning ends or the screen is left.
    val scanWindow = (context as? Activity)?.window
    DisposableEffect(isScanActive) {
        if (isScanActive) {
            scanWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            scanWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            scanWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    return object : RoutineScanUi {
        override val available: Boolean get() = aiScanAvailable
        override val isScanning: Boolean get() = isScanActive
        override val showDraftBanner: Boolean get() = scanDraft != null && aiScanState is AiScanState.Idle

        @Composable
        override fun DraftBanner(modifier: Modifier) {
            ScanDraftBanner(
                draft = scanDraft!!,
                onResume = { viewModel.resumeScanDraft() },
                onDiscard = { showDiscardDraftDialog = true },
                modifier = modifier
            )
        }

        override fun openSource() {
            showScanSourceSheet = true
        }

        @Composable
        override fun Overlays(hazeState: HazeState, isDark: Boolean, viewWeek: Int) {
            val scanHazeState = hazeState
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
                            text = stringResource(R.string.ai_scan_source_title),
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
                                label = stringResource(R.string.camera),
                                onClick = {
                                    showScanSourceSheet = false
                                    handleScanCameraClick()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ScanOptionItem(
                                icon = Icons.Rounded.PhotoLibrary,
                                label = stringResource(R.string.gallery),
                                onClick = {
                                    showScanSourceSheet = false
                                    scanGalleryLauncher.launch("image/*")
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
                    title = { Text(stringResource(R.string.scan_draft_discard_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                    text = { Text(stringResource(R.string.scan_draft_discard_desc), color = OnSurfaceVariant) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscardDraftDialog = false
                            viewModel.discardScanDraft()
                        }) { Text(stringResource(R.string.scan_draft_discard).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDraftDialog = false }) {
                            Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                        }
                    }
                )
            }

            when (val state = aiScanState) {
                is AiScanState.Scanning -> {
                    val scanStream by viewModel.aiScanStream.collectAsState()
                    AiScanningOverlay(
                        phase = state.phase,
                        stream = scanStream,
                        showResourceAnalytics = aiResourceAnalyticsEnabled,
                        isDark = isDark,
                        hazeState = scanHazeState,
                        onCancel = { viewModel.cancelAiScan() }
                    )
                }
                is AiScanState.Success -> {
                    AiScanPreviewSheet(
                        imageUri = state.imageUri?.toString(),
                        photo = AndroidScanPhoto,
                        entries = state.entries,
                        exercises = uiState.availableExercises,
                        categories = uiState.categories,
                        languageCode = languageCode,
                        onAddCustomExercise = { nome, categoria, onCreated ->
                            viewModel.state.addCustomExercise(nome, categoria, onCreated)
                        },
                        onConfirm = { entries, saveImage ->
                            if (saveImage && state.imageUri != null) {
                                val savedPath = ImageStorageUtils.compressAndSaveImage(context, state.imageUri)
                                if (savedPath != null) {
                                    viewModel.state.addPlanImage(savedPath)
                                }
                            }
                            viewModel.applyScannedExercises(entries, targetWeek = viewWeek)
                        },
                        onDismiss = { viewModel.dismissScanResult() },
                        onEntriesChanged = { viewModel.saveScanDraft(it) }
                    )
                }
                else -> {}
            }
        }
    }
}

internal fun createScanTempImageUri(context: android.content.Context): android.net.Uri {
    val tempFile = java.io.File(context.cacheDir, "ai_scan_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
internal fun AiScanningOverlay(
    phase: com.emanuel5014.trainable.data.ai.ScanPhase,
    stream: AiScanStreamState,
    showResourceAnalytics: Boolean = false,
    isDark: Boolean,
    /** Null when the overlay lives in its own window (no blurred content behind it): a plain scrim is used. */
    hazeState: HazeState?,
    onCancel: () -> Unit
) {
    var showCancelConfirmation by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
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
                text = stringResource(R.string.ai_scanning),
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

        if (showResourceAnalytics && stream.metrics != null) {
            DeviceResourceAnalyticsCard(
                metrics = stream.metrics,
                isDark = isDark
            )
        }

        TextButton(onClick = { showThinking = !showThinking }) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showThinking) stringResource(R.string.ai_scan_hide_thinking)
                else stringResource(R.string.ai_scan_show_thinking),
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
                            text = stringResource(R.string.ai_scan_thinking_placeholder),
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
            border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_scan_stop),
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
                    text = stringResource(R.string.ai_scan_stop_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ai_scan_stop_confirm_desc),
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
                        text = stringResource(R.string.ai_scan_stop).uppercase(),
                        color = Error,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(
                        text = stringResource(R.string.ai_scan_continue).uppercase(),
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
private fun DeviceResourceAnalyticsCard(
    metrics: com.emanuel5014.trainable.data.ai.DeviceResourceMetrics,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerHighest.copy(alpha = if (isDark) 0.6f else 0.75f),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_analytics_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }

                // Backend / Model tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = metrics.inferenceBackend,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // CPU Usage Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.ai_analytics_cpu)} (${metrics.cpuCores} Core)",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${metrics.cpuUsagePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (metrics.cpuUsagePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (metrics.cpuUsagePercent > 85) Error else Primary,
                    trackColor = SurfaceContainer
                )
            }

            // RAM Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.ai_analytics_ram),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${metrics.appRamUsedMb} MB App · ${"%.1f".format(metrics.systemRamUsedGb)}/${"%.1f".format(metrics.systemRamTotalGb)} GB (${metrics.systemRamPercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (metrics.systemRamPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (metrics.systemRamPercent > 85) Error else Primary,
                    trackColor = SurfaceContainer
                )
            }

            // 2-Column Metrics: Speed / Throughput & Battery / Thermal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.ai_analytics_speed),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = if (metrics.throughputTokPerSec > 0f) {
                                    "${"%.1f".format(metrics.throughputTokPerSec)} tok/s"
                                } else {
                                    "0.0 tok/s"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                        }
                    }
                }

                // Battery & Temp Chip
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else SurfaceContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (metrics.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.ai_analytics_battery_temp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else OnSurfaceVariant
                            )
                            Text(
                                text = buildString {
                                    if (metrics.batteryPercent != null) append("${metrics.batteryPercent}%")
                                    if (metrics.batteryTemperatureC != null) {
                                        if (isNotEmpty()) append(" · ")
                                        append("${"%.1f".format(metrics.batteryTemperatureC)}°C")
                                    }
                                    if (isEmpty()) append("--")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (metrics.isThermalThrottling) MaterialTheme.colorScheme.onErrorContainer else OnSurface
                            )
                        }
                    }
                }
            }

            // Thermal Status Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Android Runtime",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = OnSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.ai_analytics_thermal)}: ${metrics.thermalStatus}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (metrics.isThermalThrottling) Error else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun aiPhaseLabel(phase: com.emanuel5014.trainable.data.ai.ScanPhase): String =
    when (phase) {
        com.emanuel5014.trainable.data.ai.ScanPhase.LOADING_MODEL -> stringResource(R.string.ai_scan_phase_model)
        com.emanuel5014.trainable.data.ai.ScanPhase.CONNECTING -> stringResource(R.string.ai_scan_phase_connecting)
        com.emanuel5014.trainable.data.ai.ScanPhase.READING_SHEET -> stringResource(R.string.ai_scan_phase_reading)
        com.emanuel5014.trainable.data.ai.ScanPhase.PARSING -> stringResource(R.string.ai_scan_phase_generating)
    }

@Composable
internal fun ScanOptionItem(
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
    draft: com.emanuel5014.trainable.data.ai.ScanDraft,
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
                text = stringResource(R.string.scan_draft_title),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.scan_draft_subtitle, draft.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        TextButton(onClick = onResume) {
            Text(stringResource(R.string.scan_draft_resume).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
        }
        IconButton(onClick = onDiscard) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.scan_draft_discard), tint = OnSurfaceVariant)
        }
    }
}


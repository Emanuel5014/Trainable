package com.emanuel5014.trainable.ui.screens.routines

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
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
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.RoutineImagePicker
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.TargetSecondsSlider
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private fun getSupersetRange(index: Int, list: List<PlanExerciseWithDetails>): IntRange {
    val sid = list.getOrNull(index)?.planExercise?.supersetId ?: return index..index
    
    var start = index
    while (start > 0 && list[start - 1].planExercise.supersetId == sid) {
        start--
    }
    
    var end = index
    while (end < list.lastIndex && list[end + 1].planExercise.supersetId == sid) {
        end++
    }
    
    return start..end
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineDetailScreen(
    onNavigateBack: () -> Unit,
    onStartWorkout: (Int, Int?) -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val editablePresetExercises by viewModel.editablePresetExercises.collectAsState()
    val aiScanAvailable by viewModel.aiScanAvailable.collectAsState()
    val aiResourceAnalyticsEnabled by viewModel.aiResourceAnalyticsEnabled.collectAsState()
    val aiScanState by viewModel.aiScanState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    val themeMode by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.THEME_MODE] ?: 0 }
    }.collectAsState(initial = 0)
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val exerciseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val routineSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scanHazeState = rememberHazeState()

    var showExerciseSheet by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

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

    var showRoutineEditSheet by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var existingSessionForPlan by remember { mutableStateOf<SessionWithPlanName?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseType by remember { mutableStateOf("strength") }
    var timeTargetSecondsText by remember { mutableStateOf("45") }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("8-12") }
    var restText by remember { mutableStateOf("120") }
    var cardioDurationText by remember { mutableStateOf("20") }

    // Local state for dragging to ensure smoothness
    val localExercises = remember { mutableStateListOf<PlanExerciseWithDetails>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val trainedMuscleGroups = remember(localExercises.size, localExercises.map { it.exercise.categoria }) {
        localExercises.map { it.exercise.categoria }
            .distinct()
            .filter { it.isNotBlank() }
    }

    var editingExerciseId by remember { mutableStateOf<Int?>(null) }
    val editingExercise = remember(editingExerciseId, localExercises.size, localExercises.map { it.planExercise.supersetId }) {
        localExercises.find { it.planExercise.id == editingExerciseId }
    }

    // Sync local list with UI state when not dragging
    LaunchedEffect(uiState.planDetails?.exercises) {
        if (draggedItemIndex == null) {
            localExercises.clear()
            uiState.planDetails?.exercises?.let { localExercises.addAll(it) }
        }
    }

    LaunchedEffect(aiScanState) {
        val state = aiScanState
        if (state is AiScanState.Error) {
            Toast.makeText(
                context,
                if (state.message == null) context.getString(R.string.ai_scan_no_exercises)
                else context.getString(R.string.ai_scan_failed),
                Toast.LENGTH_LONG
            ).show()
            viewModel.dismissScanResult()
        }
    }

    fun openAddSheet() {
        editingExerciseId = null
        selectedExerciseId = null
        selectedExerciseType = "strength"
        setsText = "3"
        repsText = "8"
        timeTargetSecondsText = "45"
        cardioDurationText = "20"
        // Inherit rest from the last exercise in the list, default to 120 if empty
        restText = localExercises.lastOrNull()?.planExercise?.recuperoTarget?.toString() ?: "120"
        showExercisePicker = true
    }

    fun openEditSheet(item: PlanExerciseWithDetails) {
        editingExerciseId = item.planExercise.id
        selectedExerciseId = item.exercise.id
        selectedExerciseType = item.planExercise.exerciseType
        setsText = item.planExercise.serieTarget.toString()
        repsText = item.planExercise.repsTarget
        timeTargetSecondsText = item.planExercise.durataTargetSecondi?.toString() ?: item.planExercise.repsTarget.filter { it.isDigit() }.ifBlank { "45" }
        restText = item.planExercise.recuperoTarget.toString()
        cardioDurationText = item.planExercise.durataTargetSecondi?.let { (it / 60).toString() } ?: "20"
        showExerciseSheet = true
    }

    fun openRoutineEditSheet() {
        uiState.planDetails?.plan?.let { plan ->
            routineName = plan.nome
            routineNote = plan.note.orEmpty()
            startDate = plan.dataInizio
            endDate = plan.dataFine
            selectedDays.clear()
            plan.giorniSettimana?.split(",")?.forEach {
                it.toIntOrNull()?.let { value -> selectedDays.add(DayOfWeek.of(value)) }
            }
            showRoutineEditSheet = true
        }
    }

    if (existingSessionForPlan != null) {
        AlertDialog(
            onDismissRequest = { existingSessionForPlan = null },
            title = { Text(stringResource(R.string.resume_workout)) },
            text = { Text(stringResource(R.string.existing_workout_message, existingSessionForPlan!!.displayName)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        existingSessionForPlan?.let { session ->
                            onStartWorkout(session.session.planId, session.session.id)
                        }
                        existingSessionForPlan = null
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.resume_workout), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { existingSessionForPlan = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.reset_routine_content)) },
            text = { Text(stringResource(R.string.reset_routine_content_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.resetRoutineContent()
                        showResetConfirmDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.reset).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showResetConfirmDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isScanActive) Modifier.hazeSource(state = scanHazeState) else Modifier
            ),
        containerColor = Surface,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { openAddSheet() },
                    containerColor = Surface,
                    contentColor = Primary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = Spacing.small)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Exercise")
                }
                
                if (localExercises.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            val planId = uiState.planDetails?.plan?.id ?: return@FloatingActionButton
                            val existing = uiState.unfinishedSessions.find { it.session.planId == planId }
                            if (existing != null) {
                                existingSessionForPlan = existing
                            } else {
                                onStartWorkout(planId, null)
                            }
                        },
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.start))
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(stringResource(R.string.start), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else if (uiState.planDetails != null) {
            val details = uiState.planDetails!!

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScreenHeader(
                        title = details.plan.nome,
                        subtitle = stringResource(R.string.routine_details).uppercase(),
                        navigationIcon = {
                            GymIconButton(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                onClick = onNavigateBack,
                                containerColor = Color.Transparent,
                                contentColor = OnSurface,
                                description = "Back"
                            )
                        },
                        actions = {
                            GymIconButton(
                                icon = Icons.Rounded.RestartAlt,
                                onClick = { showResetConfirmDialog = true },
                                containerColor = SurfaceContainerHigh,
                                contentColor = Error,
                                description = "Reset Routine Content"
                            )
                            GymIconButton(
                                icon = Icons.Rounded.Edit,
                                onClick = { openRoutineEditSheet() },
                                containerColor = SurfaceContainerHigh,
                                contentColor = OnSurface,
                                description = "Edit Routine"
                            )
                        },
                        titleInRow = true,
                        titleStyle = MaterialTheme.typography.headlineLarge
                    )
                }

                item {
                    RoutineImagePicker(
                        images = details.images,
                        onImageAdd = { uri -> viewModel.addPlanImage(uri) },
                        onImageRemove = { image -> viewModel.removePlanImage(image) },
                        modifier = Modifier.padding(horizontal = ResponsiveSize.cardPadding)
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = ResponsiveSize.horizontalPadding).padding(top = 8.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.start_date) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(details.plan.dataInizio),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                details.plan.dataFine?.let { expiry ->
                                    Text(
                                        text = stringResource(R.string.expires) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(expiry),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (expiry < System.currentTimeMillis()) Error else OnSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            
                            if (!details.plan.giorniSettimana.isNullOrBlank()) {
                                val scheduledDays = remember(details.plan.giorniSettimana) {
                                    details.plan.giorniSettimana.split(",").mapNotNull { 
                                        it.toIntOrNull()?.let { value -> DayOfWeek.of(value) }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                                ) {
                                    DayOfWeek.entries.forEachIndexed { index, day ->
                                        val isScheduled = scheduledDays.contains(day)
                                        val dayShape = if (isScheduled) RoundedCornerShape(50)
                                        else when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes().shape
                                            DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes().shape
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes().shape
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(dayShape)
                                                .background(if (isScheduled) Primary else SurfaceContainerHigh)
                                                .then(
                                                    if (!isScheduled) Modifier.border(
                                                        BorderStroke(1.dp, OnSurfaceVariant.copy(alpha = 0.2f)),
                                                        dayShape
                                                    ) else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isScheduled) OnPrimary else OnSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        details.plan.note?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (trainedMuscleGroups.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                trainedMuscleGroups.forEach { category ->
                                    val translatedCategory = ExerciseTranslations.translateCategory(category, languageCode)
                                    androidx.compose.material3.Surface(
                                        color = SurfaceContainerHigh,
                                        contentColor = OnSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = translatedCategory,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (localExercises.isEmpty()) {
                    item {
                        if (aiScanAvailable) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ResponsiveSize.horizontalPadding)
                                    .padding(top = 24.dp, bottom = 40.dp),
                                shape = Shapes.medium,
                                color = SurfaceContainerHigh,
                                tonalElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Primary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DocumentScanner,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ai_scan_empty_state_title),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)
                                            ),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OnSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(R.string.ai_scan_empty_state_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    GymButton(
                                        onClick = { showScanSourceSheet = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = Primary,
                                        contentColor = OnPrimary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DocumentScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.ai_scan_empty_state_scan_button),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp, bottom = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_exercises_in_routine),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OnSurface
                                )
                                Spacer(modifier = Modifier.height(Spacing.xtraSmall))
                                Text(
                                    text = stringResource(R.string.tap_plus_to_add),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 48.dp)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(localExercises, key = { _, item -> item.planExercise.id }) { index, item ->
                        val draggedRange = draggedItemIndex?.let { getSupersetRange(it, localExercises) }
                        val isDragging = draggedRange != null && index in draggedRange
                        val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                        
                        // Superset logic
                        val currentSid = item.planExercise.supersetId
                        val isSuperset = currentSid != null
                        val isStart = isSuperset && (index == 0 || localExercises[index - 1].planExercise.supersetId != currentSid)
                        val isEnd = isSuperset && (index == localExercises.lastIndex || localExercises[index + 1].planExercise.supersetId != currentSid)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                        ) {
                            if (isStart) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 64.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.superset),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        color = Primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        scaleX = if (isDragging) 1.02f else 1f
                                        scaleY = if (isDragging) 1.02f else 1f
                                        shadowElevation = if (isDark) elevation.toPx() else 0f
                                        shape = RoundedCornerShape(28.dp)
                                        clip = isDragging
                                    }
                                    .pointerInput(item.planExercise.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                draggedItemIndex = localExercises.indexOfFirst { it.planExercise.id == item.planExercise.id }
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                
                                                val itemHeight = 80.dp.toPx()
                                                val currentDraggedIndex = draggedItemIndex
                                                if (currentDraggedIndex != null) {
                                                    val range = getSupersetRange(currentDraggedIndex, localExercises)
                                                    val size = range.endInclusive - range.start + 1
                                                    
                                                    if (dragOffsetY > 0 && range.endInclusive < localExercises.lastIndex) {
                                                        val nextIndex = range.endInclusive + 1
                                                        val nextRange = getSupersetRange(nextIndex, localExercises)
                                                        val nextSize = nextRange.endInclusive - nextRange.start + 1
                                                        val threshold = (nextSize * itemHeight) / 2f
                                                        
                                                        if (dragOffsetY > threshold) {
                                                            val draggedItems = localExercises.subList(range.start, range.endInclusive + 1).toList()
                                                            repeat(size) {
                                                                localExercises.removeAt(range.start)
                                                            }
                                                            val insertIndex = range.start + nextSize
                                                            localExercises.addAll(insertIndex, draggedItems)
                                                            
                                                            draggedItemIndex = insertIndex + (currentDraggedIndex - range.start)
                                                            dragOffsetY -= nextSize * itemHeight
                                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    } else if (dragOffsetY < 0 && range.start > 0) {
                                                        val prevIndex = range.start - 1
                                                        val prevRange = getSupersetRange(prevIndex, localExercises)
                                                        val prevSize = prevRange.endInclusive - prevRange.start + 1
                                                        val threshold = -(prevSize * itemHeight) / 2f
                                                        
                                                        if (dragOffsetY < threshold) {
                                                            val draggedItems = localExercises.subList(range.start, range.endInclusive + 1).toList()
                                                            repeat(size) {
                                                                localExercises.removeAt(range.start)
                                                            }
                                                            val insertIndex = prevRange.start
                                                            localExercises.addAll(insertIndex, draggedItems)
                                                            
                                                            draggedItemIndex = insertIndex + (currentDraggedIndex - range.start)
                                                            dragOffsetY += prevSize * itemHeight
                                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                viewModel.updateExercisesOrder(localExercises.map { it.planExercise })
                                                draggedItemIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggedItemIndex = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSuperset && !isDragging) {
                                        val density = androidx.compose.ui.platform.LocalDensity.current
                                        val strokeWidthPx = with(density) { 3.dp.toPx() }
                                        val lineLengthPx = with(density) { 100.dp.toPx() }
                                        val lineColor = Primary

                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val centerY = size.height / 2
                                            val centerX = size.width / 2
                                            
                                            if (!isStart) {
                                                drawLine(
                                                    color = lineColor,
                                                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                                    end = androidx.compose.ui.geometry.Offset(centerX, centerY - lineLengthPx),
                                                    strokeWidth = strokeWidthPx
                                                )
                                            }
                                            if (!isEnd) {
                                                drawLine(
                                                    color = lineColor,
                                                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                                    end = androidx.compose.ui.geometry.Offset(centerX, centerY + lineLengthPx),
                                                    strokeWidth = strokeWidthPx
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDragging || isSuperset) Primary else SurfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isDragging || isSuperset) OnPrimary else Primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                ExerciseEntryCard(
                                    item = item,
                                    onClick = { openEditSheet(item) },
                                    modifier = Modifier.weight(1f),
                                    languageCode = languageCode,
                                    isSuperset = isSuperset
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(140.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Routine not found", color = OnSurfaceVariant)
            }
        }
    }

    if (showExerciseSheet) {
        val selectedExercise = uiState.availableExercises.firstOrNull { it.id == selectedExerciseId }
        val isSelectedCardio = selectedExercise?.categoria?.equals("Cardio", ignoreCase = true) == true || editingExercise?.planExercise?.exerciseType == "cardio"

        ModalBottomSheet(
            onDismissRequest = { showExerciseSheet = false },
            sheetState = exerciseSheetState,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ResponsiveSize.cardPadding)
                    .padding(top = Spacing.medium, bottom = ResponsiveSize.cardPadding)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                    Text(
                        text = if (editingExercise == null) stringResource(R.string.add_exercise) else stringResource(R.string.edit_exercise),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (editingExercise == null) stringResource(R.string.exercise_details) else stringResource(R.string.update_exercise),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                if (!exerciseSheetState.isVisible) {
                                    showExerciseSheet = false
                                    showExercisePicker = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OnSurface
                        ),
                        border = BorderStroke(1.dp, OnSurfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = selectedExercise?.let {
                                        ExerciseTranslations.translate(it.nome, languageCode)
                                    } ?: stringResource(R.string.select_exercise),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.bodyLarge.fontSize)),
                                    color = OnSurface
                                )
                                selectedExercise?.let {
                                    Text(
                                        text = ExerciseTranslations.translateCategory(it.categoria, languageCode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OnSurfaceVariant
                            )
                        }
                    }

                    if (isSelectedCardio) {
                        CardioDurationSlider(
                            valueMinutes = cardioDurationText.toIntOrNull() ?: 20,
                            onValueChange = { cardioDurationText = it.toString() },
                            hapticEnabled = hapticEnabled,
                            haptic = haptic,
                            modifier = Modifier.fillMaxWidth()
                        )

                        RestSlider(
                            value = restText.toIntOrNull() ?: 120,
                            onValueChange = { restText = it.toString() },
                            hapticEnabled = hapticEnabled,
                            haptic = haptic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedExerciseType == "strength",
                                onClick = { selectedExerciseType = "strength" },
                                label = { Text(stringResource(R.string.exercise_type_strength)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                                    selectedLabelColor = Primary,
                                    selectedLeadingIconColor = Primary
                                )
                            )
                            FilterChip(
                                selected = selectedExerciseType == "time_and_weight",
                                onClick = { selectedExerciseType = "time_and_weight" },
                                label = { Text(stringResource(R.string.exercise_type_time_and_weight)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                                    selectedLabelColor = Primary,
                                    selectedLeadingIconColor = Primary
                                )
                            )
                        }

                        if (selectedExerciseType == "time_and_weight") {
                            GymInputField(
                                value = setsText,
                                onValueChange = { setsText = it },
                                label = stringResource(R.string.sets),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            TargetSecondsSlider(
                                valueSeconds = timeTargetSecondsText.toIntOrNull() ?: 45,
                                onValueChange = { timeTargetSecondsText = it.toString() },
                                hapticEnabled = hapticEnabled,
                                haptic = haptic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                GymInputField(
                                    value = setsText,
                                    onValueChange = { setsText = it },
                                    label = stringResource(R.string.sets),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                GymInputField(
                                    value = repsText,
                                    onValueChange = {
                                        repsText = it
                                        val repCount = it.split("-").count { n -> n.trim().toIntOrNull() != null }
                                        if (repCount > 1 && repCount != (setsText.toIntOrNull() ?: 0)) {
                                            setsText = repCount.toString()
                                        }
                                    },
                                    label = stringResource(R.string.reps),
                                    supportingText = stringResource(R.string.reps_hint),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        RestSlider(
                            value = restText.toIntOrNull() ?: 120,
                            onValueChange = { restText = it.toString() },
                            hapticEnabled = hapticEnabled,
                            haptic = haptic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                        if (editingExercise != null && localExercises.indexOfFirst { it.planExercise.id == editingExercise.planExercise.id } < localExercises.size - 1) {
                            val nextItem = localExercises.getOrNull(localExercises.indexOfFirst { it.planExercise.id == editingExercise.planExercise.id } + 1)
                            val isLinked = editingExercise.planExercise.supersetId != null && editingExercise.planExercise.supersetId == nextItem?.planExercise?.supersetId
                            
                            GymButton(
                                onClick = {
                                    editingExercise.let { current ->
                                        val index = localExercises.indexOfFirst { it.planExercise.id == current.planExercise.id }
                                        if (index != -1 && index < localExercises.size - 1) {
                                            val nextIndex = index + 1
                                            val nextItem = localExercises[nextIndex]
                                            val newSid = if (isLinked) null else (current.planExercise.supersetId ?: nextItem.planExercise.supersetId ?: java.util.UUID.randomUUID().toString())
                                            
                                            // Update local list for instant feedback
                                            val updatedCurrent = current.copy(planExercise = current.planExercise.copy(supersetId = newSid))
                                            val updatedNext = nextItem.copy(planExercise = nextItem.planExercise.copy(supersetId = newSid))
                                            
                                            localExercises[index] = updatedCurrent
                                            localExercises[nextIndex] = updatedNext
                                            
                                            viewModel.toggleSupersetWithNext(current.planExercise, newSid)
                                        }
                                    }
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = if (isLinked) Error.copy(alpha = 0.12f) else Primary.copy(alpha = 0.12f),
                                contentColor = if (isLinked) Error else Primary
                            ) {
                                Icon(
                                    imageVector = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLinked) stringResource(R.string.unlink_superset).uppercase() else stringResource(R.string.link_with_next).uppercase(),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     GymButton(
                        onClick = {
                            scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                if (!exerciseSheetState.isVisible) {
                                    showExerciseSheet = false
                                }
                            }
                            editingExercise?.let {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeExercise(it.planExercise)
                            }
                        },
                        modifier = Modifier.size(60.dp),
                        height = 56,
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error,
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(28.dp))
                    }

                    GymButton(
                        onClick = {
                            scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                if (!exerciseSheetState.isVisible) {
                                    showExerciseSheet = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    
                    GymButton(
                        onClick = {
                            val exerciseId = selectedExerciseId ?: return@GymButton
                            val current = editingExercise
                            if (isSelectedCardio) {
                                val durSec = cardioDurationText.trim().toIntOrNull()?.let { it * 60 }
                                val rest = restText.trim().toIntOrNull() ?: 0
                                val category = selectedExercise?.categoria ?: "Cardio"

                                if (current == null) {
                                    viewModel.addCardioExercise(
                                        exerciseId = exerciseId,
                                        cardioCategoria = category,
                                        durataTargetSecondi = durSec,
                                        recuperoTarget = rest
                                    )
                                } else {
                                    viewModel.updateExercise(
                                        original = current.planExercise.copy(
                                            exerciseType = "cardio",
                                            cardioCategoria = category,
                                            durataTargetSecondi = durSec
                                        ),
                                        exerciseId = exerciseId,
                                        serieTarget = 1,
                                        repsTarget = "1",
                                        recuperoTarget = rest
                                    )
                                }
                            } else if (selectedExerciseType == "time_and_weight") {
                                val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                val targetSec = timeTargetSecondsText.trim().toIntOrNull() ?: 45

                                if (current == null) {
                                    viewModel.addExercise(
                                        exerciseId = exerciseId,
                                        serieTarget = sets,
                                        repsTarget = "${targetSec}s",
                                        recuperoTarget = rest,
                                        exerciseType = "time_and_weight",
                                        durataTargetSecondi = targetSec
                                    )
                                } else {
                                    viewModel.updateExercise(
                                        original = current.planExercise,
                                        exerciseId = exerciseId,
                                        serieTarget = sets,
                                        repsTarget = "${targetSec}s",
                                        recuperoTarget = rest,
                                        exerciseType = "time_and_weight",
                                        durataTargetSecondi = targetSec
                                    )
                                }
                            } else {
                                val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                val reps = repsText.trim().ifBlank { return@GymButton }

                                if (current == null) {
                                    viewModel.addExercise(
                                        exerciseId = exerciseId,
                                        serieTarget = sets,
                                        repsTarget = reps,
                                        recuperoTarget = rest,
                                        exerciseType = "strength",
                                        durataTargetSecondi = null
                                    )
                                } else {
                                    viewModel.updateExercise(
                                        original = current.planExercise,
                                        exerciseId = exerciseId,
                                        serieTarget = sets,
                                        repsTarget = reps,
                                        recuperoTarget = rest,
                                        exerciseType = "strength",
                                        durataTargetSecondi = null
                                    )
                                }
                            }
                            scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                if (!exerciseSheetState.isVisible) {
                                    showExerciseSheet = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (editingExercise == null) stringResource(R.string.add).uppercase() else stringResource(R.string.save).uppercase(),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = uiState.availableExercises,
            categories = uiState.categories,
            onExerciseSelected = { exercise ->
                selectedExerciseId = exercise.id
                showExercisePicker = false
                showExerciseSheet = true
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                viewModel.addCustomExercise(nome, categoria, onCreated)
            },
            onEditCustomExercise = { exercise ->
                viewModel.updateCustomExercise(exercise)
            },
            onDeleteCustomExercise = { exercise ->
                viewModel.deleteCustomExercise(exercise)
            },
            onDismiss = { showExercisePicker = false },
            languageCode = languageCode,
            editablePresetExercises = editablePresetExercises
        )
    }

    if (showRoutineEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRoutineEditSheet = false },
            sheetState = routineSheetState,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ResponsiveSize.cardPadding)
                    .padding(bottom = ResponsiveSize.cardPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.edit_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.update_plan),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymInputField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = stringResource(R.string.routine_name),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }) {
                            GymInputField(
                                value = com.emanuel5014.trainable.ui.util.DateFormatter.format(startDate),
                                onValueChange = {},
                                label = stringResource(R.string.start_date).replace(":", ""),
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }) {
                            GymInputField(
                                value = endDate?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(R.string.tap_to_set),
                                onValueChange = {},
                                label = stringResource(R.string.expiration_date),
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (endDate != null) Primary else OnSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.schedule_days),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            DayOfWeek.entries.forEachIndexed { index, day ->
                                val isSelected = selectedDays.contains(day)
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedDays.remove(day)
                                        else selectedDays.add(day)
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Text(
                                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.bodyLarge.fontSize)),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    GymInputField(
                        value = routineNote,
                        onValueChange = { routineNote = it },
                        label = stringResource(R.string.routine_notes),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymButton(
                        onClick = {
                            scope.launch { routineSheetState.hide() }.invokeOnCompletion {
                                if (!routineSheetState.isVisible) {
                                    showRoutineEditSheet = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    
                    GymButton(
                        onClick = {
                            val trimmedName = routineName.trim()
                            if (trimmedName.isNotEmpty()) {
                                val note = routineNote.trim().takeIf { it.isNotBlank() }
                                val daysString = if (selectedDays.isEmpty()) null 
                                               else selectedDays.sortedBy { it.value }.joinToString(",") { it.value.toString() }
                                viewModel.updatePlan(
                                    nome = trimmedName,
                                    note = note,
                                    giorniSettimana = daysString,
                                    dataInizio = startDate,
                                    dataFine = endDate
                                )
                                scope.launch { routineSheetState.hide() }.invokeOnCompletion {
                                    if (!routineSheetState.isVisible) {
                                        showRoutineEditSheet = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.save).uppercase(),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showStartDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        endDate = endDatePickerState.selectedDateMillis
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = {
                        endDate = null
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Error
                ) {
                    Text(stringResource(R.string.reset).uppercase())
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

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
                imageUri = state.imageUri,
                entries = state.entries,
                exercises = uiState.availableExercises,
                categories = uiState.categories,
                languageCode = languageCode,
                editablePresetExercises = editablePresetExercises,
                onAddCustomExercise = { nome, categoria, onCreated ->
                    viewModel.addCustomExercise(nome, categoria, onCreated)
                },
                onConfirm = { entries, saveImage ->
                    if (saveImage && state.imageUri != null) {
                        val savedPath = ImageStorageUtils.compressAndSaveImage(context, state.imageUri)
                        if (savedPath != null) {
                            viewModel.addPlanImage(savedPath)
                        }
                    }
                    viewModel.applyScannedExercises(entries)
                },
                onDismiss = { viewModel.dismissScanResult() }
            )
        }
        else -> {}
    }
}

private fun createScanTempImageUri(context: android.content.Context): android.net.Uri {
    val tempFile = java.io.File(context.cacheDir, "ai_scan_temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
private fun AiScanningOverlay(
    phase: com.emanuel5014.trainable.data.ai.ScanPhase,
    stream: AiScanStreamState,
    showResourceAnalytics: Boolean = false,
    isDark: Boolean,
    hazeState: HazeState,
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

    val infiniteTransition = rememberInfiniteTransition(label = "ai_scan_glow_transition")

    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "glow_angle_1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing)
        ),
        label = "glow_angle_2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val scrimColor = MaterialTheme.colorScheme.surface
        .copy(alpha = if (isDark) 0.65f else 0.85f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Consume touches to prevent triggering underlying buttons */ }
            }
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                tints = listOf(HazeTint(scrimColor))
                noiseFactor = 0.05f
            }
            .drawBehind {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                // Orb 1: Primary color orbital glow
                val rad1 = Math.toRadians(angle1.toDouble())
                val offset1 = Offset(
                    x = centerX + (cos(rad1) * width * 0.30f).toFloat(),
                    y = centerY + (sin(rad1) * height * 0.22f).toFloat()
                )
                val radius1 = (width * 0.72f * pulseScale).coerceAtLeast(160f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDark) 0.40f * pulseAlpha else 0.28f * pulseAlpha),
                            primaryColor.copy(alpha = if (isDark) 0.15f * pulseAlpha else 0.09f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = offset1,
                        radius = radius1
                    ),
                    center = offset1,
                    radius = radius1
                )

                // Orb 2: Tertiary / Accent color orbital glow (counter-rotation)
                val rad2 = Math.toRadians(angle2.toDouble())
                val offset2 = Offset(
                    x = centerX + (cos(rad2) * width * 0.34f).toFloat(),
                    y = centerY + (sin(rad2) * height * 0.25f).toFloat()
                )
                val radius2 = (width * 0.65f * (2.1f - pulseScale)).coerceAtLeast(160f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = if (isDark) 0.35f * pulseAlpha else 0.22f * pulseAlpha),
                            tertiaryColor.copy(alpha = if (isDark) 0.12f * pulseAlpha else 0.07f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = offset2,
                        radius = radius2
                    ),
                    center = offset2,
                    radius = radius2
                )

                // Orb 3: Central breathing ambient aura behind card
                val radiusCenter = width * 0.52f * pulseScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDark) 0.25f * pulseAlpha else 0.16f * pulseAlpha),
                            secondaryColor.copy(alpha = if (isDark) 0.10f * pulseAlpha else 0.05f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radiusCenter
                    ),
                    center = Offset(centerX, centerY),
                    radius = radiusCenter
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceContainerHigh.copy(alpha = if (isDark) 0.92f else 0.96f),
            tonalElevation = 4.dp,
            shadowElevation = if (isDark) 16.dp else 4.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        tertiaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.15f),
                        tertiaryColor.copy(alpha = 0.45f),
                        primaryColor.copy(alpha = 0.5f)
                    )
                )
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
private fun aiPhaseLabel(phase: com.emanuel5014.trainable.data.ai.ScanPhase): String =
    when (phase) {
        com.emanuel5014.trainable.data.ai.ScanPhase.LOADING_MODEL -> stringResource(R.string.ai_scan_phase_model)
        com.emanuel5014.trainable.data.ai.ScanPhase.READING_SHEET -> stringResource(R.string.ai_scan_phase_reading)
        com.emanuel5014.trainable.data.ai.ScanPhase.PARSING -> stringResource(R.string.ai_scan_phase_generating)
    }

@Composable
private fun ScanOptionItem(
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

@Composable
private fun RestSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(0, 30, 60, 90, 120, 180, 240, 300)
    val currentIndex = remember(value) { steps.indexOf(value).coerceAtLeast(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.rest_seconds),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${value}s",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = formatRestTime(value),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }
        
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != currentIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun formatRestTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes == 0 -> "${secs}s"
        secs == 0 -> "${minutes}m"
        else -> "${minutes}m ${secs}s"
    }
}

@Composable
private fun CardioDurationSlider(
    valueMinutes: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val closestIndex = remember(valueMinutes) {
        steps.indexOf(steps.minByOrNull { kotlin.math.abs(it - valueMinutes) } ?: 20).coerceAtLeast(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cardio_duration_slider),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${valueMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            val displayMinutes = valueMinutes / 60
            val displaySecs = valueMinutes % 60
            if (displayMinutes > 0 || displaySecs > 0) {
                Text(
                    text = when {
                        displayMinutes == 0 -> "${displaySecs}s"
                        displaySecs == 0 -> "${displayMinutes}m"
                        else -> "${displayMinutes}m ${displaySecs}s"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Slider(
            value = closestIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != closestIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DropMenuItem(
    text: String,
    category: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        },
        onClick = onClick
    )
}

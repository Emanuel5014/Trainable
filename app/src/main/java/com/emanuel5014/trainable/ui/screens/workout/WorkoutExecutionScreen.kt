package com.emanuel5014.trainable.ui.screens.workout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.emanuel5014.trainable.ui.components.ExerciseMediaCard
import com.emanuel5014.trainable.ui.components.ExerciseMediaMessageToasts
import com.emanuel5014.trainable.ui.components.rememberExerciseMediaPicker
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseMediaSlot
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.components.ExerciseNavigation
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.PlateCalculatorEnableChip
import com.emanuel5014.trainable.ui.components.PlateCalculatorSheet
import com.emanuel5014.trainable.ui.components.PlateChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.emanuel5014.trainable.ui.components.RestTimerSection
import com.emanuel5014.trainable.ui.components.SetLogRow
import com.emanuel5014.trainable.ui.components.SwapExerciseBottomSheet
import com.emanuel5014.trainable.ui.components.TimerAdjustButton
import com.emanuel5014.trainable.ui.components.WeightRepsInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.OutlinedButton
import com.emanuel5014.trainable.ui.components.WeightTimeInput
import com.emanuel5014.trainable.ui.components.RestSlider
import com.emanuel5014.trainable.ui.components.formatRestTime
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Shapes
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.RpeSelector
import com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels
import com.emanuel5014.trainable.ui.components.techniqueLabel
import kotlinx.coroutines.launch

/** Key of the large exercise media item in the sets list, used to read where the sets end. */
private const val MEDIA_CARD_KEY = "exercise_media_card"

/** The large media card only appears when at least this much room is left under the sets. */
private val MEDIA_CARD_MIN_HEIGHT = 140.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutExecutionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val availableExercises by viewModel.availableExercises.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // Keep the screen on while a cardio or set timer is active (running or paused).
    // Released automatically when the timer finishes, is reset, or the screen is left.
    val context = LocalContext.current
    val activityWindow = (context as? Activity)?.window
    val keepScreenOn = (state.keepScreenOnCardioTimer && (state.cardioTimerRunning || state.cardioTimerPaused)) ||
        (state.keepScreenOnSetTimer && (state.setTimerRunning || state.setTimerPaused))
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activityWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activityWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activityWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    var isEditingValues by remember { mutableStateOf(false) }
    // Index of a set being edited in place in the bottom hub, done or still to do (none by default); it resets with the exercise
    var editingSetIndex by remember(state.currentExerciseIndex) { mutableStateOf<Int?>(null) }
    var lastEditedIndex by remember { mutableIntStateOf(0) }
    var showSwapExerciseSheet by remember { mutableStateOf(false) }
    // Exercises that use the plate calculator (Workout settings -> Plate calculator) and their bar weight in kg
    val plateExercises by viewModel.plateCalculatorExercises.collectAsState()
    var showPlateSheet by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var isAddingAfterCurrent by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newSessionName by remember { mutableStateOf(state.planName) }
    var showWarmupTimerDialog by remember { mutableStateOf(false) }
    var warmupTimerDuration by remember { mutableStateOf(120) }
    // Exercises whose EMOM sets the lifter logs by hand instead of with the minute clock.
    var emomManualExercises by remember { mutableStateOf(emptySet<Int>()) }
    val usesEmomClock = { exercise: WorkoutExerciseState ->
        !exercise.isCardio && exercise.emomStartIndex != null && exercise.exercise.id !in emomManualExercises
    }
    // The user's own image/GIF of each exercise (optional, see Workout settings -> Exercise media)
    val exerciseMedia by viewModel.exerciseMedia.collectAsState()
    var mediaViewer by remember { mutableStateOf<Pair<Int, String>?>(null) }
    // Height of the bottom interaction hub, so the large media card only takes the room left above it
    var hubHeightPx by remember { mutableIntStateOf(0) }
    val pickMedia = rememberExerciseMediaPicker { exerciseId, uri -> viewModel.setExerciseMedia(exerciseId, uri) }
    ExerciseMediaMessageToasts(viewModel.mediaMessages)
    var cardioDistanceInput by remember(state.currentExercise?.exercise?.id) {
        mutableStateOf(
            if ((state.currentExercise?.cardioDistanceKm ?: 0f) > 0f) state.currentExercise?.cardioDistanceKm.toString() else ""
        )
    }

    mediaViewer?.let { (exerciseId, exerciseName) ->
        val mediaFile = exerciseMedia[exerciseId]
        // The file disappears when the user removes it from the viewer: close it then
        LaunchedEffect(mediaFile) { if (mediaFile == null) mediaViewer = null }
        if (mediaFile != null) {
            ExerciseMediaViewer(
                fileName = mediaFile,
                exerciseName = exerciseName,
                onDismiss = { mediaViewer = null },
                onReplace = { pickMedia(exerciseId) },
                onRemove = { viewModel.removeExerciseMedia(exerciseId) }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_workout)) },
            text = {
                com.emanuel5014.trainable.ui.components.GymInputField(
                    value = newSessionName,
                    onValueChange = { newSessionName = it },
                    label = stringResource(R.string.routine_name),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSessionName(newSessionName)
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    val prescriptionLabels = rememberPrescriptionLabels(state.weightUnit)

    state.oneRepMaxSuggestions.firstOrNull()?.let { suggestion ->
        val fmt = { kg: Float -> WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, state.weightUnit), state.weightUnit) }
        AlertDialog(
            onDismissRequest = { viewModel.resolveOneRepMaxSuggestion(accept = false) },
            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Primary) },
            title = { Text(stringResource(R.string.new_one_rep_max_title), fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    stringResource(
                        R.string.new_one_rep_max_message,
                        fmt(suggestion.weightKg), suggestion.reps, suggestion.exerciseName,
                        fmt(suggestion.currentKg), fmt(suggestion.suggestedKg)
                    )
                )
            },
            confirmButton = {
                GymButton(
                    onClick = { viewModel.resolveOneRepMaxSuggestion(accept = true) },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.save_new_one_rep_max).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolveOneRepMaxSuggestion(accept = false) }) {
                    Text(stringResource(R.string.not_now).uppercase(), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GymLoadingIndicator()
        }
        return
    }

    val currentExState = state.currentExercise
    val activeSetIndex = remember(currentExState?.sets) {
        currentExState?.sets?.indexOfFirst { !it.isCompleted }?.takeIf { it != -1 } ?: ((currentExState?.sets?.size ?: 1) - 1)
    }
    val activeSet = remember(currentExState?.sets, activeSetIndex) {
        currentExState?.sets?.getOrNull(activeSetIndex)
    }
    val isExerciseCompleted = remember(activeSet) {
        activeSet == null || activeSet.isCompleted
    }
    val setBeingEdited = remember(currentExState?.sets, editingSetIndex) {
        editingSetIndex?.let { currentExState?.sets?.getOrNull(it) }
    }
    LaunchedEffect(setBeingEdited, activeSetIndex) {
        val editing = setBeingEdited
        when {
            // The set is gone (removed)
            editing == null -> editingSetIndex = null
            // The set being edited came up next: it moves to the panel of the active set, which can also log it
            // (while a rest is running that panel waits for the rest to end, so it is not opened behind it)
            editingSetIndex == activeSetIndex && !editing.isCompleted -> {
                editingSetIndex = null
                isEditingValues = state.remainingRestSeconds <= 0
            }
        }
    }
    // Kept so the hub still has something to show while it animates away
    LaunchedEffect(editingSetIndex) {
        editingSetIndex?.let { lastEditedIndex = it }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.cancelCustomVibration()
                viewModel.restartCardioTimerIfNeeded()
                viewModel.restartSetTimerIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    
    // Prevent NavHost deadlocks by ensuring the screen has fully transitioned in before popping
    val safeNavigateBack: () -> Unit = {
        coroutineScope. launch {
            while (!lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                kotlinx.coroutines.delay(50)
            }
            onNavigateBack()
        }
    }



    // Auto-collapse editing when a set is logged or timer starts
    LaunchedEffect(activeSetIndex, state.remainingRestSeconds > 0) {
        if (state.remainingRestSeconds > 0) {
            isEditingValues = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is WorkoutViewModel.WorkoutNavEvent.NavigateBack -> safeNavigateBack()
                is WorkoutViewModel.WorkoutNavEvent.ProgramCompleted -> android.widget.Toast.makeText(
                    context, context.getString(R.string.program_completed), android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { 
                                if (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) {
                                        showRenameDialog = true
                                    } else {
                                        state.planId?.let { onNavigateToRoutine(it) }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.planName, 
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Text(
                            text = stringResource(R.string.session_in_progress_title), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                navigationIcon = {
                    GymIconButton(
                        icon = Icons.Rounded.Close,
                        onClick = safeNavigateBack,
                        containerColor = Color.Transparent,
                        description = stringResource(R.string.close_workout)
                    )
                },
                actions = {
                    if (state.totalExercises > 0) {
                        Surface(
                            color = Primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${state.completedExercises}/${state.totalExercises}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        if (state.warmupTimerEnabled) {
                            IconButton(
                                onClick = {
                                    warmupTimerDuration = 120
                                    showWarmupTimerDialog = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = stringResource(R.string.warmup_timer),
                                    tint = if (state.warmupTimerRemaining > 0) Primary else OnSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    Surface(
                        onClick = { showCancelDialog = true },
                        shape = CircleShape,
                        color = Error.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.cancel_workout),
                            tint = Error,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.totalExercises == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = null,
                            tint = OnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.no_exercises_in_session),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        GymButton(onClick = { showAddExerciseSheet = true }) {
                            Text(stringResource(R.string.add_exercise_to_workout).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } else if (currentExState != null) {
                AnimatedContent(
                    targetState = state.currentExerciseIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 380f
                            )
                        ) { width -> direction * width } + fadeIn(
                            animationSpec = spring(stiffness = 380f)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 380f
                                )
                            ) { width -> -direction * width } + fadeOut(
                                animationSpec = spring(stiffness = 380f)
                            )
                        )
                    },
                    label = "exercise_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetIndex ->
                    val targetExState = state.exercises.getOrNull(targetIndex)
                    if (targetExState != null) {
                        val exerciseListState = rememberLazyListState()
                        val targetActiveSetIndex = remember(targetExState.sets) {
                            targetExState.sets.indexOfFirst { !it.isCompleted }.takeIf { it != -1 } ?: ((targetExState.sets.size) - 1)
                        }
                        val targetActiveSet = remember(targetExState.sets, targetActiveSetIndex) {
                            targetExState.sets.getOrNull(targetActiveSetIndex)
                        }
                        val targetIsExerciseCompleted = remember(targetActiveSet) {
                            targetActiveSet == null || targetActiveSet.isCompleted
                        }

                        LaunchedEffect(targetActiveSetIndex) {
                            if (targetActiveSetIndex != -1 && targetExState.sets.isNotEmpty()) {
                                exerciseListState.animateScrollToItem(targetActiveSetIndex)
                            }
                        }

                        // Bring the set being edited above the hub that opens under it
                        LaunchedEffect(editingSetIndex) {
                            val editing = editingSetIndex
                            if (editing != null && targetIndex == state.currentExerciseIndex && editing in targetExState.sets.indices) {
                                exerciseListState.animateScrollToItem(editing)
                            }
                        }

                        val swipeOffset = remember { Animatable(0f) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (state.swipeActionsEnabled) {
                                        Modifier
                                            .graphicsLayer { translationX = swipeOffset.value }
                                            .pointerInput(state.currentExerciseIndex) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { coroutineScope.launch { swipeOffset.snapTo(0f) } },
                                                    onDragEnd = {
                                                        val threshold = with(density) { 50.dp.toPx() }
                                                        if (swipeOffset.value > threshold && targetIndex > 0) {
                                                            coroutineScope.launch { swipeOffset.animateTo(0f, tween(200)) }
                                                            viewModel.previousExercise()
                                                        } else if (swipeOffset.value < -threshold && targetIndex < state.exercises.size - 1) {
                                                            coroutineScope.launch { swipeOffset.animateTo(0f, tween(200)) }
                                                            viewModel.nextExercise()
                                                        } else {
                                                            coroutineScope.launch { swipeOffset.animateTo(0f, spring()) }
                                                        }
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        coroutineScope.launch { swipeOffset.snapTo(swipeOffset.value + dragAmount) }
                                                    }
                                                )
                                            }
                                    } else Modifier
                                )
                        ) {
                            if (targetExState.isCardio) {
                                val showBottomHubForCardio = targetExState.isCardioCompleted || (!state.cardioTimerRunning && !state.cardioTimerPaused)
                                val cardioBottomPadding by animateDpAsState(
                                    targetValue = when {
                                        showBottomHubForCardio && (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) -> 190.dp
                                        showBottomHubForCardio || state.remainingRestSeconds > 0 -> 140.dp
                                        else -> 32.dp
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "cardioBottomPadding"
                                )
                                CardioExerciseContent(
                                    exerciseState = targetExState,
                                    languageCode = languageCode,
                                    state = state,
                                    viewModel = viewModel,
                                    onSwap = { showSwapExerciseSheet = true },
                                    onAddExercise = {
                                        isAddingAfterCurrent = false
                                        showAddExerciseSheet = true
                                    },
                                    distanceInput = cardioDistanceInput,
                                    onDistanceChange = { cardioDistanceInput = it },
                                    bottomPadding = cardioBottomPadding
                                )
                            } else if (usesEmomClock(targetExState)) {
                                val emomIdle = state.emomRun == null
                                val emomBottomPadding by animateDpAsState(
                                    targetValue = when {
                                        !emomIdle -> 32.dp
                                        state.remainingRestSeconds > 0 -> 230.dp
                                        state.isQuickWorkout || state.inlineExerciseModificationsEnabled -> 190.dp
                                        else -> 140.dp
                                    },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "emomBottomPadding"
                                )
                                EmomExerciseContent(
                                    exerciseIndex = targetIndex,
                                    exerciseState = targetExState,
                                    languageCode = languageCode,
                                    state = state,
                                    viewModel = viewModel,
                                    onSwap = { showSwapExerciseSheet = true },
                                    onLogManually = { emomManualExercises = emomManualExercises + targetExState.exercise.id },
                                    bottomPadding = emomBottomPadding
                                )
                            } else {
                                var expandedSetIndex by remember(targetIndex) { mutableStateOf<Int?>(targetActiveSetIndex) }
                                LaunchedEffect(targetActiveSetIndex) {
                                    if (targetExState.isTimeAndWeight) {
                                        expandedSetIndex = targetActiveSetIndex
                                    }
                                }

                                // Large exercise media: fills the room left between the last set and the hub, and
                                // gives way (header thumbnail only) as soon as the sets or the hub need that room.
                                val localDensity = LocalDensity.current
                                val largeMediaFile = exerciseMedia[targetExState.exercise.id]
                                    ?.takeIf { state.exerciseMediaEnabled && state.exerciseMediaLarge }
                                var freeMediaPx by remember(targetIndex) { mutableIntStateOf(0) }
                                val largeMediaVisible = largeMediaFile != null &&
                                    freeMediaPx >= with(localDensity) { MEDIA_CARD_MIN_HEIGHT.roundToPx() }
                                LaunchedEffect(largeMediaFile != null, hubHeightPx) {
                                    if (largeMediaFile == null || hubHeightPx <= 0) {
                                        freeMediaPx = 0
                                        return@LaunchedEffect
                                    }
                                    val bottomGapPx = with(localDensity) { 16.dp.roundToPx() }
                                    snapshotFlow {
                                        // While the list is scrolled the sets' position says nothing about free room: keep the last value
                                        if (exerciseListState.firstVisibleItemIndex != 0 || exerciseListState.firstVisibleItemScrollOffset != 0) {
                                            null
                                        } else {
                                            val info = exerciseListState.layoutInfo
                                            info.visibleItemsInfo.firstOrNull { it.key == MEDIA_CARD_KEY }
                                                ?.let { card -> info.viewportSize.height - card.offset - hubHeightPx - bottomGapPx }
                                                ?.coerceAtLeast(0)
                                                ?: 0
                                        }
                                    }.collect { free -> if (free != null) freeMediaPx = free }
                                }

                                // Exercise Header
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // An advanced exercise reads what the current week prescribes: the plan's own
                                        // sets and reps targets are only kept in step with the first week
                                        val weekTargets = if (targetExState.isAdvanced) {
                                            PrescriptionExpander.legacyTargets(targetExState.blocks)
                                        } else null
                                        val setsCount = weekTargets?.first ?: targetExState.sets.size
                                        val repsCount = if (targetExState.isTimeAndWeight) {
                                            val sec = targetExState.timeTargetSeconds ?: targetExState.sets.firstOrNull()?.timeSeconds ?: 45
                                            "${sec}s"
                                        } else {
                                            weekTargets?.second ?: targetExState.planDetails?.repsTarget ?: targetExState.customRepsTarget ?: run {
                                                if (targetExState.sets.isEmpty()) "0"
                                                else {
                                                    val allReps = targetExState.sets.map { it.reps }
                                                    if (allReps.distinct().size == 1) allReps.first().toString()
                                                    else allReps.joinToString("-")
                                                }
                                            }
                                        }
                                        if (targetExState.isAdvanced) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                state.programWeek?.let { week ->
                                                    PrescriptionPill(
                                                        text = stringResource(R.string.week_short, week),
                                                        containerColor = Primary,
                                                        contentColor = OnPrimary,
                                                        emphasized = true
                                                    )
                                                }
                                                val usesPercent = targetExState.blocks.any { it.intensityType == IntensityType.PERCENT }
                                                if (usesPercent) {
                                                    PrescriptionPill(
                                                        text = targetExState.oneRepMaxKg?.let { stringResource(R.string.one_rep_max_value, prescriptionLabels.weight(it)) }
                                                            ?: stringResource(R.string.one_rep_max_missing),
                                                        containerColor = if (targetExState.oneRepMaxKg != null) Primary.copy(alpha = 0.12f) else Error.copy(alpha = 0.12f),
                                                        contentColor = if (targetExState.oneRepMaxKg != null) Primary else Error
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$setsCount × $repsCount",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Primary,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        } else Text(
                                            text = "$setsCount × $repsCount",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (targetIndex < state.exercises.lastIndex) {
                                            val nextEx = state.exercises[targetIndex + 1]
                                            val isLinked = targetExState.supersetId != null && targetExState.supersetId == nextEx.supersetId
                                            IconButton(
                                                onClick = { viewModel.toggleSupersetWithNext(targetIndex) }
                                            ) {
                                                Icon(
                                                    imageVector = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                                                    contentDescription = stringResource(R.string.toggle_superset_with_next),
                                                    tint = if (isLinked) Primary else OnSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { showSwapExerciseSheet = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.SwapHoriz,
                                                contentDescription = stringResource(R.string.swap_exercise),
                                                tint = OnSurfaceVariant
                                            )
                                        }
                                    }
                                    val exerciseDisplayName = ExerciseTranslations.translate(targetExState.exercise.nome, languageCode)
                                    if (state.exerciseMediaEnabled) {
                                        // The thumbnail takes width from the name, so the name steps down one size
                                        // to wrap onto the same number of lines it did at full width.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = exerciseDisplayName,
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = OnSurface,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ExerciseMediaSlot(
                                                fileName = exerciseMedia[targetExState.exercise.id],
                                                onOpen = { mediaViewer = targetExState.exercise.id to exerciseDisplayName },
                                                onAdd = { pickMedia(targetExState.exercise.id) },
                                                playing = !largeMediaVisible
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = exerciseDisplayName,
                                            style = MaterialTheme.typography.displaySmall,
                                            color = OnSurface,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    if (targetExState.emomStartIndex != null && targetExState.exercise.id in emomManualExercises) {
                                        Surface(
                                            onClick = { emomManualExercises = emomManualExercises - targetExState.exercise.id },
                                            color = Tertiary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Timer,
                                                    contentDescription = null,
                                                    tint = Tertiary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.emom_use_timer),
                                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                    color = Tertiary,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }

                                    if (targetExState.supersetId != null) {
                                        Surface(
                                            color = Primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Timer,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.superset),
                                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                    color = Primary,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Sets List
                                LazyColumn(
                                    state = exerciseListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(targetExState.sets) { index, set ->
                                        val isActive = index == targetActiveSetIndex && !set.isCompleted
                                        var showDeleteConfirm by remember { mutableStateOf(false) }

                                        if (showDeleteConfirm) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteConfirm = false },
                                                title = { Text(stringResource(R.string.remove_set)) },
                                                text = { Text(stringResource(R.string.remove_set_confirm)) },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        editingSetIndex = null
                                                        viewModel.removeSetFromExercise(targetIndex, index)
                                                        showDeleteConfirm = false
                                                    }) {
                                                        Text(stringResource(R.string.delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                                        Text(stringResource(R.string.cancel).uppercase())
                                                    }
                                                },
                                                containerColor = SurfaceContainerHigh,
                                                titleContentColor = OnSurface,
                                                textContentColor = OnSurfaceVariant
                                            )
                                        }

                                        val haptic = LocalHapticFeedback.current
                                        val isExpanded = targetExState.isTimeAndWeight && (expandedSetIndex == index) && !set.isCompleted

                                        if (targetExState.isAdvanced) {
                                            val previous = targetExState.sets.getOrNull(index - 1)
                                            val p = set.prescription
                                            val startsGroup = index == 0 ||
                                                previous?.isExtra != set.isExtra ||
                                                previous?.prescription?.blockIndex != p?.blockIndex
                                            if (startsGroup) {
                                                val block = p?.let { targetExState.blocks.getOrNull(it.blockIndex) }
                                                Column(
                                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = when {
                                                            block != null -> stringResource(
                                                                R.string.block_header, p.blockIndex + 1,
                                                                (listOf(PrescriptionFormatter.headline(block, prescriptionLabels)) +
                                                                    block.techniques.map { techniqueLabel(context, it) }).joinToString(" · ")
                                                            ).uppercase()
                                                            set.isExtra -> stringResource(R.string.extra_badge)
                                                            else -> ""
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                                        color = Primary,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    if (block?.repMode == RepMode.TOTAL) {
                                                        val total = block.totalReps ?: 0
                                                        val done = targetExState.sets
                                                            .filter { it.prescription?.blockIndex == p.blockIndex && it.isCompleted }
                                                            .sumOf { it.reps }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            androidx.compose.material3.LinearWavyProgressIndicator(
                                                                progress = { if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f },
                                                                modifier = Modifier.weight(1f),
                                                                color = Primary,
                                                                trackColor = SurfaceContainerHighest
                                                            )
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(
                                                                text = stringResource(R.string.total_reps_progress, done, total),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = Primary,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                    }
                                                    block?.note?.let { note ->
                                                        Text(note, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }

                                        val setPrescription = set.prescription
                                        SetLogRow(
                                            repsLabel = if (set.isAmrap) stringResource(R.string.max_label) else null,
                                            intensityBadge = setPrescription?.let { PrescriptionFormatter.intensity(it.intensityType, it.intensityValue, prescriptionLabels) }
                                                ?.takeIf { setPrescription.intensityType == IntensityType.PERCENT || setPrescription.intensityType == IntensityType.RPE },
                                            badges = buildList {
                                                if (set.isAmrap && set.isCompleted) add(stringResource(R.string.max_label))
                                                setPrescription?.techniques?.forEach { add(techniqueLabel(context, it)) }
                                                set.rpe?.let { add(stringResource(R.string.rpe_short, PrescriptionFormatter.number(it))) }
                                                if (set.isExtra) add(stringResource(R.string.extra_badge))
                                            },
                                            setNumber = set.setNumber,
                                            weight = set.weight,
                                            reps = set.reps,
                                            note = set.note,
                                            isWarmup = set.isWarmup,
                                            isCompleted = set.isCompleted,
                                            onToggleComplete = { 
                                                if (set.isAmrap && !set.isCompleted && index == targetActiveSetIndex) {
                                                    // AMRAP: reps must be entered before logging
                                                    isEditingValues = true
                                                } else if (targetExState.isTimeAndWeight && !set.isCompleted) {
                                                    viewModel.skipTimerAndLogSet(
                                                        targetIndex,
                                                        index,
                                                        set.weight,
                                                        set.timeSeconds ?: targetExState.timeTargetSeconds ?: 45
                                                    )
                                                } else {
                                                    viewModel.toggleSetComplete(targetIndex, index) 
                                                }
                                            },
                                            onNoteChange = { newNote ->
                                                viewModel.updateSetNote(targetIndex, index, newNote)
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showDeleteConfirm = true
                                            },
                                            onEditValues = {
                                                editingSetIndex = null
                                                isEditingValues = !isEditingValues
                                            },
                                            // A tap on the card edits that set in place, done or still to do; the square
                                            // on the right is what checks and unchecks it
                                            onEdit = {
                                                editingSetIndex = if (editingSetIndex == index) null else index
                                                isEditingValues = false
                                            },
                                            isEditing = editingSetIndex == index && targetIndex == state.currentExerciseIndex,
                                            isActive = isActive,
                                            weightUnit = state.weightUnit,
                                            previousNote = set.previousNote,
                                            timeSeconds = set.timeSeconds,
                                            isExpanded = isExpanded,
                                            onToggleExpanded = if (targetExState.isTimeAndWeight && !set.isCompleted) {
                                                { expandedSetIndex = if (expandedSetIndex == index) null else index }
                                            } else null,
                                            expandedContent = if (isExpanded) {
                                                {
                                                    val targetSeconds = set.timeSeconds ?: targetExState.timeTargetSeconds ?: 45
                                                    val isCurrentActive = index == targetActiveSetIndex
                                                    val timerSeconds = if (isCurrentActive) state.setTimerSeconds else 0
                                                    val isRunning = isCurrentActive && state.setTimerRunning
                                                    val isPaused = isCurrentActive && state.setTimerPaused
                                                    val isTargetReached = targetSeconds > 0 && timerSeconds >= targetSeconds
                                                    val timerColor = if (isTargetReached) Color(0xFF4CAF50) else Primary
                                                    val timerTrackColor = if (isTargetReached) Color(0xFF4CAF50).copy(alpha = 0.25f) else Primary.copy(alpha = 0.2f)
                                                    val progress = if (targetSeconds > 0) (timerSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f) else 0f

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(SurfaceContainerHighest.copy(alpha = 0.5f))
                                                            .padding(vertical = 16.dp, horizontal = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.size(130.dp)
                                                            ) {
                                                                CircularWavyProgressIndicator(
                                                                    progress = { progress },
                                                                    modifier = Modifier.size(130.dp),
                                                                    color = timerColor,
                                                                    trackColor = timerTrackColor,
                                                                    stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                                                                    trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                                                                    gapSize = 8.dp,
                                                                    wavelength = 28.dp,
                                                                    amplitude = { p ->
                                                                        if (!isRunning) 0f
                                                                        else if (p <= 0.1f) 0f
                                                                        else 1f
                                                                    }
                                                                )
                                                                if (state.autoStopTimeWeightAtTarget && isTargetReached) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(80.dp)
                                                                            .background(timerColor, CircleShape),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.ThumbUp,
                                                                            contentDescription = null,
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(36.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    CardioToggleButton(
                                                                        isRunning = isRunning,
                                                                        isPaused = isPaused,
                                                                        onToggle = {
                                                                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            when {
                                                                                isRunning -> viewModel.pauseSetTimer()
                                                                                else -> viewModel.startSetTimer()
                                                                            }
                                                                        },
                                                                        modifier = Modifier.size(80.dp),
                                                                        activeColor = timerColor,
                                                                        onActiveColor = if (isTargetReached) Color.White else OnPrimary
                                                                    )
                                                                }
                                                            }

                                                            val curFormatted = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60)
                                                            val tarFormatted = String.format("%02d:%02d", targetSeconds / 60, targetSeconds % 60)
                                                            Row(
                                                                verticalAlignment = Alignment.Bottom,
                                                                horizontalArrangement = Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = curFormatted,
                                                                    style = MaterialTheme.typography.headlineMedium,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = if (isTargetReached) timerColor else if (isRunning) Primary else OnSurface
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = "/ $tarFormatted",
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    color = OnSurfaceVariant,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                                )
                                                            }

                                                            if (isRunning || isPaused || timerSeconds > 0) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.Center,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    FilledIconButton(
                                                                        onClick = {
                                                                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            viewModel.resetSetTimer()
                                                                        },
                                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                                            containerColor = SurfaceContainerHigh,
                                                                            contentColor = OnSurfaceVariant
                                                                        ),
                                                                        modifier = Modifier.size(42.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.Replay,
                                                                            contentDescription = stringResource(R.string.time_weight_reset_timer),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }

                                                                    Spacer(modifier = Modifier.width(12.dp))

                                                                    GymButton(
                                                                        onClick = {
                                                                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            viewModel.finishSetTimer(targetIndex, index, set.weight)
                                                                        },
                                                                        containerColor = timerColor,
                                                                        contentColor = if (isTargetReached) Color.White else OnPrimary,
                                                                        modifier = Modifier.fillMaxWidth(0.72f),
                                                                        height = 42
                                                                    ) {
                                                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                        Text(
                                                                            stringResource(R.string.time_weight_finish_set),
                                                                            style = MaterialTheme.typography.labelLarge,
                                                                            fontWeight = FontWeight.ExtraBold
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else null
                                        )
                                    }
                                    if (targetExState.isAdvanced) {
                                        item {
                                            GymButton(
                                                onClick = { viewModel.addExtraSet(targetIndex) },
                                                modifier = Modifier.fillMaxWidth(),
                                                containerColor = Primary.copy(alpha = 0.1f),
                                                contentColor = Primary,
                                                height = 48
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.add_extra_set).uppercase(), fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                     if (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                GymButton(
                                                    onClick = { viewModel.addSetToExercise(targetIndex) },
                                                    modifier = Modifier.weight(1f),
                                                    containerColor = SurfaceContainerHigh,
                                                    contentColor = Primary
                                                ) {
                                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(stringResource(R.string.add_set), fontWeight = FontWeight.ExtraBold)
                                                }
                                                if (!isExerciseCompleted) {
                                                    val isLastExercise = targetIndex == state.exercises.size - 1
                                                    GymButton(
                                                        onClick = {
                                                            isAddingAfterCurrent = !state.isQuickWorkout
                                                            showAddExerciseSheet = true
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        containerColor = SurfaceContainerHigh,
                                                        contentColor = Primary
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                                            contentDescription = null
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = stringResource(
                                                                if (state.isQuickWorkout) {
                                                                    if (isLastExercise) R.string.add_exercise else R.string.next_exercise_wrk
                                                                } else {
                                                                    R.string.add_exercise
                                                                }
                                                            ).uppercase(),
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (largeMediaFile != null) {
                                        item(key = MEDIA_CARD_KEY) {
                                            val targetHeight = if (largeMediaVisible) with(localDensity) { freeMediaPx.toDp() } else 0.dp
                                            val cardHeight by animateDpAsState(targetHeight, label = "exerciseMediaCardHeight")
                                            if (cardHeight > 0.dp) {
                                                ExerciseMediaCard(
                                                    fileName = largeMediaFile,
                                                    onOpen = {
                                                        mediaViewer = targetExState.exercise.id to
                                                            ExerciseTranslations.translate(targetExState.exercise.nome, languageCode)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(cardHeight)
                                                )
                                            }
                                        }
                                    }
                                    item(key = "hub_spacer") {
                                        // With the large media card the list already ends exactly above the hub
                                        val hubSpacer = if (largeMediaVisible) {
                                            with(localDensity) { hubHeightPx.toDp() }
                                        } else if (com.emanuel5014.trainable.ui.theme.ResponsiveSize.isShortHeight) 220.dp else 280.dp
                                        Spacer(modifier = Modifier.height(hubSpacer)) // Space for the dynamic hub
                                    }
                                }
                            }
                        }
                    }
                }

                // DYNAMIC INTERACTION HUB
                val currentExState = state.currentExercise
                val isCardioActive = currentExState?.isCardio == true
                val isEmomActive = currentExState != null && usesEmomClock(currentExState)
                // While the minute clock runs, its screen has everything the lifter needs.
                val showBottomHub = (!isCardioActive || currentExState.isCardioCompleted || (!state.cardioTimerRunning && !state.cardioTimerPaused)) &&
                    !(isEmomActive && state.emomRun != null)

                AnimatedVisibility(
                    visible = showBottomHub,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { hubHeightPx = it.height },
                        color = Surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = com.emanuel5014.trainable.ui.theme.ResponsiveSize.horizontalPadding, vertical = 16.dp)
                    ) {
                        val isResting = state.remainingRestSeconds > 0

                        AnimatedVisibility(
                            visible = state.warmupTimerEnabled && state.warmupTimerRemaining > 0,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                        ) {
                            val warmupProgress = if (state.warmupTimerTotalSeconds > 0)
                                1f - (state.warmupTimerRemaining.toFloat() / state.warmupTimerTotalSeconds.toFloat()) else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Tertiary)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularWavyProgressIndicator(
                                                progress = { warmupProgress },
                                                modifier = Modifier.size(48.dp),
                                                color = OnTertiary,
                                                trackColor = OnTertiary.copy(alpha = 0.2f),
                                                stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                                                trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Timer,
                                                contentDescription = null,
                                                tint = OnTertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.warmup_label).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OnTertiary.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val wMinutes = state.warmupTimerRemaining / 60
                                            val wSeconds = state.warmupTimerRemaining % 60
                                            Text(
                                                text = String.format("%d:%02d", wMinutes, wSeconds),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = OnTertiary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (state.showTimerSubtract) {
                                            TimerAdjustButton(
                                                label = "−${state.timerSubtractSeconds}s",
                                                onClick = { viewModel.adjustWarmupTime(-state.timerSubtractSeconds) }
                                            )
                                        }
                                        if (state.showTimerAdd) {
                                            TimerAdjustButton(
                                                label = "+${state.timerAddSeconds}s",
                                                onClick = { viewModel.adjustWarmupTime(state.timerAddSeconds) }
                                            )
                                        }
                                        FilledIconButton(
                                            onClick = { viewModel.skipWarmupTimer() },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = OnTertiary,
                                                contentColor = Tertiary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.cancel)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedContent(
                            targetState = when {
                                // A set being edited wins over the rest card, which stays visible in a slim form
                                setBeingEdited != null && !isCardioActive && !isEmomActive -> HubMode.EditingSet
                                isResting -> HubMode.Resting
                                // Cardio and the EMOM clock have their own screens: the hub only navigates.
                                isCardioActive || isEmomActive -> HubMode.Cardio
                                activeSet == null || activeSet.isCompleted -> HubMode.Completed
                                isEditingValues -> HubMode.Editing
                                else -> HubMode.Logging
                            },
                            label = "HubTransition",
                            transitionSpec = {
                                (fadeIn() + expandVertically(expandFrom = Alignment.Bottom))
                                    .togetherWith(fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom))
                            }
                        ) { mode ->
                            when (mode) {
                                HubMode.Cardio -> {
                                    currentExState?.let { ex ->
                                        CardioHubSection(
                                            exerciseState = ex,
                                            state = state,
                                            viewModel = viewModel,
                                            languageCode = languageCode,
                                            onFinishWorkout = { showFinishDialog = true },
                                            onAddExercise = {
                                                isAddingAfterCurrent = false
                                                showAddExerciseSheet = true
                                            }
                                        )
                                    }
                                }
                                HubMode.EditingSet -> {
                                    val editedIndex = editingSetIndex ?: lastEditedIndex
                                    val exercise = currentExState
                                    val editedSet = exercise?.sets?.getOrNull(editedIndex)
                                    if (exercise != null && editedSet != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    stringResource(R.string.adjust_set_number, editedSet.setNumber),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Primary,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // The rest card is hidden while editing: keep the countdown in sight
                                                    if (isResting) RestTimerPill(state.remainingRestSeconds)
                                                    IconButton(onClick = { editingSetIndex = null }) {
                                                        Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.collapse))
                                                    }
                                                }
                                            }
                                            if (exercise.isTimeAndWeight) {
                                                WeightTimeInput(
                                                    weight = editedSet.weight,
                                                    seconds = editedSet.timeSeconds ?: exercise.timeTargetSeconds ?: 45,
                                                    onWeightChange = { viewModel.editSetWeight(state.currentExerciseIndex, editedIndex, it) },
                                                    onSecondsChange = { viewModel.editSetSeconds(state.currentExerciseIndex, editedIndex, it) },
                                                    weightUnit = state.weightUnit
                                                )
                                            } else {
                                                WeightRepsInput(
                                                    weight = editedSet.weight,
                                                    reps = editedSet.reps,
                                                    onWeightChange = { viewModel.editSetWeight(state.currentExerciseIndex, editedIndex, it) },
                                                    onRepsChange = { viewModel.editSetReps(state.currentExerciseIndex, editedIndex, it) },
                                                    weightUnit = state.weightUnit
                                                )
                                                if (rpeInputShown(state.rpeInputMode, exercise, editedSet)) {
                                                    RpeSelector(
                                                        value = editedSet.rpe,
                                                        onValueChange = { viewModel.editSetRpe(state.currentExerciseIndex, editedIndex, it) }
                                                    )
                                                }
                                            }
                                            GymButton(
                                                onClick = { editingSetIndex = null },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Rounded.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    stringResource(R.string.done).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                                HubMode.Resting -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RestTimerSection(
                                            remainingSeconds = state.remainingRestSeconds,
                                            totalRestSeconds = state.totalRestSeconds,
                                            onAddTime = { viewModel.adjustRestTime(state.timerAddSeconds) },
                                            onSubtractTime = { viewModel.adjustRestTime(-state.timerSubtractSeconds) },
                                            addSeconds = state.timerAddSeconds,
                                            subtractSeconds = state.timerSubtractSeconds,
                                            showAdd = state.showTimerAdd,
                                            showSubtract = state.showTimerSubtract,
                                            onSkip = { viewModel.skipRestTimer() }
                                        )
                                        // The rest is the time to load the bar for the next set
                                        ActiveSetPlateChip(currentExState, activeSet, state, plateExercises) { showPlateSheet = true }
                                    }
                                }
                                HubMode.Editing -> {
                                    activeSet?.let { set ->
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    stringResource(R.string.adjust_set_number, set.setNumber),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Primary,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                IconButton(onClick = { isEditingValues = false }) {
                                                    Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.collapse))
                                                }
                                            }
                                            if (currentExState?.isTimeAndWeight == true) {
                                                WeightTimeInput(
                                                    weight = set.weight,
                                                    seconds = set.timeSeconds ?: currentExState.timeTargetSeconds ?: 45,
                                                    onWeightChange = { newW -> viewModel.updateSetWeight(state.currentExerciseIndex, activeSetIndex, newW) },
                                                    onSecondsChange = { newT -> viewModel.updateSetTimeSeconds(state.currentExerciseIndex, activeSetIndex, newT) },
                                                    weightUnit = state.weightUnit
                                                )
                                            } else {
                                                if (set.isAmrap) {
                                                    Text(
                                                        text = stringResource(R.string.amrap_enter_reps),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                WeightRepsInput(
                                                    weight = set.weight,
                                                    reps = set.reps,
                                                    onWeightChange = { newW -> viewModel.updateSetWeight(state.currentExerciseIndex, activeSetIndex, newW) },
                                                    onRepsChange = { newR -> viewModel.updateSetReps(state.currentExerciseIndex, activeSetIndex, newR) },
                                                    weightUnit = state.weightUnit
                                                )
                                                if (state.plateCalculatorEnabled && currentExState != null) {
                                                    val exerciseId = currentExState.exercise.id
                                                    if (exerciseId in plateExercises) {
                                                        PlateChip(
                                                            weightKg = set.weight,
                                                            barKg = plateExercises[exerciseId],
                                                            weightUnit = state.weightUnit,
                                                            plates = state.availablePlates,
                                                            onClick = { showPlateSheet = true }
                                                        )
                                                    } else {
                                                        PlateCalculatorEnableChip(onClick = {
                                                            viewModel.setPlateCalculator(exerciseId, true, null)
                                                            showPlateSheet = true
                                                        })
                                                    }
                                                }
                                                val showRpe = rpeInputShown(state.rpeInputMode, currentExState, set)
                                                if (showRpe) {
                                                    RpeSelector(
                                                        value = set.rpe,
                                                        onValueChange = { viewModel.updateSetRpe(state.currentExerciseIndex, activeSetIndex, it) }
                                                    )
                                                }
                                            }
                                            LogSetButton(onClick = {
                                                if (currentExState?.isTimeAndWeight == true) {
                                                    viewModel.skipTimerAndLogSet(
                                                        state.currentExerciseIndex,
                                                        activeSetIndex,
                                                        set.weight,
                                                        set.timeSeconds ?: currentExState.timeTargetSeconds ?: 45
                                                    )
                                                } else {
                                                    viewModel.toggleSetComplete(state.currentExerciseIndex, activeSetIndex)
                                                }
                                                isEditingValues = false
                                            })
                                        }
                                    }
                                }
                                HubMode.Logging -> {
                                    activeSet?.let { set ->
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(SurfaceContainerHigh)
                                                    .clickable { isEditingValues = true }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(Primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${set.setNumber}", color = Primary, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.active_set), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                                    val repsOrTime = if (currentExState?.isTimeAndWeight == true) {
                                                        "${set.timeSeconds ?: currentExState.timeTargetSeconds ?: 45}s"
                                                    } else if (set.isAmrap) {
                                                        stringResource(R.string.max_label)
                                                    } else {
                                                        "${set.reps}"
                                                    }
                                                    Text(
                                                        text = WeightUnitConverter.formatWithUnit(
                                                            WeightUnitConverter.convertDisplay(set.weight, state.weightUnit),
                                                            state.weightUnit
                                                        ) + " × $repsOrTime", 
                                                        style = MaterialTheme.typography.titleLarge, 
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    val p = set.prescription
                                                    val detail = listOfNotNull(
                                                        p?.let { PrescriptionFormatter.intensity(it.intensityType, it.intensityValue, prescriptionLabels) }
                                                    ) + p?.techniques.orEmpty().map { techniqueLabel(context, it) } +
                                                        listOfNotNull(if (set.isExtra) stringResource(R.string.extra_badge) else null)
                                                    if (detail.isNotEmpty()) {
                                                        Text(
                                                            text = detail.joinToString(" · "),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = Primary,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Icon(Icons.Rounded.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                LogSetButton(
                                                    onClick = {
                                                        if (set.isAmrap) {
                                                            isEditingValues = true
                                                        } else if (currentExState?.isTimeAndWeight == true) {
                                                            viewModel.skipTimerAndLogSet(
                                                                state.currentExerciseIndex,
                                                                activeSetIndex,
                                                                set.weight,
                                                                set.timeSeconds ?: currentExState.timeTargetSeconds ?: 45
                                                            )
                                                        } else {
                                                            viewModel.toggleSetComplete(state.currentExerciseIndex, activeSetIndex)
                                                        }
                                                    },
                                                    modifier = Modifier.width(120.dp),
                                                    compact = true
                                                )
                                            }
                                            ActiveSetPlateChip(currentExState, set, state, plateExercises) { showPlateSheet = true }
                                        }
                                    }
                                }
                                HubMode.Completed -> {
                                    val isLastExercise = state.currentExerciseIndex == state.exercises.size - 1
                                    val allowModify = state.isQuickWorkout || state.inlineExerciseModificationsEnabled

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (allowModify) {
                                            GymButton(
                                                onClick = {
                                                    isAddingAfterCurrent = false
                                                    showAddExerciseSheet = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                containerColor = SurfaceContainerHigh,
                                                contentColor = Primary
                                            ) {
                                                Icon(Icons.Rounded.KeyboardDoubleArrowRight, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.add_exercise).uppercase(),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Previous Exercise Button
                                            if (state.currentExerciseIndex > 0) {
                                                val prevName = state.exercises[state.currentExerciseIndex - 1].exercise.nome
                                                val translatedPrev = ExerciseTranslations.translate(prevName, languageCode)
                                                GymButton(
                                                    onClick = { viewModel.previousExercise() },
                                                    modifier = Modifier.weight(1f),
                                                    containerColor = SurfaceContainerHigh,
                                                    contentColor = OnSurface,
                                                    height = 64
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                                        contentDescription = null
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(
                                                        horizontalAlignment = Alignment.Start,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.previous_exercise_btn).uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = OnSurfaceVariant,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                        Text(
                                                            text = translatedPrev,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Black,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }

                                            // Next / Finish Button
                                            if (!isLastExercise) {
                                                val nextName = state.exercises[state.currentExerciseIndex + 1].exercise.nome
                                                val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                                                GymButton(
                                                    onClick = { viewModel.nextExercise() },
                                                    modifier = Modifier.weight(1f),
                                                    height = 64
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.next_exercise_btn).uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = OnPrimary.copy(alpha = 0.75f),
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                        Text(
                                                            text = translatedNext,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = OnPrimary,
                                                            fontWeight = FontWeight.Black,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                        contentDescription = null
                                                    )
                                                }
                                            } else if (state.isQuickWorkout) {
                                                // Quick workout last exercise finish button
                                                GymButton(
                                                    onClick = { showFinishDialog = true },
                                                    modifier = Modifier.weight(1f),
                                                    height = 64
                                                ) {
                                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.finish_workout).uppercase(),
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            } else {
                                                // Guided / Quick workout last exercise finish button
                                                GymButton(
                                                    onClick = { showFinishDialog = true },
                                                    modifier = Modifier.weight(1f),
                                                    height = 64
                                                ) {
                                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.finish_workout).uppercase(),
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if ((!isExerciseCompleted && !isEmomActive) || isResting) {
                            Spacer(modifier = Modifier.height(16.dp))

                            ExerciseNavigation(
                                onPrevious = { viewModel.previousExercise() },
                                onNext = {
                                    if (state.currentExerciseIndex == state.exercises.size - 1) {
                                        showFinishDialog = true
                                    } else {
                                        viewModel.nextExercise()
                                    }
                                },
                                hasPrevious = state.currentExerciseIndex > 0,
                                hasNext = state.currentExerciseIndex < state.exercises.size - 1,
                                previousName = if (state.currentExerciseIndex > 0) state.exercises[state.currentExerciseIndex - 1].exercise.nome else null,
                                nextName = if (state.currentExerciseIndex < state.exercises.size - 1) state.exercises[state.currentExerciseIndex + 1].exercise.nome else null,
                                languageCode = languageCode
                            )
                        }
                    }
                }
            }
            }

            if (state.isFinishing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    GymLoadingIndicator()
                }
            }
        }
        
        if (showWarmupTimerDialog) {
            AlertDialog(
                onDismissRequest = { showWarmupTimerDialog = false },
                title = {
                    Text(stringResource(R.string.warmup_timer), fontWeight = FontWeight.Black)
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.warmup_timer_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val haptic = LocalHapticFeedback.current
                        RestSlider(
                            value = warmupTimerDuration,
                            onValueChange = { warmupTimerDuration = it },
                            hapticEnabled = true,
                            haptic = haptic
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.startWarmupTimer(warmupTimerDuration)
                        showWarmupTimerDialog = false
                    }) {
                        Text(stringResource(R.string.start).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWarmupTimerDialog = false }) {
                        Text(stringResource(R.string.cancel).uppercase())
                    }
                },
                containerColor = SurfaceContainerHigh,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant
            )
        }

        if (showPlateSheet && activeSet != null) {
            currentExState?.let { exState ->
                val exerciseId = exState.exercise.id
                PlateCalculatorSheet(
                    exerciseName = ExerciseTranslations.translate(exState.exercise.nome, languageCode),
                    weightKg = activeSet.weight,
                    barKg = plateExercises[exerciseId],
                    weightUnit = state.weightUnit,
                    plates = state.availablePlates,
                    onApply = { newWeightKg ->
                        viewModel.updateSetWeight(state.currentExerciseIndex, activeSetIndex, newWeightKg)
                    },
                    onBarChange = { barKg -> viewModel.setPlateCalculator(exerciseId, true, barKg) },
                    onDisable = { viewModel.setPlateCalculator(exerciseId, false, plateExercises[exerciseId]) },
                    onDismiss = { showPlateSheet = false }
                )
            }
        }

        if (showSwapExerciseSheet) {
            currentExState?.let { exState ->
                SwapExerciseBottomSheet(
                    // A cardio exercise has no sets or reps to carry over
                    currentSets = exState.sets.size.takeIf { it > 0 } ?: 3,
                    currentReps = if (exState.isCardio) "8" else exState.planDetails?.repsTarget ?: exState.customRepsTarget ?: "8",
                    availableExercises = availableExercises,
                    languageCode = languageCode,
                    onExerciseSelected = { newExercise, sets, reps, rest, exerciseType, durataTargetSec ->
                        viewModel.swapExercise(state.currentExerciseIndex, newExercise.id, sets, reps, rest, exerciseType, durataTargetSec)
                        showSwapExerciseSheet = false
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
                    onDismiss = { showSwapExerciseSheet = false },
                    editablePresetExercises = state.editablePresetExercises,
                    categories = categories,
                    onCardioExerciseSelected = { newExercise, durationMinutes, rest ->
                        viewModel.swapToCardioExercise(state.currentExerciseIndex, newExercise.id, durationMinutes, rest)
                        showSwapExerciseSheet = false
                    },
                    onAdvancedExerciseSelected = { newExercise, blocks, rest ->
                        val (sets, reps) = com.emanuel5014.trainable.domain.prescription.PrescriptionExpander.legacyTargets(blocks)
                        viewModel.swapExercise(state.currentExerciseIndex, newExercise.id, sets, reps, rest, blocks = blocks)
                        showSwapExerciseSheet = false
                    },
                    initialBlocks = exState.blocks,
                    currentExerciseId = exState.exercise.id
                )
            }
        }

        if (showAddExerciseSheet) {
            SwapExerciseBottomSheet(
                currentSets = 3,
                currentReps = "8",
                availableExercises = availableExercises,
                languageCode = languageCode,
                onExerciseSelected = { exercise, sets, reps, rest, exerciseType, durataTargetSec ->
                    if (isAddingAfterCurrent) {
                        viewModel.addExerciseAfterCurrent(exercise, sets, reps, rest, exerciseType = exerciseType, durataTargetSecondi = durataTargetSec)
                    } else {
                        viewModel.addExerciseToActiveSession(exercise, sets, reps, rest, exerciseType = exerciseType, durataTargetSecondi = durataTargetSec)
                    }
                    isAddingAfterCurrent = false
                    showAddExerciseSheet = false
                },
                onAddCustomExercise = { nome, categoria, onCreated ->
                    viewModel.addCustomExercise(nome, categoria, onCreated)
                },
                onEditCustomExercise = viewModel::updateCustomExercise,
                onDeleteCustomExercise = viewModel::deleteCustomExercise,
                onDismiss = {
                    isAddingAfterCurrent = false
                    showAddExerciseSheet = false
                },
                isAdding = true,
                editablePresetExercises = state.editablePresetExercises,
                categories = categories,
                onCardioExerciseSelected = { exercise, durationMinutes, rest ->
                    if (isAddingAfterCurrent) {
                        viewModel.addExerciseAfterCurrent(exercise, 1, "1", rest, durationMinutes)
                    } else {
                        viewModel.addExerciseToActiveSession(exercise, 1, "1", rest, durationMinutes)
                    }
                    isAddingAfterCurrent = false
                    showAddExerciseSheet = false
                },
                onAdvancedExerciseSelected = { exercise, blocks, rest ->
                    val (sets, reps) = com.emanuel5014.trainable.domain.prescription.PrescriptionExpander.legacyTargets(blocks)
                    if (isAddingAfterCurrent) {
                        viewModel.addExerciseAfterCurrent(exercise, sets, reps, rest, blocks = blocks)
                    } else {
                        viewModel.addExerciseToActiveSession(exercise, sets, reps, rest, blocks = blocks)
                    }
                    isAddingAfterCurrent = false
                    showAddExerciseSheet = false
                }
            )
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text(stringResource(R.string.cancel_workout_title)) },
                text = { Text(stringResource(R.string.cancel_workout_message)) },
                confirmButton = {
                    GymButton(
                        onClick = {
                            viewModel.cancelWorkout { safeNavigateBack() }
                            showCancelDialog = false
                        },
                        containerColor = Error.copy(alpha = 0.1f),
                        contentColor = Error,
                        modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                    ) {
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { showCancelDialog = false },
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

        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text(stringResource(R.string.finish_workout_title)) },
                text = {
                    Text(
                        stringResource(
                            if (state.completedExercises == state.totalExercises) R.string.finish_workout_message
                            else R.string.finish_workout_message_incomplete
                        )
                    )
                },
                confirmButton = {
                    GymButton(
                        onClick = {
                            viewModel.finishWorkout()
                            showFinishDialog = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                    ) {
                        Text(stringResource(R.string.finish_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { showFinishDialog = false },
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
    }
}

enum class HubMode { Logging, Editing, EditingSet, Resting, Completed, Cardio }

@Composable
fun rememberAnimatedShape(
    morph: Morph,
    progress: Float
): Shape {
    return remember(morph, progress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val path = morph.toPath(progress).asComposePath()
                val matrix = Matrix()
                matrix.scale(size.width, size.height)
                path.transform(matrix)
                path.translate(size.center - path.getBounds().center)
                return Outline.Generic(path)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardioToggleButton(
    isRunning: Boolean,
    isPaused: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Primary,
    onActiveColor: Color = OnPrimary
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isRunning -> activeColor
            isPaused -> Tertiary
            else -> activeColor
        },
        label = "cardioBtnBg",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isRunning -> onActiveColor
            isPaused -> OnTertiary
            else -> onActiveColor
        },
        label = "cardioBtnContent",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val shapeProgress by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardioShapeProgress"
    )

    val morph = remember { Morph(MaterialShapes.Cookie4Sided, MaterialShapes.Cookie9Sided) }
    val animatedShape = rememberAnimatedShape(morph, shapeProgress)

    val rotation = remember { Animatable(0f) }
    val transitionDuration = 1500
    val continuousTurnDuration = 15000
    val smoothHandoffEasing = remember { CubicBezierEasing(0.4f, 0.0f, 0.2f, 0.96f) }

    LaunchedEffect(isRunning) {
        rotation.snapTo(rotation.value % 360f)
        if (isRunning) {
            val startAngle = rotation.value
            val startupDelta = 720f
            val startupDurationNanos = transitionDuration * 1_000_000L
            val steadyDegreesPerNano = 360f / (continuousTurnDuration * 1_000_000f)
            var startNanos = 0L
            while (true) {
                val frameNanos = androidx.compose.runtime.withFrameNanos { it }
                if (startNanos == 0L) startNanos = frameNanos
                val elapsedNanos = frameNanos - startNanos
                val angle = if (elapsedNanos <= startupDurationNanos) {
                    val t = (elapsedNanos.toFloat() / startupDurationNanos).coerceIn(0f, 1f)
                    startAngle + startupDelta * smoothHandoffEasing.transform(t)
                } else {
                    val afterStartupNanos = elapsedNanos - startupDurationNanos
                    startAngle + startupDelta + (afterStartupNanos * steadyDegreesPerNano)
                }
                rotation.snapTo(angle)
            }
        } else {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = transitionDuration, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { rotationZ = rotation.value }
            .background(backgroundColor, animatedShape)
            .clip(animatedShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(0.4f)
                .graphicsLayer { rotationZ = -rotation.value },
            imageVector = when {
                isRunning -> Icons.Rounded.Pause
                isPaused -> Icons.Rounded.PlayArrow
                else -> Icons.Rounded.PlayArrow
            },
            contentDescription = null,
            tint = contentColor
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardioExerciseContent(
    exerciseState: WorkoutExerciseState,
    languageCode: String,
    state: WorkoutState,
    viewModel: WorkoutViewModel,
    onSwap: () -> Unit,
    onAddExercise: (() -> Unit)? = null,
    distanceInput: String,
    onDistanceChange: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completeMinutes by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    // Marking the exercise as done without the timer logs the time it was planned with. Without a planned time
    // there is nothing to log, so the minutes are asked for.
    val plannedSeconds = exerciseState.cardioDurataTargetSeconds?.takeIf { it > 0 }
    if (showCompleteDialog) {
        val typedSeconds = ((completeMinutes.replace(',', '.').toFloatOrNull() ?: 0f) * 60).toInt()
        val secondsToLog = plannedSeconds ?: typedSeconds
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showCompleteDialog = false },
            title = { Text(stringResource(R.string.cardio_mark_completed)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (plannedSeconds != null) {
                        val planned = if (plannedSeconds % 60 == 0) {
                            stringResource(R.string.cardio_min_format, plannedSeconds / 60)
                        } else {
                            String.format("%d:%02d", plannedSeconds / 60, plannedSeconds % 60)
                        }
                        Text(
                            text = stringResource(R.string.cardio_complete_logs_time, planned),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        OutlinedTextField(
                            value = completeMinutes,
                            onValueChange = { completeMinutes = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            label = { Text(stringResource(R.string.cardio_complete_duration)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = onDistanceChange,
                        label = { Text(stringResource(R.string.cardio_distance_label) + " " + stringResource(R.string.optional_suffix)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = secondsToLog > 0,
                    onClick = {
                        viewModel.completeCardio(distanceInput.replace(',', '.').toFloatOrNull() ?: 0f, secondsToLog)
                        showCompleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showStopDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showStopDialog = false },
            title = { Text(stringResource(R.string.cardio_enter_distance)) },
            text = {
                OutlinedTextField(
                    value = distanceInput,
                    onValueChange = onDistanceChange,
                    label = { Text(stringResource(R.string.cardio_distance_label) + " " + stringResource(R.string.optional_suffix)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dist = distanceInput.toFloatOrNull() ?: 0f
                    viewModel.stopCardioTimer(dist)
                    showStopDialog = false
                }) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = com.emanuel5014.trainable.ui.theme.ResponsiveSize.horizontalPadding)
            .padding(top = 16.dp, bottom = bottomPadding)
    ) {
        // Responsive scale: width-based so short/landscape heights don't collapse the UI.
        // Clamped to avoid glitches on extreme DPI / smallest-width settings.
        // The height counts too: the circle gives way when the content (about 260 dp besides the circle) would not fit
        val widthScale = (maxWidth / 400.dp).coerceIn(0.8f, 1.15f)
        val heightScale = ((maxHeight - 260.dp) / 200.dp).coerceIn(0.4f, 1.15f)
        val scale = minOf(widthScale, heightScale)
        val cIndicatorSize = (200f * scale).dp
        val cButtonSize = (135f * scale).dp
        // The time stays readable when the rest has to shrink a lot
        val cTimerFontSize = (46f * scale.coerceAtLeast(0.7f)).sp
        // Gaps and the action buttons give way a little too on a short screen
        val cRowGap = (12f * scale.coerceIn(0.6f, 1f)).dp
        val cActionHeight = if (scale < 0.7f) 40 else 48
        val cCookieSize = (130f * scale).dp
        val cCookieIconSize = (50f * scale).dp
        val cGapSize = (10f * scale).dp
        val cWavelength = (36f * scale).dp

        // Top bar: Cardio badge + swap button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Tertiary.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.cardio_badge),
                        style = MaterialTheme.typography.titleMedium,
                        color = Tertiary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            IconButton(onClick = onSwap) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = stringResource(R.string.swap_exercise),
                    tint = OnSurfaceVariant
                )
            }
        }

        // Centered content column — bottomPadding already shifts this up naturally
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(cRowGap)
        ) {
            Text(
                text = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode),
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val targetMin = exerciseState.cardioDurataTargetSeconds?.let { it / 60 }
            val targetKm = exerciseState.cardioDistanzaTargetKm
            if (!exerciseState.isCardioCompleted && (targetMin != null || targetKm != null)) {
                Surface(
                    color = SurfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (targetMin != null && targetMin > 0) {
                            Text(
                                text = stringResource(R.string.cardio_target_min_format, targetMin),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (targetMin != null && targetKm != null && targetMin > 0 && targetKm > 0) {
                            Text(text = "•", color = OnSurfaceVariant.copy(alpha = 0.5f))
                        }
                        if (targetKm != null && targetKm > 0) {
                            Text(
                                text = stringResource(R.string.cardio_target_km_format, targetKm.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!exerciseState.isCardioCompleted) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(cIndicatorSize)
                ) {
                    val cardioProgress = remember(state.cardioTimerSeconds, exerciseState.cardioDurataTargetSeconds) {
                        val target = exerciseState.cardioDurataTargetSeconds
                        if (target != null && target > 0) {
                            (state.cardioTimerSeconds.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    }
                    CircularWavyProgressIndicator(
                        progress = { cardioProgress },
                        modifier = Modifier.size(cIndicatorSize),
                        color = Primary,
                        trackColor = Primary.copy(alpha = 0.2f),
                        stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                        trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                        gapSize = cGapSize,
                        wavelength = cWavelength,
                        amplitude = { progress ->
                            if (!state.cardioTimerRunning) 0f
                            else if (progress <= 0.1f) 0f
                            else 1f
                        }
                    )
                        CardioToggleButton(
                            isRunning = state.cardioTimerRunning,
                            isPaused = state.cardioTimerPaused,
                            onToggle = {
                                if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                when {
                                    state.cardioTimerRunning -> viewModel.pauseCardioTimer()
                                    else -> viewModel.startCardioTimer()
                                }
                            },
                            modifier = Modifier.size(cButtonSize)
                        )
                }

                val timerSeconds = state.cardioTimerSeconds
                val hours = timerSeconds / 3600
                val minutes = (timerSeconds % 3600) / 60
                val seconds = timerSeconds % 60
                val timeFormatted = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }

                Text(
                    text = timeFormatted,
                    // The line height follows the size, or the text keeps taking the room of the big one
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = cTimerFontSize, lineHeight = cTimerFontSize * 1.15f),
                    fontWeight = FontWeight.Black,
                    color = if (state.cardioTimerRunning) Primary else OnSurface
                )

                Text(
                    text = when {
                        state.cardioTimerRunning -> stringResource(R.string.cardio_timer_running)
                        state.cardioTimerPaused -> stringResource(R.string.cardio_timer_paused)
                        else -> stringResource(R.string.cardio_timer_ready)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.cardioTimerRunning) Primary else OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                // Done without the timer: only while it is idle (a running or paused timer has its own stop and save)
                AnimatedVisibility(
                    visible = !state.cardioTimerRunning && !state.cardioTimerPaused,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GymButton(
                        onClick = {
                            completeMinutes = ""
                            showCompleteDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(top = 4.dp),
                        height = cActionHeight,
                        containerColor = Tertiary.copy(alpha = 0.15f),
                        contentColor = Tertiary
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cardio_mark_completed), fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Stop button shown inline when paused — no layout jump, just appears below status label
                AnimatedVisibility(
                    visible = state.cardioTimerPaused,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GymButton(
                        onClick = { showStopDialog = true },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(top = 4.dp),
                        height = cActionHeight,
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cardio_stop_save), fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(-8.dp))
                
                val elapsed = exerciseState.cardioElapsedSeconds
                val mins = elapsed / 60
                val secs = elapsed % 60
                val timeStr = String.format("%02d:%02d", mins, secs)
                
                val targetDurationMet = exerciseState.cardioDurataTargetSeconds?.let { 
                    elapsed >= it 
                } ?: false
                val targetDistanceMet = exerciseState.cardioDistanzaTargetKm?.let { 
                    exerciseState.cardioDistanceKm >= it 
                } ?: false
                val goalAchieved = targetDurationMet || targetDistanceMet
                
                val progress = exerciseState.cardioDurataTargetSeconds?.let { target ->
                    (elapsed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                } ?: 1f
                
                val cookieRotation = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        cookieRotation.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(
                                durationMillis = 20000,
                                easing = LinearEasing
                            )
                        )
                        cookieRotation.snapTo(0f)
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(cIndicatorSize)
                    ) {
                        CircularWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(cIndicatorSize),
                            color = if (goalAchieved) Primary else Error,
                            trackColor = if (goalAchieved) Primary.copy(alpha = 0.2f) else Error.copy(alpha = 0.2f),
                            stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                            trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                            gapSize = cGapSize,
                            wavelength = cWavelength
                        )
                        
                        val cookieShape = remember {
                            object : Shape {
                                override fun createOutline(
                                    size: Size,
                                    layoutDirection: LayoutDirection,
                                    density: Density
                                ): Outline {
                                    val path = MaterialShapes.Cookie6Sided.toPath().asComposePath()
                                    val matrix = Matrix()
                                    matrix.scale(size.width, size.height)
                                    path.transform(matrix)
                                    path.translate(size.center - path.getBounds().center)
                                    return Outline.Generic(path)
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(cCookieSize)
                                .graphicsLayer { rotationZ = cookieRotation.value }
                                .background(
                                    color = if (goalAchieved) Primary else Error,
                                    shape = cookieShape
                                )
                                .clip(cookieShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (goalAchieved) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbDown,
                                contentDescription = null,
                                tint = Surface,
                                modifier = Modifier
                                    .size(cCookieIconSize)
                                    .graphicsLayer { rotationZ = -cookieRotation.value }
                            )
                        }
                    }
                    
                    Surface(
                        color = Surface,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Black
                                )
                                if (exerciseState.cardioDistanceKm > 0f) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = stringResource(R.string.cardio_dist_value, exerciseState.cardioDistanceKm.toString()),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (exerciseState.cardioDurataTargetSeconds != null) {
                                    val targetMins = exerciseState.cardioDurataTargetSeconds / 60
                                    val targetSecs = exerciseState.cardioDurataTargetSeconds % 60
                                    Surface(
                                        color = if (targetDurationMet) Primary.copy(alpha = 0.15f) else Error.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.cardio_target_label),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (targetDurationMet) Primary else Error,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "${String.format("%d:%02d", targetMins, targetSecs)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (targetDurationMet) Primary else Error,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                if (exerciseState.cardioDistanceKm > 0f && exerciseState.cardioDistanzaTargetKm != null) {
                                    Surface(
                                        color = if (targetDistanceMet) Primary.copy(alpha = 0.15f) else Error.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (targetDistanceMet) Icons.Rounded.Check else Icons.Rounded.Close,
                                                contentDescription = null,
                                                tint = if (targetDistanceMet) Primary else Error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.cardio_dist_value, exerciseState.cardioDistanzaTargetKm.toString()),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (targetDistanceMet) Primary else Error,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Whether the RPE selector goes with a set: on every exercise, on none, or only on advanced ones and extra sets. */
private fun rpeInputShown(rpeInputMode: Int, exercise: WorkoutExerciseState?, set: WorkoutSetState): Boolean =
    when (rpeInputMode) {
        1 -> true
        2 -> false
        else -> exercise?.isAdvanced == true || set.isExtra
    }

/** The rest countdown in a small pill, for when the bottom hub is busy with something else. */
@Composable
private fun RestTimerPill(remainingSeconds: Int, modifier: Modifier = Modifier) {
    val time = String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60)
    val description = stringResource(R.string.rest_timer_remaining, time)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Tertiary)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = OnTertiary, modifier = Modifier.size(16.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.labelLarge,
            color = OnTertiary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** The plate calculator line for the set about to be done, when this exercise uses the calculator. */
@Composable
private fun ActiveSetPlateChip(
    exercise: WorkoutExerciseState?,
    set: WorkoutSetState?,
    state: WorkoutState,
    plateExercises: Map<Int, Float?>,
    onClick: () -> Unit
) {
    if (!state.plateCalculatorEnabled || exercise == null || set == null || set.isCompleted) return
    if (exercise.isTimeAndWeight || exercise.isCardio || exercise.exercise.id !in plateExercises) return
    PlateChip(
        weightKg = set.weight,
        barKg = plateExercises[exercise.exercise.id],
        weightUnit = state.weightUnit,
        plates = state.availablePlates,
        onClick = onClick
    )
}

@Composable
fun CardioHubSection(
    exerciseState: WorkoutExerciseState,
    state: WorkoutState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    onFinishWorkout: () -> Unit,
    onAddExercise: (() -> Unit)? = null
) {
    val isLastExercise = state.currentExerciseIndex == state.exercises.lastIndex
    val allowModify = state.isQuickWorkout || state.inlineExerciseModificationsEnabled

    if (allowModify && onAddExercise != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row (in alto): Add Exercise Button
            GymButton(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = Primary
            ) {
                Icon(Icons.Rounded.KeyboardDoubleArrowRight, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_exercise).uppercase(),
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Bottom Row (in basso): Exercise Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentExerciseIndex > 0) {
                    val prevName = state.exercises[state.currentExerciseIndex - 1].exercise.nome
                    val translatedPrev = ExerciseTranslations.translate(prevName, languageCode)
                    GymButton(
                        onClick = { viewModel.previousExercise() },
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = stringResource(R.string.previous_exercise_btn).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = translatedPrev,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isLastExercise) {
                    val nextName = state.exercises[state.currentExerciseIndex + 1].exercise.nome
                    val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                    GymButton(
                        onClick = { viewModel.nextExercise() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.next_exercise_btn).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnPrimary.copy(alpha = 0.75f),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = translatedNext,
                                style = MaterialTheme.typography.labelLarge,
                                color = OnPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                } else {
                    GymButton(
                        onClick = onFinishWorkout,
                        modifier = if (state.currentExerciseIndex > 0) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        containerColor = Tertiary.copy(alpha = 0.1f),
                        contentColor = Tertiary
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.finish_workout).uppercase(),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.currentExerciseIndex > 0) {
                val prevName = state.exercises[state.currentExerciseIndex - 1].exercise.nome
                val translatedPrev = ExerciseTranslations.translate(prevName, languageCode)
                GymButton(
                    onClick = { viewModel.previousExercise() },
                    modifier = Modifier.weight(1f),
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurface,
                    height = 64
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.previous_exercise_btn).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = translatedPrev,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!isLastExercise) {
                val nextName = state.exercises[state.currentExerciseIndex + 1].exercise.nome
                val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                GymButton(
                    onClick = { viewModel.nextExercise() },
                    modifier = Modifier.weight(1f),
                    height = 64
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.next_exercise_btn).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnPrimary.copy(alpha = 0.75f),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = translatedNext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnPrimary,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            } else {
                GymButton(
                    onClick = onFinishWorkout,
                    modifier = Modifier.weight(1f),
                    height = 64
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.finish_workout).uppercase(),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun LogSetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    GymButton(
        onClick = onClick,
        modifier = modifier.then(if (!compact) Modifier.fillMaxWidth() else Modifier.height(48.dp)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            if (!compact) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.log_set), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


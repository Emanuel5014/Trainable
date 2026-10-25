package com.emanuel5014.trainable.ui.screens.workout

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.platform.LocalDensity
import com.emanuel5014.trainable.ui.components.ExerciseMediaMessageToasts
import com.emanuel5014.trainable.ui.components.rememberExerciseMediaPicker
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import kotlin.math.roundToInt
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.components.rememberHubSheetState
import com.emanuel5014.trainable.ui.components.HubSlot
import com.emanuel5014.trainable.ui.components.HubMotion
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LoadPercentChip
import com.emanuel5014.trainable.ui.components.PlateChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.ui.components.RpeSelector
import com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels
import kotlinx.coroutines.launch

/** Key of the large exercise media item in the sets list, used to read where the sets end. */
internal const val MEDIA_CARD_KEY = "exercise_media_card"

/** The large media card only appears when at least this much room is left under the sets. */
private val MEDIA_CARD_MIN_HEIGHT = 140.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, kotlinx.coroutines.FlowPreview::class)
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
    val ui = rememberWorkoutScreenUiState(state.currentExerciseIndex, state.planName)
    // Exercises that use the plate calculator (Workout settings -> Plate calculator) and their bar weight in kg
    val plateExercises by viewModel.plateCalculatorExercises.collectAsState()
    // is open for, and the exercise whose list of changes is shown
    val usesEmomClock = { exercise: WorkoutExerciseState ->
        !exercise.isCardio && exercise.emomStartIndex != null && exercise.exercise.id !in ui.emomManualExercises
    }
    // The user's own image/GIF of each exercise (optional, see Workout settings -> Exercise media)
    val exerciseMedia by viewModel.exerciseMedia.collectAsState()
    var detailExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Height of the bottom interaction hub, so the large media card only takes the room left above it
    var hubHeightPx by remember { mutableIntStateOf(0) }
    // The lifter drags the hub between open and a thin strip; it stays where it was left while the screen is open
    val hubSheet = rememberHubSheetState()
    val screenDensity = LocalDensity.current
    val pickMedia = rememberExerciseMediaPicker { exerciseId, uri -> viewModel.setExerciseMedia(exerciseId, uri) }
    ExerciseMediaMessageToasts(viewModel.mediaMessages)
    // Each cardio set has its own distance
    var cardioDistanceInput by remember(state.currentExercise?.exercise?.id, state.currentExercise?.activeCardioSetIndex) {
        mutableStateOf(
            if ((state.currentExercise?.cardioDistanceKm ?: 0f) > 0f) state.currentExercise?.cardioDistanceKm.toString() else ""
        )
    }

    WorkoutEarlyDialogs(
        state = state,
        ui = ui,
        viewModel = viewModel,
        languageCode = languageCode,
        exerciseMedia = exerciseMedia,
        pickMedia = pickMedia,
        detailExerciseId = detailExerciseId,
        onDetailDismiss = { detailExerciseId = null }
    )



    val prescriptionLabels = rememberPrescriptionLabels(state.weightUnit)



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
    val setBeingEdited = remember(currentExState?.sets, ui.editingSetIndex) {
        ui.editingSetIndex?.let { currentExState?.sets?.getOrNull(it) }
    }
    LaunchedEffect(setBeingEdited, activeSetIndex) {
        val editing = setBeingEdited
        when {
            // The set is gone (removed)
            editing == null -> ui.editingSetIndex = null
            // The set being edited came up next: it moves to the panel of the active set, which can also log it
            // (while a rest is running that panel waits for the rest to end, so it is not opened behind it)
            ui.editingSetIndex == activeSetIndex && !editing.isCompleted -> {
                ui.editingSetIndex = null
                ui.isEditingValues = state.remainingRestSeconds <= 0
            }
        }
    }
    // Kept so the hub still has something to show while it animates away
    LaunchedEffect(ui.editingSetIndex) {
        ui.editingSetIndex?.let { ui.lastEditedIndex = it }
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
            ui.isEditingValues = false
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
            WorkoutTopBar(
                state = state,
                ui = ui,
                onClose = safeNavigateBack,
                onNavigateToRoutine = onNavigateToRoutine
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
                        GymButton(onClick = { ui.showAddExerciseSheet = true }) {
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
                        LaunchedEffect(ui.editingSetIndex) {
                            val editing = ui.editingSetIndex
                            if (editing != null && targetIndex == state.currentExerciseIndex && editing in targetExState.sets.indices) {
                                exerciseListState.animateScrollToItem(editing)
                            }
                        }

                        val swipeOffset = remember { Animatable(0f) }
                        // The header of this exercise shrinks to one row while its sets are scrolled
                        val headerCollapse = rememberHeaderCollapseState(targetIndex)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(headerCollapse.connection)
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
                                // The content stays above the hub, whatever step it is on
                                val cardioBottomPadding = if (showBottomHubForCardio || state.remainingRestSeconds > 0) {
                                    with(screenDensity) { hubHeightPx.toDp() } + 16.dp
                                } else 32.dp
                                CardioExerciseContent(
                                    exerciseIndex = targetIndex,
                                    exerciseState = targetExState,
                                    languageCode = languageCode,
                                    state = state,
                                    viewModel = viewModel,
                                    onSwap = { ui.showSwapExerciseSheet = true },
                                    onAddExercise = {
                                        ui.isAddingAfterCurrent = false
                                        ui.showAddExerciseSheet = true
                                    },
                                    distanceInput = cardioDistanceInput,
                                    onDistanceChange = { cardioDistanceInput = it },
                                    bottomPadding = cardioBottomPadding
                                )
                            } else if (usesEmomClock(targetExState)) {
                                val emomIdle = state.emomRun == null
                                // The content stays above the hub, whatever step it is on
                                val emomBottomPadding = if (emomIdle) with(screenDensity) { hubHeightPx.toDp() } + 16.dp else 32.dp
                                EmomExerciseContent(
                                    exerciseIndex = targetIndex,
                                    exerciseState = targetExState,
                                    languageCode = languageCode,
                                    state = state,
                                    viewModel = viewModel,
                                    onSwap = { ui.showSwapExerciseSheet = true },
                                    onLogManually = { ui.emomManualExercises = ui.emomManualExercises + targetExState.exercise.id },
                                    bottomPadding = emomBottomPadding
                                )
                            } else {
                                var expandedSetIndex by remember(targetIndex) { mutableStateOf<Int?>(targetActiveSetIndex) }
                                LaunchedEffect(targetActiveSetIndex) {
                                    if (targetExState.isTimed) {
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
                                LaunchedEffect(largeMediaFile != null) {
                                    if (largeMediaFile == null) {
                                        freeMediaPx = 0
                                        return@LaunchedEffect
                                    }
                                    val bottomGapPx = with(localDensity) { 16.dp.roundToPx() }
                                    snapshotFlow {
                                        // While the list is scrolled the sets' position says nothing about free room: keep the last value
                                        if (hubHeightPx <= 0) {
                                            0
                                        } else if (exerciseListState.firstVisibleItemIndex != 0 || exerciseListState.firstVisibleItemScrollOffset != 0) {
                                            null
                                        } else {
                                            val info = exerciseListState.layoutInfo
                                            info.visibleItemsInfo.firstOrNull { it.key == MEDIA_CARD_KEY }
                                                ?.let { card -> info.viewportSize.height - card.offset - hubHeightPx - bottomGapPx - headerCollapse.offsetPx.roundToInt() }
                                                ?.coerceAtLeast(0)
                                                ?: 0
                                        }
                                    }.collect { free -> if (free != null) freeMediaPx = free }
                                }

                                ExerciseHeader(
                                    targetExState = targetExState,
                                    targetIndex = targetIndex,
                                    state = state,
                                    ui = ui,
                                    viewModel = viewModel,
                                    languageCode = languageCode,
                                    exerciseMedia = exerciseMedia,
                                    largeMediaVisible = largeMediaVisible,
                                    pickMedia = pickMedia,
                                    prescriptionLabels = prescriptionLabels,
                                    onOpenDetail = { detailExerciseId = it },
                                    collapse = headerCollapse
                                )

                                // Only a list that does not fit on screen has anything to gain from a collapsed header
                                headerCollapse.canCollapse = { exerciseListState.canScrollForward || exerciseListState.canScrollBackward }

                                // A list moved by code (to the set being done) away from the top takes the header with it.
                                // Each step runs to its end: a list that settles back while the header collapses must not
                                // leave the header half way.
                                LaunchedEffect(headerCollapse, exerciseListState) {
                                    snapshotFlow { exerciseListState.firstVisibleItemIndex > 0 || exerciseListState.firstVisibleItemScrollOffset > 0 }
                                        .filter { it }
                                        .collect { headerCollapse.snapTo(HeaderCollapse.snapTarget(listScrolled = true)) }
                                }
                                // Whatever leaves the header half way (a drag that stopped, a list that settled) ends at the nearer end
                                LaunchedEffect(headerCollapse, exerciseListState) {
                                    snapshotFlow { headerCollapse.offsetPx }
                                        .debounce(400)
                                        .collect { if (!exerciseListState.isScrollInProgress) headerCollapse.settle() }
                                }

                                SetList(
                                    targetExState = targetExState,
                                    targetIndex = targetIndex,
                                    targetActiveSetIndex = targetActiveSetIndex,
                                    exerciseListState = exerciseListState,
                                    expandedSetIndex = expandedSetIndex,
                                    onExpandedSetIndexChange = { expandedSetIndex = it },
                                    largeMediaFile = largeMediaFile,
                                    largeMediaVisible = largeMediaVisible,
                                    freeMediaPx = freeMediaPx,
                                    hubHeightPx = hubHeightPx,
                                    isExerciseCompleted = isExerciseCompleted,
                                    state = state,
                                    ui = ui,
                                    viewModel = viewModel,
                                    languageCode = languageCode,
                                    prescriptionLabels = prescriptionLabels
                                )
                            }
                        }
                    }
                }

                // DYNAMIC INTERACTION HUB
                val currentExState = state.currentExercise
                WorkoutHub(
                    state = state,
                    ui = ui,
                    viewModel = viewModel,
                    currentExState = currentExState,
                    activeSet = activeSet,
                    activeSetIndex = activeSetIndex,
                    isExerciseCompleted = isExerciseCompleted,
                    setBeingEdited = setBeingEdited,
                    usesEmomClock = usesEmomClock,
                    hubSheet = hubSheet,
                    onHubHeightChanged = { hubHeightPx = it },
                    languageCode = languageCode,
                    prescriptionLabels = prescriptionLabels,
                    plateExercises = plateExercises
                )
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
        
        WorkoutDialogs(
            state = state,
            ui = ui,
            viewModel = viewModel,
            currentExState = currentExState,
            activeSet = activeSet,
            activeSetIndex = activeSetIndex,
            plateExercises = plateExercises,
            languageCode = languageCode,
            availableExercises = availableExercises,
            categories = categories,
            safeNavigateBack = safeNavigateBack
        )
    }
}

enum class HubMode { Logging, Editing, EditingSet, Resting, MiniSet, Completed, Cardio }

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
    exerciseIndex: Int,
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
    var openedSetIndex by remember(exerciseState.exercise.id) { mutableStateOf<Int?>(null) }
    // The set the note button and the note line are about: the one on the timer, or the last one once all are done
    val noteSetIndex = exerciseState.activeCardioSetIndex.takeIf { it >= 0 } ?: exerciseState.cardioSets.lastIndex
    openedSetIndex?.let { index ->
        exerciseState.cardioSets.getOrNull(index)?.let { set ->
            val onTimer = index == exerciseState.activeCardioSetIndex && (state.cardioTimerRunning || state.cardioTimerPaused)
            CardioSetDialog(
                set = set,
                rpeScale = if (state.rpeEnabled || set.rpe != null) state.rpeScale else null,
                onDismiss = { openedSetIndex = null },
                onSave = { seconds, km, note, rpe ->
                    if (set.isCompleted) viewModel.editCardioSet(exerciseIndex, index, seconds, km, note, rpe)
                    else viewModel.updateCardioSetNote(exerciseIndex, index, note)
                    openedSetIndex = null
                },
                onDelete = if (exerciseState.cardioSets.size > 1 && !onTimer) {
                    {
                        viewModel.removeCardioSet(exerciseIndex, index)
                        openedSetIndex = null
                    }
                } else null
            )
        }
    }
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
        // The circle takes the room the rest leaves (every part is measured, nothing is guessed in dp), up to this size
        val maxCircle = 200.dp * (maxWidth / 400.dp).coerceIn(0.8f, 1.15f)
        // Right after a set is done, until the next one starts, how hard it felt is asked under the chips
        val justDoneIndex = exerciseState.cardioSets.indexOfLast { it.isCompleted }
        val rpeAsked = state.rpeEnabled && justDoneIndex >= 0 && !state.cardioTimerRunning && !state.cardioTimerPaused
        val fits = remember(rpeAsked) { cardioFits(rpeAsked) }
        val exerciseName = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode)
        val viewportHeight = maxHeight

        // Only when nothing fits, even in the densest look (a huge font on a short screen), the screen scrolls
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CardioFitLayout(
                fits = fits,
                viewportHeight = viewportHeight,
                maxCircle = maxCircle,
                modifier = Modifier.fillMaxWidth(),
                header = { fit ->
                    CardioHeader(
                        fit = fit,
                        name = exerciseName,
                        noteFilled = !exerciseState.cardioSets.getOrNull(noteSetIndex)?.note.isNullOrBlank(),
                        onNote = { openedSetIndex = noteSetIndex },
                        onSwap = onSwap
                    )
                },
                top = { fit ->
                    CardioTopBlock(
                        fit = fit,
                        exerciseState = exerciseState,
                        name = exerciseName,
                        noteSetIndex = noteSetIndex,
                        justDoneIndex = justDoneIndex,
                        rpeScale = state.rpeScale,
                        hapticEnabled = state.hapticEnabled,
                        onSetClick = { openedSetIndex = it },
                        onAddSet = {
                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addCardioSet(exerciseIndex)
                        },
                        onRpeChange = { viewModel.updateCardioSetRpe(exerciseIndex, justDoneIndex, it) }
                    )
                },
                circle = { _, size ->
                    CardioCircle(
                        size = size,
                        state = state,
                        exerciseState = exerciseState,
                        onToggle = {
                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when {
                                state.cardioTimerRunning -> viewModel.pauseCardioTimer()
                                else -> viewModel.startCardioTimer()
                            }
                        }
                    )
                },
                bottom = { fit ->
                    CardioBottomBlock(
                        fit = fit,
                        state = state,
                        exerciseState = exerciseState,
                        onMarkCompleted = {
                            completeMinutes = ""
                            showCompleteDialog = true
                        },
                        onStop = { showStopDialog = true }
                    )
                }
            )
        }
    }
}

/** The RPE selector of a set, with the scale the user chose; nothing while the RPE switch is off. */
@Composable
internal fun RpeInput(state: WorkoutState, value: Float?, onValueChange: (Float?) -> Unit) {
    if (state.rpeEnabled) RpeSelector(value = value, onValueChange = onValueChange, scale = state.rpeScale, hapticEnabled = state.hapticEnabled)
}

/** The rest countdown in a small pill, for when the bottom hub is busy with something else. */
@Composable
internal fun RestTimerPill(remainingSeconds: Int, modifier: Modifier = Modifier) {
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

/** The way into the load editor for a set that asks for a %1RM and is still to do. */
@Composable
internal fun SetPercentChip(exercise: WorkoutExerciseState?, set: WorkoutSetState?, onClick: () -> Unit) {
    val prescription = set?.prescription ?: return
    if (exercise == null || set.isCompleted || set.isExtra || prescription.intensityType != IntensityType.PERCENT) return
    if (prescription.blockIndex !in ProgramEditing.percentBlocksToDo(exercise.sets, exercise.blocks)) return
    LoadPercentChip(
        percentText = PrescriptionFormatter.intensity(prescription.intensityType, prescription.intensityValue).orEmpty(),
        onClick = onClick
    )
}

/** The plate calculator line for the set about to be done, when this exercise uses the calculator. */
@Composable
internal fun ActiveSetPlateChip(
    exercise: WorkoutExerciseState?,
    set: WorkoutSetState?,
    state: WorkoutState,
    plateExercises: Map<Int, Float?>,
    onClick: () -> Unit
) {
    if (!state.plateCalculatorEnabled || exercise == null || set == null || set.isCompleted) return
    if (exercise.isTimed || exercise.isCardio || exercise.exercise.id !in plateExercises) return
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
    onAddExercise: (() -> Unit)? = null,
    /** How open the bottom hub is (1 open, 0 thin strip). */
    expansion: () -> Float = { 1f }
) {
    val isLastExercise = state.currentExerciseIndex == state.exercises.lastIndex
    val allowModify = state.isQuickWorkout || state.inlineExerciseModificationsEnabled
    val navHeight = hubNavHeight(expansion())

    if (allowModify && onAddExercise != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row (in alto): Add Exercise Button, gone on the strip
            HubSlot(expansion = expansion, from = 0.55f, to = 0.8f, gapBelow = 8.dp) {
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
            }

            // Bottom Row (in basso): Exercise Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentExerciseIndex > 0) {
                    val prevName = state.exercises[state.currentExerciseIndex - 1].exercise.nome
                    HubPreviousButton(
                        previousName = ExerciseTranslations.translate(prevName, languageCode),
                        expansion = expansion,
                        onClick = { viewModel.previousExercise() },
                        nameStyle = MaterialTheme.typography.labelLarge,
                        nameWeight = FontWeight.ExtraBold
                    )
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
                HubPreviousButton(
                    previousName = ExerciseTranslations.translate(prevName, languageCode),
                    expansion = expansion,
                    onClick = { viewModel.previousExercise() },
                    nameStyle = MaterialTheme.typography.bodyMedium,
                    nameWeight = FontWeight.Black,
                    height = navHeight
                )
            }

            if (!isLastExercise) {
                val nextName = state.exercises[state.currentExerciseIndex + 1].exercise.nome
                val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                GymButton(
                    onClick = { viewModel.nextExercise() },
                    modifier = Modifier.weight(1f),
                    height = navHeight
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
                    height = navHeight
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

/** Height of the exercise buttons in the bottom hub: a little lower on the strip. */
internal fun hubNavHeight(expansion: Float): Int = lerp(52f, 64f, expansion).roundToInt()

/**
 * "Previous exercise" in the bottom hub. As the hub shrinks to its strip the button gives up its label and most
 * of its width and keeps only the arrow, so "next" can take the room.
 */
@Composable
internal fun RowScope.HubPreviousButton(
    previousName: String,
    expansion: () -> Float,
    onClick: () -> Unit,
    nameStyle: TextStyle,
    nameWeight: FontWeight,
    height: Int? = null
) {
    val label = HubMotion.window(expansion(), 0.25f, 0.6f)
    val fullPadding = if (ResponsiveSize.isCompact) 16.dp else 24.dp
    GymButton(
        onClick = onClick,
        modifier = Modifier.weight(lerp(0.2f, 1f, label)),
        containerColor = SurfaceContainerHigh,
        contentColor = OnSurface,
        height = height ?: if (ResponsiveSize.isCompact) 48 else 56,
        contentPadding = PaddingValues(horizontal = lerp(0.dp, fullPadding, label))
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            // With the label gone the arrow has to say what the button does
            contentDescription = if (label <= 0f) stringResource(R.string.previous_exercise_btn) else null
        )
        if (label > 0f) {
            Spacer(modifier = Modifier.width(8.dp * label))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = label }
            ) {
                Text(
                    text = stringResource(R.string.previous_exercise_btn).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = previousName,
                    style = nameStyle,
                    fontWeight = nameWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        pressMorph = true,
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



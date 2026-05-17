package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseNavigation
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.RestTimerSection
import com.emanuel5014.trainable.ui.components.SetLogRow
import com.emanuel5014.trainable.ui.components.SwapExerciseBottomSheet
import com.emanuel5014.trainable.ui.components.WeightRepsInput
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val availableExercises by viewModel.availableExercises.collectAsState()
    val listState = rememberLazyListState()
    var isEditingValues by remember { mutableStateOf(false) }
    var showSwapExerciseSheet by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newSessionName by remember { mutableStateOf(state.planName) }

    if (showRenameDialog) {
        AlertDialog(
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

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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

    LaunchedEffect(activeSetIndex) {
        if (activeSetIndex != -1) {
            listState.animateScrollToItem(activeSetIndex)
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
                                    if (state.isQuickWorkout) {
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
                Column(modifier = Modifier.fillMaxSize()) {
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
                            val setsCount = currentExState.sets.size
                            val repsCount = currentExState.planDetails?.repsTarget ?: currentExState.sets.firstOrNull()?.reps?.toString() ?: "0"
                            Text(
                                text = "$setsCount × $repsCount",
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
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
                        Text(
                            text = ExerciseTranslations.translate(currentExState.exercise.nome, languageCode),
                            style = MaterialTheme.typography.displaySmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    // Sets List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(currentExState.sets) { index, set ->
                            val isActive = index == activeSetIndex && !set.isCompleted
                            var showDeleteConfirm by remember { mutableStateOf(false) }

                            if (showDeleteConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirm = false },
                                    title = { Text(stringResource(R.string.remove_set)) },
                                    text = { Text("Are you sure you want to remove this set?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.removeSetFromExercise(state.currentExerciseIndex, index)
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
                            SetLogRow(
                                setNumber = set.setNumber,
                                weight = set.weight,
                                reps = set.reps,
                                note = set.note,
                                isWarmup = set.isWarmup,
                                isCompleted = set.isCompleted,
                                onToggleComplete = { 
                                    viewModel.toggleSetComplete(state.currentExerciseIndex, index) 
                                },
                                onNoteChange = { newNote ->
                                    viewModel.updateSetNote(state.currentExerciseIndex, index, newNote)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDeleteConfirm = true
                                },
                                onEditValues = { isEditingValues = !isEditingValues },
                                isActive = isActive,
                                weightUnit = state.weightUnit
                            )
                        }

                        if (state.isQuickWorkout) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GymButton(
                                        onClick = { viewModel.addSetToExercise(state.currentExerciseIndex) },
                                        modifier = Modifier.weight(1f),
                                        containerColor = SurfaceContainerHigh,
                                        contentColor = Primary
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.add_set), fontWeight = FontWeight.ExtraBold)
                                    }
                                    GymButton(
                                        onClick = { showAddExerciseSheet = true },
                                        modifier = Modifier.weight(1f),
                                        containerColor = SurfaceContainerHigh,
                                        contentColor = Primary
                                    ) {
                                        Icon(Icons.Rounded.KeyboardDoubleArrowRight, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.next_exercise_wrk).uppercase(), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(300.dp)) // Sufficient space for the dynamic hub
                        }
                    }
                }

                // DYNAMIC INTERACTION HUB
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        val activeSet = currentExState.sets.getOrNull(activeSetIndex)
                        val isResting = state.remainingRestSeconds > 0

                        AnimatedContent(
                            targetState = when {
                                isResting -> HubMode.Resting
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
                                HubMode.Resting -> {
                                    RestTimerSection(
                                        remainingSeconds = state.remainingRestSeconds,
                                        totalRestSeconds = state.totalRestSeconds,
                                        onAddTime = { viewModel.addRestTime(30) },
                                        onSkip = { viewModel.skipRestTimer() }
                                    )
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
                                            WeightRepsInput(
                                                weight = set.weight,
                                                reps = set.reps,
                                                onWeightChange = { newW -> viewModel.updateSetWeight(state.currentExerciseIndex, activeSetIndex, newW) },
                                                onRepsChange = { newR -> viewModel.updateSetReps(state.currentExerciseIndex, activeSetIndex, newR) },
                                                weightUnit = state.weightUnit
                                            )
                                            LogSetButton(onClick = { viewModel.toggleSetComplete(state.currentExerciseIndex, activeSetIndex) })
                                        }
                                    }
                                }
                                HubMode.Logging -> {
                                    activeSet?.let { set ->
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
                                                Text(
                                                    text = WeightUnitConverter.formatWithUnit(
                                                        WeightUnitConverter.convertDisplay(set.weight, state.weightUnit),
                                                        state.weightUnit
                                                    ) + " × ${set.reps}", 
                                                    style = MaterialTheme.typography.titleLarge, 
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            LogSetButton(
                                                onClick = { viewModel.toggleSetComplete(state.currentExerciseIndex, activeSetIndex) },
                                                modifier = Modifier.width(120.dp),
                                                compact = true
                                            )
                                        }
                                    }
                                }
                                HubMode.Completed -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Tertiary.copy(alpha = 0.1f))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = Tertiary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.exercise_completed_title), style = MaterialTheme.typography.labelLarge, color = Tertiary, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

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

            if (state.isFinishing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    GymLoadingIndicator()
                }
            }
        }
        
        if (showSwapExerciseSheet) {
            currentExState?.let { exState ->
                SwapExerciseBottomSheet(
                    currentSets = exState.sets.size,
                    currentReps = exState.planDetails?.repsTarget ?: "8",
                    availableExercises = availableExercises,
                    languageCode = languageCode,
                    onExerciseSelected = { newExercise, sets, reps, rest ->
                        viewModel.swapExercise(state.currentExerciseIndex, newExercise.id, sets, reps, rest)
                        showSwapExerciseSheet = false
                    },
                    onAddCustomExercise = { nome, categoria ->
                        viewModel.addCustomExercise(nome, categoria)
                    },
                    onEditCustomExercise = { exercise ->
                        viewModel.updateCustomExercise(exercise)
                    },
                    onDeleteCustomExercise = { exercise ->
                        viewModel.deleteCustomExercise(exercise)
                    },
                    onDismiss = { showSwapExerciseSheet = false }
                )
            }
        }

        if (showAddExerciseSheet) {
            SwapExerciseBottomSheet(
                currentSets = 3,
                currentReps = "8",
                availableExercises = availableExercises,
                languageCode = languageCode,
                onExerciseSelected = { exercise, sets, reps, rest ->
                    viewModel.addExerciseToActiveSession(exercise, sets, reps, rest)
                    showAddExerciseSheet = false
                },
                onAddCustomExercise = viewModel::addCustomExercise,
                onEditCustomExercise = viewModel::updateCustomExercise,
                onDeleteCustomExercise = viewModel::deleteCustomExercise,
                onDismiss = { showAddExerciseSheet = false },
                isAdding = true
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
                text = { Text(stringResource(R.string.finish_workout_message)) },
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

enum class HubMode { Logging, Editing, Resting, Completed }

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

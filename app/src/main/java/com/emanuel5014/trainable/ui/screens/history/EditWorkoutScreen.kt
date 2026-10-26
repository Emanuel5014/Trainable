package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.ui.components.cardioSetValueText
import com.emanuel5014.trainable.data.local.entity.CardioExerciseLogs
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.setsOf
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.kind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Percent
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.data.local.entity.forExercise
import com.emanuel5014.trainable.data.local.entity.toPrescriptionBlocks
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.ui.components.ExercisePrescriptionSheet
import com.emanuel5014.trainable.ui.components.AddCardioDialog
import com.emanuel5014.trainable.ui.components.CardioInputForm
import com.emanuel5014.trainable.ui.components.SwapExerciseBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.journal.JournalRow
import com.emanuel5014.trainable.ui.components.journal.JournalSheet
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.rememberLazyListReorderState
import com.emanuel5014.trainable.ui.components.reorderGestures
import com.emanuel5014.trainable.ui.components.reorderableItem
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Stable lazy-list key of a row in the edit screen: exercises and cardio logs share one list. An exercise done twice in
 * the workout is two rows, so the second one carries its [EditExerciseState.instance]; the key does not use the order,
 * which changes whenever the user drags a row.
 */
internal fun getWorkoutItemKey(item: Any): String = when (item) {
    is EditExerciseState -> if (item.instance == 0) "exercise_${item.exercise.id}" else "exercise_${item.exercise.id}_${item.instance}"
    is CardioExerciseLogs -> "cardio_${item.first.id}"
    else -> item.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val setPerformanceEnabled by viewModel.setPerformanceEnabled.collectAsState()
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val listState = rememberLazyListState()
    val mergedItems = remember(state.exercises, state.cardioLogs) {
        val items = mutableListOf<Pair<Int, Any>>()
        state.exercises.forEach { ex ->
            items.add(Pair(ex.sets.firstOrNull()?.ordineEsercizio ?: 0, ex as Any))
        }
        // The sets of a cardio exercise are one card
        state.cardioLogs.groupedByExercise().forEach { cardio ->
            items.add(Pair(cardio.order, cardio as Any))
        }
        items.sortBy { it.first }
        items.map { it.second }
    }
    // Exercises in the same superset are dragged and dropped as one block
    val supersetGroups = remember(mergedItems) {
        mergedItems.associate<Any, Any, String?> { item ->
            getWorkoutItemKey(item) to (item as? EditExerciseState)?.sets?.firstOrNull()?.supersetId
        }
    }
    val reorderState = rememberLazyListReorderState(
        lazyListState = listState,
        keys = { mergedItems.map(::getWorkoutItemKey) },
        groupOf = { supersetGroups[it] },
        hapticEnabled = hapticEnabled,
        onCommit = { newOrder ->
            val byKey = mergedItems.associateBy<Any, Any>(::getWorkoutItemKey)
            viewModel.updateItemsOrder(newOrder.mapNotNull { byKey[it] })
        }
    )

    var editingSet by remember { mutableStateOf<SetLogEntity?>(null) }
    var editingCardio by remember { mutableStateOf<CardioLogEntity?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseToSwap by remember { mutableStateOf<ExerciseKey?>(null) }
    var showDeleteExerciseDialog by remember { mutableStateOf<ExerciseKey?>(null) }
    var showEditDetailsSheet by remember { mutableStateOf(false) }
    var showJournalSheet by remember { mutableStateOf(false) }
    var showAddCardio by remember { mutableStateOf(false) }
    var pendingCardioCategory by remember { mutableStateOf<String?>(null) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var exerciseToEditPrescription by remember { mutableStateOf<ExerciseKey?>(null) }
    var pendingPrescription by remember { mutableStateOf<Pair<ExerciseKey, List<PrescriptionBlock>>?>(null) }

    LaunchedEffect(reorderState.isDragging) {
        if (reorderState.isDragging) reorderState.driveWhileDragging()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.planName) {
                            "Cardio" -> stringResource(R.string.add_cardio).replace(stringResource(R.string.add) + " ", "")
                            "Custom Workout" -> stringResource(R.string.custom_workout)
                            else -> state.planName
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEditDetailsSheet = true }
                    )
                },
                navigationIcon = {
                    GymIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onClick = onNavigateBack,
                        containerColor = Color.Transparent
                    )
                },
                actions = {
                    GymIconButton(
                        icon = Icons.Rounded.Delete,
                        onClick = { showDeleteSessionDialog = true },
                        containerColor = Error.copy(alpha = 0.1f),
                        contentColor = Error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    exerciseToSwap = null
                    showExercisePicker = true
                },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                WorkoutSessionHeaderCard(
                    sessionTimestamp = state.sessionTimestamp,
                    sessionDurationMs = state.sessionDurationMs,
                    exerciseCount = mergedItems.size,
                    onEditClick = { showEditDetailsSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (state.journalEnabled) {
                    JournalRow(
                        rating = state.journal.rating,
                        tags = state.journalTags,
                        note = state.journal.note,
                        onClick = { showJournalSheet = true },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                if (state.exercises.isEmpty() && state.cardioLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Primary.copy(alpha = 0.12f), CircleShape)
                                    .padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_exercises_in_session),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.tap_plus_to_add),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        userScrollEnabled = !reorderState.isDragging,
                        modifier = Modifier
                            .fillMaxSize()
                            .reorderGestures(reorderState),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    val displayedItems = reorderState.ordered(mergedItems, ::getWorkoutItemKey)
                    itemsIndexed(displayedItems, key = { _, item -> getWorkoutItemKey(item) }) { index, item ->
                        val itemKey = getWorkoutItemKey(item)
                        val isDragging = reorderState.isDragged(itemKey)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    reorderableItem(
                                        state = reorderState,
                                        key = itemKey,
                                        shape = Shapes.extraLarge,
                                        liftedScale = 1.03f,
                                        liftedElevation = 16.dp
                                    )
                                )
                        ) {
                            when (item) {
                                is EditExerciseState -> {
                                    val exerciseState = item
                                    val currentSid = exerciseState.sets.firstOrNull()?.supersetId
                                    val isSuperset = currentSid != null
                                    val isLinked = isSuperset && index < displayedItems.lastIndex &&
                                            (displayedItems[index + 1] as? EditExerciseState)?.sets?.firstOrNull()?.supersetId == currentSid

                                    EditExerciseCard(
                                        exerciseState = exerciseState,
                                        languageCode = languageCode,
                                        isFirst = index == 0,
                                        isLast = index == displayedItems.lastIndex,
                                        isSuperset = isSuperset,
                                        isLinked = isLinked,
                                        isDragging = isDragging,
                                        weightUnit = state.weightUnit,
                                        onEditSet = { editingSet = it },
                                        onAddSet = { viewModel.addSet(exerciseState.key) },
                                        onSwapExercise = {
                                            exerciseToSwap = exerciseState.key
                                            showExercisePicker = true
                                        },
                                        onEditPrescription = { exerciseToEditPrescription = exerciseState.key },
                                        onDeleteExercise = { showDeleteExerciseDialog = exerciseState.key },
                                        onMoveSetUp = { viewModel.moveSetUp(it) },
                                        onMoveSetDown = { viewModel.moveSetDown(it) },
                                        onMoveExerciseUp = { viewModel.moveExerciseUp(exerciseState.key) },
                                        onMoveExerciseDown = { viewModel.moveExerciseDown(exerciseState.key) },
                                        onToggleSuperset = { viewModel.toggleSupersetWithNext(exerciseState.key) },
                                        programEdits = state.prescriptionEdits.forExercise(exerciseState.exercise.id),
                                        onRestoreExercise = { viewModel.restoreExercise(exerciseState.exercise.id) }
                                    )
                                }
                                is CardioExerciseLogs -> {
                                    CardioEditCard(
                                        cardio = item,
                                        isFirst = index == 0,
                                        isLast = index == displayedItems.lastIndex,
                                        isDragging = isDragging,
                                        onEditSet = { editingCardio = it },
                                        onAddSet = { viewModel.addCardioSet(item) },
                                        onDelete = { viewModel.deleteCardioExercise(item) },
                                        onMoveUp = { viewModel.moveCardio(item, displayedItems, -1) },
                                        onMoveDown = { viewModel.moveCardio(item, displayedItems, 1) }
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
    }

    if (editingSet != null) {
        EditSetDialog(
            set = editingSet!!,
            weightUnit = state.weightUnit,
            isNewSet = false,
            setPerformanceEnabled = setPerformanceEnabled,
            onDismiss = { editingSet = null },
            onConfirm = {
                viewModel.updateSet(it)
                editingSet = null
            },
            onDelete = {
                viewModel.deleteSet(it)
                editingSet = null
            }
        )
    }

    editingCardio?.let { log ->
        val siblings = state.cardioLogs.setsOf(log)
        EditCardioDialog(
            cardioLog = log,
            onDismiss = { editingCardio = null },
            onConfirm = {
                viewModel.updateCardioLog(it)
                editingCardio = null
            },
            onDelete = if (siblings.size > 1) {
                {
                    viewModel.deleteCardioLog(log)
                    editingCardio = null
                }
            } else null,
            setNumber = if (siblings.size > 1) siblings.indexOfFirst { it.id == log.id } + 1 else null
        )
    }

    if (showAddCardio) {
        AddCardioDialog(
            initialCategoria = pendingCardioCategory,
            onDismiss = {
                showAddCardio = false
                pendingCardioCategory = null
            },
            onConfirm = { categoria, distanza, durataSecondi, note ->
                viewModel.addCardioLog(categoria, distanza, durataSecondi, note = note)
                showAddCardio = false
                pendingCardioCategory = null
            }
        )
    }

    if (showJournalSheet) {
        JournalSheet(
            initial = state.journal,
            onSave = {
                viewModel.saveJournal(it)
                showJournalSheet = false
            },
            onDismiss = { showJournalSheet = false },
            hapticEnabled = hapticEnabled
        )
    }

    if (showEditDetailsSheet) {
        EditWorkoutDetailsBottomSheet(
            currentName = state.planName,
            currentTimestamp = state.sessionTimestamp,
            currentDurationMs = state.sessionDurationMs,
            onDismiss = { showEditDetailsSheet = false },
            onConfirm = { newName, newTimestamp, newDurationMs ->
                viewModel.updateSessionDetails(newName, newTimestamp, newDurationMs)
                showEditDetailsSheet = false
            }
        )
    }

    if (showExercisePicker) {
        val exerciseStateToSwap = exerciseToSwap?.let { swapKey -> state.exercises.find { it.key == swapKey } }
        val currentSets = exerciseStateToSwap?.sets?.size ?: 3
        // A range the exercise was done against is offered again (its bottom and top), otherwise the first set's reps
        val currentRange = exerciseStateToSwap?.sets?.firstNotNullOfOrNull { it.repsRange }
        val currentReps = currentRange?.min?.toString() ?: exerciseStateToSwap?.sets?.firstOrNull()?.let {
            if (it.kind == SetLogKind.TIME_AND_WEIGHT) "${it.durataSecondi}" else "${it.repsEffettive}"
        } ?: "8"

        SwapExerciseBottomSheet(
            currentSets = currentSets,
            currentReps = currentReps,
            currentRepsMax = currentRange?.max,
            // The sheet picks by exercise: one that is already in the workout cannot be added or swapped in a second time
            excludedExerciseIds = state.exercises.map { it.exercise.id }.toSet() - setOfNotNull(exerciseStateToSwap?.exercise?.id),
            availableExercises = state.availableExercises,
            languageCode = languageCode,
            isAdding = exerciseToSwap == null,
            currentExerciseId = exerciseStateToSwap?.exercise?.id,
            categories = state.categories,
            onExerciseSelected = { exercise, sets, reps, rest, exerciseType, durataTargetSec ->
                val currentSwapKey = exerciseToSwap
                if (currentSwapKey != null) {
                    viewModel.swapExercise(currentSwapKey, exercise.id, exerciseType, durataTargetSec, repsRange = reps.range.takeIf { exerciseType == "strength" })
                } else {
                    viewModel.addExercise(exercise.id, sets, reps.repsTarget, exerciseType, durataTargetSec, repsMax = reps.repsMax)
                }
                exerciseToSwap = null
                showExercisePicker = false
            },
            onCardioExerciseSelected = { exercise, durationMinutes, _, sets ->
                val currentSwapKey = exerciseToSwap
                if (currentSwapKey != null) {
                    viewModel.swapExerciseWithCardio(currentSwapKey, exercise.nome, durationMinutes, sets)
                } else {
                    viewModel.addCardioLog(exercise.nome, 0f, durationMinutes * 60, sets = sets)
                }
                exerciseToSwap = null
                showExercisePicker = false
            },
            onAdvancedExerciseSelected = { exercise, blocks, _ ->
                val currentSwapKey = exerciseToSwap
                if (currentSwapKey != null) {
                    viewModel.swapExerciseAdvanced(currentSwapKey, exercise.id, blocks)
                } else {
                    viewModel.addAdvancedExercise(exercise.id, blocks)
                }
                exerciseToSwap = null
                showExercisePicker = false
            },
            initialBlocks = exerciseStateToSwap?.sets?.toPrescriptionBlocks().orEmpty(),
            onAddCustomExercise = { name, category, onCreated ->
                viewModel.addCustomExercise(name, category, onCreated)
            },
            onDismiss = {
                exerciseToSwap = null
                showExercisePicker = false
            }
        )
    }

    exerciseToEditPrescription?.let { editingKey ->
        val target = state.exercises.find { it.key == editingKey }
        if (target == null) {
            exerciseToEditPrescription = null
        } else {
            val currentBlocks = remember(target.sets) { target.sets.toPrescriptionBlocks() }
            val seeded = remember(target.sets) {
                // A plain exercise starts from one free block that matches its sets
                val reps = target.sets.map { it.repsEffettive }
                PrescriptionBlock(
                    sets = target.sets.size.coerceAtLeast(1),
                    reps = when {
                        reps.isEmpty() -> "5"
                        reps.distinct().size == 1 -> reps.first().toString()
                        else -> reps.joinToString("-")
                    }
                )
            }
            ExercisePrescriptionSheet(
                exerciseId = editingKey.exerciseId,
                exerciseName = ExerciseTranslations.translate(target.exercise.nome, languageCode),
                initialBlocks = currentBlocks.ifEmpty { listOf(seeded) },
                onDismiss = { exerciseToEditPrescription = null },
                onSave = { blocks ->
                    val removed = viewModel.removedSetsCount(editingKey, blocks)
                    if (removed > 0) {
                        pendingPrescription = editingKey to blocks
                    } else {
                        viewModel.applyPrescription(editingKey, blocks)
                        exerciseToEditPrescription = null
                    }
                },
                onRemove = if (currentBlocks.isNotEmpty()) {
                    {
                        viewModel.applyPrescription(editingKey, emptyList())
                        exerciseToEditPrescription = null
                    }
                } else null
            )
        }
    }

    pendingPrescription?.let { (exerciseKey, blocks) ->
        AlertDialog(
            onDismissRequest = { pendingPrescription = null },
            title = { Text(stringResource(R.string.prescription_remove_sets_title), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.prescription_remove_sets_message, viewModel.removedSetsCount(exerciseKey, blocks))) },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.applyPrescription(exerciseKey, blocks)
                        pendingPrescription = null
                        exerciseToEditPrescription = null
                    },
                    containerColor = Error.copy(alpha = 0.12f),
                    contentColor = Error
                ) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { pendingPrescription = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showDeleteExerciseDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteExerciseDialog = null },
            title = { Text(stringResource(R.string.delete_routine)) },
            text = { Text(stringResource(R.string.delete_routine_message)) },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(bottom = Spacing.small)
                ) {
                    GymButton(
                        onClick = { showDeleteExerciseDialog = null },
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    GymButton(
                        onClick = {
                            showDeleteExerciseDialog?.let { viewModel.deleteExercise(it) }
                            showDeleteExerciseDialog = null
                        },
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Text(stringResource(R.string.delete).uppercase())
                    }
                }
            },
            dismissButton = {},
            containerColor = SurfaceContainerHigh
        )
    }


    if (showDeleteSessionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionDialog = false },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.deleteSession()
                        showDeleteSessionDialog = false
                        onNavigateBack()
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showDeleteSessionDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
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

@Composable
fun EditExerciseCard(
    exerciseState: EditExerciseState,
    languageCode: String,
    isFirst: Boolean,
    isLast: Boolean,
    isSuperset: Boolean,
    isLinked: Boolean,
    isDragging: Boolean = false,
    weightUnit: String = "kg",
    onEditSet: (SetLogEntity) -> Unit,
    onAddSet: () -> Unit,
    onSwapExercise: () -> Unit,
    onEditPrescription: () -> Unit,
    onDeleteExercise: () -> Unit,
    onMoveSetUp: (SetLogEntity) -> Unit,
    onMoveSetDown: (SetLogEntity) -> Unit,
    onMoveExerciseUp: () -> Unit,
    onMoveExerciseDown: () -> Unit,
    onToggleSuperset: () -> Unit,
    /** Changes made to the prescription of this exercise while the workout was done, oldest first. */
    programEdits: List<com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity> = emptyList(),
    /** Adds the exercise back to the exercise list when the user deleted it from there. */
    onRestoreExercise: () -> Unit = {}
) {
    var showRestoreDialog by remember { mutableStateOf(false) }
    if (showRestoreDialog) {
        com.emanuel5014.trainable.ui.components.RestoreExerciseDialog(
            exerciseNames = listOf(ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode)),
            onRestore = {
                onRestoreExercise()
                showRestoreDialog = false
            },
            onSkip = { showRestoreDialog = false },
            onDismiss = { showRestoreDialog = false },
            skipLabel = stringResource(R.string.cancel)
        )
    }
    var showProgramEdits by remember { mutableStateOf(false) }
    if (showProgramEdits) {
        com.emanuel5014.trainable.ui.components.ProgramEditHistoryDialog(
            exerciseName = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode),
            edits = programEdits,
            weightUnit = weightUnit,
            onDismiss = { showProgramEdits = false }
        )
    }
    val cardBgColor = when {
        isDragging -> SurfaceContainerHigh
        else -> SurfaceContainerLow
    }
    val cardBorder = when {
        isDragging -> androidx.compose.foundation.BorderStroke(2.dp, Primary)
        isSuperset -> androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
        else -> null
    }

    GymCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = cardBgColor,
        border = cardBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ExerciseTranslations.translateCategory(exerciseState.exercise.categoria, languageCode).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onMoveExerciseUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = if (isFirst) OnSurfaceVariant.copy(alpha = 0.2f) else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveExerciseDown,
                        enabled = !isLast,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isLast) OnSurfaceVariant.copy(alpha = 0.2f) else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!isLast) {
                        IconButton(
                            onClick = onToggleSuperset,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                                contentDescription = null,
                                tint = if (isLinked) Primary else OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onSwapExercise,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteExercise,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                // Deleted from the exercise list: still in the workout, greyed out
                color = if (exerciseState.exercise.isDeleted) OnSurfaceVariant else OnSurface,
                modifier = Modifier.fillMaxWidth()
            )
            if (exerciseState.exercise.isDeleted) {
                Spacer(modifier = Modifier.height(4.dp))
                com.emanuel5014.trainable.ui.components.DeletedExercisePill(Modifier.clickable { showRestoreDialog = true })
            }
            programEdits.lastOrNull()?.let { edit ->
                Spacer(modifier = Modifier.height(4.dp))
                com.emanuel5014.trainable.ui.components.ProgramEditPill(edit, Modifier.clickable { showProgramEdits = true })
            }

            val setKind = exerciseState.sets.kind
            val isTimeAndWeight = setKind.isTimed
            if (isSuperset || isTimeAndWeight) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSuperset) {
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.superset).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                    color = Primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    if (isTimeAndWeight) {
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (setKind == SetLogKind.TIME_AND_REPS) Icons.Rounded.Timelapse else Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(
                                        if (setKind == SetLogKind.TIME_AND_REPS) R.string.time_and_reps_badge else R.string.time_and_weight_badge
                                    ).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                    color = Primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val context = LocalContext.current
            val prescriptionLabels = com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels(weightUnit)
            val advancedOn = com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming.current
            val isAdvanced = advancedOn && exerciseState.sets.any { it.blockIndex != null }
            val blocksByIndex = remember(exerciseState.sets) {
                val indexes = exerciseState.sets.filter { it.blockIndex != null && !it.isExtra }.map { it.blockIndex!! }.distinct().sorted()
                indexes.zip(exerciseState.sets.toPrescriptionBlocks()).toMap()
            }

            exerciseState.sets.forEachIndexed { index, set ->
                val previous = exerciseState.sets.getOrNull(index - 1)
                if (isAdvanced && (index == 0 || previous?.blockIndex != set.blockIndex || previous?.isExtra != set.isExtra)) {
                    val block = if (set.isExtra) null else set.blockIndex?.let { blocksByIndex[it] }
                    val headerText = when {
                        block != null -> stringResource(
                            R.string.block_header,
                            (set.blockIndex ?: 0) + 1,
                            (listOf(PrescriptionFormatter.headline(block, prescriptionLabels)) +
                                block.techniques.map { com.emanuel5014.trainable.ui.components.techniqueLabel(context, it) }).joinToString(" · ")
                        )
                        set.isExtra -> stringResource(R.string.extra_badge)
                        else -> null
                    }
                    if (headerText != null) {
                        Text(
                            text = headerText.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Primary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 10.dp, bottom = 2.dp)
                        )
                    }
                }
                EditSetRow(
                    set = set,
                    isFirst = index == 0,
                    isLast = index == exerciseState.sets.size - 1,
                    weightUnit = weightUnit,
                    onEdit = { onEditSet(set) },
                    onMoveUp = { onMoveSetUp(set) },
                    onMoveDown = { onMoveSetDown(set) }
                )
                if (index < exerciseState.sets.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = SurfaceContainerHighest,
                        thickness = 0.5.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GymButton(
                    onClick = onAddSet,
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add).uppercase(), style = MaterialTheme.typography.labelLarge)
                }
                if (!isTimeAndWeight && advancedOn) {
                    GymButton(
                        onClick = onEditPrescription,
                        containerColor = if (isAdvanced) Primary else Primary.copy(alpha = 0.1f),
                        contentColor = if (isAdvanced) OnPrimary else Primary,
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Percent, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.prescription).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditSetRow(
    set: SetLogEntity,
    isFirst: Boolean,
    isLast: Boolean,
    weightUnit: String = "kg",
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onEdit() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = set.numeroSerie.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = com.emanuel5014.trainable.ui.components.setValueText(set, weightUnit),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            com.emanuel5014.trainable.ui.components.SetLogBadges(set = set, modifier = Modifier.padding(vertical = 2.dp))
            if (!set.note.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = set.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
        
        Row {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (isFirst) OnSurfaceVariant.copy(alpha = 0.2f) else OnSurfaceVariant
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isLast) OnSurfaceVariant.copy(alpha = 0.2f) else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CardioEditCard(
    cardio: CardioExerciseLogs,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean = false,
    onEditSet: (CardioLogEntity) -> Unit,
    onAddSet: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val languageCode = java.util.Locale.getDefault().language
    val translatedTitle = ExerciseTranslations.translate(cardio.categoria, languageCode)
    val cardBgColor = if (isDragging) SurfaceContainerHigh else SurfaceContainerLow
    val cardBorder = if (isDragging) androidx.compose.foundation.BorderStroke(2.dp, Primary) else null

    GymCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = cardBgColor,
        border = cardBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cardio_cat_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = if (isFirst) OnSurfaceVariant.copy(alpha = 0.2f) else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isLast) OnSurfaceVariant.copy(alpha = 0.2f) else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = translatedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            cardio.sets.forEachIndexed { index, set ->
                CardioSetEditRow(set = set, number = index + 1, onClick = { onEditSet(set) })
                if (index < cardio.sets.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = SurfaceContainerHighest,
                        thickness = 0.5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GymButton(
                onClick = onAddSet,
                containerColor = Primary.copy(alpha = 0.1f),
                contentColor = Primary,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add).uppercase(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One set of a cardio exercise in the edit screen: its time and distance, and its note under them. */
@Composable
private fun CardioSetEditRow(set: CardioLogEntity, number: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = OnSurface)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cardioSetValueText(set),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            if (!set.note.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = set.note, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
fun EditCardioDialog(
    cardioLog: CardioLogEntity,
    onDismiss: () -> Unit,
    onConfirm: (CardioLogEntity) -> Unit,
    /** Removes the set; null when it is the only one of its exercise. */
    onDelete: (() -> Unit)? = null,
    /** Shown in the title when the exercise has more than one set. */
    setNumber: Int? = null
) {
    var note by remember { mutableStateOf(cardioLog.note.orEmpty()) }
    var rpe by remember { mutableStateOf(cardioLog.rpe) }
    val rpeScale = com.emanuel5014.trainable.ui.components.editableRpeScale(hasValue = cardioLog.rpe != null)
    var categoria by remember { mutableStateOf(cardioLog.categoria) }
    var distanza by remember { mutableStateOf(cardioLog.distanza.toString()) }
    var durataOre by remember { mutableStateOf((cardioLog.durataSecondi / 3600).let { if (it > 0) it.toString() else "" }) }
    var durataMinuti by remember { mutableStateOf(((cardioLog.durataSecondi % 3600) / 60).toString()) }
    var durataSecondi by remember { mutableStateOf((cardioLog.durataSecondi % 60).toString()) }

    val isValid = categoria.isNotBlank() && (distanza.isNotBlank() || durataMinuti.isNotBlank() || durataOre.isNotBlank())

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { 
            Text(
                setNumber?.let { stringResource(R.string.set_number, it) } ?: stringResource(R.string.edit_exercise_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            ) 
        },
        text = {
            Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
CardioInputForm(
                categoria = categoria,
                onCategoriaChange = { categoria = it },
                distanza = distanza,
                onDistanzaChange = { distanza = it },
                durataOre = durataOre,
                onDurataOreChange = { durataOre = it },
                durataMinuti = durataMinuti,
                onDurataMinutiChange = { durataMinuti = it },
                durataSecondi = durataSecondi,
                onDurataSecondiChange = { durataSecondi = it },
                showCategory = false,
                note = note,
                onNoteChange = { note = it }
            )
                rpeScale?.let { scale ->
                    com.emanuel5014.trainable.ui.components.RpeSelector(value = rpe, onValueChange = { rpe = it }, scale = scale)
                }
              }
}
        },
        confirmButton = {
            GymButton(
                onClick = {
                    val dist = distanza.toFloatOrNull() ?: 0f
                    val h = durataOre.toIntOrNull() ?: 0
                    val m = durataMinuti.toIntOrNull() ?: 0
                    val s = durataSecondi.toIntOrNull() ?: 0
                    val dur = h * 3600 + m * 60 + s
                    onConfirm(cardioLog.copy(categoria = categoria, distanza = dist, durataSecondi = dur, note = note.trim().ifBlank { null }, rpe = rpe))
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cardio_remove_set), tint = Error)
                    }
                }
                GymButton(
                    onClick = onDismiss,
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        },
        containerColor = Surface,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = Shapes.extraLarge
    )
}

@Composable
fun WorkoutSessionHeaderCard(
    sessionTimestamp: Long,
    sessionDurationMs: Long?,
    exerciseCount: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onEditClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FlowRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = remember(sessionTimestamp) {
                            SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(Date(sessionTimestamp))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                }

                // Duration
                val durationText = remember(sessionDurationMs) {
                    if (sessionDurationMs != null && sessionDurationMs > 0L) {
                        val totalSec = sessionDurationMs / 1000
                        val h = totalSec / 3600
                        val m = (totalSec % 3600) / 60
                        if (h > 0) "${h}h ${m}m" else "${m}m"
                    } else null
                }
                if (durationText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface
                        )
                    }
                }

                // Exercise count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    val countLabel = if (exerciseCount == 1) {
                        stringResource(R.string.exercise)
                    } else {
                        stringResource(R.string.share_exercises).lowercase()
                    }
                    Text(
                        text = "$exerciseCount $countLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = SurfaceContainerHigh,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutDetailsBottomSheet(
    currentName: String,
    currentTimestamp: Long,
    currentDurationMs: Long?,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newTimestamp: Long, newDurationMs: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameText by remember {
        mutableStateOf(if (currentName == "Custom Workout" || currentName == "Cardio") "" else currentName)
    }
    var selectedTimestamp by remember { mutableLongStateOf(currentTimestamp) }

    val initialDurationMs = currentDurationMs ?: 0L
    var hoursText by remember {
        mutableStateOf(if (initialDurationMs > 0L) (initialDurationMs / 3600000).toString() else "")
    }
    var minutesText by remember {
        mutableStateOf(if (initialDurationMs > 0L) (((initialDurationMs % 3600000) / 60000)).toString() else "")
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = 24.dp, spacing = 20.dp) {
                Text(
                    text = stringResource(R.string.session_details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )

                // 1. Workout Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.rename_workout),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = { Text(text = stringResource(R.string.custom_workout)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        singleLine = true,
                        shape = Shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Workout Date
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.edit_date),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    Surface(
                        onClick = { showDatePickerDialog = true },
                        shape = Shapes.medium,
                        color = SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = Primary
                            )
                            Text(
                                text = remember(selectedTimestamp) {
                                    SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(Date(selectedTimestamp))
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = OnSurface
                            )
                        }
                    }
                }

                // 3. Duration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.workout_duration),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { hoursText = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.hours)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = Primary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium
                        )
                        Text(
                            ":",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = OnSurface
                        )
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.minutes)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
            SheetFormFooter(horizontalPadding = 24.dp) {
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymButton(
                        onClick = onDismiss,
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
                    }

                    GymButton(
                        onClick = {
                            val h = hoursText.toIntOrNull() ?: 0
                            val m = minutesText.toIntOrNull() ?: 0
                            val totalMs = (h * 3600L + m * 60L) * 1000L
                            val newDuration = if (totalMs > 0) totalMs else null
                            onConfirm(nameText, selectedTimestamp, newDuration)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedTimestamp = it
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = Surface
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

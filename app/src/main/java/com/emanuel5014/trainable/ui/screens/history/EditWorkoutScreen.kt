package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.AddCardioDialog
import com.emanuel5014.trainable.ui.components.CardioInputForm
import com.emanuel5014.trainable.ui.components.SwapExerciseBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val editablePresetExercises by viewModel.editablePresetExercises.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val localMergedItems = remember { mutableStateListOf<Any>() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragDropState = rememberEditWorkoutDragDropState(
        lazyListState = listState,
        items = localMergedItems,
        haptic = haptic,
        hapticEnabled = hapticEnabled,
        scope = scope,
        onOrderChanged = { newOrder ->
            viewModel.updateItemsOrder(newOrder)
        }
    )

    LaunchedEffect(state.exercises, state.cardioLogs) {
        if (!dragDropState.isDragging) {
            val items = mutableListOf<Pair<Int, Any>>()
            state.exercises.forEach { ex ->
                val order = ex.sets.firstOrNull()?.ordineEsercizio ?: 0
                items.add(Pair(order, ex as Any))
            }
            state.cardioLogs.forEach { cardio ->
                val order = cardio.ordineEsercizio
                items.add(Pair(order, cardio as Any))
            }
            items.sortBy { it.first }
            localMergedItems.clear()
            localMergedItems.addAll(items.map { it.second })
        }
    }

    var editingSet by remember { mutableStateOf<SetLogEntity?>(null) }
    var editingCardio by remember { mutableStateOf<CardioLogEntity?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseToSwap by remember { mutableStateOf<Int?>(null) }
    var showDeleteExerciseDialog by remember { mutableStateOf<Int?>(null) }
    var showEditDetailsSheet by remember { mutableStateOf(false) }
    var showAddCardio by remember { mutableStateOf(false) }
    var pendingCardioCategory by remember { mutableStateOf<String?>(null) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }

    val autoScrollThreshold = with(density) { 48.dp.toPx() }
    val maxAutoScrollSpeed = with(density) { 12.dp.toPx() }

    LaunchedEffect(dragDropState.isDragging) {
        if (dragDropState.isDragging) {
            while (true) {
                val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                if (viewportHeight > 0f) {
                    val fingerY = dragDropState.fingerY
                    if (fingerY < autoScrollThreshold) {
                        val ratio = (1f - (fingerY / autoScrollThreshold)).coerceIn(0f, 1f)
                        val speed = maxAutoScrollSpeed * ratio
                        if (speed > 0.5f) {
                            listState.scrollBy(-speed)
                            dragDropState.onScroll(-speed)
                            dragDropState.onDrag(fingerY)
                        }
                    } else if (fingerY > viewportHeight - autoScrollThreshold) {
                        val distanceToBottom = viewportHeight - fingerY
                        val ratio = (1f - (distanceToBottom / autoScrollThreshold)).coerceIn(0f, 1f)
                        val speed = maxAutoScrollSpeed * ratio
                        if (speed > 0.5f) {
                            listState.scrollBy(speed)
                            dragDropState.onScroll(speed)
                            dragDropState.onDrag(fingerY)
                        }
                    }
                }
                delay(16)
            }
        }
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
                    exerciseCount = localMergedItems.size,
                    onEditClick = { showEditDetailsSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

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
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        dragDropState.onDragStart(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragDropState.onDrag(change.position.y)
                                    },
                                    onDragEnd = {
                                        dragDropState.onDragEnd()
                                    },
                                    onDragCancel = {
                                        dragDropState.onDragEnd()
                                    }
                                )
                            },
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    itemsIndexed(localMergedItems, key = { _, item -> getWorkoutItemKey(item) }) { index, item ->
                        val itemKey = getWorkoutItemKey(item)
                        val isDragging = itemKey in dragDropState.draggedItemKeys
                        val translationY = dragDropState.dragTranslationY(itemKey)

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isDragging) 1.04f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "exercise_drag_scale"
                        )
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (dragDropState.isDragging && !isDragging) 0.65f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "exercise_drag_alpha"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 16.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "exercise_drag_elevation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isDragging) Modifier
                                    else Modifier.animateItem(
                                        placementSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = 0.85f
                                        )
                                    )
                                )
                                .zIndex(if (isDragging) 100f else 1f)
                                .graphicsLayer {
                                    this.translationY = translationY
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = animatedAlpha
                                    shadowElevation = elevation.toPx()
                                    shape = Shapes.extraLarge
                                    clip = false
                                }
                        ) {
                            when (item) {
                                is EditExerciseState -> {
                                    val exerciseState = item
                                    val currentSid = exerciseState.sets.firstOrNull()?.supersetId
                                    val isSuperset = currentSid != null
                                    val isLinked = isSuperset && index < localMergedItems.lastIndex &&
                                            (localMergedItems[index + 1] as? EditExerciseState)?.sets?.firstOrNull()?.supersetId == currentSid

                                    EditExerciseCard(
                                        exerciseState = exerciseState,
                                        languageCode = languageCode,
                                        isFirst = index == 0,
                                        isLast = index == localMergedItems.lastIndex,
                                        isSuperset = isSuperset,
                                        isLinked = isLinked,
                                        isDragging = isDragging,
                                        weightUnit = state.weightUnit,
                                        onEditSet = { editingSet = it },
                                        onAddSet = { viewModel.addSet(exerciseState.exercise.id) },
                                        onSwapExercise = {
                                            exerciseToSwap = exerciseState.exercise.id
                                            showExercisePicker = true
                                        },
                                        onDeleteExercise = { showDeleteExerciseDialog = exerciseState.exercise.id },
                                        onMoveSetUp = { viewModel.moveSetUp(it) },
                                        onMoveSetDown = { viewModel.moveSetDown(it) },
                                        onMoveExerciseUp = { viewModel.moveExerciseUp(exerciseState.exercise.id) },
                                        onMoveExerciseDown = { viewModel.moveExerciseDown(exerciseState.exercise.id) },
                                        onToggleSuperset = { viewModel.toggleSupersetWithNext(exerciseState.exercise.id) }
                                    )
                                }
                                is CardioLogEntity -> {
                                    CardioEditCard(
                                        cardioLog = item,
                                        isFirst = index == 0,
                                        isLast = index == localMergedItems.lastIndex,
                                        isDragging = isDragging,
                                        onEdit = { editingCardio = it },
                                        onDelete = { viewModel.deleteCardioLog(it) },
                                        onMoveUp = { viewModel.moveCardioUp(it) },
                                        onMoveDown = { viewModel.moveCardioDown(it) }
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

    if (editingCardio != null) {
        EditCardioDialog(
            cardioLog = editingCardio!!,
            onDismiss = { editingCardio = null },
            onConfirm = {
                viewModel.updateCardioLog(it)
                editingCardio = null
            }
        )
    }

    if (showAddCardio) {
        AddCardioDialog(
            initialCategoria = pendingCardioCategory,
            onDismiss = {
                showAddCardio = false
                pendingCardioCategory = null
            },
            onConfirm = { categoria, distanza, durataSecondi ->
                viewModel.addCardioLog(categoria, distanza, durataSecondi)
                showAddCardio = false
                pendingCardioCategory = null
            }
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
        val exerciseStateToSwap = exerciseToSwap?.let { swapId -> state.exercises.find { it.exercise.id == swapId } }
        val currentSets = exerciseStateToSwap?.sets?.size ?: 3
        val currentReps = exerciseStateToSwap?.sets?.firstOrNull()?.let {
            if (it.durataSecondi != null) "${it.durataSecondi}" else "${it.repsEffettive}"
        } ?: "8"

        SwapExerciseBottomSheet(
            currentSets = currentSets,
            currentReps = currentReps,
            availableExercises = state.availableExercises,
            languageCode = languageCode,
            isAdding = exerciseToSwap == null,
            editablePresetExercises = editablePresetExercises,
            categories = state.categories,
            onExerciseSelected = { exercise, sets, reps, rest, exerciseType, durataTargetSec ->
                val currentSwapId = exerciseToSwap
                if (currentSwapId != null) {
                    viewModel.swapExercise(currentSwapId, exercise.id, exerciseType, durataTargetSec)
                } else {
                    viewModel.addExercise(exercise.id, sets, reps, exerciseType, durataTargetSec)
                }
                exerciseToSwap = null
                showExercisePicker = false
            },
            onCardioExerciseSelected = { exercise, durationMinutes, rest ->
                val currentSwapId = exerciseToSwap
                if (currentSwapId != null) {
                    viewModel.swapExerciseWithCardio(currentSwapId, exercise.nome, durationMinutes)
                } else {
                    viewModel.addCardioLog(exercise.nome, 0f, durationMinutes * 60)
                }
                exerciseToSwap = null
                showExercisePicker = false
            },
            onAddCustomExercise = { name, category, onCreated ->
                viewModel.addCustomExercise(name, category, onCreated)
            },
            onEditCustomExercise = viewModel::updateCustomExercise,
            onDeleteCustomExercise = viewModel::deleteCustomExercise,
            onDismiss = {
                exerciseToSwap = null
                showExercisePicker = false
            }
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
    onDeleteExercise: () -> Unit,
    onMoveSetUp: (SetLogEntity) -> Unit,
    onMoveSetDown: (SetLogEntity) -> Unit,
    onMoveExerciseUp: () -> Unit,
    onMoveExerciseDown: () -> Unit,
    onToggleSuperset: () -> Unit
) {
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
                color = OnSurface,
                modifier = Modifier.fillMaxWidth()
            )

            val isTimeAndWeight = exerciseState.sets.any { it.durataSecondi != null }
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
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.time_and_weight_badge).uppercase(),
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
            
            exerciseState.sets.forEachIndexed { index, set ->
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
            val repsOrTime = if (set.durataSecondi != null) "${set.durataSecondi}s" else "${set.repsEffettive}"
            Text(
                text = WeightUnitConverter.formatWithUnit(
                    WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                    weightUnit
                ) + " × $repsOrTime",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
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
    cardioLog: CardioLogEntity,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean = false,
    onEdit: (CardioLogEntity) -> Unit,
    onDelete: (CardioLogEntity) -> Unit,
    onMoveUp: (CardioLogEntity) -> Unit,
    onMoveDown: (CardioLogEntity) -> Unit
) {
    val languageCode = java.util.Locale.getDefault().language
    val translatedTitle = ExerciseTranslations.translate(cardioLog.categoria, languageCode)
    val cardBgColor = if (isDragging) SurfaceContainerHigh else SurfaceContainerLow
    val cardBorder = if (isDragging) androidx.compose.foundation.BorderStroke(2.dp, Primary) else null
    
    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(cardioLog) },
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
                    IconButton(
                        onClick = { onMoveUp(cardioLog) },
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
                        onClick = { onMoveDown(cardioLog) },
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
                    IconButton(
                        onClick = { onDelete(cardioLog) },
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
                text = translatedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cardioLog.distanza > 0f) {
                    Text(
                        text = "${cardioLog.distanza} km",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                val h = cardioLog.durataSecondi / 3600
                val m = (cardioLog.durataSecondi % 3600) / 60
                val s = cardioLog.durataSecondi % 60
                val durationText = if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
        }
    }
}

@Composable
fun EditCardioDialog(
    cardioLog: CardioLogEntity,
    onDismiss: () -> Unit,
    onConfirm: (CardioLogEntity) -> Unit
) {
    var categoria by remember { mutableStateOf(cardioLog.categoria) }
    var distanza by remember { mutableStateOf(cardioLog.distanza.toString()) }
    var durataOre by remember { mutableStateOf((cardioLog.durataSecondi / 3600).let { if (it > 0) it.toString() else "" }) }
    var durataMinuti by remember { mutableStateOf(((cardioLog.durataSecondi % 3600) / 60).toString()) }
    var durataSecondi by remember { mutableStateOf((cardioLog.durataSecondi % 60).toString()) }

    val isValid = categoria.isNotBlank() && (distanza.isNotBlank() || durataMinuti.isNotBlank() || durataOre.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.edit_exercise_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            ) 
        },
        text = {
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
                showCategory = false
            )
        },
        confirmButton = {
            GymButton(
                onClick = {
                    val dist = distanza.toFloatOrNull() ?: 0f
                    val h = durataOre.toIntOrNull() ?: 0
                    val m = durataMinuti.toIntOrNull() ?: 0
                    val s = durataSecondi.toIntOrNull() ?: 0
                    val dur = h * 3600 + m * 60 + s
                    onConfirm(cardioLog.copy(categoria = categoria, distanza = dist, durataSecondi = dur))
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant
            ) {
                Text(stringResource(R.string.cancel).uppercase())
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
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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

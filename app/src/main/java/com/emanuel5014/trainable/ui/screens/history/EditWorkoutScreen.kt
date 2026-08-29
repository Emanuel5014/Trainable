package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
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
import java.text.SimpleDateFormat
import java.util.Date

private fun getMergedSupersetRange(index: Int, list: List<Any>): IntRange {
    val currentEx = list.getOrNull(index) as? EditExerciseState ?: return index..index
    val sid = currentEx.sets.firstOrNull()?.supersetId ?: return index..index

    var start = index
    while (start > 0) {
        val prevEx = list[start - 1] as? EditExerciseState ?: break
        if (prevEx.sets.firstOrNull()?.supersetId != sid) break
        start--
    }

    var end = index
    while (end < list.lastIndex) {
        val nextEx = list[end + 1] as? EditExerciseState ?: break
        if (nextEx.sets.firstOrNull()?.supersetId != sid) break
        end++
    }

    return start..end
}

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
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.exercises, state.cardioLogs) {
        if (draggedItemIndex == null) {
            val items = mutableListOf<Pair<Int, Any>>()
            state.exercises.forEach { ex ->
                val order = ex.sets.firstOrNull()?.ordineEsercizio ?: 0
                items.add(Pair(order, ex as Any))
            }
            state.cardioLogs.forEach { cardio ->
                val order = cardio.ordineEsercizio
                items.add(Pair(if (order > 0) order else Int.MAX_VALUE, cardio as Any))
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCardio by remember { mutableStateOf(false) }
    var pendingCardioCategory by remember { mutableStateOf<String?>(null) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showRenameDialog = true }
                                .padding(end = 8.dp)
                        ) {
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
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showDatePicker = true }
                        ) {
                            Text(
                                text = SimpleDateFormat("EEEE, d MMMM yyyy", LocalLocale.current.platformLocale)
                                    .format(Date(state.sessionTimestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = " • " + stringResource(R.string.edit_date).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showDurationDialog = true }
                                .padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.sessionDurationMs != null) {
                                    val totalSec = state.sessionDurationMs!! / 1000
                                    val h = totalSec / 3600
                                    val m = (totalSec % 3600) / 60
                                    if (h > 0) "${h}h ${m}m" else "${m}m"
                                } else stringResource(R.string.add_duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
            if (state.exercises.isEmpty() && state.cardioLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(localMergedItems, key = { _, item ->
                        when (item) {
                            is EditExerciseState -> -item.exercise.id
                            is CardioLogEntity -> item.id
                            else -> 0
                        }
                    }) { index, item ->
                        val currentDraggedIndex = draggedItemIndex
                        val draggedRange = if (currentDraggedIndex != null) getMergedSupersetRange(currentDraggedIndex, localMergedItems) else null
                        val isDragging = draggedRange != null && index in draggedRange

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isDragging) 1.04f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "exercise_drag_scale"
                        )
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 16.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "exercise_drag_elevation"
                        )

                        val itemKey = when (item) {
                            is EditExerciseState -> -item.exercise.id
                            is CardioLogEntity -> item.id
                            else -> 0
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (draggedItemIndex != null && !isDragging) Modifier.animateItem() else Modifier)
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    shadowElevation = elevation.toPx()
                                    shape = Shapes.extraLarge
                                    clip = false
                                }
                                .pointerInput(itemKey) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedItemIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val itemHeight = 160.dp.toPx()
                                            val currentIdx = draggedItemIndex
                                            if (currentIdx != null) {
                                                val range = getMergedSupersetRange(currentIdx, localMergedItems)
                                                val size = range.endInclusive - range.start + 1

                                                if (dragOffsetY > 0 && range.endInclusive < localMergedItems.lastIndex) {
                                                    val nextIndex = range.endInclusive + 1
                                                    val nextRange = getMergedSupersetRange(nextIndex, localMergedItems)
                                                    val nextSize = nextRange.endInclusive - nextRange.start + 1
                                                    val threshold = (nextSize * itemHeight) / 2f

                                                    if (dragOffsetY > threshold) {
                                                        val draggedItems = localMergedItems.subList(range.start, range.endInclusive + 1).toList()
                                                        repeat(size) {
                                                            localMergedItems.removeAt(range.start)
                                                        }
                                                        val insertIndex = range.start + nextSize
                                                        localMergedItems.addAll(insertIndex, draggedItems)

                                                        draggedItemIndex = insertIndex + (currentIdx - range.start)
                                                        dragOffsetY -= nextSize * itemHeight
                                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                } else if (dragOffsetY < 0 && range.start > 0) {
                                                    val prevIndex = range.start - 1
                                                    val prevRange = getMergedSupersetRange(prevIndex, localMergedItems)
                                                    val prevSize = prevRange.endInclusive - prevRange.start + 1
                                                    val threshold = -(prevSize * itemHeight) / 2f

                                                    if (dragOffsetY < threshold) {
                                                        val draggedItems = localMergedItems.subList(range.start, range.endInclusive + 1).toList()
                                                        repeat(size) {
                                                            localMergedItems.removeAt(range.start)
                                                        }
                                                        val insertIndex = prevRange.start
                                                        localMergedItems.addAll(insertIndex, draggedItems)

                                                        draggedItemIndex = insertIndex + (currentIdx - range.start)
                                                        dragOffsetY += prevSize * itemHeight
                                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            viewModel.updateItemsOrder(localMergedItems.toList())
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

    if (showRenameDialog) {
        var newName by remember { 
            mutableStateOf(
                if (state.planName == "Custom Workout") "" else state.planName
            )
        }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = stringResource(R.string.rename_workout)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text(text = stringResource(R.string.custom_workout)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSessionName(newName)
                        showRenameDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDurationDialog) {
        val initialDurationMs = state.sessionDurationMs ?: 0L
        var hoursText by remember { mutableStateOf(((initialDurationMs / 3600000).toInt()).toString()) }
        var minutesText by remember { mutableStateOf((((initialDurationMs % 3600000) / 60000).toInt()).toString()) }
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text(stringResource(R.string.workout_duration), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.hours)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = OnSurface)
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.minutes)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = hoursText.toIntOrNull() ?: 0
                        val m = minutesText.toIntOrNull() ?: 0
                        val totalMs = (h * 3600L + m * 60L) * 1000L
                        viewModel.updateSessionDuration(if (totalMs > 0) totalMs else null)
                        showDurationDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            },
            containerColor = Surface,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = state.availableExercises,
            categories = state.categories,
            onDismiss = { showExercisePicker = false },
            onExerciseSelected = { exercise ->
                val currentSwapId = exerciseToSwap
                if (currentSwapId != null) {
                    viewModel.swapExercise(currentSwapId, exercise.id)
                } else if (exercise.categoria.equals("Cardio", ignoreCase = true)) {
                    pendingCardioCategory = ExerciseTranslations.translate(exercise.nome, languageCode)
                    showAddCardio = true
                } else {
                    viewModel.addExercise(exercise.id)
                }
                showExercisePicker = false
            },
            onAddCustomExercise = { name, category, onCreated ->
                viewModel.addCustomExercise(name, category, onCreated)
            },
            languageCode = languageCode,
            editablePresetExercises = editablePresetExercises
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.sessionTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { 
                            viewModel.updateSessionDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
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
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.superset).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                    color = Primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = exerciseState.exercise.categoria.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
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
            Text(
                text = WeightUnitConverter.formatWithUnit(
                    WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                    weightUnit
                ) + " × ${set.repsEffettive}",
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
                    fontWeight = FontWeight.ExtraBold
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

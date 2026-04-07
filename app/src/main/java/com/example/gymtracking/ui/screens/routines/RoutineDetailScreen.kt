package com.example.gymtracking.ui.screens.routines

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.R
import com.example.gymtracking.data.repository.dataStore
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.data.local.relation.PlanExerciseWithDetails
import com.example.gymtracking.ui.components.ExerciseEntryCard
import com.example.gymtracking.ui.components.ExercisePickerBottomSheet
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymIconButton
import com.example.gymtracking.ui.components.GymInputField
import com.example.gymtracking.ui.components.RoutineImagePicker
import com.example.gymtracking.ui.theme.*
import com.example.gymtracking.util.AppLocaleManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    onNavigateBack: () -> Unit,
    onStartWorkout: (Int) -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    val listState = rememberLazyListState()

    var editingExercise by remember { mutableStateOf<PlanExerciseWithDetails?>(null) }
    var showExerciseSheet by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("8-12") }
    var restText by remember { mutableStateOf("120") }

    // Local state for dragging to ensure smoothness
    val localExercises = remember { mutableStateListOf<PlanExerciseWithDetails>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Sync local list with UI state when not dragging
    LaunchedEffect(uiState.planDetails?.exercises) {
        if (draggedItemIndex == null) {
            localExercises.clear()
            uiState.planDetails?.exercises?.let { localExercises.addAll(it) }
        }
    }

    fun openAddSheet() {
        editingExercise = null
        selectedExerciseId = null
        setsText = "3"
        repsText = "8"
        restText = "120"
        showExercisePicker = true
    }

    fun openEditSheet(item: PlanExerciseWithDetails) {
        editingExercise = item
        selectedExerciseId = item.exercise.id
        setsText = item.planExercise.serieTarget.toString()
        repsText = item.planExercise.repsTarget
        restText = item.planExercise.recuperoTarget.toString()
        showExerciseSheet = true
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { Text(uiState.planDetails?.plan?.nome ?: "", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface,
                            description = "Back"
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        GymIconButton(
                            icon = Icons.Rounded.Delete,
                            onClick = { viewModel.deletePlan(onNavigateBack) },
                            containerColor = SurfaceContainerHigh,
                            contentColor = Error,
                            description = "Delete Routine"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        },
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
                        onClick = { uiState.planDetails?.plan?.id?.let(onStartWorkout) },
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
                            Text(stringResource(R.string.start), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.planDetails != null) {
            val details = uiState.planDetails!!

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RoutineImagePicker(
                        currentImageUri = details.plan.imageUri,
                        onImageSelected = { uri -> viewModel.updatePlanImage(uri) }
                    )
                }

                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            text = stringResource(R.string.start_date) + " " + com.example.gymtracking.ui.util.DateFormatter.format(details.plan.dataInizio),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        details.plan.note?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                itemsIndexed(localExercises, key = { _, item -> item.planExercise.id }) { index, item ->
                    val isDragging = draggedItemIndex == index
                    val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = if (isDragging) 1.02f else 1f
                                scaleY = if (isDragging) 1.02f else 1f
                                shadowElevation = elevation.toPx()
                                shape = RoundedCornerShape(28.dp)
                                clip = isDragging
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedItemIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        
                                        val itemHeight = 80.dp.toPx() 
                                        if (dragOffsetY > itemHeight / 2 && draggedItemIndex!! < localExercises.size - 1) {
                                            val targetIndex = draggedItemIndex!! + 1
                                            localExercises.add(targetIndex, localExercises.removeAt(draggedItemIndex!!))
                                            draggedItemIndex = targetIndex
                                            dragOffsetY -= itemHeight
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (dragOffsetY < -itemHeight / 2 && draggedItemIndex!! > 0) {
                                            val targetIndex = draggedItemIndex!! - 1
                                            localExercises.add(targetIndex, localExercises.removeAt(draggedItemIndex!!))
                                            draggedItemIndex = targetIndex
                                            dragOffsetY += itemHeight
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }

                                    },
                                    onDragEnd = {
                                        val finalIndex = draggedItemIndex!!
                                        viewModel.moveExercise(index, finalIndex)
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
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDragging) Primary else SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDragging) OnPrimary else Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ExerciseEntryCard(
                            item = item,
                            onMenuClick = { openEditSheet(item) },
                            modifier = Modifier.weight(1f),
                            languageCode = languageCode
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(140.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Routine not found", color = OnSurfaceVariant)
            }
        }
    }

    if (showExerciseSheet) {
        var expanded by remember { mutableStateOf(false) }
        val selectedExercise = uiState.availableExercises.firstOrNull { it.id == selectedExerciseId }

        ModalBottomSheet(
            onDismissRequest = { showExerciseSheet = false },
            containerColor = Surface,
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
                    .padding(horizontal = Spacing.CardPadding)
                    .padding(bottom = Spacing.extreme),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                    Text(
                        text = if (editingExercise == null) stringResource(R.string.add_exercise) else stringResource(R.string.edit_exercise),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (editingExercise == null) stringResource(R.string.exercise_details) else stringResource(R.string.update_exercise),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GymInputField(
                            value = selectedExercise?.nome ?: "",
                            onValueChange = {},
                            label = stringResource(R.string.exercise),
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(SurfaceContainerHigh)
                        ) {
                            uiState.availableExercises.forEach { exercise ->
                                DropMenuItem(
                                    text = exercise.nome,
                                    category = exercise.categoria,
                                    onClick = {
                                        selectedExerciseId = exercise.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

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
                            onValueChange = { repsText = it },
                            label = stringResource(R.string.reps),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    GymInputField(
                        value = restText,
                        onValueChange = { restText = it },
                        label = stringResource(R.string.rest_seconds),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GymButton(
                        onClick = {
                            editingExercise?.let {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeExercise(it.planExercise)
                            }
                            showExerciseSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    GymButton(
                        onClick = { showExerciseSheet = false },
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    
                    GymButton(
                        onClick = {
                            val exerciseId = selectedExerciseId ?: return@GymButton
                            val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                            val rest = restText.trim().toIntOrNull() ?: return@GymButton
                            val reps = repsText.trim().ifBlank { return@GymButton }

                            val current = editingExercise
                            if (current == null) {
                                viewModel.addExercise(
                                    exerciseId = exerciseId,
                                    serieTarget = sets,
                                    repsTarget = reps,
                                    recuperoTarget = rest
                                )
                            } else {
                                viewModel.updateExercise(
                                    original = current.planExercise,
                                    exerciseId = exerciseId,
                                    serieTarget = sets,
                                    repsTarget = reps,
                                    recuperoTarget = rest
                                )
                            }
                            showExerciseSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (editingExercise == null) stringResource(R.string.add).uppercase() else stringResource(R.string.save).uppercase(),
                            fontWeight = FontWeight.ExtraBold
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
            onAddCustomExercise = { nome, categoria ->
                viewModel.addCustomExercise(nome, categoria)
            },
            onEditCustomExercise = { exercise ->
                viewModel.updateCustomExercise(exercise)
            },
            onDeleteCustomExercise = { exercise ->
                viewModel.deleteCustomExercise(exercise)
            },
            onDismiss = { showExercisePicker = false },
            languageCode = languageCode
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

package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
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
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.map
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

    val scope = rememberCoroutineScope()
    val exerciseSheetState = rememberModalBottomSheetState()
    val routineSheetState = rememberModalBottomSheetState()

    var editingExercise by remember { mutableStateOf<PlanExerciseWithDetails?>(null) }
    var showExerciseSheet by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showRoutineEditSheet by remember { mutableStateOf(false) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

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
        // Inherit rest from the last exercise in the list, default to 120 if empty
        restText = localExercises.lastOrNull()?.planExercise?.recuperoTarget?.toString() ?: "120"
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

    fun openRoutineEditSheet() {
        uiState.planDetails?.plan?.let { plan ->
            routineName = plan.nome
            routineNote = plan.note.orEmpty()
            selectedDays.clear()
            plan.giorniSettimana?.split(",")?.forEach {
                it.toIntOrNull()?.let { value -> selectedDays.add(DayOfWeek.of(value)) }
            }
            showRoutineEditSheet = true
        }
    }

    Scaffold(
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
                        modifier = Modifier.padding(horizontal = Spacing.CardPadding)
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.start_date) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(details.plan.dataInizio),
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (!details.plan.giorniSettimana.isNullOrBlank()) {
                                val scheduledDays = remember(details.plan.giorniSettimana) {
                                    details.plan.giorniSettimana.split(",").mapNotNull { 
                                        it.toIntOrNull()?.let { value -> DayOfWeek.of(value) }
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DayOfWeek.entries.forEach { day ->
                                        val isScheduled = scheduledDays.contains(day)
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(if (isScheduled) Primary else SurfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isScheduled) OnPrimary else OnSurfaceVariant.copy(alpha = 0.5f)
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
                    }
                }

                if (localExercises.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp, bottom = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_exercises_in_routine),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                } else {
                    itemsIndexed(localExercises, key = { _, item -> item.planExercise.id }) { index, item ->
                        val isDragging = draggedItemIndex == index
                        val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                    shadowElevation = elevation.toPx()
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
                                onClick = { openEditSheet(item) },
                                modifier = Modifier.weight(1f),
                                languageCode = languageCode
                            )
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
                    .padding(horizontal = Spacing.CardPadding)
                    .padding(top = Spacing.medium, bottom = Spacing.extreme)
                    .navigationBarsPadding(),
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurface
                                )
                                selectedExercise?.let {
                                    Text(
                                        text = it.categoria,
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

                    RestSlider(
                        value = restText.toIntOrNull() ?: 120,
                        onValueChange = { restText = it.toString() },
                        hapticEnabled = hapticEnabled,
                        haptic = haptic,
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.edit_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.update_plan),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymInputField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = stringResource(R.string.routine_name),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.schedule_days),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                val isSelected = selectedDays.contains(day)
                                val backgroundColor by animateColorAsState(
                                    targetValue = if (isSelected) Primary else SurfaceContainerHigh,
                                    label = "day_bg"
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (isSelected) OnPrimary else OnSurfaceVariant,
                                    label = "day_content"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(backgroundColor)
                                        .clickable {
                                            if (isSelected) selectedDays.remove(day)
                                            else selectedDays.add(day)
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
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
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    
                    GymButton(
                        onClick = {
                            val trimmedName = routineName.trim()
                            if (trimmedName.isNotEmpty()) {
                                val note = routineNote.trim().takeIf { it.isNotBlank() }
                                val daysString = if (selectedDays.isEmpty()) null 
                                               else selectedDays.sortedBy { it.value }.joinToString(",") { it.value.toString() }
                                viewModel.updatePlan(trimmedName, note, daysString)
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
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
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
                fontWeight = FontWeight.Bold
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

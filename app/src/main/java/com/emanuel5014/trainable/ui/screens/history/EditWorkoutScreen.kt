package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.util.WeightUnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    var editingSet by remember { mutableStateOf<SetLogEntity?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseToSwap by remember { mutableStateOf<Int?>(null) }
    var showDeleteExerciseDialog by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.planName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.edit_exercise_title).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    GymIconButton(
                        icon = Icons.Rounded.ArrowBack,
                        onClick = onNavigateBack,
                        containerColor = Color.Transparent
                    )
                },
                actions = {
                    GymIconButton(
                        icon = Icons.Rounded.Add,
                        onClick = { 
                            exerciseToSwap = null
                            showExercisePicker = true 
                        },
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(state.exercises) { index, exerciseState ->
                    EditExerciseCard(
                        exerciseState = exerciseState,
                        languageCode = languageCode ?: "en",
                        isFirst = index == 0,
                        isLast = index == state.exercises.size - 1,
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
                        onMoveExerciseDown = { viewModel.moveExerciseDown(exerciseState.exercise.id) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
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

    if (showExercisePicker) {
        val categories = remember(state.availableExercises) {
            state.availableExercises.map { it.categoria }.distinct().sorted()
        }

        ExercisePickerBottomSheet(
            exercises = state.availableExercises,
            categories = categories,
            onDismiss = { showExercisePicker = false },
            onExerciseSelected = { exercise ->
                val currentSwapId = exerciseToSwap
                if (currentSwapId != null) {
                    viewModel.swapExercise(currentSwapId, exercise.id)
                } else {
                    viewModel.addExercise(exercise.id)
                }
                showExercisePicker = false
            },
            onAddCustomExercise = { name, category ->
                viewModel.addCustomExercise(name, category)
            },
            languageCode = languageCode ?: "en"
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
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
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
}

@Composable
fun EditExerciseCard(
    exerciseState: EditExerciseState,
    languageCode: String,
    isFirst: Boolean,
    isLast: Boolean,
    weightUnit: String = "kg",
    onEditSet: (SetLogEntity) -> Unit,
    onAddSet: () -> Unit,
    onSwapExercise: () -> Unit,
    onDeleteExercise: () -> Unit,
    onMoveSetUp: (SetLogEntity) -> Unit,
    onMoveSetDown: (SetLogEntity) -> Unit,
    onMoveExerciseUp: () -> Unit,
    onMoveExerciseDown: () -> Unit
) {
    GymCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseState.exercise.categoria.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = OnSurface
                    )
                }
                
                Row {
                    IconButton(onClick = onMoveExerciseUp, enabled = !isFirst) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = if (isFirst) OnSurfaceVariant.copy(alpha = 0.2f) else Primary)
                    }
                    IconButton(onClick = onMoveExerciseDown, enabled = !isLast) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = if (isLast) OnSurfaceVariant.copy(alpha = 0.2f) else Primary)
                    }
                    IconButton(onClick = onSwapExercise) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = OnSurfaceVariant)
                    }
                    IconButton(onClick = onDeleteExercise) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Error)
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
                containerColor = Primary.copy(alpha = 0.05f),
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
                fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.SemiBold,
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

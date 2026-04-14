package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapExerciseBottomSheet(
    currentSets: Int,
    currentReps: String,
    availableExercises: List<ExerciseEntity>,
    languageCode: String,
    onExerciseSelected: (ExerciseEntity, Int, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }
    var selectedExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var setsText by remember { mutableStateOf(currentSets.toString()) }
    var repsText by remember { mutableStateOf(currentReps) }

    val filteredExercises = remember(availableExercises, searchQuery, selectedCategory, languageCode) {
        availableExercises.filter { exercise ->
            val exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode)
            val matchesSearch = searchQuery.isBlank() || 
                exercise.nome.contains(searchQuery, ignoreCase = true) ||
                exerciseName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || 
                exercise.categoria == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val categories = remember(availableExercises) {
        availableExercises.map { it.categoria }.distinct().sorted()
    }

    when (step) {
        1 -> {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                containerColor = Surface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(OnSurfaceVariant.copy(alpha = 0.4f))
                    )
                },
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.CardPadding)
                        .padding(bottom = Spacing.extreme)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                        Text(
                            text = stringResource(R.string.swap_exercise),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.swap_exercise_message),
                            style = MaterialTheme.typography.headlineMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    GymInputField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = stringResource(R.string.search_exercises),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.all_categories)) }
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            val isSelected = exercise.id == selectedExercise?.id
                            ExerciseListItem(
                                exercise = exercise,
                                languageCode = languageCode,
                                onClick = { 
                                    selectedExercise = exercise
                                    step = 2
                                },
                                isSelected = isSelected,
                                showCurrent = exercise.id == availableExercises.firstOrNull()?.id
                            )
                        }
                    }
                }
            }
        }
        2 -> {
            if (selectedExercise != null) {
                SwapExerciseConfigDialog(
                    exercise = selectedExercise!!,
                    languageCode = languageCode,
                    initialSets = setsText,
                    initialReps = repsText,
                    onConfirm = { sets, reps ->
                        onExerciseSelected(selectedExercise!!, sets, reps)
                        onDismiss()
                    },
                    onBack = { step = 1 },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    languageCode: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    showCurrent: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = when {
            isSelected -> Primary.copy(alpha = 0.1f)
            else -> SurfaceContainerHigh
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExerciseTranslations.translate(exercise.nome, languageCode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = exercise.categoria.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
            if (isSelected) {
                Surface(
                    color = Primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (showCurrent) {
                Surface(
                    color = Surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwapExerciseConfigDialog(
    exercise: ExerciseEntity,
    languageCode: String,
    initialSets: String,
    initialReps: String,
    onConfirm: (Int, String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var setsText by remember { mutableStateOf(initialSets) }
    var repsText by remember { mutableStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(
                text = stringResource(R.string.swap_exercise),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = ExerciseTranslations.translate(exercise.nome, languageCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )

                Text(
                    text = "Configure the new exercise:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sets = setsText.trim().toIntOrNull() ?: return@TextButton
                    val reps = repsText.trim().takeIf { it.isNotBlank() } ?: return@TextButton
                    onConfirm(sets, reps)
                }
            ) {
                Text(stringResource(R.string.confirm), color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back), color = OnSurfaceVariant)
            }
        }
    )
}

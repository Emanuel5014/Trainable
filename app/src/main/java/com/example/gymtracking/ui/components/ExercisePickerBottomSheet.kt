package com.example.gymtracking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracking.data.ExerciseTranslations
import com.example.gymtracking.data.local.entity.ExerciseEntity as ExerciseEntity
import com.example.gymtracking.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    exercises: List<ExerciseEntity>,
    categories: List<String>,
    onExerciseSelected: (ExerciseEntity) -> Unit,
    onAddCustomExercise: (String, String) -> Unit,
    onEditCustomExercise: ((ExerciseEntity) -> Unit)? = null,
    onDeleteCustomExercise: ((ExerciseEntity) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en"
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedCategory, languageCode) {
        exercises.filter { exercise ->
            val exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode)
            val matchesSearch = searchQuery.isBlank() || 
                exercise.nome.contains(searchQuery, ignoreCase = true) ||
                exerciseName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || 
                exercise.categoria == selectedCategory
            matchesSearch && matchesCategory
        }
    }

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
            Text(
                text = "SELECT EXERCISE",
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.xtraSmall))
            Text(
                text = "Choose from library",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f),
                    focusedLeadingIconColor = Primary,
                    unfocusedLeadingIconColor = OnSurfaceVariant,
                    cursorColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary
                    )
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = OnPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(filteredExercises) { exercise ->
                    ExercisePickerItem(
                        exercise = exercise,
                        onClick = {
                            onExerciseSelected(exercise)
                            onDismiss()
                        },
                        onEditClick = if (onEditCustomExercise != null && exercise.id >= 1000) {
                            { exerciseToEdit = exercise }
                        } else null,
                        onDeleteClick = if (onDeleteCustomExercise != null && exercise.id >= 1000) {
                            { exerciseToDelete = exercise }
                        } else null,
                        languageCode = languageCode
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            OutlinedButton(
                onClick = { showAddCustomDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                ),
                border = BorderStroke(1.dp, Primary)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Add Custom Exercise", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomExerciseDialog(
            categories = categories,
            onConfirm = { name, category ->
                onAddCustomExercise(name, category)
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false }
        )
    }

    if (exerciseToEdit != null) {
        EditCustomExerciseDialog(
            exercise = exerciseToEdit!!,
            categories = categories,
            onConfirm = { updatedExercise ->
                onEditCustomExercise?.invoke(updatedExercise)
                exerciseToEdit = null
            },
            onDismiss = { exerciseToEdit = null }
        )
    }

    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            containerColor = SurfaceContainerHigh,
            title = {
                Text("Delete Exercise?", fontWeight = FontWeight.Bold, color = OnSurface)
            },
            text = {
                Text("Delete \"${exerciseToDelete!!.nome}\"? This cannot be undone.", color = OnSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomExercise?.invoke(exerciseToDelete!!)
                        exerciseToDelete = null
                    }
                ) {
                    Text("DELETE", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("CANCEL", color = Primary)
                }
            }
        )
    }
}

@Composable
private fun ExercisePickerItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    languageCode: String = "en"
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.large)
            .clickable(onClick = onClick),
        color = SurfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ExerciseTranslations.translate(exercise.nome, languageCode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (exercise.id >= 1000) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = SurfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = exercise.categoria,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            if (onEditClick != null || onDeleteClick != null) {
                Row {
                    if (onEditClick != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = OnSurfaceVariant
                            )
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = Error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add",
                tint = Primary
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddCustomExerciseDialog(
    categories: List<String>,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Altro") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Exercise",
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = Primary
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!categoryExpanded) categoryExpanded = true
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            enabled = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = Shapes.large,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (exerciseName.isNotBlank()) {
                        onConfirm(exerciseName.trim(), selectedCategory)
                    }
                },
                enabled = exerciseName.isNotBlank()
            ) {
                Text("ADD", color = if (exerciseName.isNotBlank()) Primary else OnSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainer
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditCustomExerciseDialog(
    exercise: ExerciseEntity,
    categories: List<String>,
    onConfirm: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseName by remember { mutableStateOf(exercise.nome) }
    var selectedCategory by remember { mutableStateOf(exercise.categoria) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Exercise",
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = Primary
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!categoryExpanded) categoryExpanded = true
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            enabled = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = Shapes.large,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (exerciseName.isNotBlank()) {
                        onConfirm(
                            exercise.copy(
                                nome = exerciseName.trim(),
                                categoria = selectedCategory
                            )
                        )
                    }
                },
                enabled = exerciseName.isNotBlank()
            ) {
                Text("SAVE", color = if (exerciseName.isNotBlank()) Primary else OnSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainer
    )
}

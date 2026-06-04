package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.rememberResponsiveSize

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
    rememberResponsiveSize()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyCustom by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedCategory, showOnlyCustom, languageCode) {
        exercises.filter { exercise ->
            val exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode)
            val matchesSearch = searchQuery.isBlank() ||
                exercise.nome.contains(searchQuery, ignoreCase = true) ||
                exerciseName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null ||
                exercise.categoria == selectedCategory
            val matchesCustom = !showOnlyCustom || exercise.id >= 1000
            matchesSearch && matchesCategory && matchesCustom
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(bottom = ResponsiveSize.cardPadding)
        ) {
            Text(
                text = stringResource(R.string.select_exercise).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = ResponsiveSize.labelLargeSize
                ),
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(Spacing.xtraSmall))
            Text(
                text = stringResource(R.string.choose_from_library),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = ResponsiveSize.headlineMediumSize
                ),
                color = OnSurface,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_exercises)) },
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
                    selected = selectedCategory == null && !showOnlyCustom,
                    onClick = {
                        selectedCategory = null
                        showOnlyCustom = false
                    },
                    label = { Text(stringResource(R.string.all_categories)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary
                    )
                )
                FilterChip(
                    selected = showOnlyCustom,
                    onClick = { showOnlyCustom = !showOnlyCustom },
                    label = { Text(stringResource(R.string.filter_custom)) },
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
                Text(stringResource(R.string.add_custom_exercise), fontWeight = FontWeight.ExtraBold)
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
                Text(stringResource(R.string.delete_exercise_title), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Text(stringResource(R.string.delete_exercise_message, exerciseToDelete!!.nome), color = OnSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomExercise?.invoke(exerciseToDelete!!)
                        exerciseToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
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
                        fontWeight = FontWeight.SemiBold
                    )
                    if (exercise.id >= 1000) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = SurfaceContainerHighest,
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = "Custom",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.padding(4.dp).size(12.dp)
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
                text = stringResource(R.string.new_exercise),
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
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
                            label = { Text(stringResource(R.string.category)) },
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
                Text(stringResource(R.string.add).uppercase(), color = if (exerciseName.isNotBlank()) Primary else OnSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                text = stringResource(R.string.edit_exercise_title),
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
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
                            label = { Text(stringResource(R.string.category)) },
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
                Text(stringResource(R.string.save).uppercase(), color = if (exerciseName.isNotBlank()) Primary else OnSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainer
    )
}

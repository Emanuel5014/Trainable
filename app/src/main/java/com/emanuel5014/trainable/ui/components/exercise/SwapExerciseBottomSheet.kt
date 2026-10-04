package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
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
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapExerciseBottomSheet(
    currentSets: Int,
    currentReps: String,
    availableExercises: List<ExerciseEntity>,
    languageCode: String,
    onExerciseSelected: (ExerciseEntity, Int, String, Int?, String, Int?) -> Unit,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit,
    onEditCustomExercise: ((ExerciseEntity) -> Unit)? = null,
    onDeleteCustomExercise: ((ExerciseEntity) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isAdding: Boolean = false,
    editablePresetExercises: Boolean = false,
    categories: List<String>? = null,
    onCardioExerciseSelected: ((ExerciseEntity, Int, Int?) -> Unit)? = null,
    /** When set, an "Advanced" (%1RM / blocks) type is offered. Receives the blocks and the rest in seconds. */
    onAdvancedExerciseSelected: ((ExerciseEntity, List<PrescriptionBlock>, Int?) -> Unit)? = null,
    /** Prescription of the exercise being swapped, so the new one can keep it. */
    initialBlocks: List<PrescriptionBlock> = emptyList()
) {
    rememberResponsiveSize()

    var step by remember { mutableStateOf(1) }
    var selectedExercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var setsText by remember { mutableStateOf(currentSets.toString()) }
    var repsText by remember { mutableStateOf(currentReps) }
    
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showOnlyCustom by remember { mutableStateOf(false) }

    val filteredExercises = remember(availableExercises, searchQuery, selectedCategory, showOnlyCustom, languageCode) {
        availableExercises.filter { exercise ->
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

    val allCategories = categories ?: remember(availableExercises) {
        availableExercises.map { it.categoria }.distinct().sorted()
    }

    val imeVisible = isKeyboardVisible

    when (step) {
        1 -> {
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
                    // Title and the custom-exercise button make way for the list while the keyboard is up
                    AnimatedVisibility(visible = !imeVisible) {
                        Column {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                                Text(
                                    text = if (isAdding) stringResource(R.string.add_exercise) else stringResource(R.string.swap_exercise),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = ResponsiveSize.labelLargeSize
                                ),
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isAdding) stringResource(R.string.exercise_details) else stringResource(R.string.swap_exercise_message),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = ResponsiveSize.headlineMediumSize
                                ),
                                color = OnSurface,
                                fontWeight = FontWeight.Black
                            )
                            }

                            Spacer(modifier = Modifier.height(Spacing.large))
                        }
                    }

                    GymInputField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = stringResource(R.string.search_exercises),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    AnimatedVisibility(visible = !imeVisible) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null && !showOnlyCustom,
                                    onClick = { 
                                        selectedCategory = null
                                        showOnlyCustom = false
                                    },
                                    label = { Text(stringResource(R.string.all_categories)) }
                                )
                                FilterChip(
                                    selected = showOnlyCustom,
                                    onClick = { showOnlyCustom = !showOnlyCustom },
                                    label = { Text(stringResource(R.string.filter_custom)) }
                                )
                                allCategories.forEach { category ->
                                    val translatedCategory = ExerciseTranslations.translateCategory(category, languageCode)
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(translatedCategory) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.medium))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
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
                                onEditClick = if (onEditCustomExercise != null && (exercise.id >= 1000 || editablePresetExercises)) {
                                    { exerciseToEdit = exercise }
                                } else null,
                                onDeleteClick = if (onDeleteCustomExercise != null && exercise.id >= 1000) {
                                    { exerciseToDelete = exercise }
                                } else null,
                                isSelected = isSelected,
                                showCurrent = exercise.id == availableExercises.firstOrNull()?.id
                            )
                        }
                    }

                    AnimatedVisibility(visible = !imeVisible) {
                        Column {
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
                }
            }
        }
        2 -> {
            if (selectedExercise != null) {
                val isCardio = selectedExercise!!.categoria.equals("Cardio", ignoreCase = true)
                if (isCardio && onCardioExerciseSelected != null) {
                    SwapCardioConfigDialog(
                        exercise = selectedExercise!!,
                        languageCode = languageCode,
                        onConfirm = { durationMinutes, rest ->
                            onCardioExerciseSelected(selectedExercise!!, durationMinutes, rest)
                            onDismiss()
                        },
                        onBack = { step = 1 },
                        onDismiss = onDismiss,
                        isAdding = isAdding
                    )
                } else {
                    selectedExercise?.let { ex ->
                        SwapExerciseConfigSheet(
                            exercise = ex,
                            languageCode = languageCode,
                            initialSets = setsText,
                            initialReps = repsText,
                            initialBlocks = initialBlocks,
                            advancedEnabled = onAdvancedExerciseSelected != null && LocalAdvancedProgramming.current,
                            onConfirm = { sets, reps, rest, exerciseType, durataTargetSec ->
                                onExerciseSelected(ex, sets, reps, rest, exerciseType, durataTargetSec)
                                onDismiss()
                            },
                            onConfirmAdvanced = { blocks, rest ->
                                onAdvancedExerciseSelected?.invoke(ex, blocks, rest)
                                onDismiss()
                            },
                            onBack = { step = 1 },
                            onDismiss = onDismiss,
                            isAdding = isAdding
                        )
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomExerciseDialog(
            categories = allCategories,
            languageCode = languageCode,
            onConfirm = { name, category ->
                onAddCustomExercise(name, category) { createdExercise ->
                    selectedExercise = createdExercise
                    step = 2
                }
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false }
        )
    }

    if (exerciseToEdit != null) {
        EditCustomExerciseDialog(
            exercise = exerciseToEdit!!,
            categories = allCategories,
            languageCode = languageCode,
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
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    languageCode: String,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
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
                    text = ExerciseTranslations.translateCategory(exercise.categoria, languageCode).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
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
@OptIn(ExperimentalMaterial3Api::class)
private fun AddCustomExerciseDialog(
    categories: List<String>,
    languageCode: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Altro") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.new_exercise),
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                GymInputField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = stringResource(R.string.exercise_name),
                    modifier = Modifier.fillMaxWidth()
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
                            value = ExerciseTranslations.translateCategory(selectedCategory, languageCode),
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
                                text = { Text(ExerciseTranslations.translateCategory(category, languageCode)) },
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
    languageCode: String,
    onConfirm: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var exerciseName by remember { mutableStateOf(exercise.nome) }
    var selectedCategory by remember { mutableStateOf(exercise.categoria) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.edit_exercise_title),
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                GymInputField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = stringResource(R.string.exercise_name),
                    modifier = Modifier.fillMaxWidth()
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
                            value = ExerciseTranslations.translateCategory(selectedCategory, languageCode),
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
                                text = { Text(ExerciseTranslations.translateCategory(category, languageCode)) },
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

/**
 * Step 2 of adding / swapping an exercise: sets × reps, timed sets, or an advanced (%1RM) prescription.
 * A bottom sheet rather than a dialog so the advanced block editor has room to breathe.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SwapExerciseConfigSheet(
    exercise: ExerciseEntity,
    languageCode: String,
    initialSets: String,
    initialReps: String,
    initialBlocks: List<PrescriptionBlock>,
    advancedEnabled: Boolean,
    onConfirm: (Int, String, Int?, String, Int?) -> Unit,
    onConfirmAdvanced: (List<PrescriptionBlock>, Int?) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    isAdding: Boolean = false
) {
    var selectedExerciseType by remember {
        mutableStateOf(if (advancedEnabled && initialBlocks.isNotEmpty()) "advanced" else "strength")
    }
    var setsText by remember { mutableStateOf(initialSets) }
    var repsText by remember { mutableStateOf(initialReps) }
    var timeTargetSecondsText by remember { mutableStateOf("45") }
    var restText by remember { mutableStateOf("120") }
    var advancedBlocks by remember { mutableStateOf(mapOf(1 to initialBlocks)) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[UserPreferencesRepository.HAPTIC_ENABLED] ?: true
        }
    }.collectAsState(initial = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.cardPadding, spacing = Spacing.large) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                    Text(
                        text = (if (isAdding) stringResource(R.string.add_exercise) else stringResource(R.string.swap_exercise)).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = ExerciseTranslations.translate(exercise.nome, languageCode),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)
                        ),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    if (!isAdding && initialBlocks.isNotEmpty() && advancedEnabled) {
                        Text(
                            text = stringResource(R.string.keep_prescription_on_swap),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                ExerciseTypeSelector(
                    selectedType = selectedExerciseType,
                    showAdvanced = advancedEnabled,
                    onTypeSelected = { type ->
                        if (type == "advanced" && advancedBlocks[1].isNullOrEmpty()) {
                            // Carry the plain sets × reps over as a first free block
                            val sets = setsText.trim().toIntOrNull() ?: 3
                            val reps = repsText.trim().takeIf { r -> r.split("-").all { it.trim().toIntOrNull() != null } } ?: "5"
                            advancedBlocks = mapOf(1 to listOf(PrescriptionBlock(sets = sets, reps = reps)))
                        }
                        selectedExerciseType = type
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when (selectedExerciseType) {
                    "advanced" -> AdvancedPrescriptionEditor(
                        blocksByWeek = advancedBlocks,
                        onBlocksByWeekChange = { advancedBlocks = it },
                        exerciseId = exercise.id,
                        exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode)
                    )
                    "time_and_weight" -> {
                        GymInputField(
                            value = setsText,
                            onValueChange = { setsText = it },
                            label = stringResource(R.string.sets),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TargetSecondsSlider(
                            valueSeconds = timeTargetSecondsText.toIntOrNull() ?: 45,
                            onValueChange = { timeTargetSecondsText = it.toString() },
                            hapticEnabled = hapticEnabled,
                            haptic = haptic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymInputField(
                            value = setsText,
                            onValueChange = { setsText = it },
                            label = stringResource(R.string.sets),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymInputField(
                            value = repsText,
                            onValueChange = {
                                repsText = it
                                val repCount = it.split("-").count { n -> n.trim().toIntOrNull() != null }
                                if (repCount > 1 && repCount != (setsText.toIntOrNull() ?: 0)) {
                                    setsText = repCount.toString()
                                }
                            },
                            label = stringResource(R.string.reps),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                RestSlider(
                    value = restText.toIntOrNull() ?: 120,
                    onValueChange = { restText = it.toString() },
                    hapticEnabled = hapticEnabled,
                    haptic = haptic,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SheetFormFooter(horizontalPadding = ResponsiveSize.cardPadding) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    GymButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.back).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    GymButton(
                        onClick = {
                            val rest = restText.toIntOrNull()
                            when (selectedExerciseType) {
                                "advanced" -> {
                                    val blocks = advancedBlocks[1].orEmpty()
                                    if (blocks.isEmpty()) {
                                        android.widget.Toast.makeText(context, context.getString(R.string.advanced_needs_block), android.widget.Toast.LENGTH_SHORT).show()
                                        return@GymButton
                                    }
                                    onConfirmAdvanced(blocks, rest)
                                }
                                "time_and_weight" -> {
                                    val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                    val targetSec = timeTargetSecondsText.trim().toIntOrNull() ?: 45
                                    onConfirm(sets, "${targetSec}s", rest, "time_and_weight", targetSec)
                                }
                                else -> {
                                    val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                    val reps = repsText.trim().takeIf { it.isNotBlank() } ?: return@GymButton
                                    onConfirm(sets, reps, rest, "strength", null)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwapCardioConfigDialog(
    exercise: ExerciseEntity,
    languageCode: String,
    onConfirm: (Int, Int?) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    isAdding: Boolean = false
) {
    var durationText by remember { mutableStateOf("20") }
    var restText by remember { mutableStateOf("120") }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[UserPreferencesRepository.HAPTIC_ENABLED] ?: true
        }
    }.collectAsState(initial = true)

    val durationSteps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val durationMinutes = durationText.toIntOrNull() ?: 20
    val closestIndex = remember(durationMinutes) {
        durationSteps.indexOf(durationSteps.minByOrNull { kotlin.math.abs(it - durationMinutes) } ?: 20).coerceAtLeast(0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(
                text = if (isAdding) stringResource(R.string.add_exercise) else stringResource(R.string.swap_exercise),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
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
                    text = stringResource(R.string.cardio_duration_slider),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${durationMinutes} min",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                androidx.compose.material3.Slider(
                    value = closestIndex.toFloat(),
                    onValueChange = { rawValue ->
                        val index = kotlin.math.round(rawValue).toInt()
                        val clampedIndex = index.coerceIn(0, durationSteps.size - 1)
                        if (clampedIndex != closestIndex) {
                            if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            durationText = durationSteps[clampedIndex].toString()
                        }
                    },
                    valueRange = 0f..(durationSteps.size - 1).toFloat(),
                    steps = durationSteps.size - 2,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                RestSlider(
                    value = restText.toIntOrNull() ?: 120,
                    onValueChange = { restText = it.toString() },
                    hapticEnabled = hapticEnabled,
                    haptic = haptic,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = durationText.trim().toIntOrNull() ?: return@TextButton
                    val rest = restText.toIntOrNull()
                    onConfirm(duration, rest)
                }
            ) {
                Text(stringResource(R.string.confirm), color = Primary, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
fun RestSlider(
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
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = formatRestTime(value),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }
        
        androidx.compose.material3.Slider(
            value = currentIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != currentIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun formatRestTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes == 0 -> "${secs}s"
        secs == 0 -> "${minutes}m"
        else -> "${minutes}m ${secs}s"
    }
}

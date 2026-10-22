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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog
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
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    /** Only to choose one: no way to add, edit, delete or open the details of an exercise (e.g. picking what to merge into). */
    pickOnly: Boolean = false,
    /** Replaces the line under the title, e.g. to say what the choice is for. */
    hint: String? = null
) {
    rememberResponsiveSize()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyCustom by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var exerciseMediaToView by remember { mutableStateOf<ExerciseEntity?>(null) }
    var detailExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }

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

    val showThumbnails = rememberShowExerciseThumbnails(exercises)

    val imeVisible = isKeyboardVisible

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

                if (hint != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.large))
                }
            }

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

            AnimatedVisibility(visible = !imeVisible) {
                Column {
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
                        if (!pickOnly) {
                            FilterChip(
                                selected = showOnlyCustom,
                                onClick = { showOnlyCustom = !showOnlyCustom },
                                label = { Text(stringResource(R.string.filter_custom)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = OnPrimary
                                )
                            )
                        }
                        categories.forEach { category ->
                            val translatedCategory = ExerciseTranslations.translateCategory(category, languageCode)
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(translatedCategory) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = OnPrimary
                                )
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
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(filteredExercises) { exercise ->
                    ExercisePickerItem(
                        exercise = exercise,
                        onClick = {
                            onExerciseSelected(exercise)
                            onDismiss()
                        },
                        languageCode = languageCode,
                        showThumbnail = showThumbnails,
                        onThumbnailClick = { exerciseMediaToView = exercise },
                        onInfoClick = if (pickOnly) null else { { detailExerciseId = exercise.id } }
                    )
                }
            }

            AnimatedVisibility(visible = !imeVisible && !pickOnly) {
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

    if (showAddCustomDialog) {
        ExerciseFormDialog(
            title = stringResource(R.string.new_exercise),
            categories = categories,
            existingExercises = exercises,
            languageCode = languageCode,
            showDescription = false,
            onConfirm = { form ->
                onAddCustomExercise(form.name, form.category) { createdExercise ->
                    onExerciseSelected(createdExercise)
                    onDismiss()
                }
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false }
        )
    }

    ExerciseMediaPeek(exerciseMediaToView, languageCode) { exerciseMediaToView = null }
    detailExerciseId?.let { ExerciseDetailDialog(exerciseId = it, onDismiss = { detailExerciseId = null }) }

}

@Composable
private fun ExercisePickerItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    languageCode: String = "en",
    showThumbnail: Boolean = false,
    onThumbnailClick: () -> Unit = {},
    onInfoClick: (() -> Unit)? = null
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
            if (showThumbnail) {
                ExerciseListThumbnail(fileName = exercise.mediaPath, onClick = onThumbnailClick)
                Spacer(modifier = Modifier.width(Spacing.medium))
            }
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
                    text = ExerciseTranslations.translateCategory(exercise.categoria, languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            if (onInfoClick != null) ExerciseInfoButton(onClick = onInfoClick)
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add",
                tint = Primary
            )
        }
    }
}

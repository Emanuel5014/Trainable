package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseForm
import com.emanuel5014.trainable.domain.exercise.ExerciseNames
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * The form to create or edit an exercise, one for every screen that does either. The name is refused while it is that of
 * another exercise of the library ([ExerciseNames.isTaken]); [editingId] is the exercise being edited, which may keep its
 * own name. The category is picked among [categories] (new ones are made in the categories list of the exercises page).
 * [showDescription] is off where the caller has nowhere to keep a description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormDialog(
    title: String,
    categories: List<String>,
    existingExercises: List<ExerciseEntity>,
    languageCode: String,
    onConfirm: (ExerciseForm) -> Unit,
    onDismiss: () -> Unit,
    initial: ExerciseForm = ExerciseForm(),
    editingId: Int? = null,
    showDescription: Boolean = true
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category.ifBlank { categories.firstOrNull() ?: "Custom" }) }
    var description by remember { mutableStateOf(initial.description) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val taken = remember(name, existingExercises, languageCode, editingId) {
        ExerciseNames.isTaken(name, existingExercises, languageCode, editingId)
    }
    val canConfirm = name.isNotBlank() && !taken

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = OnSurface, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                GymInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.exercise_name),
                    supportingText = if (taken) stringResource(R.string.exercise_name_taken) else null,
                    isError = taken,
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
                            value = ExerciseTranslations.translateCategory(category, languageCode),
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
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(ExerciseTranslations.translateCategory(option, languageCode)) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                if (showDescription) {
                    GymInputField(
                        value = description,
                        onValueChange = { description = it },
                        label = stringResource(R.string.exercise_description),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = { if (canConfirm) onConfirm(ExerciseForm(name.trim(), category, description.trim())) },
                enabled = canConfirm
            ) {
                Text(
                    text = stringResource(if (editingId != null) R.string.save else R.string.add).uppercase(),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainerHigh,
        shape = Shapes.extraLarge
    )
}

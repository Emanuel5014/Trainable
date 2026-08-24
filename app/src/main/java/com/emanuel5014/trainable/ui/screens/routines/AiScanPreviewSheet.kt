package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScanPreviewSheet(
    entries: List<ScannedExerciseEntry>,
    exercises: List<ExerciseEntity>,
    categories: List<String>,
    languageCode: String,
    editablePresetExercises: Boolean,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit,
    onConfirm: (List<ScannedExerciseEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableEntries = remember { mutableStateListOf(*entries.toTypedArray()) }
    var pickingIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(bottom = ResponsiveSize.cardPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.ai_scan_preview_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineSmall.fontSize)
                        ),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.ai_scan_preview_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            editableEntries.forEachIndexed { index, entry ->
                ScanEntryCard(
                    entry = entry,
                    index = index,
                    onRemove = { editableEntries.removeAt(index) },
                    onUpdate = { updated -> editableEntries[index] = updated },
                    onChangeExercise = { pickingIndex = index }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                GymButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                }

                GymButton(
                    onClick = { onConfirm(editableEntries.toList()) },
                    enabled = editableEntries.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.ai_scan_add_all).uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (pickingIndex != null) {
        ExercisePickerBottomSheet(
            exercises = exercises,
            categories = categories,
            onExerciseSelected = { exercise ->
                val i = pickingIndex ?: return@ExercisePickerBottomSheet
                val entry = editableEntries.getOrNull(i) ?: return@ExercisePickerBottomSheet
                editableEntries[i] = entry.copy(
                    exerciseId = exercise.id,
                    matchedName = exercise.nome,
                    suggestedCategory = exercise.categoria
                )
                pickingIndex = null
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                onAddCustomExercise(nome, categoria) { created ->
                    onCreated(created)
                }
            },
            onEditCustomExercise = { },
            onDeleteCustomExercise = { },
            onDismiss = { pickingIndex = null },
            languageCode = languageCode,
            editablePresetExercises = editablePresetExercises
        )
    }
}

@Composable
private fun ScanEntryCard(
    entry: ScannedExerciseEntry,
    index: Int,
    onRemove: () -> Unit,
    onUpdate: (ScannedExerciseEntry) -> Unit,
    onChangeExercise: () -> Unit
) {
    var setsText by remember(entry.rawName, index) { mutableStateOf(entry.sets.toString()) }
    var repsText by remember(entry.rawName, index) { mutableStateOf(entry.reps) }
    var restText by remember(entry.rawName, index) { mutableStateOf(entry.restSeconds.toString()) }
    var cardioText by remember(entry.rawName, index) { mutableStateOf((entry.cardioMinutes ?: 20).toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.large)
            .background(SurfaceContainerHigh)
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (entry.exerciseId != null) Primary else Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.exerciseId != null) com.emanuel5014.trainable.ui.theme.OnPrimary else Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).clickable { onChangeExercise() }) {
                Text(
                    text = entry.matchedName ?: entry.rawName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (entry.exerciseId == null) stringResource(R.string.ai_scan_custom_label)
                    else stringResource(R.string.ai_scan_tap_to_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.exerciseId == null) Error else OnSurfaceVariant
                )
            }

            androidx.compose.material3.IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (entry.isCardio) {
            GymInputField(
                value = cardioText,
                onValueChange = { value ->
                    cardioText = value
                    value.toIntOrNull()?.let { onUpdate(entry.copy(cardioMinutes = it)) }
                },
                label = stringResource(R.string.cardio_duration_slider),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                GymInputField(
                    value = setsText,
                    onValueChange = { value ->
                        setsText = value
                        value.toIntOrNull()?.let { onUpdate(entry.copy(sets = it)) }
                    },
                    label = stringResource(R.string.sets),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GymInputField(
                    value = repsText,
                    onValueChange = { value ->
                        repsText = value
                        onUpdate(entry.copy(reps = value))
                    },
                    label = stringResource(R.string.reps),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GymInputField(
            value = restText,
            onValueChange = { value ->
                restText = value
                value.toIntOrNull()?.let { onUpdate(entry.copy(restSeconds = it)) }
            },
            label = stringResource(R.string.rest_seconds),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

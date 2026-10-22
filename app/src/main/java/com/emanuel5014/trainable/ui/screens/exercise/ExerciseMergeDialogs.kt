package com.emanuel5014.trainable.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.dao.MergePreview
import com.emanuel5014.trainable.data.local.dao.MergePreviewKind
import com.emanuel5014.trainable.domain.exercise.ExerciseMergeInfo
import com.emanuel5014.trainable.domain.exercise.ExerciseMergeState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter

/**
 * Asks to merge [loserName] into [winnerName], telling what will move. [preview] is null while it is being counted: the
 * button appears once the user can see what the merge does.
 */
@Composable
fun MergePreviewDialog(
    loserName: String,
    winnerName: String,
    preview: MergePreview?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.exercise_merge_preview_title, loserName, winnerName), fontWeight = FontWeight.ExtraBold)
        },
        text = {
            if (preview == null) {
                CircularProgressIndicator()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(mergePreviewText(preview, loserName))
                    Text(
                        text = stringResource(R.string.exercise_merge_footer, loserName),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (preview != null) {
                GymButton(
                    onClick = onConfirm,
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.exercise_merge_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant) }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

@Composable
private fun mergePreviewText(preview: MergePreview, loserName: String): String {
    if (preview.isEmpty) return stringResource(R.string.exercise_merge_nothing, loserName)
    return preview.counts().map { (kind, count) ->
        val plural = when (kind) {
            MergePreviewKind.SETS -> R.plurals.exercise_merge_n_sets
            MergePreviewKind.WORKOUTS -> R.plurals.exercise_merge_n_workouts
            MergePreviewKind.ROUTINES -> R.plurals.exercise_merge_n_routines
            MergePreviewKind.MAXES -> R.plurals.exercise_merge_n_maxes
            MergePreviewKind.OTHERS -> R.plurals.exercise_merge_n_others
        }
        pluralStringResource(plural, count, count)
    }.joinToString(", ")
}

/**
 * Asks to take an exercise out of the library. A preset, or an exercise something still uses, is only hidden and can be
 * restored; [willBeDeleted] says it will be gone for good, null while that is being found out (no button until then).
 */
@Composable
fun RemoveExerciseDialog(
    name: String,
    willBeDeleted: Boolean?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val title = if (willBeDeleted == true) stringResource(R.string.exercise_action_delete)
            else stringResource(R.string.exercise_manage_hide_title, name)
            Text(title, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            when (willBeDeleted) {
                null -> CircularProgressIndicator()
                true -> Text(stringResource(R.string.exercise_manage_delete_message, name))
                false -> Text(stringResource(R.string.exercise_manage_hide_message))
            }
        },
        confirmButton = {
            if (willBeDeleted != null) {
                GymButton(
                    onClick = onConfirm,
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.height(48.dp)
                ) {
                    val label = if (willBeDeleted) R.string.exercise_action_delete else R.string.exercise_action_hide
                    Text(stringResource(label).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant) }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

/** What was merged into this exercise, or what it was merged into, each with the way back. */
@Composable
fun MergeRows(
    state: ExerciseMergeState,
    languageCode: String,
    onUndo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.asWinner.isEmpty() && state.asLoser == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        state.asWinner.forEach { info ->
            val name = ExerciseTranslations.translate(info.otherName, languageCode)
            MergeRow(
                text = stringResource(R.string.exercise_merged_in_row, name, DateFormatter.format(info.timestamp)),
                info = info,
                onUndo = onUndo
            )
        }
        state.asLoser?.let { info ->
            val name = ExerciseTranslations.translate(info.otherName, languageCode)
            MergeRow(text = stringResource(R.string.exercise_merged_into_row, name), info = info, onUndo = onUndo)
        }
    }
}

@Composable
private fun MergeRow(text: String, info: ExerciseMergeInfo, onUndo: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (info.canUndo) {
            TextButton(onClick = { onUndo(info.mergeId) }) {
                Text(stringResource(R.string.exercise_merge_undo).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

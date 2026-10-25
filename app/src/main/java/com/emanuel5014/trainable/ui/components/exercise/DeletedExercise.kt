package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** Marks an exercise the user deleted from the exercise list while a routine or a workout still has it. */
@Composable
fun DeletedExercisePill(modifier: Modifier = Modifier, containerColor: Color = SurfaceContainerHigh) {
    PrescriptionPill(text = stringResource(R.string.exercise_deleted_badge), modifier = modifier, containerColor = containerColor)
}

/**
 * Offers to put [exerciseNames], exercises deleted from the list that a routine or workout still uses, back into the
 * exercise list. [onSkip] carries on without them, [onDismiss] drops whatever the user was doing.
 */
@Composable
fun RestoreExerciseDialog(
    exerciseNames: List<String>,
    onRestore: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    skipLabel: String = stringResource(R.string.not_now)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deleted_exercise_title), fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                if (exerciseNames.size == 1) stringResource(R.string.deleted_exercise_message, exerciseNames.first())
                else stringResource(R.string.deleted_exercises_message, exerciseNames.joinToString(", "))
            )
        },
        confirmButton = {
            GymButton(
                onClick = onRestore,
                containerColor = Primary,
                contentColor = OnPrimary,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.deleted_exercise_restore).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(skipLabel.uppercase(), color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

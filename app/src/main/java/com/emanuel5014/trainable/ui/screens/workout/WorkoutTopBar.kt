package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.theme.*
import com.emanuel5014.trainable.ui.theme.Error

/** Equal-size actions and a bounded title share the same vertical center. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutTopBar(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    onClose: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit
) {
    val fractions = remember(state.exercises) { ExerciseProgress.fractions(state.exercises) }
    val actionShape = RoundedCornerShape(14.dp)
    Column {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymIconButton(icon = Icons.Rounded.Close, onClick = onClose, shape = actionShape,
                containerColor = Color.Transparent, quiet = true, description = stringResource(R.string.close_workout))
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) {
                    if (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) ui.showRenameDialog = true
                    else state.planId?.let(onNavigateToRoutine)
                },
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.planName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                    color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.session_in_progress_title), style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.totalExercises > 0 && state.warmupTimerEnabled) {
                    GymIconButton(icon = Icons.Rounded.Timer, onClick = {
                        ui.warmupTimerDuration = state.lastWarmupSeconds
                        ui.showWarmupTimerDialog = true
                    }, shape = actionShape, quiet = true,
                        contentColor = if (state.warmupTimerRemaining > 0) Primary else OnSurface,
                        description = stringResource(R.string.warmup_timer))
                }
                GymIconButton(icon = Icons.Rounded.Delete, onClick = { ui.showCancelDialog = true },
                    shape = actionShape, quiet = true, contentColor = Error,
                    description = stringResource(R.string.cancel_workout))
            }
        }
        ExerciseProgressStrip(fractions = fractions, currentIndex = state.currentExerciseIndex, completedCount = state.completedExercises)
    }
}

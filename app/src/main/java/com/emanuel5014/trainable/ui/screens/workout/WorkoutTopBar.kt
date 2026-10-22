package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymIconButton
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface

/** The top bar of the workout: close, plan name, progress, warm-up timer and cancel. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WorkoutTopBar(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    onClose: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit
) {
    TopAppBar(
        title = { 
            Column {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { 
                        if (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) {
                                ui.showRenameDialog = true
                            } else {
                                state.planId?.let { onNavigateToRoutine(it) }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.planName, 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = stringResource(R.string.session_in_progress_title), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        navigationIcon = {
            GymIconButton(
                icon = Icons.Rounded.Close,
                onClick = onClose,
                containerColor = Color.Transparent,
                description = stringResource(R.string.close_workout)
            )
        },
        actions = {
            if (state.totalExercises > 0) {
                Surface(
                    color = Primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${state.completedExercises}/${state.totalExercises}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                if (state.warmupTimerEnabled) {
                    IconButton(
                        onClick = {
                            ui.warmupTimerDuration = 120
                            ui.showWarmupTimerDialog = true
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = stringResource(R.string.warmup_timer),
                            tint = if (state.warmupTimerRemaining > 0) Primary else OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Surface(
                onClick = { ui.showCancelDialog = true },
                shape = CircleShape,
                color = Error.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cancel_workout),
                    tint = Error,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface,
            titleContentColor = OnSurface,
            navigationIconContentColor = OnSurface
        )
    )
}

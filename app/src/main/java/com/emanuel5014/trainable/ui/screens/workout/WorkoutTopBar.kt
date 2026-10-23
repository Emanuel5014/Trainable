package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.remember
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
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
    val fractions = remember(state.exercises) { ExerciseProgress.fractions(state.exercises) }
    Column {
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
            // Timer and cancel are one connected pair, like reset and edit on the routine screen
            val showWarmupTimer = state.totalExercises > 0 && state.warmupTimerEnabled
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                if (showWarmupTimer) {
                    GymIconButton(
                        icon = Icons.Rounded.Timer,
                        onClick = {
                            ui.warmupTimerDuration = 120
                            ui.showWarmupTimerDialog = true
                        },
                        containerColor = SecondaryContainer,
                        contentColor = if (state.warmupTimerRemaining > 0) Primary else OnSecondaryContainer,
                        description = stringResource(R.string.warmup_timer),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                }
                GymIconButton(
                    icon = Icons.Rounded.Delete,
                    onClick = { ui.showCancelDialog = true },
                    containerColor = SecondaryContainer,
                    contentColor = Error,
                    description = stringResource(R.string.cancel_workout),
                    shape = if (showWarmupTimer) {
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                    } else {
                        RoundedCornerShape(16.dp)
                    }
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
    ExerciseProgressStrip(
        fractions = fractions,
        currentIndex = state.currentExerciseIndex,
        completedCount = state.completedExercises,
        waving = state.remainingRestSeconds > 0
    )
    }
}

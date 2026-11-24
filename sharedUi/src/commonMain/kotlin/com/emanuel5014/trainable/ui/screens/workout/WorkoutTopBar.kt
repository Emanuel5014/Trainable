package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.ui.screens.history.displayText
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.collectAsState
import com.emanuel5014.trainable.ui.components.rememberWorkoutElapsed
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.sharedui.resources.open_routine
import com.emanuel5014.trainable.sharedui.resources.deload_workout
import com.emanuel5014.trainable.sharedui.resources.deload_workout_desc
import com.emanuel5014.trainable.sharedui.resources.deload_weights_at
import com.emanuel5014.trainable.ui.components.deload.DeloadBadge
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.emanuel5014.trainable.sharedui.resources.rename_workout
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel_workout
import com.emanuel5014.trainable.sharedui.resources.close_workout
import com.emanuel5014.trainable.sharedui.resources.session_in_progress_title
import com.emanuel5014.trainable.sharedui.resources.warmup_timer
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.glass.headerGlassOn
import com.emanuel5014.trainable.ui.glass.headerGroupGlass

/** The top bar of the workout: close, plan name, progress, warm-up timer and cancel. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutTopBar(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    onClose: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit,
    onSetDeload: (Boolean) -> Unit = {}
) {
    val fractions = remember(state.exercises) { ExerciseProgress.fractions(state.exercises) }
    Column {
    TopAppBar(
        title = { 
            Column {
                // The title opens a menu: rename (quick workouts, or with inline changes on), open the routine, and
                // mark the workout as a deload
                var showTitleMenu by remember { mutableStateOf(false) }
                val routineId = state.planId?.takeUnless { state.isQuickWorkout }
                val canRename = state.isQuickWorkout || state.inlineExerciseModificationsEnabled
                Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showTitleMenu = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.title.displayText(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (state.isDeload) DeloadBadge(showIcon = false)
                }
                DropdownMenu(expanded = showTitleMenu, onDismissRequest = { showTitleMenu = false }) {
                    LocalPlatformGlass.current.NotAnOverlay()
                    if (canRename) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.rename_workout)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                showTitleMenu = false
                                ui.newSessionName = state.editableName
                                ui.showRenameDialog = true
                            }
                        )
                    }
                    if (routineId != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.open_routine)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null) },
                            onClick = {
                                showTitleMenu = false
                                onNavigateToRoutine(routineId)
                            }
                        )
                    }
                    if (canRename || routineId != null) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // A deload workout stays in the history, out of stats, records and the next workout's values
                    DropdownMenuItem(
                        text = {
                            Column(
                                modifier = Modifier.widthIn(max = 220.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(stringResource(Res.string.deload_workout))
                                Text(
                                    stringResource(Res.string.deload_workout_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.TrendingDown, contentDescription = null) },
                        trailingIcon = {
                            SettingsSwitch(checked = state.isDeload, onCheckedChange = { onSetDeload(it) })
                        },
                        onClick = { onSetDeload(!state.isDeload) },
                        modifier = Modifier.semantics { toggleableState = ToggleableState(state.isDeload) }
                    )
                }
                }
                val startedAt = state.sessionStartTime
                if (state.isDeload) {
                    DeloadSubtitle(state.deloadPercent, if (state.showWorkoutElapsed) startedAt else null)
                } else if (state.showWorkoutElapsed && startedAt != null) {
                    SessionElapsedLabel(startedAt)
                } else {
                    Text(
                        text = stringResource(Res.string.session_in_progress_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        navigationIcon = {
            GymIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onClose,
                containerColor = Color.Transparent,
                description = stringResource(Res.string.close_workout)
            )
        },
        actions = {
            // Timer and cancel are one connected pair, like reset and edit on the routine screen
            val showWarmupTimer = state.totalExercises > 0 && state.warmupTimerEnabled
            if (headerGlassOn()) {
                // On glass the pair is one capsule; a lone cancel button is a glass circle of its own
                val cancel: @Composable (Color) -> Unit = { container ->
                    GymIconButton(
                        icon = Icons.Rounded.Delete,
                        onClick = { ui.showCancelDialog = true },
                        containerColor = container,
                        contentColor = Error,
                        description = stringResource(Res.string.cancel_workout)
                    )
                }
                if (showWarmupTimer) {
                    Row(modifier = Modifier.headerGroupGlass()) {
                        GymIconButton(
                            icon = Icons.Rounded.Timer,
                            onClick = {
                                ui.warmupTimerDuration = state.lastWarmupSeconds
                                ui.showWarmupTimerDialog = true
                            },
                            containerColor = Color.Transparent,
                            contentColor = if (state.warmupTimerRemaining > 0) Primary else OnSurface,
                            description = stringResource(Res.string.warmup_timer)
                        )
                        cancel(Color.Transparent)
                    }
                } else {
                    cancel(SurfaceContainerHigh)
                }
            } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                if (showWarmupTimer) {
                    GymIconButton(
                        icon = Icons.Rounded.Timer,
                        onClick = {
                            ui.warmupTimerDuration = state.lastWarmupSeconds
                            ui.showWarmupTimerDialog = true
                        },
                        containerColor = SecondaryContainer,
                        contentColor = if (state.warmupTimerRemaining > 0) Primary else OnSecondaryContainer,
                        description = stringResource(Res.string.warmup_timer),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                }
                GymIconButton(
                    icon = Icons.Rounded.Delete,
                    onClick = { ui.showCancelDialog = true },
                    containerColor = SecondaryContainer,
                    contentColor = Error,
                    description = stringResource(Res.string.cancel_workout),
                    shape = if (showWarmupTimer) {
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                    } else {
                        RoundedCornerShape(16.dp)
                    }
                )
            }
            }
            // The bar keeps 4 dp at its edge: with these 16 the buttons end 20 dp from the screen, like the exercise tools
            Spacer(modifier = Modifier.width(16.dp))
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
        completedCount = state.completedExercises
    )
    }
}

/** "WEIGHTS AT 60%", with the clock of the workout after it when that is on ("WEIGHTS AT 60% · 0:42:07"). */
@Composable
private fun DeloadSubtitle(percent: Int, startedAt: Long?) {
    val label = stringResource(Res.string.deload_weights_at, "$percent%")
    if (startedAt == null) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSecondaryContainer,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        return
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val resumed by lifecycle.currentStateFlow.collectAsState()
    val elapsed = rememberWorkoutElapsed(startedAt, isActive = resumed.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
    Text(
        text = "$label · $elapsed",
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = OnSecondaryContainer,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

/**
 * "SESSION IN PROGRESS · 0:42:07": the clock of the whole workout (Workout settings), alone in its composable so only
 * this line redraws every second.
 */
@Composable
private fun SessionElapsedLabel(startedAt: Long) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val resumed by lifecycle.currentStateFlow.collectAsState()
    val elapsed = rememberWorkoutElapsed(startedAt, isActive = resumed.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
    Text(
        text = stringResource(Res.string.session_in_progress_title) + " · " + elapsed,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = OnSurfaceVariant,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

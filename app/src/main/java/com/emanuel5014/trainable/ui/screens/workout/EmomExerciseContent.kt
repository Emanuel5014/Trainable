package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.domain.emom.EmomClock
import com.emanuel5014.trainable.domain.emom.EmomPhase
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.WeightRepsInput
import com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels
import com.emanuel5014.trainable.ui.components.techniqueLabel
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * The execution screen of an EMOM exercise: a minute clock built on the timer of the cardio and
 * timed sets, showing the set to do now, the one coming next, and a countdown that gets the
 * lifter ready for the top of every minute.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmomExerciseContent(
    exerciseIndex: Int,
    exerciseState: WorkoutExerciseState,
    languageCode: String,
    state: WorkoutState,
    viewModel: WorkoutViewModel,
    onSwap: () -> Unit,
    onLogManually: () -> Unit,
    bottomPadding: Dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prescriptionLabels = rememberPrescriptionLabels(state.weightUnit)

    val run = state.emomRun?.takeIf { it.exerciseIndex == exerciseIndex }
    val sets = exerciseState.sets
    val startIndex = run?.startSetIndex ?: exerciseState.emomStartIndex ?: 0
    val roundCount = run?.roundCount ?: EmomClock.runLength(sets.map { it.isEmom }, startIndex)
    val roundsLogged = if (run != null) {
        (startIndex until startIndex + roundCount).count { sets.getOrNull(it)?.isCompleted == true }
    } else 0

    val isRunning = run != null && state.setTimerRunning
    val isPaused = run != null && state.setTimerPaused
    val elapsed = state.setTimerSeconds
    val snapshot = EmomClock.snapshot(elapsed, started = run != null, roundsLogged = roundsLogged, totalRounds = roundCount)

    // The set that matters now: the round on the clock, or the one coming up while resting.
    val focusIndex = when (snapshot.phase) {
        EmomPhase.Work -> startIndex + snapshot.round - 1
        EmomPhase.Rest -> startIndex + snapshot.round
        else -> startIndex
    }
    val runEnd = startIndex + roundCount
    val focusSet = sets.getOrNull(focusIndex)?.takeIf { focusIndex < runEnd }
    val thenSet = sets.getOrNull(focusIndex + 1)?.takeIf { focusIndex + 1 < runEnd }

    val accent = if (snapshot.phase == EmomPhase.Work || snapshot.phase == EmomPhase.Idle) Primary else Tertiary
    val onAccent = if (accent == Primary) OnPrimary else OnTertiary
    val inCountdown = isRunning && EmomClock.isCountdownTick(elapsed)
    val clockColor = when {
        inCountdown -> Error
        isPaused -> OnSurfaceVariant
        snapshot.phase == EmomPhase.Idle -> OnSurface
        else -> accent
    }

    // The ring creeps forward between the one-second ticks instead of jumping.
    val ringProgress = remember { Animatable(0f) }
    LaunchedEffect(elapsed, isRunning, snapshot.phase) {
        ringProgress.snapTo(snapshot.progress)
        if (isRunning) {
            val step = if (snapshot.phase == EmomPhase.LeadIn) 1f / EmomClock.LEAD_IN_SECONDS else 1f / EmomClock.MINUTE_SECONDS
            ringProgress.animateTo((snapshot.progress + step).coerceAtMost(1f), tween(durationMillis = 1000, easing = LinearEasing))
        }
    }

    // Each of the last seconds before a round pulses the countdown.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(elapsed, inCountdown) {
        if (inCountdown) {
            pulse.snapTo(1.18f)
            pulse.animateTo(1f, tween(durationMillis = 600))
        } else {
            pulse.snapTo(1f)
        }
    }

    var editIndex by remember { mutableStateOf<Int?>(null) }
    editIndex?.let { index ->
        val set = sets.getOrNull(index)
        if (set == null || set.isCompleted) {
            editIndex = null
        } else {
            AlertDialog(
                onDismissRequest = { editIndex = null },
                title = { Text(stringResource(R.string.adjust_set_number, set.setNumber)) },
                text = {
                    WeightRepsInput(
                        weight = set.weight,
                        reps = set.reps,
                        onWeightChange = { viewModel.updateSetWeight(exerciseIndex, index, it) },
                        onRepsChange = { viewModel.updateSetReps(exerciseIndex, index, it) },
                        weightUnit = state.weightUnit
                    )
                },
                confirmButton = {
                    TextButton(onClick = { editIndex = null }) {
                        Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                containerColor = SurfaceContainerHigh,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ResponsiveSize.horizontalPadding)
            .padding(top = 12.dp, bottom = bottomPadding)
    ) {
        // The fixed parts of the screen take about 340dp; the ring and clock share the rest.
        val scale = minOf(maxWidth / 400.dp, (maxHeight - 340.dp) / 236.dp).coerceIn(0.6f, 1.1f)
        val ringSize = (180f * scale).dp
        val buttonSize = (120f * scale).dp
        val clockFontSize = (46f * scale).sp
        val roomForHint = maxHeight >= 560.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Tertiary.copy(alpha = 0.15f), shape = CircleShape) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.technique_emom),
                                style = MaterialTheme.typography.titleMedium,
                                color = Tertiary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    if (roundCount > 0) {
                        Surface(color = Primary.copy(alpha = 0.12f), shape = CircleShape) {
                            Text(
                                text = stringResource(
                                    R.string.emom_round_progress,
                                    (focusIndex - startIndex + 1).coerceIn(1, roundCount),
                                    roundCount
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                // The exercise cannot be swapped while the clock is on it.
                if (run == null) {
                    IconButton(onClick = onSwap) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.swap_exercise),
                            tint = OnSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            Text(
                text = ExerciseTranslations.translate(exerciseState.exercise.nome, languageCode),
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (focusSet != null) {
                val maxLabel = stringResource(R.string.max_label)
                val prescription = focusSet.prescription
                // A fixed weight is already in the load line; only %1RM and RPE add information.
                val detail = listOfNotNull(
                    prescription
                        ?.takeIf { it.intensityType == IntensityType.PERCENT || it.intensityType == IntensityType.RPE }
                        ?.let { PrescriptionFormatter.intensity(it.intensityType, it.intensityValue, prescriptionLabels) }
                ) +prescription?.techniques.orEmpty().filter { it != Technique.Emom }.map { techniqueLabel(context, it) } +
                    listOfNotNull(if (focusSet.isExtra) stringResource(R.string.extra_badge) else null)

                Surface(
                    color = SurfaceContainerHigh,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    when (snapshot.phase) {
                                        EmomPhase.Work -> R.string.emom_label_now
                                        EmomPhase.Rest -> R.string.emom_label_next
                                        else -> R.string.emom_label_first
                                    },
                                    focusSet.setNumber
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = emomSetLoad(focusSet, state.weightUnit, maxLabel),
                                style = MaterialTheme.typography.headlineMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (detail.isNotEmpty()) {
                                Text(
                                    text = detail.joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (thenSet != null) {
                                Text(
                                    text = stringResource(R.string.emom_then, emomSetLoad(thenSet, state.weightUnit, maxLabel)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { editIndex = focusIndex }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.adjust_set_number, focusSet.setNumber),
                                tint = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
                CircularWavyProgressIndicator(
                    progress = { ringProgress.value },
                    modifier = Modifier.size(ringSize),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.2f),
                    stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                    trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                    gapSize = (10f * scale).dp,
                    wavelength = (34f * scale).dp,
                    amplitude = { progress ->
                        if (!isRunning) 0f
                        else if (progress <= 0.1f) 0f
                        else 1f
                    }
                )
                CardioToggleButton(
                    isRunning = isRunning,
                    isPaused = isPaused,
                    onToggle = {
                        if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRunning) viewModel.pauseEmom() else viewModel.startEmom()
                    },
                    modifier = Modifier.size(buttonSize),
                    activeColor = accent,
                    onActiveColor = onAccent
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val left = snapshot.secondsLeft
                Text(
                    text = String.format("%d:%02d", left / 60, left % 60),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = clockFontSize),
                    fontWeight = FontWeight.Black,
                    color = clockColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulse.value
                        scaleY = pulse.value
                    }
                )
                Text(
                    text = stringResource(
                        when {
                            isPaused -> R.string.emom_status_paused
                            snapshot.phase == EmomPhase.Idle -> R.string.emom_status_ready
                            snapshot.phase == EmomPhase.LeadIn -> R.string.emom_status_get_ready
                            snapshot.phase == EmomPhase.Rest -> R.string.emom_status_rest
                            EmomClock.isGoFlash(elapsed) -> R.string.emom_status_go
                            else -> R.string.emom_status_work
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = clockColor,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            if (roundCount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until roundCount) {
                        val setIndex = startIndex + i
                        val barColor = when {
                            sets.getOrNull(setIndex)?.isCompleted == true -> Primary
                            run != null && setIndex == focusIndex -> accent.copy(alpha = 0.55f)
                            else -> SurfaceContainerHighest
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(barColor)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (roomForHint) 96.dp else 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    isPaused -> GymButton(
                        onClick = { viewModel.stopEmom() },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        height = 52,
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.emom_stop), fontWeight = FontWeight.ExtraBold)
                    }
                    isRunning && snapshot.phase == EmomPhase.Work -> GymButton(
                        onClick = { viewModel.completeEmomRound() },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        height = 52
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.emom_done),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    run == null -> TextButton(onClick = onLogManually) {
                        Text(
                            text = stringResource(R.string.emom_log_manually),
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (roomForHint) {
                    val hint = when {
                        isRunning && snapshot.phase == EmomPhase.Work -> stringResource(R.string.emom_hint_work)
                        run == null -> stringResource(R.string.emom_hint_idle, EmomClock.LEAD_IN_SECONDS)
                        else -> null
                    }
                    hint?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun emomSetLoad(set: WorkoutSetState, weightUnit: String, maxLabel: String): String {
    val weight = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(set.weight, weightUnit), weightUnit)
    return "$weight × ${if (set.isAmrap) maxLabel else set.reps.toString()}"
}

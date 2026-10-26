package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.toPath
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.ui.components.RpeSelector
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.cardioKmText
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary

// The parts of the cardio screen, laid out by CardioFitLayout. Each one takes the density it was picked for.

/** The bar on top: the badge, the buttons for the note and for swapping, and on a short screen the exercise name too. */
@Composable
internal fun CardioHeader(
    fit: CardioFit,
    name: String,
    noteFilled: Boolean,
    onNote: () -> Unit,
    onSwap: () -> Unit
) {
    val nameInBar = fit.density.nameInBar
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Tertiary.copy(alpha = 0.15f), shape = CircleShape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = if (nameInBar) 8.dp else 12.dp,
                    vertical = if (nameInBar) 8.dp else 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                    contentDescription = if (nameInBar) stringResource(R.string.cardio_badge) else null,
                    tint = Tertiary,
                    modifier = Modifier.size(if (nameInBar) 20.dp else 16.dp)
                )
                if (!nameInBar) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.cardio_badge),
                        style = MaterialTheme.typography.titleMedium,
                        color = Tertiary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        if (nameInBar) {
            // One line that shrinks to the room between the badge and the buttons: a second line would cost a whole row
            val nameStyle = MaterialTheme.typography.titleLarge
            Text(
                text = name,
                style = nameStyle,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = nameStyle.fontSize),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        IconButton(onClick = onNote) {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = stringResource(R.string.cardio_set_note),
                tint = if (noteFilled) Primary else OnSurfaceVariant
            )
        }
        IconButton(onClick = onSwap) {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = stringResource(R.string.swap_exercise),
                tint = OnSurfaceVariant
            )
        }
    }
}

/** Under the bar: the name, the targets, the set chips, the set's note, and how hard the set just done felt. */
@Composable
internal fun CardioTopBlock(
    fit: CardioFit,
    exerciseState: WorkoutExerciseState,
    name: String,
    noteSetIndex: Int,
    justDoneIndex: Int,
    rpeScale: RpeScale,
    hapticEnabled: Boolean,
    onSetClick: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRpeChange: (Float?) -> Unit
) {
    val density = fit.density
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(density.gap)
    ) {
        if (!density.nameInBar) {
            Text(
                text = name,
                style = if (density == CardioDensity.Regular) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val targetMin = exerciseState.cardioDurataTargetSeconds?.let { it / 60 }
        val targetKm = exerciseState.cardioDistanzaTargetKm
        if (!exerciseState.isCardioCompleted && (targetMin != null || targetKm != null)) {
            Surface(color = SurfaceContainerHigh, shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = when (density) {
                            CardioDensity.Regular -> 8.dp
                            CardioDensity.Compact -> 6.dp
                            CardioDensity.Tight -> 4.dp
                        }
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (targetMin != null && targetMin > 0) {
                        Text(
                            text = stringResource(R.string.cardio_target_min_format, targetMin),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (targetMin != null && targetKm != null && targetMin > 0 && targetKm > 0) {
                        Text(text = "•", color = OnSurfaceVariant.copy(alpha = 0.5f))
                    }
                    if (targetKm != null && targetKm > 0) {
                        Text(
                            text = stringResource(R.string.cardio_target_km_format, targetKm.toString()),
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        CardioSetsStrip(
            sets = exerciseState.cardioSets,
            activeIndex = exerciseState.activeCardioSetIndex,
            onSetClick = onSetClick,
            onAdd = onAddSet
        )
        exerciseState.cardioSets.getOrNull(noteSetIndex)?.note?.takeIf { it.isNotBlank() }?.let { note ->
            CardioSetNoteLine(note = note, onClick = { onSetClick(noteSetIndex) })
        }

        val justDone = exerciseState.cardioSets.getOrNull(justDoneIndex)
        if (justDone != null) {
            when (fit.rpe) {
                CardioRpe.Prompt -> Surface(onClick = { onSetClick(justDoneIndex) }, shape = CircleShape, color = SurfaceContainerHigh) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.set_number, justDone.setNumber) + " · " + stringResource(R.string.rpe_prompt),
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                CardioRpe.Row -> Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.set_number, justDone.setNumber),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    RpeSelector(
                        value = justDone.rpe,
                        onValueChange = onRpeChange,
                        scale = rpeScale,
                        hapticEnabled = hapticEnabled
                    )
                }
                CardioRpe.None -> Unit
            }
        }
    }
}

/** Where a finished cardio exercise stands against its targets (several sets add up, and each one has to reach them). */
internal class CardioGoal(exerciseState: WorkoutExerciseState) {
    val elapsed = exerciseState.cardioTotalSeconds
    val totalKm = exerciseState.cardioTotalKm
    val durationMet = exerciseState.cardioDurataTargetSeconds?.let { target ->
        exerciseState.cardioSets.all { it.elapsedSeconds >= target }
    } ?: false
    val distanceMet = exerciseState.cardioDistanzaTargetKm?.let { target ->
        exerciseState.cardioSets.all { it.distanceKm >= target }
    } ?: false
    val achieved = durationMet || distanceMet
    val progress = exerciseState.cardioDurataTargetSeconds?.let { target ->
        (elapsed.toFloat() / (target.toFloat() * exerciseState.cardioSets.size)).coerceIn(0f, 1f)
    } ?: 1f
}

/**
 * The circle, [size] wide. While the exercise is on, it is the timer ring with the play / pause button; once it is
 * finished, the ring shows how the targets went around the cookie. Every part follows [size] (it is drawn at 200 dp).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CardioCircle(
    size: Dp,
    state: WorkoutState,
    exerciseState: WorkoutExerciseState,
    onToggle: () -> Unit
) {
    val k = size / 200.dp
    if (!exerciseState.isCardioCompleted) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            val cardioProgress = remember(state.cardioTimerSeconds, exerciseState.cardioDurataTargetSeconds) {
                val target = exerciseState.cardioDurataTargetSeconds
                if (target != null && target > 0) {
                    (state.cardioTimerSeconds.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                } else 0f
            }
            CircularWavyProgressIndicator(
                progress = { cardioProgress },
                modifier = Modifier.size(size),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.2f),
                stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                gapSize = 10.dp * k,
                wavelength = 36.dp * k,
                amplitude = { progress ->
                    if (!state.cardioTimerRunning) 0f
                    else if (progress <= 0.1f) 0f
                    else 1f
                }
            )
            CardioToggleButton(
                isRunning = state.cardioTimerRunning,
                isPaused = state.cardioTimerPaused,
                onToggle = onToggle,
                modifier = Modifier.size(135.dp * k)
            )
        }
    } else {
        val goal = CardioGoal(exerciseState)
        val color = if (goal.achieved) Primary else Error
        val cookieRotation = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                cookieRotation.animateTo(targetValue = 360f, animationSpec = tween(durationMillis = 20000, easing = LinearEasing))
                cookieRotation.snapTo(0f)
            }
        }
        val cookieShape = remember {
            object : Shape {
                override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                    val path = MaterialShapes.Cookie6Sided.toPath().asComposePath()
                    val matrix = Matrix()
                    matrix.scale(size.width, size.height)
                    path.transform(matrix)
                    path.translate(size.center - path.getBounds().center)
                    return Outline.Generic(path)
                }
            }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            CircularWavyProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.size(size),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                gapSize = 10.dp * k,
                wavelength = 36.dp * k
            )
            Box(
                modifier = Modifier
                    .size(130.dp * k)
                    .graphicsLayer { rotationZ = cookieRotation.value }
                    .background(color = color, shape = cookieShape)
                    .clip(cookieShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (goal.achieved) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbDown,
                    contentDescription = null,
                    tint = Surface,
                    modifier = Modifier
                        .size(50.dp * k)
                        .graphicsLayer { rotationZ = -cookieRotation.value }
                )
            }
        }
    }
}

/**
 * Under the circle: the time with its button to mark it done or to stop it, or, once finished, the summary. The row of
 * buttons keeps its room whatever the timer does, so the circle and the time do not jump when it starts or pauses.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardioBottomBlock(
    fit: CardioFit,
    state: WorkoutState,
    exerciseState: WorkoutExerciseState,
    onMarkCompleted: () -> Unit,
    onStop: () -> Unit
) {
    val density = fit.density
    if (!exerciseState.isCardioCompleted) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(density.gap)
        ) {
            Text(
                text = cardioTimeText(state.cardioTimerSeconds),
                // The line height follows the size, or the text keeps taking the room of the big one
                style = MaterialTheme.typography.displayLarge.copy(fontSize = density.timerSize, lineHeight = density.timerSize * 1.15f),
                fontWeight = FontWeight.Black,
                color = if (state.cardioTimerRunning) Primary else OnSurface,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = density.timerSize)
            )
            if (density.showStatus) {
                Text(
                    text = when {
                        state.cardioTimerRunning -> stringResource(R.string.cardio_timer_running)
                        state.cardioTimerPaused -> stringResource(R.string.cardio_timer_paused)
                        else -> stringResource(R.string.cardio_timer_ready)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.cardioTimerRunning) Primary else OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }
            CardioActions(density, state, onMarkCompleted, onStop)
        }
    } else {
        val goal = CardioGoal(exerciseState)
        val padding = when (density) {
            CardioDensity.Regular -> 20.dp
            CardioDensity.Compact -> 14.dp
            CardioDensity.Tight -> 8.dp
        }
        val spacing = when (density) {
            CardioDensity.Regular -> 16.dp
            CardioDensity.Compact -> 10.dp
            CardioDensity.Tight -> 6.dp
        }
        val totalStyle = when (density) {
            CardioDensity.Regular -> MaterialTheme.typography.displayMedium
            CardioDensity.Compact -> MaterialTheme.typography.headlineLarge
            CardioDensity.Tight -> MaterialTheme.typography.headlineMedium
        }
        Surface(color = Surface, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time and distance on one line, which shrinks to the width of the screen instead of breaking
                val bulletColor = OnSurfaceVariant
                val total = buildAnnotatedString {
                    append(cardioTimeText(goal.elapsed))
                    if (goal.totalKm > 0f) {
                        withStyle(SpanStyle(color = bulletColor)) { append("  •  ") }
                        append(stringResource(R.string.cardio_dist_value, cardioKmText(goal.totalKm)))
                    }
                }
                Text(
                    text = total,
                    style = totalStyle,
                    color = OnSurface,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = totalStyle.fontSize)
                )
                // The targets wrap onto a second line on a narrow screen
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    val durationTarget = exerciseState.cardioDurataTargetSeconds
                    if (durationTarget != null) {
                        val tint = if (goal.durationMet) Primary else Error
                        Surface(color = tint.copy(alpha = 0.15f), shape = CircleShape) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cardio_target_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tint,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = String.format("%d:%02d", durationTarget / 60, durationTarget % 60),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tint,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    val distanceTarget = exerciseState.cardioDistanzaTargetKm
                    if (goal.totalKm > 0f && distanceTarget != null) {
                        val tint = if (goal.distanceMet) Primary else Error
                        Surface(color = tint.copy(alpha = 0.15f), shape = CircleShape) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (goal.distanceMet) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.cardio_dist_value, distanceTarget.toString()),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = tint,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The button to mark the exercise done (idle timer) or to stop and save it (paused); none while it runs. */
@Composable
private fun CardioActions(
    density: CardioDensity,
    state: WorkoutState,
    onMarkCompleted: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(density.actionHeight.dp + 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Done without the timer: only while it is idle (a running or paused timer has its own stop and save)
        AnimatedVisibility(
            visible = !state.cardioTimerRunning && !state.cardioTimerPaused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GymButton(
                onClick = onMarkCompleted,
                modifier = Modifier.widthIn(min = 220.dp),
                height = density.actionHeight,
                containerColor = Tertiary.copy(alpha = 0.15f),
                contentColor = Tertiary
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cardio_mark_completed), fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AnimatedVisibility(visible = state.cardioTimerPaused, enter = fadeIn(), exit = fadeOut()) {
            GymButton(
                onClick = onStop,
                modifier = Modifier.widthIn(min = 200.dp),
                height = density.actionHeight,
                containerColor = Error.copy(alpha = 0.15f),
                contentColor = Error
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cardio_stop_save), fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_all_done
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_continue
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_delete
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_emom_hint
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_emom_round
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_every_minute
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_exercise_of
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_exercises_header
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_hours
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_in_progress
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_minutes
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_more_exercises
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_next_exercise
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_now_set
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_of_total
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_open_workout
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_paused
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_rest
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_resume_emom
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_sets_left
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_skip_rest
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_then
import com.emanuel5014.trainable.sharedui.resources.dashboard_live_warmup
import com.emanuel5014.trainable.sharedui.resources.next_set_done
import com.emanuel5014.trainable.sharedui.resources.resume_workout
import com.emanuel5014.trainable.sharedui.resources.routine_sets_count
import com.emanuel5014.trainable.ui.screens.onboarding.rememberReduceMotion
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.delay
import com.emanuel5014.trainable.ui.components.rememberWorkoutElapsed
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * The clock the live card reads: every second while something counts and the dashboard is on screen, every half
 * minute otherwise (the elapsed minutes), never while off screen.
 */
@Composable
internal fun rememberLiveNow(isActive: Boolean, counting: Boolean): Long {
    var now by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(isActive, counting) {
        now = nowMillis()
        while (isActive) {
            val step = if (counting) 1000L - now % 1000L else 30_000L
            delay(step)
            now = nowMillis()
        }
    }
    return now
}

/** True while the card shows a number that moves every second. */
internal val LiveWorkoutUi.counts: Boolean
    get() = phase == LiveWorkoutUi.Phase.REST || phase == LiveWorkoutUi.Phase.WARMUP ||
        ((phase == LiveWorkoutUi.Phase.SET_TIMER || phase == LiveWorkoutUi.Phase.CARDIO) && !paused)

private fun clock(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = (s % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$sec" else "$m:$sec"
}

@Composable
private fun elapsedText(minutes: Int): String =
    if (minutes < 60) stringResource(Res.string.dashboard_live_minutes, minutes.toString())
    else stringResource(Res.string.dashboard_live_hours, (minutes / 60).toString(), (minutes % 60).toString().padStart(2, '0'))

/** "50 kg × 8", or the whole value of a timed or cardio set. */
fun LiveNextSet.loadText(): String = text ?: run {
    val weightText = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(weight, weightUnit), weightUnit)
    "$weightText × ${repsLabel ?: reps}"
}

private val LiveNextSet.icon: ImageVector
    get() = when (kind) {
        SetKind.CARDIO -> Icons.AutoMirrored.Rounded.DirectionsRun
        SetKind.TIMED, SetKind.EMOM -> Icons.Rounded.Timer
        SetKind.STRENGTH -> Icons.Rounded.FitnessCenter
    }

@Composable
private fun phaseLabel(ui: LiveWorkoutUi): String = stringResource(
    if (ui.phase == LiveWorkoutUi.Phase.WARMUP) Res.string.dashboard_live_warmup else Res.string.dashboard_live_rest
).uppercase()

@Composable
private fun nowSetLabel(next: LiveNextSet): String =
    if (next.kind == SetKind.EMOM && next.emomRound != null && next.emomRounds != null) emomLabel(next)
    else stringResource(Res.string.dashboard_live_now_set, next.setNumber.toString(), next.setsInExercise.toString()).uppercase()

@Composable
private fun emomLabel(next: LiveNextSet): String =
    stringResource(Res.string.dashboard_live_emom_round, (next.emomRound ?: 1).toString(), (next.emomRounds ?: 1).toString()).uppercase()

@Composable
private fun thenLine(next: LiveNextSet): String =
    stringResource(Res.string.dashboard_live_then, next.exerciseName, next.setNumber.toString(), next.setsInExercise.toString())

/** What a timer ring shows under the time: "of 20:00", "paused", or nothing. */
@Composable
private fun timerFootnote(ui: LiveWorkoutUi): String? = when {
    ui.paused -> stringResource(Res.string.dashboard_live_paused)
    ui.totalSeconds > 0 -> stringResource(Res.string.dashboard_live_of_total, clock(ui.totalSeconds))
    else -> null
}

/**
 * How far a ring is: the rest or warm-up gone (it fills clockwise, as the rest card of the workout does), the share of
 * a set or cardio target, the minute of a stopwatch, the EMOM rounds done.
 */
private fun LiveWorkoutUi.ringProgress(): Float = when (phase) {
    LiveWorkoutUi.Phase.REST, LiveWorkoutUi.Phase.WARMUP ->
        if (totalSeconds > 0) 1f - seconds.toFloat() / totalSeconds else 0f
    LiveWorkoutUi.Phase.SET_TIMER, LiveWorkoutUi.Phase.CARDIO ->
        if (totalSeconds > 0) seconds.toFloat() / totalSeconds else (seconds % 60) / 60f
    LiveWorkoutUi.Phase.EMOM -> next?.let { n ->
        val rounds = n.emomRounds ?: 0
        if (rounds > 0) ((n.emomRound ?: 1) - 1).toFloat() / rounds else 0f
    } ?: 0f
    else -> 0f
}.coerceIn(0f, 1f)

/** Waves move only on screen, without reduced motion, and while something really runs. */
@Composable
private fun wavesMove(isActive: Boolean, ui: LiveWorkoutUi): Boolean = isActive && !rememberReduceMotion() && ui.counts

/** The wave and the amber of the resume card, moving only on screen and without reduced motion. */
@Composable
private fun Modifier.liveAmber(isActive: Boolean, shape: Shape): Modifier {
    val reduceMotion = rememberReduceMotion()
    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(isActive, reduceMotion) {
        if (isActive && !reduceMotion) {
            while (true) {
                wavePhase.animateTo(1f, tween(6000, easing = LinearEasing))
                wavePhase.snapTo(0f)
            }
        }
    }
    return this
        .clip(shape)
        .background(Surface)
        .background(resumeBrush())
        .drawBehind { drawResumeWave(wavePhase.value) }
}

/**
 * The Material 3 Expressive wavy ring in ink on amber, as the rest card of the workout draws it: it creeps between
 * the one-second ticks and its wave travels while [moving].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiveRing(
    progress: Float,
    size: Dp,
    stroke: Dp,
    moving: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val shown by animateFloatAsState(progress, tween(if (moving) 1000 else 300, easing = LinearEasing), label = "liveRing")
    val density = LocalDensity.current
    val strokePx = with(density) { Stroke(stroke.toPx(), cap = StrokeCap.Round) }
    val wavelength = (size.value * 0.22f).coerceIn(12f, 40f).dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator(
            progress = { shown },
            modifier = Modifier.size(size),
            color = ResumeInk,
            trackColor = ResumeInk.copy(alpha = 0.16f),
            stroke = strokePx,
            trackStroke = strokePx,
            gapSize = stroke * 0.7f,
            amplitude = { p -> if (p <= 0.02f || p >= 0.98f) 0f else 1f },
            wavelength = wavelength,
            waveSpeed = if (moving) wavelength else 0.dp
        )
        content()
    }
}

/**
 * The status line on top of the card and the panel: a dot, "IN PROGRESS · 0:32:07". The clock of the workout always
 * shows here and moves on the second while the dashboard is on screen; only this line redraws.
 */
@Composable
private fun LiveStatus(ui: LiveWorkoutUi, isActive: Boolean) {
    val elapsed = rememberWorkoutElapsed(ui.startedAt, isActive)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(ResumeInk))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(Res.string.dashboard_live_in_progress, elapsed).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.ExtraBold,
            color = ResumeInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The play / open button: a dark circle with the "live" wave leaving it. */
@Composable
private fun LivePlayButton(isActive: Boolean, size: Dp, contentDescription: String?, onClick: (() -> Unit)?) {
    val reduceMotion = rememberReduceMotion()
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(isActive, reduceMotion) {
        if (isActive && !reduceMotion) {
            while (true) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
                delay(400)
            }
        } else pulse.snapTo(0f)
    }
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val p = pulse.value
                if (p > 0f) drawCircle(ResumeInk.copy(alpha = 0.35f * (1f - p)), radius = this.size.minDimension / 2f * (1f + 0.45f * p))
            }
            .clip(CircleShape)
            .background(ResumeInk)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = ResumeAmber, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * One dash per set, like the segments of the workout's strip: done ones full, the one to do half tinted, the others
 * faint. Past 16 sets one bar filled up to the sets done.
 */
@Composable
private fun SetDashes(
    dots: List<Boolean>,
    modifier: Modifier,
    height: Dp,
    color: Color = ResumeInk,
    track: Color = color.copy(alpha = 0.18f),
    markNext: Boolean = true
) {
    if (dots.isEmpty()) return
    if (dots.size > 16) {
        val fill = dots.count { it }.toFloat() / dots.size
        Box(modifier.height(height).clip(CircleShape).background(track).drawBehind { drawRect(color, size = Size(size.width * fill, size.height)) })
        return
    }
    val next = if (markNext) dots.indexOfFirst { !it } else -1
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (dots.size > 8) 3.dp else 4.dp)) {
        dots.forEachIndexed { i, done ->
            Box(
                Modifier.weight(1f).height(height).clip(CircleShape).background(
                    when {
                        done -> color
                        i == next -> color.copy(alpha = 0.45f)
                        else -> track
                    }
                )
            )
        }
    }
}

/**
 * The workout's progress as its header strip draws it: one segment per exercise, done ones full, the one under way a
 * travelling wave up to its sets done, the others faint; past 12 exercises one wavy bar for the whole workout.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseProgress(ui: LiveWorkoutUi, isActive: Boolean) {
    val moving = isActive && !rememberReduceMotion()
    val track = ResumeInk.copy(alpha = 0.18f)
    Column {
        if (ui.exercises.size <= 12) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ui.exercises.forEachIndexed { i, ex ->
                    val fill = if (ex.total > 0) (ex.done.toFloat() / ex.total).coerceIn(0f, 1f) else 0f
                    if (i == ui.currentIndex && fill < 1f) {
                        LinearWavyProgressIndicator(
                            progress = { fill.coerceAtLeast(0.08f) },
                            modifier = Modifier.weight(1.6f).height(10.dp),
                            color = ResumeInk,
                            trackColor = track,
                            wavelength = 12.dp,
                            waveSpeed = if (moving) 12.dp else 0.dp
                        )
                    } else {
                        Box(
                            Modifier.weight(1f).height(6.dp).clip(CircleShape)
                                .background(if (fill >= 1f) ResumeInk else track)
                        )
                    }
                }
            }
        } else {
            LinearWavyProgressIndicator(
                progress = { ui.progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = ResumeInk,
                trackColor = track,
                wavelength = 16.dp,
                waveSpeed = if (moving) 16.dp else 0.dp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            val index = (ui.currentIndex.coerceAtLeast(0) + 1).coerceAtMost(ui.exercises.size)
            Text(
                stringResource(Res.string.dashboard_live_exercise_of, index.toString(), ui.exercises.size.toString()),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                pluralStringResource(Res.plurals.dashboard_live_sets_left, ui.setsLeft, ui.setsLeft),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1
            )
        }
    }
}

/** What the screen reader says for the whole card, instead of every text and the moving seconds. */
@Composable
private fun liveSummary(ui: LiveWorkoutUi): String {
    val parts = mutableListOf(ui.title, stringResource(Res.string.dashboard_live_in_progress, elapsedText(ui.elapsedMinutes)))
    when (ui.phase) {
        LiveWorkoutUi.Phase.REST, LiveWorkoutUi.Phase.WARMUP -> {
            parts += "${phaseLabel(ui).lowercase()} ${clock(ui.seconds)}"
            ui.next?.let { parts += thenLine(it) }
        }
        LiveWorkoutUi.Phase.SET_TIMER, LiveWorkoutUi.Phase.CARDIO -> {
            ui.next?.let { parts += it.exerciseName }
            parts += clock(ui.seconds)
            timerFootnote(ui)?.let { parts += it }
        }
        LiveWorkoutUi.Phase.ALL_DONE -> parts += stringResource(Res.string.dashboard_live_all_done)
        else -> ui.next?.let { parts += "${it.exerciseName}, ${nowSetLabel(it).lowercase()}, ${it.loadText()}" }
    }
    return parts.joinToString(". ")
}

/**
 * The default live card (A): the resume card of the workout the dashboard follows, with the rest or the set to do and
 * the progress. A tap opens the workout, a long press selects it, as on the plain resume card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LiveWorkoutCard(ui: LiveWorkoutUi, isActive: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val summary = liveSummary(ui)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liveAmber(isActive, Shapes.extraLarge)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .clearAndSetSemantics { contentDescription = summary }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LiveStatus(ui, isActive)
                Text(
                    ui.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            LivePlayButton(isActive, 56.dp, contentDescription = null, onClick = null)
        }
        LiveCardMiddle(ui, isActive)
        if (ui.hasProgress) {
            Spacer(Modifier.height(14.dp))
            ExerciseProgress(ui, isActive)
        }
    }
}

/** The inner block of the live card: the rest, a timer, the EMOM round, the set to do, or "all done". */
@Composable
private fun LiveCardMiddle(ui: LiveWorkoutUi, isActive: Boolean) {
    val next = ui.next
    val moving = wavesMove(isActive, ui)
    val ink = MaterialTheme.typography
    val content: (@Composable RowScope.() -> Unit)? = when (ui.phase) {
        LiveWorkoutUi.Phase.REST, LiveWorkoutUi.Phase.WARMUP -> {
            {
                LiveRing(ui.ringProgress(), 56.dp, 5.dp, moving)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(clock(ui.seconds), fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                        Spacer(Modifier.width(8.dp))
                        Text(phaseLabel(ui), style = ink.labelSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    if (next != null) {
                        Text(thenLine(next), style = ink.bodyMedium, fontWeight = FontWeight.Bold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        LiveWorkoutUi.Phase.SET_TIMER, LiveWorkoutUi.Phase.CARDIO -> {
            {
                LiveRing(ui.ringProgress(), 56.dp, 5.dp, moving) {
                    Icon(
                        if (ui.phase == LiveWorkoutUi.Phase.CARDIO) Icons.AutoMirrored.Rounded.DirectionsRun else Icons.Rounded.Timer,
                        contentDescription = null, tint = ResumeInk, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (next != null) {
                        Text(nowSetLabel(next), style = ink.labelSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                        Text(next.exerciseName, style = ink.titleMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(clock(ui.seconds), fontSize = 24.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                        timerFootnote(ui)?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = ink.labelMedium, fontWeight = FontWeight.Bold, color = ResumeInk.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }
        LiveWorkoutUi.Phase.EMOM -> if (next == null) null else {
            {
                LiveRing(ui.ringProgress(), 56.dp, 5.dp, moving = false) {
                    Text("${next.emomRound ?: 1}/${next.emomRounds ?: 1}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(emomLabel(next), style = ink.labelSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                    Text(next.exerciseName, style = ink.titleMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(Res.string.dashboard_live_every_minute, next.loadText()),
                        style = ink.bodyMedium, fontWeight = FontWeight.Bold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        LiveWorkoutUi.Phase.LIFTING -> if (next == null) null else {
            {
                IconTile(next.icon)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(nowSetLabel(next), style = ink.labelSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                    Text(next.exerciseName, style = ink.titleLarge, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(next.loadText(), style = ink.bodyMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(8.dp))
                        SetDashes(ui.setDots, Modifier.width((ui.setDots.size * 14).coerceIn(28, 96).dp), 5.dp)
                    }
                }
            }
        }
        LiveWorkoutUi.Phase.ALL_DONE -> {
            {
                IconTile(Icons.Rounded.Check)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(Res.string.dashboard_live_all_done), style = ink.titleSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk, modifier = Modifier.weight(1f))
            }
        }
    }
    if (content == null) return
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ResumeInk.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
private fun IconTile(icon: ImageVector, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(ResumeInk),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = ResumeAmber, modifier = Modifier.size(size / 2))
    }
}

/**
 * Workout mode (B): the big panel on top while a workout runs. The rest with its −/+/Skip buttons; the set to do with
 * "Serie fatta"; a timed set or a cardio set on its running ring; the EMOM round waiting to be resumed; and always a
 * way to open the workout and to delete it.
 */
@Composable
internal fun LiveWorkoutPanel(
    ui: LiveWorkoutUi,
    buttons: RestButtons,
    isActive: Boolean,
    onOpen: () -> Unit,
    onRest: (RestButton) -> Unit,
    onSetDone: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val next = ui.next
    val moving = wavesMove(isActive, ui)
    val bigRing = if (ResponsiveSize.isCompact) 140.dp else 164.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liveAmber(isActive, RoundedCornerShape(40.dp))
            .clickable(onClick = onOpen)
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LiveStatus(ui, isActive)
                Text(ui.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            val deleteLabel = stringResource(Res.string.dashboard_live_delete)
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(ResumeInk.copy(alpha = 0.12f))
                    .clickable(role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = deleteLabel },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ResumeInk) }
            Spacer(Modifier.width(8.dp))
            LivePlayButton(isActive, 52.dp, stringResource(Res.string.dashboard_live_open_workout), onOpen)
        }
        Spacer(Modifier.height(18.dp))
        when (ui.phase) {
            LiveWorkoutUi.Phase.REST, LiveWorkoutUi.Phase.WARMUP -> {
                LiveRing(ui.ringProgress(), bigRing, 12.dp, moving, Modifier.align(Alignment.CenterHorizontally)) {
                    RingCenter(phaseLabel(ui), clock(ui.seconds), stringResource(Res.string.dashboard_live_of_total, clock(ui.totalSeconds)))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (buttons.showSubtract) PanelPill("−${buttons.subtractSeconds}s") { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRest(RestButton.SUBTRACT) }
                    if (buttons.showAdd) PanelPill("+${buttons.addSeconds}s") { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRest(RestButton.ADD) }
                    if (buttons.showSkip) {
                        val skip = stringResource(Res.string.dashboard_live_skip_rest)
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(ResumeInk)
                                .clickable(role = Role.Button) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRest(RestButton.SKIP) }
                                .semantics { contentDescription = skip },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = ResumeAmber) }
                    }
                }
                if (next != null) {
                    Spacer(Modifier.height(14.dp))
                    PanelFootnote(thenLine(next) + " · " + next.loadText())
                }
            }
            LiveWorkoutUi.Phase.SET_TIMER, LiveWorkoutUi.Phase.CARDIO -> {
                LiveRing(ui.ringProgress(), bigRing, 12.dp, moving, Modifier.align(Alignment.CenterHorizontally)) {
                    RingCenter(next?.let { nowSetLabel(it) } ?: "", clock(ui.seconds), timerFootnote(ui))
                }
                Spacer(Modifier.height(14.dp))
                if (next != null) {
                    PanelTitle(next.exerciseName)
                    PanelFootnote(next.loadText())
                }
                Spacer(Modifier.height(16.dp))
                InkButton(Icons.Rounded.PlayArrow, stringResource(Res.string.dashboard_live_continue), onOpen)
            }
            LiveWorkoutUi.Phase.EMOM -> if (next != null) {
                LiveRing(ui.ringProgress(), bigRing, 12.dp, moving = false, Modifier.align(Alignment.CenterHorizontally)) {
                    RingCenter("EMOM", "${next.emomRound ?: 1}/${next.emomRounds ?: 1}", null)
                }
                Spacer(Modifier.height(14.dp))
                PanelTitle(next.exerciseName)
                PanelFootnote(stringResource(Res.string.dashboard_live_every_minute, next.loadText()))
                Spacer(Modifier.height(16.dp))
                InkButton(Icons.Rounded.PlayArrow, stringResource(Res.string.dashboard_live_resume_emom).uppercase(), onOpen)
                Spacer(Modifier.height(10.dp))
                PanelFootnote(stringResource(Res.string.dashboard_live_emom_hint), alpha = 0.75f)
            }
            LiveWorkoutUi.Phase.LIFTING -> {
                if (next != null) {
                    Text(nowSetLabel(next), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                    Text(next.exerciseName, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        next.loadText(),
                        fontSize = if (next.kind == SetKind.STRENGTH) 40.sp else 28.sp,
                        lineHeight = if (next.kind == SetKind.STRENGTH) 44.sp else 32.sp,
                        fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    SetDashes(ui.setDots, Modifier.fillMaxWidth(), 8.dp)
                    Spacer(Modifier.height(18.dp))
                    val token = next.keyToken
                    if (buttons.showSetDone && token != null) {
                        InkButton(Icons.Rounded.Check, stringResource(Res.string.next_set_done).uppercase()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSetDone(token)
                        }
                    } else {
                        // A timed set, a cardio set or an EMOM is started on its own screen
                        InkButton(Icons.Rounded.PlayArrow, stringResource(Res.string.dashboard_live_continue), onOpen)
                    }
                    ui.nextExerciseName?.let { name ->
                        Spacer(Modifier.height(12.dp))
                        PanelFootnote(stringResource(Res.string.dashboard_live_next_exercise, name))
                    }
                } else {
                    Text(stringResource(Res.string.resume_workout), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                    Spacer(Modifier.height(16.dp))
                    InkButton(Icons.Rounded.PlayArrow, stringResource(Res.string.dashboard_live_continue), onOpen)
                }
            }
            LiveWorkoutUi.Phase.ALL_DONE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Check)
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(Res.string.dashboard_live_all_done), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ResumeInk)
                }
                Spacer(Modifier.height(16.dp))
                InkButton(Icons.Rounded.PlayArrow, stringResource(Res.string.dashboard_live_continue), onOpen)
            }
        }
    }
}

/** Label, big number and footnote in the middle of the big ring. */
@Composable
private fun RingCenter(label: String, value: String, footnote: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Text(value, fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.ExtraBold, color = ResumeInk, maxLines = 1)
        footnote?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ResumeInk.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.PanelTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ResumeInk,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PanelFootnote(text: String, alpha: Float = 1f) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ResumeInk.copy(alpha = alpha),
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis
    )
}

/** The full-width dark button of the panel: "Serie fatta", or open the workout to go on. */
@Composable
private fun InkButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(ResumeInk)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ResumeAmber)
        Spacer(Modifier.width(10.dp))
        Text(label, color = ResumeAmber, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanelPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(48.dp).clip(CircleShape).background(ResumeInk.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = ResumeInk, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The exercise list of workout mode: the last one done, the one under way and the next ones, joined like the set
 * list. Each row has its sets as dashes (the same segments as the workout's strip) and, while sets are left, what its
 * next set asks; a tap opens the workout.
 */
@Composable
internal fun LiveExerciseList(ui: LiveWorkoutUi, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (!ui.hasProgress) return
    val scheme = MaterialTheme.colorScheme
    val current = ui.currentIndex.coerceIn(0, ui.exercises.lastIndex)
    val from = (current - 1).coerceAtLeast(0)
    val to = (from + 3).coerceAtMost(ui.exercises.lastIndex)
    val more = ui.exercises.lastIndex - to
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(Res.string.dashboard_live_exercises_header, (current + 1).toString(), ui.exercises.size.toString()).uppercase(),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(pluralStringResource(Res.plurals.dashboard_live_sets_left, ui.setsLeft, ui.setsLeft), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in from..to) {
                val outerTop = if (i == from) 24.dp else 8.dp
                val outerBottom = if (i == to && more == 0) 24.dp else 8.dp
                LiveExerciseRow(
                    number = i + 1,
                    exercise = ui.exercises[i],
                    isCurrent = i == current,
                    shape = if (i == current) RoundedCornerShape(24.dp) else RoundedCornerShape(outerTop, outerTop, outerBottom, outerBottom),
                    onOpen = onOpen
                )
            }
            if (more > 0) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp))
                        .background(scheme.surfaceContainerHigh)
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pluralStringResource(Res.plurals.dashboard_live_more_exercises, more, more),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** One exercise of the list: number (a check once done), name, sets done of all, what comes next, the set dashes. */
@Composable
private fun LiveExerciseRow(number: Int, exercise: LiveExercise, isCurrent: Boolean, shape: Shape, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val done = exercise.total > 0 && exercise.done >= exercise.total
    val accent = when {
        done -> scheme.tertiary
        isCurrent -> scheme.primary
        else -> scheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (isCurrent) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .then(if (isCurrent) Modifier.drawBehind {
                drawRoundRect(scheme.primary.copy(alpha = 0.7f), cornerRadius = CornerRadius(24.dp.toPx()), style = Stroke(2.dp.toPx()))
            } else Modifier)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (done) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(scheme.tertiary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onTertiary, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(if (isCurrent) scheme.primary else scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = if (isCurrent) scheme.onPrimary else scheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (done) scheme.onSurfaceVariant else scheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text("${exercise.done}/${exercise.total}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
            }
            if (!done && exercise.detail != null) {
                Text(
                    pluralStringResource(Res.plurals.routine_sets_count, exercise.total, exercise.total) + " · " + exercise.detail,
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            SetDashes(
                List(exercise.total) { it < exercise.done },
                Modifier.fillMaxWidth(),
                height = if (isCurrent) 6.dp else 5.dp,
                color = accent,
                track = scheme.onSurface.copy(alpha = 0.12f),
                markNext = isCurrent
            )
        }
    }
}

/** The small picture under the onboarding switch: the amber ring next to a short list, no real names. */
@Composable
internal fun WorkoutModePreview() {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.width(110.dp).height(84.dp).clip(RoundedCornerShape(18.dp)).background(Surface).background(resumeBrush()),
            contentAlignment = Alignment.Center
        ) {
            LiveRing(0.7f, 52.dp, 5.dp, moving = !rememberReduceMotion())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(true, false, false).forEachIndexed { i, done ->
                val shape = when (i) { 0 -> RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp); 2 -> RoundedCornerShape(4.dp, 4.dp, 12.dp, 12.dp); else -> RoundedCornerShape(10.dp) }
                Row(
                    Modifier.weight(1f).fillMaxWidth().clip(shape)
                        .background(if (i == 1) scheme.secondaryContainer else scheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (done) scheme.tertiary else if (i == 1) scheme.primary else scheme.surfaceContainerHighest))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.height(6.dp).width(if (i == 1) 72.dp else 56.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = if (i == 1) 0.7f else 0.35f)))
                }
            }
        }
    }
}

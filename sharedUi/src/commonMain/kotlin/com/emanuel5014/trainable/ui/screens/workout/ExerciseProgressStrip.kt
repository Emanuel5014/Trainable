package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.workout_progress_description
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource

/** Height of the strip: room for the wave of the current exercise to swing, so the strip never changes height. */
private val STRIP_HEIGHT = 12.dp

/** Thickness of the current exercise's line, waving or straight: a bit more than the others, so it stays in view. */
private val CURRENT_STROKE = 5.dp
private val OTHER_HEIGHT = 4.dp

/**
 * One segment per exercise under the top bar. The current one stands out: while its sets are being done it is a thick
 * wave over a tinted track, filled by the sets logged; once they are all done it turns into a straight line, still a
 * bit thicker and in full colour, so where you are shows even then. The others are thin straight lines, full (a bit
 * dimmer) when done or partly filled by their sets. [fractions] come from [ExerciseProgress.fractions]; on the right,
 * which exercise this is out of all. With [onSelect], a tap on a segment goes to that exercise: the whole height of
 * the strip and its side margins take the tap, since a segment alone is too thin for a finger.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseProgressStrip(
    fractions: List<Float>,
    currentIndex: Int,
    completedCount: Int,
    modifier: Modifier = Modifier,
    hapticEnabled: Boolean = true,
    onSelect: ((Int) -> Unit)? = null
) {
    if (fractions.isEmpty()) return
    val description = stringResource(Res.string.workout_progress_description, currentIndex + 1, fractions.size, completedCount)
    val haptic = LocalHapticFeedback.current
    val count = fractions.size
    val strip = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val segments = remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Where each segment sits: they differ in width, so a tap is matched against them one by one
    val segmentCoordinates = remember(count) { arrayOfNulls<LayoutCoordinates>(count) }
    val gap: Dp = if (count > 12) 2.dp else 3.dp
    // The tap handler lives across recompositions: it reads the newest values through these
    val shownIndex by rememberUpdatedState(currentIndex)
    val select by rememberUpdatedState(onSelect)
    val haptics by rememberUpdatedState(hapticEnabled)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onSelect == null) Modifier else Modifier
                    .onGloballyPositioned { strip.value = it }
                    .pointerInput(count) {
                        detectTapGestures { tap ->
                            val from = strip.value?.takeIf { it.isAttached } ?: return@detectTapGestures
                            val box = segments.value?.takeIf { it.isAttached } ?: return@detectTapGestures
                            val x = box.localPositionOf(from, tap).x
                            // The count on the right is not a segment
                            if (x > box.size.width + gap.toPx() * 2) return@detectTapGestures
                            val starts = FloatArray(count)
                            val ends = FloatArray(count)
                            for (i in 0 until count) {
                                val segment = segmentCoordinates[i]?.takeIf { it.isAttached } ?: return@detectTapGestures
                                starts[i] = box.localPositionOf(segment, Offset.Zero).x
                                ends[i] = starts[i] + segment.size.width
                            }
                            val index = ExerciseProgress.segmentAt(x, starts, ends)
                            val go = select ?: return@detectTapGestures
                            if (index >= 0 && index != shownIndex) {
                                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                go(index)
                            }
                        }
                    }
            )
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f).height(STRIP_HEIGHT).onGloballyPositioned { segments.value = it },
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fractions.forEachIndexed { index, fraction ->
                StripSegment(
                    fraction = fraction,
                    isCurrent = index == currentIndex,
                    modifier = Modifier.weight(1f).onGloballyPositioned { segmentCoordinates[index] = it }
                )
            }
        }
        Text(
            text = "${currentIndex + 1}/${fractions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StripSegment(fraction: Float, isCurrent: Boolean, modifier: Modifier) {
    val shown = animateFloatAsState(fraction, WorkoutMotion.spatial(), label = "stripSegmentFill")
    if (isCurrent && fraction < 1f) {
        // Under way: a thick wave over a tinted track, so it stands out even before the first set
        val strokePx = with(LocalDensity.current) { CURRENT_STROKE.toPx() }
        val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }
        LinearWavyProgressIndicator(
            progress = { shown.value.coerceIn(0f, 1f) },
            modifier = modifier.height(STRIP_HEIGHT),
            color = Primary,
            trackColor = Primary.copy(alpha = 0.28f),
            stroke = stroke,
            trackStroke = stroke,
            stopSize = 0.dp,
            amplitude = { 1f },
            wavelength = 14.dp,
            // A wave that travels redraws the screen on every frame: the strip stands still, the rest ring is the one that moves
            waveSpeed = 0.dp
        )
    } else {
        // Done, or not the current one: a straight line. The current one, all done, stays thicker and in full colour
        val thickness by animateDpAsState(if (isCurrent) CURRENT_STROKE else OTHER_HEIGHT, WorkoutMotion.spatial(), label = "stripSegmentHeight")
        val fill by animateColorAsState(
            if (isCurrent) Primary else Primary.copy(alpha = 0.6f),
            WorkoutMotion.effects(),
            label = "stripSegmentColor"
        )
        Box(
            modifier = modifier
                // Springs overshoot: a height must stay above zero
                .height(thickness.coerceAtLeast(1.dp))
                .clip(CircleShape)
                .background(SurfaceContainerHighest)
                .drawBehind { drawRect(fill, size = Size(size.width * shown.value.coerceIn(0f, 1f), size.height)) }
        )
    }
}

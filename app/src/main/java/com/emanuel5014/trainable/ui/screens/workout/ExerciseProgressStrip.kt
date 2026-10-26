package com.emanuel5014.trainable.ui.screens.workout
import com.emanuel5014.trainable.ui.components.glass.glassPanelBackground

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

/** A segment narrower than this is a plain bar: the wave needs room to read as a wave. */
private val MIN_WAVY_SEGMENT_WIDTH = 20.dp

/**
 * One segment per exercise under the top bar: full when the exercise is done, partly filled while it is being done (the
 * fill follows the sets logged), with a wave on the current one. [fractions] come from [ExerciseProgress.fractions];
 * on the right, how many exercises are done. It only shows progress: it takes no taps.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExerciseProgressStrip(
    fractions: List<Float>,
    currentIndex: Int,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    if (fractions.isEmpty()) return
    val description = stringResource(R.string.workout_progress_description, currentIndex + 1, fractions.size, completedCount)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val gap: Dp = if (fractions.size > 12) 2.dp else 3.dp
            val segmentWidth = (maxWidth - gap * (fractions.size - 1)) / fractions.size
            val wavy = segmentWidth >= MIN_WAVY_SEGMENT_WIDTH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fractions.forEachIndexed { index, fraction ->
                    StripSegment(
                        fraction = fraction,
                        isCurrent = index == currentIndex,
                        wavy = wavy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            text = "$completedCount/${fractions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StripSegment(fraction: Float, isCurrent: Boolean, wavy: Boolean, modifier: Modifier) {
    val shown = animateFloatAsState(fraction, WorkoutMotion.spatial(), label = "stripSegmentFill")
    if (isCurrent && wavy && fraction < 1f) {
        // The wave swells when the segment becomes the current one
        val amplitude = remember { Animatable(0f) }
        LaunchedEffect(Unit) { amplitude.animateTo(1f, WorkoutMotion.spatial()) }
        LinearWavyProgressIndicator(
            progress = { shown.value.coerceIn(0f, 1f) },
            modifier = modifier,
            color = Primary,
            trackColor = SurfaceContainerHighest,
            amplitude = { amplitude.value.coerceIn(0f, 1f) },
            // A wave that travels redraws the screen on every frame: the strip stands still, the rest ring is the one that moves
            waveSpeed = 0.dp
        )
    } else {
        val fill = Primary
        Box(
            modifier = modifier
                .height(6.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHighest)
                .drawBehind { drawRect(fill, size = Size(size.width * shown.value.coerceIn(0f, 1f), size.height)) }
        )
    }
}

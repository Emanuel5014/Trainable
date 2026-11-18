package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.ui.components.motion.toComposePath
import kotlin.math.floor

/** Time spent on each shape, the morph to the next one included. */
private const val STEP_MS = 2_400

/** Share of each step the shape holds still before morphing on. */
private const val HOLD = 0.3f

/** One slow turn, so the morph never looks frozen. */
private const val TURN_MS = 24_000

/**
 * The Welcome logo on one primary-container shape that keeps morphing into the next Material shape. The shape
 * stays on the centre of the box (it never slides) and the logo does not turn with it. With Reduce Motion /
 * animations off it is a still cookie.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingLogoShape(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val color = MaterialTheme.colorScheme.primaryContainer
    val morphs = remember {
        val shapes = listOf(
            MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Cookie12Sided,
            MaterialShapes.Gem, MaterialShapes.Flower, MaterialShapes.SoftBurst, MaterialShapes.Cookie6Sided
        )
        shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) }
    }

    // Read only while drawing: each frame redraws the shape without recomposing the page
    val clock = remember { mutableLongStateOf(0L) }
    if (!reduceMotion) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) withFrameMillis { clock.longValue = it - start }
        }
    }

    Box(
        modifier = modifier.drawBehind {
            val time = clock.longValue
            val steps = time.toFloat() / STEP_MS
            val index = floor(steps).toInt() % morphs.size
            val within = steps - floor(steps)
            val progress = FastOutSlowInEasing.transform(((within - HOLD) / (1f - HOLD)).coerceIn(0f, 1f))

            // The shapes live in a 0..1 square centred on (0.5, 0.5): scale it to the box and pin that point to the
            // box centre, instead of centring each frame's bounds (which change with the morph and made it drift)
            val side = size.minDimension
            val path = morphs[index].toComposePath(progress)
            path.transform(Matrix().apply { scale(side, side) })
            path.translate(center - Offset(side / 2f, side / 2f))
            rotate((time.toFloat() / TURN_MS) * 360f, pivot = center) { drawPath(path, color) }
        },
        contentAlignment = Alignment.Center,
        content = content
    )
}

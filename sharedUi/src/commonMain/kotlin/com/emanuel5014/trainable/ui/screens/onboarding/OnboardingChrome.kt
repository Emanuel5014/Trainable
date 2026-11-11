package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape
import org.jetbrains.compose.resources.stringResource

/** Back button and the wavy progress of the setup, above every step except Welcome. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTopRow(progress: Float, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) tween(0) else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "onboardingProgress"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalIconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
        }
        LinearWavyProgressIndicator(
            progress = { shown },
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            amplitude = { if (reduceMotion) 0f else 1f }
        )
    }
}

/**
 * The step's hero shape ([heroShapeFor]). When the step becomes the current page ([active]) it morphs from the
 * previous step's shape into its own; on the last step it fills with the primary colour and draws a check instead
 * of [content].
 */
@Composable
fun OnboardingHeroShape(
    stepIndex: Int,
    active: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(1f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(active) {
        if (active && !reduceMotion && stepIndex > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, spec)
        } else {
            progress.snapTo(1f)
        }
    }
    val morph = remember(stepIndex) { Morph(heroShapeFor(stepIndex - 1), heroShapeFor(stepIndex)) }
    val shape = rememberAnimatedShape(morph, progress.value)
    val container = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier.size(56.dp).clip(shape).background(container),
        contentAlignment = Alignment.Center
    ) {
        if (done) DrawnCheck(animate = active && !reduceMotion) else content()
    }
}

/** The step number inside the hero shape. */
@Composable
fun HeroNumber(number: Int) {
    Text(
        number.toString(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

/** The check of the Ready step, drawn stroke by stroke in about 600 ms. */
@Composable
private fun DrawnCheck(animate: Boolean) {
    val drawn = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) { if (animate) { drawn.snapTo(0f); drawn.animateTo(1f, tween(600, delayMillis = 200)) } else drawn.snapTo(1f) }
    val color = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.52f)
            lineTo(size.width * 0.40f, size.height * 0.78f)
            lineTo(size.width * 0.88f, size.height * 0.24f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * drawn.value, part, true)
        drawPath(part, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** The big pill at the bottom: "Continua" with an arrow, or "Inizia ad allenarti" with a check on the last step. */
@Composable
fun OnboardingNextButton(isLast: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GymButton(onClick = onClick, enabled = enabled, pressMorph = true, modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(if (isLast) Res.string.onboarding_start_training else Res.string.continue_text),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

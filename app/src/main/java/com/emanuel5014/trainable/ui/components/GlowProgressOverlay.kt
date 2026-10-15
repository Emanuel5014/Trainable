package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.cos
import kotlin.math.sin

/**
 * The full-screen "working" overlay of the AI scan: slowly orbiting colour glows behind a card that holds [content]
 * (usually a [GymLoadingIndicator], a title and a progress bar). It swallows touches so nothing underneath can be
 * pressed. Used by the AI scan and by the workout history import, so the two wait the same way.
 *
 * @param hazeState blur of the content behind; null when the overlay has nothing behind it to blur (own window, or
 * covering the whole screen): a plain scrim is used then.
 */
@Composable
fun GlowProgressOverlay(
    isDark: Boolean,
    hazeState: HazeState?,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_scan_glow_transition")

    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "glow_angle_1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing)
        ),
        label = "glow_angle_2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val scrimColor = MaterialTheme.colorScheme.surface
        .copy(alpha = if (isDark) 0.65f else 0.85f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Consume touches to prevent triggering underlying buttons */ }
            }
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 24.dp
                        tints = listOf(HazeTint(scrimColor))
                        noiseFactor = 0.05f
                    }
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.92f else 0.95f))
                }
            )
            .drawBehind {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                // Orb 1: Primary color orbital glow
                val rad1 = Math.toRadians(angle1.toDouble())
                val offset1 = Offset(
                    x = centerX + (cos(rad1) * width * 0.30f).toFloat(),
                    y = centerY + (sin(rad1) * height * 0.22f).toFloat()
                )
                val radius1 = (width * 0.72f * pulseScale).coerceAtLeast(160f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDark) 0.40f * pulseAlpha else 0.28f * pulseAlpha),
                            primaryColor.copy(alpha = if (isDark) 0.15f * pulseAlpha else 0.09f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = offset1,
                        radius = radius1
                    ),
                    center = offset1,
                    radius = radius1
                )

                // Orb 2: Tertiary / Accent color orbital glow (counter-rotation)
                val rad2 = Math.toRadians(angle2.toDouble())
                val offset2 = Offset(
                    x = centerX + (cos(rad2) * width * 0.34f).toFloat(),
                    y = centerY + (sin(rad2) * height * 0.25f).toFloat()
                )
                val radius2 = (width * 0.65f * (2.1f - pulseScale)).coerceAtLeast(160f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = if (isDark) 0.35f * pulseAlpha else 0.22f * pulseAlpha),
                            tertiaryColor.copy(alpha = if (isDark) 0.12f * pulseAlpha else 0.07f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = offset2,
                        radius = radius2
                    ),
                    center = offset2,
                    radius = radius2
                )

                // Orb 3: Central breathing ambient aura behind card
                val radiusCenter = width * 0.52f * pulseScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isDark) 0.25f * pulseAlpha else 0.16f * pulseAlpha),
                            secondaryColor.copy(alpha = if (isDark) 0.10f * pulseAlpha else 0.05f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radiusCenter
                    ),
                    center = Offset(centerX, centerY),
                    radius = radiusCenter
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceContainerHigh.copy(alpha = if (isDark) 0.92f else 0.96f),
            tonalElevation = 4.dp,
            shadowElevation = if (isDark) 16.dp else 4.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        tertiaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.15f),
                        tertiaryColor.copy(alpha = 0.45f),
                        primaryColor.copy(alpha = 0.5f)
                    )
                )
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                content()
            }
        }
    }
}

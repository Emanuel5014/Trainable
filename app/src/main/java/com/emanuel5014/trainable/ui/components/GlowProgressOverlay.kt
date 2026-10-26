package com.emanuel5014.trainable.ui.components

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
import com.emanuel5014.trainable.ui.components.glass.GlassSurface as Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * The full-screen "working" overlay of the AI scan: a uniform scrim behind a card that holds [content]
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
                    Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = HazeBlurStyle {
                        blurRadius(24.dp)
                        colorEffects(listOf(HazeColorEffect.tint(scrimColor)))
                        noiseFactor(0.05f)
                    })
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.92f else 0.95f))
                }
            ),
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

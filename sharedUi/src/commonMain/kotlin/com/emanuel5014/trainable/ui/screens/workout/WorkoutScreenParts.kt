package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.ui.glass.surfaceGlass
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.data.model.GlassPart
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import kotlin.math.roundToInt
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.components.HubMotion
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.log_set
import com.emanuel5014.trainable.sharedui.resources.previous_exercise_btn
import com.emanuel5014.trainable.ui.components.GymButton
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary

enum class HubMode { Logging, Editing, EditingSet, Resting, MiniSet, Completed, Cardio }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardioToggleButton(
    isRunning: Boolean,
    isPaused: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Primary,
    onActiveColor: Color = OnPrimary
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isRunning -> activeColor
            isPaused -> Tertiary
            else -> activeColor
        },
        label = "cardioBtnBg",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isRunning -> onActiveColor
            isPaused -> OnTertiary
            else -> onActiveColor
        },
        label = "cardioBtnContent",
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val shapeProgress by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardioShapeProgress"
    )

    val morph = remember { Morph(MaterialShapes.Cookie4Sided, MaterialShapes.Cookie9Sided) }
    val animatedShape = rememberAnimatedShape(morph, shapeProgress)

    val rotation = remember { Animatable(0f) }
    val transitionDuration = 1500
    val continuousTurnDuration = 15000
    val smoothHandoffEasing = remember { CubicBezierEasing(0.4f, 0.0f, 0.2f, 0.96f) }

    LaunchedEffect(isRunning) {
        rotation.snapTo(rotation.value % 360f)
        if (isRunning) {
            val startAngle = rotation.value
            val startupDelta = 720f
            val startupDurationNanos = transitionDuration * 1_000_000L
            val steadyDegreesPerNano = 360f / (continuousTurnDuration * 1_000_000f)
            var startNanos = 0L
            while (true) {
                val frameNanos = androidx.compose.runtime.withFrameNanos { it }
                if (startNanos == 0L) startNanos = frameNanos
                val elapsedNanos = frameNanos - startNanos
                val angle = if (elapsedNanos <= startupDurationNanos) {
                    val t = (elapsedNanos.toFloat() / startupDurationNanos).coerceIn(0f, 1f)
                    startAngle + startupDelta * smoothHandoffEasing.transform(t)
                } else {
                    val afterStartupNanos = elapsedNanos - startupDurationNanos
                    startAngle + startupDelta + (afterStartupNanos * steadyDegreesPerNano)
                }
                rotation.snapTo(angle)
            }
        } else {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = transitionDuration, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { rotationZ = rotation.value }
            .background(backgroundColor, animatedShape)
            .clip(animatedShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(0.4f)
                .graphicsLayer { rotationZ = -rotation.value },
            imageVector = when {
                isRunning -> Icons.Rounded.Pause
                isPaused -> Icons.Rounded.PlayArrow
                else -> Icons.Rounded.PlayArrow
            },
            contentDescription = null,
            tint = contentColor
        )
    }
}

/** Height of the exercise buttons in the bottom hub: a little lower on the strip. */
fun hubNavHeight(expansion: Float): Int = lerp(52f, 64f, expansion).roundToInt()

/**
 * "Previous exercise" in the bottom hub. As the hub shrinks to its strip the button gives up its label and most
 * of its width and keeps only the arrow, so "next" can take the room.
 */
@Composable
fun RowScope.HubPreviousButton(
    previousName: String,
    expansion: () -> Float,
    onClick: () -> Unit,
    nameStyle: TextStyle,
    nameWeight: FontWeight,
    height: Int? = null
) {
    val label = HubMotion.window(expansion(), 0.25f, 0.6f)
    val fullPadding = if (ResponsiveSize.isCompact) 16.dp else 24.dp
    val glass = LocalGlassSettings.current.isOn(GlassPart.WorkoutNav)
    GymButton(
        onClick = onClick,
        modifier = Modifier
            .weight(lerp(0.2f, 1f, label))
            .then(if (glass) Modifier.surfaceGlass(GlassPart.WorkoutNav) else Modifier),
        containerColor = if (glass) Color.Transparent else SurfaceContainerHigh,
        contentColor = OnSurface,
        shape = if (glass) CircleShape else Shapes.large,
        height = height ?: if (ResponsiveSize.isCompact) 48 else 56,
        contentPadding = PaddingValues(horizontal = lerp(0.dp, fullPadding, label))
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            // With the label gone the arrow has to say what the button does
            contentDescription = if (label <= 0f) stringResource(Res.string.previous_exercise_btn) else null
        )
        if (label > 0f) {
            Spacer(modifier = Modifier.width(8.dp * label))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = label }
            ) {
                Text(
                    text = stringResource(Res.string.previous_exercise_btn).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = previousName,
                    style = nameStyle,
                    fontWeight = nameWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LogSetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    GymButton(
        onClick = onClick,
        modifier = modifier.then(if (!compact) Modifier.fillMaxWidth() else Modifier.height(48.dp)),
        pressMorph = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            if (!compact) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.log_set), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

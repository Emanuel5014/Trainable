package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.components.motion.rememberAnimatedShape

internal enum class BadgeShape { Pending, Active, Done }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun BadgeShape.polygon(): RoundedPolygon = when (this) {
    BadgeShape.Pending -> MaterialShapes.Square
    BadgeShape.Active -> MaterialShapes.Cookie9Sided
    BadgeShape.Done -> MaterialShapes.Circle
}

/** The number of a set: a rounded square while it is to do, a cookie while it is the active one, a circle once done; it morphs between them. */
@Composable
internal fun SetNumberBadge(
    number: Int,
    shape: BadgeShape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    var from by remember { mutableStateOf(shape) }
    var to by remember { mutableStateOf(shape) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(shape) {
        if (shape != to) {
            from = to
            to = shape
            progress.snapTo(0f)
            progress.animateTo(1f, WorkoutMotion.spatial())
        }
    }
    val morph = remember(from, to) { Morph(from.polygon(), to.polygon()) }
    Box(
        modifier = modifier
            .size(size)
            .clip(rememberAnimatedShape(morph, progress.value.coerceIn(0f, 1f)))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor
        )
    }
}

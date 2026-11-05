package com.emanuel5014.trainable.ui.components.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

@Composable
fun rememberAnimatedShape(
    morph: Morph,
    progress: Float
): Shape {
    return remember(morph, progress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val path = morph.toComposePath(progress)
                val matrix = Matrix()
                matrix.scale(size.width, size.height)
                path.transform(matrix)
                path.translate(size.center - path.getBounds().center)
                return Outline.Generic(path)
            }
        }
    }
}

/** The outline of [this] at [progress] (0 = start shape, 1 = end shape), in the shape's own 0..1 space. */
internal expect fun Morph.toComposePath(progress: Float): Path

/** A polygon's outline as a Compose path, at its own size (graphics-shapes' toPath on Android). */
internal expect fun androidx.graphics.shapes.RoundedPolygon.toComposePath(): Path

package com.emanuel5014.trainable.ui.components.motion

import androidx.compose.ui.graphics.Path
import androidx.graphics.shapes.Morph

/** The same curves as graphics-shapes' Android toPath: one cubic after another from the first anchor, closed. */
internal actual fun Morph.toComposePath(progress: Float): Path {
    val path = Path()
    var first = true
    forEachCubic(progress) { cubic ->
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(cubic.control0X, cubic.control0Y, cubic.control1X, cubic.control1Y, cubic.anchor1X, cubic.anchor1Y)
    }
    path.close()
    return path
}

/** The same curves as graphics-shapes' Android toPath for a polygon: its cubics in order from the first anchor, closed. */
internal actual fun androidx.graphics.shapes.RoundedPolygon.toComposePath(): Path {
    val path = Path()
    var first = true
    cubics.forEach { cubic ->
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(cubic.control0X, cubic.control0Y, cubic.control1X, cubic.control1Y, cubic.anchor1X, cubic.anchor1Y)
    }
    path.close()
    return path
}

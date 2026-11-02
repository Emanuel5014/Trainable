package com.emanuel5014.trainable.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * How a piece of glass answers a finger, as on iOS 26: it swells, leans a little after the finger
 * and lights up softly under it. Only watches the touches, it never consumes them, so the button's
 * own click still works.
 *
 * Adapted from the InteractiveHighlight and DragGestureInspector examples of
 * Kyant0/AndroidLiquidGlass (Apache-2.0).
 */
class GlassTouch(private val scope: CoroutineScope) {

    private val pressSpec = spring(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var startPosition = Offset.Zero

    /** 0 at rest, 1 while held. Read it in draw or layer blocks only. */
    val press: Float get() = pressAnimation.value

    /** How far the finger moved since it touched down. */
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader = if (isRuntimeShaderSupported()) {
        RuntimeShader(
            """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
        )
    } else {
        null
    }

    /** The soft light under the finger, drawn over the glass body and under the button's content. */
    val light: Modifier = Modifier.drawWithContent {
        val progress = pressAnimation.value
        if (progress > 0f) {
            if (shader != null) {
                drawRect(Color.White.copy(alpha = 0.08f * progress), blendMode = BlendMode.Plus)
                val position = positionAnimation.value
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color", Color.White.copy(alpha = 0.15f * progress))
                shader.setFloatUniform("radius", size.minDimension * 1.5f)
                shader.setFloatUniform(
                    "position",
                    position.x.fastCoerceIn(0f, size.width),
                    position.y.fastCoerceIn(0f, size.height)
                )
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(Color.White.copy(alpha = 0.2f * progress), blendMode = BlendMode.Plus)
            }
        }
        drawContent()
    }

    /** Follows the finger without consuming it. */
    val gestures: Modifier = Modifier.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            startPosition = down.position
            scope.launch {
                launch { pressAnimation.animateTo(1f, pressSpec) }
                launch { positionAnimation.snapTo(down.position) }
            }
            var pointer = down.id
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.fastFirstOrNull { it.id == pointer } ?: break
                if (change.changedToUpIgnoreConsumed()) {
                    val other = event.changes.fastFirstOrNull { it.pressed } ?: break
                    pointer = other.id
                } else {
                    val position = change.position
                    scope.launch { positionAnimation.snapTo(position) }
                }
            }
            scope.launch {
                launch { pressAnimation.animateTo(0f, pressSpec) }
                launch { positionAnimation.animateTo(startPosition, positionSpec) }
            }
        }
    }
}

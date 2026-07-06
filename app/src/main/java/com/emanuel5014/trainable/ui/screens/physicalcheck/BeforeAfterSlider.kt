package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private class LeftClipShape(private val fraction: () -> Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val w = (size.width * fraction()).coerceAtLeast(0f)
        return Outline.Rectangle(Rect(0f, 0f, w, size.height))
    }
}

private class RightClipShape(private val fraction: () -> Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val left = (size.width * fraction()).coerceIn(0f, size.width)
        return Outline.Rectangle(Rect(left, 0f, size.width, size.height))
    }
}

@Composable
fun BeforeAfterSlider(
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
    beforeLabel: @Composable () -> Unit = {},
    afterLabel: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val handleSizeDp = 44.dp
    val handleSizePx = with(density) { handleSizeDp.toPx() }
    val leftClipShape = remember { LeftClipShape { sliderPosition } }
    val rightClipShape = remember { RightClipShape { sliderPosition } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { layoutSize = it }
    ) {
        Box(Modifier.fillMaxSize()) { after() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = leftClipShape
                }
        ) { before() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = leftClipShape
                }
        ) {
            Box(Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 8.dp)) {
                beforeLabel()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = rightClipShape
                }
        ) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 8.dp)) {
                afterLabel()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .offset {
                    val x = (sliderPosition * layoutSize.width).roundToInt()
                    IntOffset(x - 1, 0)
                }
                .background(Color.White)
        )

        Canvas(
            modifier = Modifier
                .size(handleSizeDp)
                .offset {
                    val x = (sliderPosition * layoutSize.width - handleSizePx / 2).roundToInt()
                    val y = (layoutSize.height / 2 - handleSizePx / 2).roundToInt()
                    IntOffset(x, y)
                }
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val arrowW = size.width * 0.18f
            val gap = arrowW * 0.4f

            drawCircle(Color.White)
            drawCircle(Color.Black.copy(alpha = 0.15f), radius = size.width / 2f - 1.dp.toPx())

            val leftArrow = Path().apply {
                moveTo(cx - gap, cy)
                lineTo(cx - gap - arrowW, cy - arrowW * 0.5f)
                lineTo(cx - gap - arrowW, cy + arrowW * 0.5f)
                close()
            }
            drawPath(leftArrow, Color.DarkGray)

            val rightArrow = Path().apply {
                moveTo(cx + gap, cy)
                lineTo(cx + gap + arrowW, cy - arrowW * 0.5f)
                lineTo(cx + gap + arrowW, cy + arrowW * 0.5f)
                close()
            }
            drawPath(rightArrow, Color.DarkGray)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (layoutSize.width > 0) {
                                sliderPosition = (offset.x / layoutSize.width).coerceIn(0f, 1f)
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (layoutSize.width > 0) {
                                sliderPosition = (change.position.x / layoutSize.width).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        )
    }
}

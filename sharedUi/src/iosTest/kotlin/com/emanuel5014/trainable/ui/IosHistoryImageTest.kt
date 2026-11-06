package com.emanuel5014.trainable.ui

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.test.*

class IosHistoryImageTest {
    @Test fun pngRoundTripPreservesDimensionsAndPixels() {
        val bitmap = ImageBitmap(3, 2)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, 3f, 2f, Paint().apply { color = Color(0xFF214B87) })
        val bytes = encodeHistoryPng(bitmap)
        assertContentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 13, 10, 26, 10), bytes.copyOfRange(0, 8))
        val decoded = bytes.decodeToImageBitmap()
        assertEquals(3, decoded.width); assertEquals(2, decoded.height)
        val pixel = decoded.toPixelMap()[1, 1]
        assertEquals(Color(0xFF214B87), pixel)
    }
}

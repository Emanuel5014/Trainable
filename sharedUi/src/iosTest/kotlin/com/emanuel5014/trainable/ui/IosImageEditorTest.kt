package com.emanuel5014.trainable.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.emanuel5014.trainable.ui.components.shared.cropAndRotateImage
import com.emanuel5014.trainable.ui.components.shared.decodeEditorImage
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosImageEditorTest {

    /** 200×100: left half red, right half blue. */
    private fun halves(): ByteArray {
        val surface = Surface.makeRasterN32Premul(200, 100)
        surface.canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(0f, 0f, 100f, 100f), Paint().apply { color = Color.RED })
        surface.canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(100f, 0f, 100f, 100f), Paint().apply { color = Color.BLUE })
        return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
    }

    private fun decodeJpeg(bytes: ByteArray): Bitmap {
        assertEquals(0xFF.toByte(), bytes[0]); assertEquals(0xD8.toByte(), bytes[1]) // JPEG magic
        return Bitmap.makeFromImage(Image.makeFromEncoded(bytes))
    }

    private fun isRed(c: Int) = Color.getR(c) > 200 && Color.getB(c) < 60
    private fun isBlue(c: Int) = Color.getB(c) > 200 && Color.getR(c) < 60

    // Container 400×200 = image fits exactly (base scale 2). Crop rect = whole container.
    private val container = Size(400f, 200f)
    private val whole = Rect(0f, 0f, 400f, 200f)

    @Test
    fun noChangesKeepsTheLayoutAndUsesTheExportSize() = runBlocking {
        val image = decodeEditorImage(halves())!!
        val out = decodeJpeg(cropAndRotateImage(image, 1f, 0f, 0f, 0f, whole, container))
        // exportScale = min(1280 / 400, 2) = 2 → 800×400
        assertEquals(800, out.width); assertEquals(400, out.height)
        assertTrue(isRed(out.getColor(100, 200)))
        assertTrue(isBlue(out.getColor(700, 200)))
    }

    @Test
    fun rotating180SwapsTheHalves() = runBlocking {
        val image = decodeEditorImage(halves())!!
        val out = decodeJpeg(cropAndRotateImage(image, 1f, 0f, 0f, 180f, whole, container))
        assertTrue(isBlue(out.getColor(100, 200)))
        assertTrue(isRed(out.getColor(700, 200)))
    }

    @Test
    fun panAndCropKeepOnlyWhatIsInsideTheFrame() = runBlocking {
        val image = decodeEditorImage(halves())!!
        // The frame is always centred (getCropRectForAspect), as the Android maths assumes. Square 1:1 frame.
        val square = Rect(120f, 20f, 280f, 180f)
        // Image moved 100 px left: the blue half's middle sits under the frame.
        val out = decodeJpeg(cropAndRotateImage(image, 1f, -100f, 0f, 0f, square, container))
        assertEquals(320, out.width); assertEquals(320, out.height)
        assertTrue(isBlue(out.getColor(160, 160)))
        assertTrue(isBlue(out.getColor(10, 160)))
        // Moved 100 px right: the red half.
        val panned = decodeJpeg(cropAndRotateImage(image, 1f, 100f, 0f, 0f, square, container))
        assertTrue(isRed(panned.getColor(160, 160)))
        assertTrue(isRed(panned.getColor(310, 160)))
    }

    @Test
    fun areaOutsideThePhotoIsBlack() = runBlocking {
        val image = decodeEditorImage(halves())!!
        val out = decodeJpeg(cropAndRotateImage(image, 0.5f, 0f, 0f, 0f, whole, container))
        val corner = out.getColor(10, 10)
        assertTrue(Color.getR(corner) < 30 && Color.getG(corner) < 30 && Color.getB(corner) < 30)
    }

    @Test
    fun notAnImageGivesNull() {
        assertNull(decodeEditorImage(byteArrayOf(1, 2, 3)))
        // Keep the import of toComposeImageBitmap meaningful for the decode path.
        assertEquals(200, Image.makeFromEncoded(halves()).toComposeImageBitmap().width)
    }
}

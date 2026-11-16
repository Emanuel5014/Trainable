package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlin.math.max
import kotlin.math.roundToInt

/** The theme colours of a photo, found the way Android finds them in the wallpaper. */
object PhotoThemeColors {
    private const val SIDE = 128

    /** Up to [count] seed colours, best first; Google blue when the photo has no usable colour at all. */
    fun extract(bitmap: ImageBitmap, count: Int = 4): List<Int> {
        val scale = SIDE.toFloat() / max(bitmap.width, bitmap.height)
        val small = if (scale >= 1f) bitmap else {
            val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            ImageBitmap(width, height).also { target ->
                Canvas(target).drawImageRect(
                    image = bitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(width, height),
                    paint = Paint().apply { filterQuality = FilterQuality.Low }
                )
            }
        }
        val pixels = IntArray(small.width * small.height)
        small.readPixels(pixels)
        val opaque = pixels.filter { (it ushr 24) == 0xFF }.toIntArray()
        return Score.score(QuantizerCelebi.quantize(opaque, 128), desired = count)
    }
}

/** Opens the platform's photo picker; [onPicked] gets the chosen photo, already scaled down. */
@Composable
expect fun rememberThemePhotoPicker(onPicked: (ImageBitmap) -> Unit): () -> Unit

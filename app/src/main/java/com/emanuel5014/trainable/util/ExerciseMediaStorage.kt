package com.emanuel5014.trainable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.StringRes
import com.emanuel5014.trainable.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The user's own image / GIF of an exercise, kept in `filesDir/exercise_media`.
 *
 * The database stores only the file name, so a restored backup keeps working even when the app's data
 * path differs (debug vs release build). Animated files (GIF, animated WebP) are copied untouched,
 * because re-encoding would flatten them to a single frame; still images are scaled down and recompressed.
 */
object ExerciseMediaStorage {

    const val DIR_NAME = "exercise_media"

    /** Largest file accepted. A looping GIF this size is already heavy to keep decoding during a workout. */
    const val MAX_BYTES = 15L * 1024 * 1024
    const val MAX_BYTES_LABEL_MB = 15

    /** Still images are scaled to this on their longest side before being stored. */
    private const val MAX_STILL_DIMENSION = 1280

    sealed interface ImportResult {
        data class Saved(val fileName: String) : ImportResult
        data object TooLarge : ImportResult
        data object Unsupported : ImportResult
    }

    fun dir(context: Context): File = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** The stored file for [fileName], or null when it is missing or tries to escape the media folder. */
    fun fileFor(context: Context, fileName: String): File? {
        if (fileName != File(fileName).name) return null
        return File(dir(context), fileName).takeIf { it.isFile }
    }

    fun delete(context: Context, fileName: String?) {
        if (fileName == null || fileName != File(fileName).name) return
        File(dir(context), fileName).delete()
    }

    /** Copies the picked [uri] into the media folder. Blocking: call from a background dispatcher. */
    fun import(context: Context, uri: Uri, exerciseId: Int): ImportResult {
        val directory = dir(context)
        val staging = File(directory, "staging_${exerciseId}_${System.nanoTime()}")
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(staging).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) return ImportResult.TooLarge
                        output.write(buffer, 0, read)
                    }
                }
                true
            } ?: false
            if (!copied) return ImportResult.Unsupported

            var mimeType: String? = null
            val decoded = ImageDecoder.decodeDrawable(ImageDecoder.createSource(staging)) { decoder, info, _ ->
                mimeType = info.mimeType
                scaleDown(decoder, info.size.width, info.size.height, MAX_STILL_DIMENSION)
            }

            val stamp = System.currentTimeMillis()
            return if (decoded is AnimatedImageDrawable) {
                val extension = if (mimeType == "image/webp") "webp" else "gif"
                val target = File(directory, "ex${exerciseId}_$stamp.$extension")
                if (!staging.renameTo(target)) return ImportResult.Unsupported
                ImportResult.Saved(target.name)
            } else {
                val bitmap = (decoded as? BitmapDrawable)?.bitmap ?: return ImportResult.Unsupported
                val keepsAlpha = bitmap.hasAlpha()
                val target = File(directory, "ex${exerciseId}_$stamp.${if (keepsAlpha) "png" else "jpg"}")
                FileOutputStream(target).use { output ->
                    if (keepsAlpha) bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    else bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                }
                ImportResult.Saved(target.name)
            }
        } catch (e: Exception) {
            return ImportResult.Unsupported
        } finally {
            staging.delete()
        }
    }

    /**
     * Decodes a stored file for display, no larger than [maxPx] on its longest side. Animated files come back as
     * an [AnimatedImageDrawable] (not started). Blocking: call from a background dispatcher.
     */
    fun decode(context: Context, fileName: String, maxPx: Int): Drawable? {
        val file = fileFor(context, fileName) ?: return null
        return try {
            ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) { decoder, info, _ ->
                scaleDown(decoder, info.size.width, info.size.height, maxPx)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(decoder: ImageDecoder, width: Int, height: Int, maxPx: Int) {
        val longest = max(width, height)
        if (longest > maxPx) {
            val scale = maxPx.toFloat() / longest
            decoder.setTargetSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1)
            )
        }
    }
}

/** A short note about a media pick, shown once as a toast. [arg] fills a `%1$d` in the string. */
data class ExerciseMediaMessage(@StringRes val resId: Int, val arg: Int? = null)

/** What to tell the user about this import, or null when it worked. */
fun ExerciseMediaStorage.ImportResult.toMessage(): ExerciseMediaMessage? = when (this) {
    is ExerciseMediaStorage.ImportResult.Saved -> null
    ExerciseMediaStorage.ImportResult.TooLarge ->
        ExerciseMediaMessage(R.string.exercise_media_too_large, ExerciseMediaStorage.MAX_BYTES_LABEL_MB)
    ExerciseMediaStorage.ImportResult.Unsupported -> ExerciseMediaMessage(R.string.exercise_media_unsupported)
}

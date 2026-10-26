package com.emanuel5014.trainable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** Import a bounded, still image into private storage; no lasting picker URI is required. */
object WallpaperStorage {
    const val DIR_NAME = "appearance"
    fun directory(context: Context) = File(context.filesDir, DIR_NAME).apply { mkdirs() }
    fun file(context: Context, name: String?): File? = name
        ?.takeIf { it.matches(Regex("wallpaper-[a-f0-9-]+\\.jpg")) }
        ?.let { File(directory(context), it) }

    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1f, 2048f / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(maxOf(1, (info.size.width * scale).roundToInt()), maxOf(1, (info.size.height * scale).roundToInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val destination = File(directory(context), "wallpaper-${UUID.randomUUID()}.jpg")
        try {
            destination.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            destination.name
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            bitmap.recycle()
        }
    }
}

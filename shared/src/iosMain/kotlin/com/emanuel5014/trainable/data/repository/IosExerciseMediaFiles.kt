@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.data.repository

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.max
import kotlin.math.min

/** A media name must stay inside its own directory, including when deleting a missing/corrupt backup entry. */
internal fun safeExerciseMediaName(name: String): Boolean =
    name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\\' !in name

fun iosExerciseMediaPath(name: String): String? {
    if (!safeExerciseMediaName(name)) return null
    return (documentsFolder("exercise_media") + "/" + name)
        .takeIf { NSFileManager.defaultManager.fileExistsAtPath(it) }
}

/** Photos picked on iOS. GIF/WebP keep their encoded frames; other images are stored as downscaled PNGs. */
class IosExerciseMediaFiles(private val directory: () -> String = { documentsFolder("exercise_media") }) : ExerciseMediaFiles {
    override fun import(source: String, exerciseId: Int): ExerciseMediaImport {
        val path = when {
            source.startsWith("file://") -> NSURL.URLWithString(source)?.path
            source.startsWith('/') -> source
            else -> null
        } ?: return ExerciseMediaImport.Unsupported
        return try { importFile(path, exerciseId) } finally {
            // Only staging files created by our picker belong to us; never remove a caller's source image.
            if (path.startsWith(NSTemporaryDirectory()) &&
                NSURL.fileURLWithPath(path).lastPathComponent?.startsWith("TrainableExercisePick_") == true) {
                NSFileManager.defaultManager.removeItemAtPath(path, null)
            }
        }
    }

    private fun importFile(path: String, exerciseId: Int): ExerciseMediaImport {
        val manager = NSFileManager.defaultManager
        val bytes = (manager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue
            ?: return ExerciseMediaImport.Unsupported
        if (bytes > 15L * 1024 * 1024) return ExerciseMediaImport.TooLarge
        val data = NSData.dataWithContentsOfFile(path) ?: return ExerciseMediaImport.Unsupported
        val image = UIImage.imageWithContentsOfFile(path) ?: return ExerciseMediaImport.Unsupported
        val size = image.size.useContents { width to height }
        if (size.first <= 0 || size.second <= 0) return ExerciseMediaImport.Unsupported
        val header = data.bytes?.reinterpret<ByteVar>()?.readBytes(minOf(12, data.length.toInt())) ?: byteArrayOf()
        val gif = header.size >= 6 && header.copyOfRange(0, 6).decodeToString() in setOf("GIF87a", "GIF89a")
        val webp = header.size >= 12 && header.copyOfRange(0, 4).decodeToString() == "RIFF" &&
            header.copyOfRange(8, 12).decodeToString() == "WEBP"
        val extension = when { gif -> "gif"; webp -> "webp"; else -> "png" }
        val name = "ex${exerciseId}_${NSUUID().UUIDString}.$extension"
        val target = directory() + "/" + name
        val encoded = if (gif || webp) data else {
            val ratio = min(1.0, 1280.0 / max(size.first, size.second))
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(size.first * ratio, size.second * ratio), false, 1.0)
            val scaled = try {
                image.drawInRect(CGRectMake(0.0, 0.0, size.first * ratio, size.second * ratio))
                UIGraphicsGetImageFromCurrentImageContext()
            } finally { UIGraphicsEndImageContext() }
            scaled?.let { UIImagePNGRepresentation(it) } ?: return ExerciseMediaImport.Unsupported
        }
        return if (encoded.writeToFile(target, atomically = true)) ExerciseMediaImport.Saved(name)
        else ExerciseMediaImport.Unsupported
    }

    override fun delete(fileName: String?) {
        if (fileName == null || !safeExerciseMediaName(fileName)) return
        NSFileManager.defaultManager.removeItemAtPath(directory() + "/" + fileName, null)
    }
}

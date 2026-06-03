package com.emanuel5014.trainable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageUtils {

    fun encodeImageToBase64(context: Context, uriString: String): String? {
        return try {
            val uri = if (uriString.startsWith("/")) {
                Uri.fromFile(File(uriString))
            } else {
                Uri.parse(uriString)
            }
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (originalBitmap == null) return null

            val rotatedBitmap = getRotatedBitmap(originalBitmap, uri, context)

            // Resize if too large
            val maxDimension = 1024
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val bitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (width > height) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (height > width) maxDimension else (maxDimension / ratio).toInt()
                Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)
            } else {
                rotatedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBase64Image(context: Context, base64String: String): String? {
        return try {
            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: return null

            val directory = File(context.filesDir, "routine_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getRotatedBitmap(bitmap: Bitmap, uri: Uri, context: Context): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }

            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    fun compressAndSaveImage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()
            val rotatedBitmap = getRotatedBitmap(originalBitmap, uri, context)
            compressAndSaveBitmap(context, rotatedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressAndSaveBitmap(context: Context, originalBitmap: Bitmap, preferredFileName: String? = null): String? {
        return try {
            // Resize if too large
            val maxDimension = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val bitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (width > height) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (height > width) maxDimension else (maxDimension / ratio).toInt()
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val directory = File(context.filesDir, "routine_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = preferredFileName ?: "routine_${System.currentTimeMillis()}.jpg"
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressExistingImages(context: Context) {
        val rootFiles = context.filesDir.listFiles() ?: return
        val routineImagesDir = File(context.filesDir, "routine_images")
        val routineImages = routineImagesDir.listFiles() ?: emptyArray()
        
        val allFiles = rootFiles + routineImages
        
        allFiles.forEach { file ->
            if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png"))) {
                // Check if it's already compressed (we can check if it's in routine_images and has a certain size/quality, 
                // but simpler to just try compressing if it's large)
                if (file.length() > 200 * 1024) { // Only compress if larger than 200KB
                    try {
                        var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            val exif = try {
                                ExifInterface(file.absolutePath)
                            } catch (e: Exception) { null }
                            val rotation = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }
                            if (rotation != 0f) {
                                val matrix = Matrix().apply { postRotate(rotation) }
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            }
                            val originalSize = file.length()
                            val newPath = compressAndSaveBitmap(context, bitmap, file.name)
                            if (newPath != null) {
                                val newFile = File(newPath)
                                // If the original was in root filesDir and the new one is in routine_images,
                                // we might need to be careful, but BackupManager and UriMigrationHelper 
                                // handle both. For safety, if we overwrite in-place or move to routine_images,
                                // we should ensure the database still points to something valid.
                                // UriMigrationHelper.fixPath handles finding the file in routine_images 
                                // even if the DB points to filesDir.
                                
                                // If the new file is smaller, we keep it. 
                                // If it's in a different location, we delete the old one.
                                if (file.absolutePath != newFile.absolutePath) {
                                    file.delete()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

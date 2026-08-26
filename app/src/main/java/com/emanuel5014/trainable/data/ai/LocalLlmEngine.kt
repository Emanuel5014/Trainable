package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LlmScanResult(
    val output: String,
    val thinking: String
)

@Singleton
class LocalLlmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var loadedPath: String? = null

    suspend fun ensureReady(modelFile: File) = withContext(Dispatchers.IO) {
        if (engine != null && loadedPath == modelFile.absolutePath && modelFile.exists()) return@withContext
        release()
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            visionBackend = Backend.GPU(),
            cacheDir = context.cacheDir.absolutePath
        )
        val newEngine = Engine(config)
        newEngine.initialize()
        engine = newEngine
        loadedPath = modelFile.absolutePath
        Unit
    }

    /**
     * Streams the model response for a routine sheet scan.
     * [onStreamUpdate] is invoked with (partialOutput, thinkingOutput) as chunks arrive.
     */
    suspend fun scanRoutineSheet(
        imageUri: Uri,
        prompt: String,
        onStreamUpdate: (String, String) -> Unit = { _, _ -> }
    ): LlmScanResult = withContext(Dispatchers.IO) {
        val activeEngine = engine ?: error("Engine not initialized")
        val tempImage = decodeScaledImage(imageUri)
        try {
            val conversation = activeEngine.createConversation()
            try {
                val output = StringBuilder()
                val thinking = StringBuilder()
                val completion = CompletableDeferred<Unit>()

                conversation.sendMessageAsync(
                    Contents.of(
                        Content.ImageFile(tempImage.absolutePath),
                        Content.Text(prompt)
                    ),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            message.contents.contents
                                .filterIsInstance<Content.Text>()
                                .forEach { output.append(it.text) }
                            message.channels[THINKING_CHANNEL]?.let { thinking.append(it) }
                            onStreamUpdate(output.toString(), thinking.toString())
                        }

                        override fun onDone() {
                            completion.complete(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            completion.completeExceptionally(throwable)
                        }
                    }
                )

                completion.await()
                LlmScanResult(
                    output = output.toString(),
                    thinking = thinking.toString()
                )
            } finally {
                conversation.close()
            }
        } finally {
            tempImage.delete()
        }
    }

    fun release() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
        loadedPath = null
    }

    private fun decodeScaledImage(imageUri: Uri): File {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_IMAGE_DIMENSION_PX) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("Could not read image")

        val scaled = if (maxOf(bitmap.width, bitmap.height) > MAX_IMAGE_DIMENSION_PX) {
            val scale = MAX_IMAGE_DIMENSION_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it != bitmap) bitmap.recycle() }
        } else bitmap

        val tempFile = File(context.cacheDir, "ai_scan_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        scaled.recycle()
        return tempFile
    }

    companion object {
        private const val MAX_IMAGE_DIMENSION_PX = 1280
        private const val THINKING_CHANNEL = "thinking"
    }
}

package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
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
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null

    suspend fun ensureReady(modelFile: File) = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            if (engine != null && loadedPath == modelFile.absolutePath && modelFile.exists()) return@withContext
            releaseInternal()
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
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Streams the model response for a routine sheet scan.
     * [onStreamUpdate] is invoked with (partialOutput, thinkingOutput) as chunks arrive.
     *
     * @param transcription true when the model is only copying text off the picture: sampling is made
     * (near) greedy so it doesn't invent characters, and the answer is capped at [maxOutputTokens].
     */
    suspend fun scanRoutineSheet(
        imageUri: Uri,
        prompt: String,
        imageMode: ScanImageMode = ScanImageMode.FULL_PAGE,
        transcription: Boolean = false,
        maxOutputTokens: Int? = null,
        onStreamUpdate: (String, String) -> Unit = { _, _ -> }
    ): LlmScanResult = withContext(Dispatchers.IO) {
        mutex.lock()
        val tempImage = try {
            ScanImageLoader.prepare(context, imageUri, imageMode)
        } catch (e: Throwable) {
            mutex.unlock()
            throw e
        }
        try {
            val activeEngine = engine ?: error("Engine not initialized")
            val conversation = activeEngine.createConversation(
                if (transcription) {
                    ConversationConfig(
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.1, seed = 0),
                        maxOutputToken = maxOutputTokens
                    )
                } else {
                    ConversationConfig()
                }
            )
            try {
                val output = StringBuilder()
                val thinking = StringBuilder()
                val completion = CompletableDeferred<Unit>()
                val isCancelled = java.util.concurrent.atomic.AtomicBoolean(false)

                conversation.sendMessageAsync(
                    Contents.of(
                        Content.ImageFile(tempImage.absolutePath),
                        Content.Text(prompt)
                    ),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            if (isCancelled.get()) return
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
                            if (!completion.isCompleted) {
                                completion.completeExceptionally(throwable)
                            }
                        }
                    }
                )

                try {
                    completion.await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    isCancelled.set(true)
                    // IMPORTANT: Do NOT close conversation or release engine synchronously while native
                    // C++ thread is actively running inference. Wait for native onDone/onError safely in NonCancellable block.
                    withContext(kotlinx.coroutines.NonCancellable) {
                        try {
                            completion.await()
                        } catch (_: Throwable) {
                        }
                        runCatching { conversation.close() }
                    }
                    throw e
                }

                LlmScanResult(
                    output = output.toString(),
                    thinking = thinking.toString()
                )
            } finally {
                runCatching { conversation.close() }
            }
        } finally {
            tempImage.delete()
            mutex.unlock()
        }
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        mutex.lock()
        try {
            releaseInternal()
        } finally {
            mutex.unlock()
        }
    }

    fun releaseNow() {
        releaseInternal()
    }

    private fun releaseInternal() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
        loadedPath = null
    }

    companion object {
        private const val THINKING_CHANNEL = "thinking"
    }
}

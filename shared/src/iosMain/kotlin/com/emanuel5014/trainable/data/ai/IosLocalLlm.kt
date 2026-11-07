package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.repository.toNSData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One on-device generation started by [IosLocalLlm]; [cancel] stops it, and its onFinished then reports [CANCELED]. */
interface IosLocalLlmRun {
    fun cancel()
}

/**
 * Gemma on the iPhone through LiteRT-LM. Its API is Swift only, so the Xcode app implements this
 * (iosApp/LocalLlmService.swift) and installs it in [IosLocalLlmBridge] at launch, like the CryptoKit bridge.
 * Only E2B runs on iPhone (A5: E4B failed every configuration on an iPhone 15 Pro): decoder on the CPU, vision on the
 * GPU, the near-greedy transcription sampler — the one configuration that read a sheet on the device.
 */
interface IosLocalLlm {
    /**
     * Loads [modelPath] (again only when the path changes; [cacheDir] keeps LiteRT-LM's compiled caches), then streams
     * the answer to [imageJpeg] (optional) and [prompt]. Callbacks may come on any thread, one at a time: [onChunk] with
     * each new piece of text, then [onFinished] once with null (completed), [CANCELED], or an error message.
     */
    fun generate(
        modelPath: String,
        cacheDir: String,
        imageJpeg: NSData?,
        prompt: String,
        maxOutputTokens: Int,
        onChunk: (String) -> Unit,
        onFinished: (String?) -> Unit
    ): IosLocalLlmRun

    /** Frees the loaded model (the next generate loads it again). Not "release": that is NSObject's own method. */
    fun unload()

    companion object {
        const val CANCELED = "canceled"
    }
}

/** Set by the Swift app before the Compose UI starts; null in tests and before installation. */
object IosLocalLlmBridge {
    var engine: IosLocalLlm? = null
}

/** The model failed to load or to answer; [message] is LiteRT-LM's own. */
class IosLocalLlmException(message: String) : Exception(message)

/** [IosLocalLlm] as a suspend call: the full answer, with [onStream] getting the text so far; cancelling stops the model. */
class IosLocalLlmEngine(private val bridge: () -> IosLocalLlm? = { IosLocalLlmBridge.engine }) {

    val isInstalled: Boolean get() = bridge() != null

    suspend fun generate(
        modelPath: String,
        cacheDir: String,
        imageJpeg: ByteArray?,
        prompt: String,
        maxOutputTokens: Int,
        onStream: (String) -> Unit = {}
    ): String {
        val engine = bridge() ?: throw IosLocalLlmException("On-device AI is not installed")
        val text = AtomicReference("")
        return suspendCancellableCoroutine { continuation ->
            val run = engine.generate(
                modelPath = modelPath,
                cacheDir = cacheDir,
                imageJpeg = imageJpeg?.toNSData(),
                prompt = prompt,
                maxOutputTokens = maxOutputTokens,
                onChunk = { piece ->
                    val so = text.value + piece
                    text.value = so
                    onStream(so)
                },
                onFinished = { error ->
                    if (!continuation.isActive) return@generate
                    when (error) {
                        null -> continuation.resume(text.value)
                        IosLocalLlm.CANCELED -> continuation.cancel(CancellationException("On-device AI canceled"))
                        else -> continuation.resumeWithException(IosLocalLlmException(error))
                    }
                }
            )
            continuation.invokeOnCancellation { run.cancel() }
        }
    }

    fun release() {
        bridge()?.unload()
    }
}

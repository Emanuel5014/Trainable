package com.emanuel5014.trainable.data.ai.backend

import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.LocalLlmEngine
import com.emanuel5014.trainable.data.ai.ModelFileManager

/** The on-device Gemma model, through the engine that already runs it. */
class LocalScanBackend(
    private val engine: LocalLlmEngine,
    private val modelFileManager: ModelFileManager,
    private val variant: AiModelVariant
) : ScanBackend {

    override val isLocal: Boolean = true

    override suspend fun prepare() {
        if (!modelFileManager.isDownloaded(variant)) throw ScanBackendException.ModelMissing()
        engine.ensureReady(modelFileManager.getModelFile(variant))
    }

    override suspend fun run(request: ScanRequest, onStreamUpdate: (String, String) -> Unit): LlmScanResult =
        engine.scanRoutineSheet(
            imageUri = request.imageUri,
            prompt = request.prompt,
            imageMode = request.imageMode,
            transcription = request.transcription,
            maxOutputTokens = request.maxOutputTokens,
            onStreamUpdate = onStreamUpdate
        )

    override suspend fun release() = engine.release()
}

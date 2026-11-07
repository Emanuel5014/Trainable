package com.emanuel5014.trainable.data.ai.remote

import android.net.Uri
import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackend
import com.emanuel5014.trainable.data.ai.backend.ScanRequest
import okhttp3.Call

/**
 * Reads the photo with a remote model ([RemoteScanSession], shared with iOS). [apiKey] is already decrypted (null when
 * the profile has none) and [keyState] says whether a stored key could be; [prepareImage] turns the photo into base64 JPEG.
 */
class RemoteScanBackend(
    profile: AiProfile,
    apiKey: String?,
    keyState: KeyState,
    factory: Call.Factory,
    private val prepareImage: suspend (Uri) -> String
) : ScanBackend {

    override val isLocal: Boolean = false

    private val session = RemoteScanSession(profile, apiKey, keyState, OkHttpAiHttp(factory))

    override suspend fun prepare() = session.prepare()

    override suspend fun run(request: ScanRequest, onStreamUpdate: (String, String) -> Unit): LlmScanResult =
        session.run(
            RemoteScanPayload(
                prompt = request.prompt,
                imageBase64 = prepareImage(request.imageUri),
                transcription = request.transcription,
                maxOutputTokens = request.maxOutputTokens
            ),
            onStreamUpdate
        )

    override suspend fun release() = Unit
}

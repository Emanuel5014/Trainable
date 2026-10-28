package com.emanuel5014.trainable.data.ai.remote

import android.net.Uri
import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackend
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.ai.backend.ScanRequest
import okhttp3.Call
import okhttp3.Response

fun createProtocolClient(profile: AiProfile, apiKey: String?): ProtocolClient = when (profile.protocol) {
    AiProtocol.OPENAI_COMPAT -> OpenAiCompatClient(profile.baseUrl, apiKey, profile.model)
    AiProtocol.ANTHROPIC -> AnthropicClient(profile.baseUrl, apiKey.orEmpty(), profile.model)
}

/**
 * Reads the photo with a remote model. [apiKey] is already decrypted (null when the profile has none) and
 * [keyState] says whether a stored key could be; [prepareImage] turns the photo into base64 JPEG.
 */
class RemoteScanBackend(
    private val profile: AiProfile,
    private val apiKey: String?,
    private val keyState: KeyState,
    factory: Call.Factory,
    private val prepareImage: suspend (Uri) -> String
) : ScanBackend {

    override val isLocal: Boolean = false

    private val http = RemoteHttp(factory)
    private val client: ProtocolClient by lazy { createProtocolClient(profile, apiKey) }

    override suspend fun prepare() {
        val missing = profile.baseUrl.isBlank() || profile.model.isBlank() ||
            (profile.protocol == AiProtocol.ANTHROPIC && profile.encryptedKey.isNullOrBlank())
        if (missing) throw ScanBackendException.NotConfigured()
        (BaseUrlPolicy.check(profile.baseUrl) as? UrlCheck.Rejected)?.let { throw ScanBackendException.PolicyBlocked(it.reason) }
        if (keyState == KeyState.UNREADABLE) throw ScanBackendException.KeyUnreadable()
        // A key a header can't carry is as good as lost: ask for it again rather than let OkHttp throw with it in the message
        if (apiKey != null && !AiProfileList.isValidKey(apiKey)) throw ScanBackendException.KeyUnreadable()
    }

    override suspend fun run(request: ScanRequest, onStreamUpdate: (String, String) -> Unit): LlmScanResult {
        val payload = RemoteScanPayload(
            prompt = request.prompt,
            imageBase64 = prepareImage(request.imageUri),
            transcription = request.transcription,
            maxOutputTokens = request.maxOutputTokens
        )
        var quirks = RequestQuirks()
        var retries = 0
        while (true) {
            val request = try {
                client.buildScanRequest(payload, quirks)
            } catch (_: IllegalArgumentException) {
                // The address or a header value is unusable; such an exception's text can carry the value itself
                throw ScanBackendException.NotConfigured()
            }
            val attempt = http.call(request) { response -> read(response, onStreamUpdate) }
            when (attempt) {
                is Attempt.Done -> return attempt.result
                is Attempt.Failed -> {
                    // Some providers name one refused parameter per 400, so each retry drops what was just refused
                    val next = if (retries >= MAX_RETRIES) null else client.retryQuirks(attempt.code, attempt.body, quirks)
                    if (next == null) throw client.mapError(attempt.code, attempt.body)
                    quirks = next
                    retries++
                }
            }
        }
    }

    override suspend fun release() = Unit

    private fun read(response: Response, onStreamUpdate: (String, String) -> Unit): Attempt {
        if (!response.isSuccessful) return Attempt.Failed(response.code, response.peekBody(ERROR_BODY_BYTES).string())
        val body = response.body
        val type = body.contentType()?.toString()?.lowercase().orEmpty()
        return when {
            "text/event-stream" in type -> Attempt.Done(client.readStream(body.source(), onStreamUpdate))
            "json" in type -> Attempt.Done(client.readWhole(body.string()))
            else -> throw ScanBackendException.BadResponse("Unexpected content type: $type")
        }
    }

    private sealed interface Attempt {
        data class Done(val result: LlmScanResult) : Attempt
        data class Failed(val code: Int, val body: String) : Attempt
    }

    private companion object {
        const val ERROR_BODY_BYTES = 4096L
        const val MAX_RETRIES = 2
    }
}

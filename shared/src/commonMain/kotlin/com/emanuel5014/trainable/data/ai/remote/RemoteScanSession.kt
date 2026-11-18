package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException

fun createProtocolClient(profile: AiProfile, apiKey: String?): ProtocolClient = when (profile.protocol) {
    AiProtocol.OPENAI_COMPAT -> OpenAiCompatClient(profile.baseUrl, apiKey, profile.model)
    AiProtocol.ANTHROPIC -> AnthropicClient(profile.baseUrl, apiKey.orEmpty(), profile.model)
}

/**
 * Reads a photo with a remote model, the same on every platform. [apiKey] is already decrypted (null when the profile
 * has none) and [keyState] says whether a stored key could be. Each platform's scan backend wraps this one.
 */
class RemoteScanSession(
    private val profile: AiProfile,
    private val apiKey: String?,
    private val keyState: KeyState,
    private val http: AiHttp
) {
    private val client: ProtocolClient by lazy { createProtocolClient(profile, apiKey) }

    /** Throws [ScanBackendException] when the profile can't be used as it is. */
    fun prepare() {
        val missing = profile.baseUrl.isBlank() || profile.model.isBlank() ||
            (requiresKey(profile.protocol, profile.baseUrl) && profile.encryptedKey.isNullOrBlank())
        if (missing) throw ScanBackendException.NotConfigured()
        (BaseUrlPolicy.check(profile.baseUrl) as? UrlCheck.Rejected)?.let { throw ScanBackendException.PolicyBlocked(it.reason) }
        if (keyState == KeyState.UNREADABLE) throw ScanBackendException.KeyUnreadable()
        // A key a header can't carry is as good as lost: ask for it again rather than let the client throw with it in the message
        if (apiKey != null && !AiProfileList.isValidKey(apiKey)) throw ScanBackendException.KeyUnreadable()
    }

    /** Sends [payload]; [onStreamUpdate] gets the cumulative (output, thinking) text. Cancellable. */
    suspend fun run(payload: RemoteScanPayload, onStreamUpdate: (String, String) -> Unit): LlmScanResult {
        var quirks = RequestQuirks()
        var retries = 0
        while (true) {
            val attempt = try {
                http.call(client.buildScanRequest(payload, quirks)) { response -> read(response, onStreamUpdate) }
            } catch (_: AiRequestUnusable) {
                // The address or a header value is unusable; such an exception's text can carry the value itself
                throw ScanBackendException.NotConfigured()
            }
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

    private fun read(response: AiResponse, onStreamUpdate: (String, String) -> Unit): Attempt {
        if (!response.isSuccessful) return Attempt.Failed(response.code, response.peekBody(ERROR_BODY_BYTES))
        val type = response.contentType?.lowercase().orEmpty()
        return when {
            "text/event-stream" in type -> Attempt.Done(client.readStream(response.source, onStreamUpdate))
            "json" in type -> Attempt.Done(client.readWhole(response.bodyString()))
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

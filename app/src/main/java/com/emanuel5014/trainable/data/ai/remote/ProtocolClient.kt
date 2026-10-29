package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import okhttp3.Request
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject

/** A runaway answer is cut off here rather than filling the phone's memory. */
const val MAX_REMOTE_OUTPUT_CHARS = 100_000

const val DEFAULT_MAX_OUTPUT_TOKENS = 4096

/** Same near-greedy sampling the on-device transcription uses. */
const val TRANSCRIPTION_TEMPERATURE = 0.1

/** One photo and the instructions for reading it, before a protocol turns them into a request. */
data class RemoteScanPayload(
    val prompt: String,
    val imageBase64: String,
    val transcription: Boolean,
    val maxOutputTokens: Int?
)

/** Parameters some providers refuse; set after a 400 that names them, for one retry. */
data class RequestQuirks(
    val omitTemperature: Boolean = false,
    val useMaxCompletionTokens: Boolean = false,
    val omitReasoningEffort: Boolean = false
)

/**
 * One wire protocol. Everything here is pure (no network): it builds requests and reads bytes, so it can be
 * tested with plain buffers. [RemoteHttp] does the calling.
 */
interface ProtocolClient {
    fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks = RequestQuirks()): Request

    /** Reads a streamed answer, calling [onUpdate] with the cumulative (output, thinking) text. */
    fun readStream(source: BufferedSource, onUpdate: (output: String, thinking: String) -> Unit): LlmScanResult

    /** Reads an answer that came back as one JSON document instead of a stream. */
    fun readWhole(body: String): LlmScanResult

    fun mapError(code: Int, body: String): ScanBackendException

    /** What to change to retry a 400 once, or null when there is nothing worth retrying. */
    fun retryQuirks(code: Int, body: String, current: RequestQuirks): RequestQuirks?

    fun buildModelsRequest(): Request
    fun parseModels(body: String): List<String>

    /** A tiny text-only request, to check the address, the key and the model. */
    fun buildPingRequest(quirks: RequestQuirks = RequestQuirks()): Request
}

object ErrorDetail {
    const val MAX = 200

    /**
     * The provider's own message (the `error.message` of a JSON body, else the raw text), with every
     * occurrence of [secret] replaced and cut to [MAX] characters. Null when there is nothing to show.
     */
    fun clean(raw: String?, secret: String?): String? {
        val text = (raw?.let(::messageOf) ?: raw)?.trim().orEmpty()
        if (text.isEmpty()) return null
        val redacted = if (secret.isNullOrBlank()) text else text.replace(secret.trim(), "••••")
        return redacted.take(MAX)
    }

    private fun messageOf(raw: String): String? {
        // Gemini wraps its error in an array: [{"error": {...}}]
        val json = try {
            if (raw.trimStart().startsWith("[")) JSONArray(raw).optJSONObject(0) else JSONObject(raw)
        } catch (_: Exception) {
            null
        } ?: return null
        val message = when (val error = json.opt("error")) {
            is JSONObject -> error.stringOrEmpty("message")
            is String -> error
            else -> json.stringOrEmpty("message")
        }
        return message.takeIf { it.isNotBlank() }
    }
}

/** `optString` answers "null" for a JSON null; this answers "". */
internal fun JSONObject.stringOrEmpty(key: String): String = if (isNull(key)) "" else optString(key, "")

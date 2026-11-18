package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.BufferedSource

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
 * tested with plain buffers. [AiHttp] does the calling.
 */
interface ProtocolClient {
    fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks = RequestQuirks()): AiRequest

    /** Reads a streamed answer, calling [onUpdate] with the cumulative (output, thinking) text. */
    fun readStream(source: BufferedSource, onUpdate: (output: String, thinking: String) -> Unit): LlmScanResult

    /** Reads an answer that came back as one JSON document instead of a stream. */
    fun readWhole(body: String): LlmScanResult

    fun mapError(code: Int, body: String): ScanBackendException

    /** What to change to retry a 400 once, or null when there is nothing worth retrying. */
    fun retryQuirks(code: Int, body: String, current: RequestQuirks): RequestQuirks?

    fun buildModelsRequest(): AiRequest
    fun parseModels(body: String): List<String>

    /** A tiny text-only request, to check the address, the key and the model. */
    fun buildPingRequest(quirks: RequestQuirks = RequestQuirks()): AiRequest
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
        val json = if (raw.trimStart().startsWith("[")) (parseJson(raw) as? JsonArray)?.objAt(0) else parseObject(raw)
        json ?: return null
        val error = json["error"]
        val message = when {
            error is JsonObject -> error.stringOrEmpty("message")
            error is JsonPrimitive && error.isString -> error.content
            else -> json.stringOrEmpty("message")
        }
        return message.takeIf { it.isNotBlank() }
    }
}

// Reading JSON the way the org.json code it replaces did: a missing or mistyped value is simply absent

private val lenientJson = Json { isLenient = true }

/** The parsed document, or null when [text] is not JSON. */
internal fun parseJson(text: String): JsonElement? = try {
    lenientJson.parseToJsonElement(text)
} catch (_: Exception) {
    null
}

/** [text] as a JSON object, or null when it is not one. */
internal fun parseObject(text: String): JsonObject? = parseJson(text) as? JsonObject

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
internal fun JsonArray.objAt(index: Int): JsonObject? = getOrNull(index) as? JsonObject

/** Like org.json's `optString` but "" for a JSON null: a number or boolean as its text, anything else as JSON. */
internal fun JsonObject.stringOrEmpty(key: String): String = when (val value = this[key]) {
    null, JsonNull -> ""
    is JsonPrimitive -> value.content
    else -> value.toString()
}

/** Whether [key] is missing or a JSON null (org.json's `isNull`). */
internal fun JsonObject.isNull(key: String): Boolean = this[key].let { it == null || it == JsonNull }

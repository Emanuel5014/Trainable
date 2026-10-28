package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Claude's native Messages API. [baseUrl] has no version segment (`https://api.anthropic.com`). */
class AnthropicClient(
    baseUrl: String,
    private val apiKey: String,
    private val model: String
) : ProtocolClient {

    private val base = BaseUrlPolicy.normalize(baseUrl)
    private val jsonType = "application/json".toMediaType()

    override fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks): Request {
        val image = JSONObject().put("type", "image").put(
            "source",
            JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", payload.imageBase64)
        )
        val text = JSONObject().put("type", "text").put("text", payload.prompt)
        val message = JSONObject().put("role", "user").put("content", JSONArray().put(image).put(text))
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", payload.maxOutputTokens ?: DEFAULT_MAX_OUTPUT_TOKENS)
            .put("stream", true)
            .put("messages", JSONArray().put(message))
        if (payload.transcription && !quirks.omitTemperature) body.put("temperature", TRANSCRIPTION_TEMPERATURE)
        return post("$base/v1/messages", body)
    }

    override fun readStream(source: BufferedSource, onUpdate: (output: String, thinking: String) -> Unit): LlmScanResult {
        val output = StringBuilder()
        var sawEvent = false
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) continue
            val payload = line.substring("data:".length).trim()
            if (payload.isEmpty()) continue
            val json = try {
                JSONObject(payload)
            } catch (_: JSONException) {
                continue
            }
            val type = json.stringOrEmpty("type")
            if (type.isEmpty()) continue
            sawEvent = true
            when (type) {
                "error" -> throw streamError(json)
                "message_stop" -> break
                "content_block_delta" -> {
                    val delta = json.optJSONObject("delta") ?: continue
                    if (delta.stringOrEmpty("type") != "text_delta") continue
                    output.append(delta.stringOrEmpty("text"))
                    if (output.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
                    onUpdate(output.toString(), "")
                }
            }
        }
        if (!sawEvent) throw ScanBackendException.BadResponse("Empty stream")
        return LlmScanResult(output.toString(), "")
    }

    override fun readWhole(body: String): LlmScanResult {
        val json = try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw ScanBackendException.BadResponse("Not JSON")
        }
        if (json.stringOrEmpty("type") == "error") throw streamError(json)
        val blocks = json.optJSONArray("content") ?: throw ScanBackendException.BadResponse("No content")
        val output = StringBuilder()
        for (index in 0 until blocks.length()) {
            val block = blocks.optJSONObject(index) ?: continue
            if (block.stringOrEmpty("type") == "text") output.append(block.stringOrEmpty("text"))
        }
        if (output.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
        return LlmScanResult(output.toString(), "")
    }

    override fun mapError(code: Int, body: String): ScanBackendException {
        val detail = ErrorDetail.clean(body, apiKey)
        return when {
            code == 401 || code == 403 -> ScanBackendException.Unauthorized(detail)
            // A bare 404 is a wrong path, not an unknown model
            code == 404 -> if ("model" in body.lowercase()) ScanBackendException.ModelNotFound(detail) else ScanBackendException.Rejected(code, detail)
            code == 413 -> ScanBackendException.PayloadTooLarge(detail)
            code == 429 -> ScanBackendException.RateLimited(detail)
            code >= 500 -> ScanBackendException.Server(code, detail)
            else -> ScanBackendException.Rejected(code, detail)
        }
    }

    override fun retryQuirks(code: Int, body: String, current: RequestQuirks): RequestQuirks? {
        if (code != 400 || current.omitTemperature || "temperature" !in body.lowercase()) return null
        return current.copy(omitTemperature = true)
    }

    override fun buildModelsRequest(): Request = authorized(Request.Builder().url("$base/v1/models")).get().build()

    override fun parseModels(body: String): List<String> = try {
        val data = JSONObject(body).optJSONArray("data")
        if (data == null) emptyList() else (0 until data.length()).mapNotNull { index ->
            data.optJSONObject(index)?.stringOrEmpty("id")?.takeIf { it.isNotBlank() }
        }
    } catch (_: JSONException) {
        emptyList()
    }

    override fun buildPingRequest(quirks: RequestQuirks): Request {
        val message = JSONObject().put("role", "user").put("content", "Reply with OK")
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", PING_MAX_TOKENS)
            .put("stream", false)
            .put("messages", JSONArray().put(message))
        return post("$base/v1/messages", body)
    }

    private fun streamError(json: JSONObject): ScanBackendException.StreamError {
        val message = json.optJSONObject("error")?.stringOrEmpty("message").orEmpty()
        return ScanBackendException.StreamError(ErrorDetail.clean(message, apiKey))
    }

    private fun post(url: String, body: JSONObject): Request =
        authorized(Request.Builder().url(url)).post(body.toString().toRequestBody(jsonType)).build()

    private fun authorized(builder: Request.Builder): Request.Builder =
        builder.header("x-api-key", apiKey.trim()).header("anthropic-version", API_VERSION)

    private companion object {
        const val API_VERSION = "2023-06-01"
        const val PING_MAX_TOKENS = 8
    }
}

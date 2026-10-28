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

/**
 * The OpenAI chat-completions protocol, which OpenAI, OpenRouter, Groq, Ollama, LM Studio and llama.cpp's
 * server all speak. [baseUrl] includes the version segment (`https://api.openai.com/v1`).
 */
class OpenAiCompatClient(
    baseUrl: String,
    private val apiKey: String?,
    private val model: String
) : ProtocolClient {

    private val base = BaseUrlPolicy.normalize(baseUrl)
    private val jsonType = "application/json".toMediaType()

    override fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks): Request {
        val text = JSONObject().put("type", "text").put("text", payload.prompt)
        val image = JSONObject().put("type", "image_url")
            .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${payload.imageBase64}"))
        val message = JSONObject().put("role", "user").put("content", JSONArray().put(text).put(image))
        val body = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put(tokenField(quirks), payload.maxOutputTokens ?: DEFAULT_MAX_OUTPUT_TOKENS)
            .put("messages", JSONArray().put(message))
        if (payload.transcription && !quirks.omitTemperature) body.put("temperature", TRANSCRIPTION_TEMPERATURE)
        return post("$base/chat/completions", body)
    }

    override fun readStream(source: BufferedSource, onUpdate: (output: String, thinking: String) -> Unit): LlmScanResult {
        val output = StringBuilder()
        val thinking = StringBuilder()
        var sawData = false
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) continue
            val payload = line.substring("data:".length).trim()
            if (payload == "[DONE]") {
                sawData = true
                break
            }
            if (payload.isEmpty()) continue
            val json = try {
                JSONObject(payload)
            } catch (_: JSONException) {
                continue
            }
            sawData = true
            throwIfError(json)
            val delta = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
            output.append(delta.stringOrEmpty("content"))
            thinking.append(delta.stringOrEmpty("reasoning_content")).append(delta.stringOrEmpty("reasoning"))
            if (output.length + thinking.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
            onUpdate(output.toString(), thinking.toString())
        }
        if (!sawData) throw ScanBackendException.BadResponse("Empty stream")
        return LlmScanResult(output.toString(), thinking.toString())
    }

    override fun readWhole(body: String): LlmScanResult {
        val json = try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw ScanBackendException.BadResponse("Not JSON")
        }
        throwIfError(json)
        val message = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            ?: throw ScanBackendException.BadResponse("No message")
        val output = message.stringOrEmpty("content")
        val thinking = message.stringOrEmpty("reasoning_content") + message.stringOrEmpty("reasoning")
        if (output.length + thinking.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
        return LlmScanResult(output, thinking)
    }

    override fun mapError(code: Int, body: String): ScanBackendException {
        val detail = ErrorDetail.clean(body, apiKey)
        return when {
            code == 401 || code == 403 -> ScanBackendException.Unauthorized(detail)
            // A bare 404 is a wrong path (an address without /v1), not an unknown model
            code == 404 -> if ("model" in body.lowercase()) ScanBackendException.ModelNotFound(detail) else ScanBackendException.Rejected(code, detail)
            code == 413 -> ScanBackendException.PayloadTooLarge(detail)
            code == 429 -> ScanBackendException.RateLimited(detail)
            code >= 500 -> ScanBackendException.Server(code, detail)
            else -> ScanBackendException.Rejected(code, detail)
        }
    }

    override fun retryQuirks(code: Int, body: String, current: RequestQuirks): RequestQuirks? {
        if (code != 400) return null
        val text = body.lowercase()
        var next = current
        if ("temperature" in text && !next.omitTemperature) next = next.copy(omitTemperature = true)
        if ("max_tokens" in text && !next.useMaxCompletionTokens) next = next.copy(useMaxCompletionTokens = true)
        return next.takeIf { it != current }
    }

    override fun buildModelsRequest(): Request = authorized(Request.Builder().url("$base/models")).get().build()

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
            .put("stream", false)
            .put(tokenField(quirks), PING_MAX_TOKENS)
            .put("messages", JSONArray().put(message))
        return post("$base/chat/completions", body)
    }

    private fun tokenField(quirks: RequestQuirks) = if (quirks.useMaxCompletionTokens) "max_completion_tokens" else "max_tokens"

    private fun throwIfError(json: JSONObject) {
        if (json.isNull("error")) return
        val message = when (val error = json.opt("error")) {
            is JSONObject -> error.stringOrEmpty("message")
            else -> error.toString()
        }
        throw ScanBackendException.StreamError(ErrorDetail.clean(message, apiKey))
    }

    private fun post(url: String, body: JSONObject): Request =
        authorized(Request.Builder().url(url)).post(body.toString().toRequestBody(jsonType)).build()

    private fun authorized(builder: Request.Builder): Request.Builder {
        val key = apiKey?.trim()
        if (!key.isNullOrEmpty()) builder.header("Authorization", "Bearer $key")
        return builder
    }

    private companion object {
        const val PING_MAX_TOKENS = 8
    }
}

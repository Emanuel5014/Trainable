package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okio.BufferedSource

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
    private val gemini = GeminiEndpoint.matches(baseUrl)

    override fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks): AiRequest {
        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
            put(tokenField(quirks), tokenLimit(payload.maxOutputTokens ?: DEFAULT_MAX_OUTPUT_TOKENS))
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", payload.prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") { put("url", "data:image/jpeg;base64,${payload.imageBase64}") }
                        }
                    }
                }
            }
            // Gemini 3 asks to keep its default temperature: a low one can make it loop
            if (payload.transcription && !quirks.omitTemperature && !gemini) put("temperature", TRANSCRIPTION_TEMPERATURE)
            applyReasoning(quirks)
        }
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
            val json = parseObject(payload) ?: continue
            sawData = true
            throwIfError(json)
            val delta = json.arr("choices")?.objAt(0)?.obj("delta") ?: continue
            output.append(delta.stringOrEmpty("content"))
            thinking.append(delta.stringOrEmpty("reasoning_content")).append(delta.stringOrEmpty("reasoning"))
            if (output.length + thinking.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
            onUpdate(output.toString(), thinking.toString())
        }
        if (!sawData) throw ScanBackendException.BadResponse("Empty stream")
        return LlmScanResult(output.toString(), thinking.toString())
    }

    override fun readWhole(body: String): LlmScanResult {
        val json = parseObject(body) ?: throw ScanBackendException.BadResponse("Not JSON")
        throwIfError(json)
        val message = json.arr("choices")?.objAt(0)?.obj("message")
            ?: throw ScanBackendException.BadResponse("No message")
        val output = message.stringOrEmpty("content")
        val thinking = message.stringOrEmpty("reasoning_content") + message.stringOrEmpty("reasoning")
        if (output.length + thinking.length > MAX_REMOTE_OUTPUT_CHARS) throw ScanBackendException.BadResponse("Answer too long")
        return LlmScanResult(output, thinking)
    }

    override fun mapError(code: Int, body: String): ScanBackendException {
        val detail = ErrorDetail.clean(body, apiKey)
        return when {
            code == 401 || code == 403 || isRejectedKey(body) -> ScanBackendException.Unauthorized(detail)
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
        if ("reasoning_effort" in text && !next.omitReasoningEffort) next = next.copy(omitReasoningEffort = true)
        return next.takeIf { it != current }
    }

    override fun buildModelsRequest(): AiRequest = AiRequest.get("$base/models", authorization())

    override fun parseModels(body: String): List<String> {
        val data = parseObject(body)?.arr("data") ?: return emptyList()
        return data.indices.mapNotNull { index ->
            // Gemini lists "models/gemini-…"; the model field takes the bare name
            data.objAt(index)?.stringOrEmpty("id")?.removePrefix("models/")?.takeIf { it.isNotBlank() }
        }
    }

    override fun buildPingRequest(quirks: RequestQuirks): AiRequest {
        val body = buildJsonObject {
            put("model", model)
            put("stream", false)
            put(tokenField(quirks), tokenLimit(PING_MAX_TOKENS))
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", "Reply with OK")
                }
            }
            applyReasoning(quirks)
        }
        return post("$base/chat/completions", body)
    }

    private fun tokenField(quirks: RequestQuirks) = if (quirks.useMaxCompletionTokens) "max_completion_tokens" else "max_tokens"

    /** Gemini counts its thinking against the output limit, so the limit needs room for the answer too. */
    private fun tokenLimit(answerTokens: Int) = if (gemini) answerTokens + GEMINI_THINKING_ALLOWANCE else answerTokens

    /** Gemini always thinks; reading a sheet needs little of it, and `low` is the one level every Gemini model accepts. */
    private fun JsonObjectBuilder.applyReasoning(quirks: RequestQuirks) {
        if (gemini && !quirks.omitReasoningEffort) put("reasoning_effort", GEMINI_REASONING_EFFORT)
    }

    /** Gemini answers a wrong key with a 400 instead of a 401. */
    private fun isRejectedKey(body: String): Boolean {
        val text = body.lowercase()
        return "api key not valid" in text || "api_key_invalid" in text
    }

    private fun throwIfError(json: JsonObject) {
        if (json.isNull("error")) return
        val message = when (val error = json["error"]) {
            is JsonObject -> error.stringOrEmpty("message")
            is JsonPrimitive -> error.content
            else -> error.toString()
        }
        throw ScanBackendException.StreamError(ErrorDetail.clean(message, apiKey))
    }

    private fun post(url: String, body: JsonObject): AiRequest = AiRequest.postJson(url, authorization(), body.toString())

    private fun authorization(): List<Pair<String, String>> {
        val key = apiKey?.trim()
        return if (key.isNullOrEmpty()) emptyList() else listOf("Authorization" to "Bearer $key")
    }

    private companion object {
        const val PING_MAX_TOKENS = 8
        const val GEMINI_THINKING_ALLOWANCE = 2048
        const val GEMINI_REASONING_EFFORT = "low"
    }
}

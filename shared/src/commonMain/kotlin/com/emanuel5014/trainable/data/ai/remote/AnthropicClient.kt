package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okio.BufferedSource

/** Claude's native Messages API. [baseUrl] has no version segment (`https://api.anthropic.com`). */
class AnthropicClient(
    baseUrl: String,
    private val apiKey: String,
    private val model: String
) : ProtocolClient {

    private val base = BaseUrlPolicy.normalize(baseUrl)

    override fun buildScanRequest(payload: RemoteScanPayload, quirks: RequestQuirks): AiRequest {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", payload.maxOutputTokens ?: DEFAULT_MAX_OUTPUT_TOKENS)
            put("stream", true)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "image")
                            putJsonObject("source") {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", payload.imageBase64)
                            }
                        }
                        addJsonObject {
                            put("type", "text")
                            put("text", payload.prompt)
                        }
                    }
                }
            }
            if (payload.transcription && !quirks.omitTemperature) put("temperature", TRANSCRIPTION_TEMPERATURE)
        }
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
            val json = parseObject(payload) ?: continue
            val type = json.stringOrEmpty("type")
            if (type.isEmpty()) continue
            sawEvent = true
            when (type) {
                "error" -> throw streamError(json)
                "message_stop" -> break
                "content_block_delta" -> {
                    val delta = json.obj("delta") ?: continue
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
        val json = parseObject(body) ?: throw ScanBackendException.BadResponse("Not JSON")
        if (json.stringOrEmpty("type") == "error") throw streamError(json)
        val blocks = json.arr("content") ?: throw ScanBackendException.BadResponse("No content")
        val output = StringBuilder()
        for (index in blocks.indices) {
            val block = blocks.objAt(index) ?: continue
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

    override fun buildModelsRequest(): AiRequest = AiRequest.get("$base/v1/models", authorization())

    override fun parseModels(body: String): List<String> {
        val data = parseObject(body)?.arr("data") ?: return emptyList()
        return data.indices.mapNotNull { index ->
            data.objAt(index)?.stringOrEmpty("id")?.takeIf { it.isNotBlank() }
        }
    }

    override fun buildPingRequest(quirks: RequestQuirks): AiRequest {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", PING_MAX_TOKENS)
            put("stream", false)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", "Reply with OK")
                }
            }
        }
        return post("$base/v1/messages", body)
    }

    private fun streamError(json: JsonObject): ScanBackendException.StreamError {
        val message = json.obj("error")?.stringOrEmpty("message").orEmpty()
        return ScanBackendException.StreamError(ErrorDetail.clean(message, apiKey))
    }

    private fun post(url: String, body: JsonObject): AiRequest = AiRequest.postJson(url, authorization(), body.toString())

    private fun authorization(): List<Pair<String, String>> =
        listOf("x-api-key" to apiKey.trim(), "anthropic-version" to API_VERSION)

    private companion object {
        const val API_VERSION = "2023-06-01"
        const val PING_MAX_TOKENS = 8
    }
}

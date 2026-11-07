package com.emanuel5014.trainable.data.ai.remote

import okio.BufferedSource

/** One HTTP request to a remote AI, independent of the client that sends it. */
data class AiRequest(
    val method: String,
    val url: String,
    val headers: List<Pair<String, String>> = emptyList(),
    /** JSON text for a POST; null for a GET. */
    val body: String? = null
) {
    /** The last value of [name] (case-insensitive), like OkHttp's `Request.header`. */
    fun header(name: String): String? = headers.lastOrNull { it.first.equals(name, ignoreCase = true) }?.second

    companion object {

        fun get(url: String, headers: List<Pair<String, String>>) = AiRequest("GET", url, headers)
        fun postJson(url: String, headers: List<Pair<String, String>>, body: String) = AiRequest("POST", url, headers, body)
    }
}

/** The answer to an [AiRequest], read inside [AiHttp.call] only. */
interface AiResponse {
    val code: Int
    val isSuccessful: Boolean get() = code in 200..299

    /** The Content-Type header as sent, or null. */
    val contentType: String?

    /** The body as a stream (an SSE answer is read line by line from here). */
    val source: BufferedSource

    /** At most [byteCount] bytes of the body as text, for error messages. */
    fun peekBody(byteCount: Long): String

    /** The whole body as text. */
    fun bodyString(): String
}

/**
 * Sends requests to a remote AI: no overall deadline (the user cancels), redirects never followed, nothing logged.
 * Implementations close the call the moment the calling coroutine is cancelled.
 */
interface AiHttp {
    /**
     * Sends [request] and hands the response to [read], which may stream the body. A network failure becomes
     * [com.emanuel5014.trainable.data.ai.backend.ScanBackendException.Unreachable]; a request the client cannot even
     * build (a malformed address, a header value it can't carry) becomes [AiRequestUnusable], before anything is sent.
     */
    suspend fun <T> call(request: AiRequest, read: (AiResponse) -> T): T
}

/** The request could not be built (address or header value unusable). Its message never carries the value. */
class AiRequestUnusable : Exception("Request cannot be built")

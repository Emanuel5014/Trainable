package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Runs one OkHttp call off the main thread and closes it the moment the coroutine is cancelled. */
class OkHttpAiHttp(private val factory: Call.Factory) : AiHttp {

    override suspend fun <T> call(request: AiRequest, read: (AiResponse) -> T): T {
        val okRequest = toOkHttp(request)
        return coroutineScope {
            val call = factory.newCall(okRequest)
            val finished = AtomicBoolean(false)
            // Reading blocks a thread, so cancelling has to reach the call from outside
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!finished.get()) call.cancel()
                }
            }
            try {
                withContext(Dispatchers.IO) { call.execute().use { read(Wrapped(it)) } }
            } catch (e: IOException) {
                ensureActive()
                throw ScanBackendException.Unreachable(e)
            } finally {
                finished.set(true)
                watcher.cancel()
            }
        }
    }

    private class Wrapped(private val response: Response) : AiResponse {
        override val code: Int get() = response.code
        override val isSuccessful: Boolean get() = response.isSuccessful
        override val contentType: String? get() = response.body.contentType()?.toString()
        override val source: BufferedSource get() = response.body.source()
        override fun peekBody(byteCount: Long): String = response.peekBody(byteCount).string()
        override fun bodyString(): String = response.body.string()
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        /**
         * The OkHttp request the protocol clients used to build themselves. OkHttp refuses a malformed address or a
         * header value it can't carry with an exception whose text may hold that value, so it becomes [AiRequestUnusable].
         */
        fun toOkHttp(request: AiRequest): Request = try {
            val builder = Request.Builder().url(request.url)
            request.headers.forEach { (name, value) -> builder.header(name, value) }
            when (val body = request.body) {
                null -> builder.method(request.method, null)
                else -> builder.method(request.method, body.toRequestBody(JSON))
            }
            builder.build()
        } catch (_: IllegalArgumentException) {
            throw AiRequestUnusable()
        }
    }
}

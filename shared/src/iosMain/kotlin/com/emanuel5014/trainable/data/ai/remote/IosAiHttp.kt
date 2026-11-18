package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.data.repository.toNSData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSource
import okio.IOException
import okio.Source
import okio.Timeout
import okio.buffer
import platform.Foundation.NSCondition
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSData
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPShouldHandleCookies
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.NSObject
import kotlin.concurrent.AtomicInt

/**
 * The remote AI over NSURLSession, set up like Android's client (AiRemoteHttp): a slow upload and a model still loading
 * get room (120 s without data), no overall deadline (the user cancels instead), a redirect is never followed so a
 * key can't be carried to another host or down to plain http, no cache or cookies, nothing logged.
 */
class IosAiHttp : AiHttp {

    override suspend fun <T> call(request: AiRequest, read: (AiResponse) -> T): T {
        val nsRequest = toNSURLRequest(request)
        val exchange = Exchange(nsRequest)
        return coroutineScope {
            val finished = AtomicInt(0)
            // Reading blocks a thread, so cancelling has to reach the task from outside
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (finished.value == 0) exchange.cancel()
                }
            }
            try {
                withContext(Dispatchers.IO) {
                    exchange.start()
                    read(exchange.awaitResponse())
                }
            } catch (e: IOException) {
                ensureActive()
                throw ScanBackendException.Unreachable(e)
            } finally {
                finished.value = 1
                watcher.cancel()
                exchange.close()
            }
        }
    }

    companion object {
        /** OkHttp's header rule: a name of visible ASCII, a value of visible ASCII, space or tab. */
        private fun headerOk(name: String, value: String) =
            name.isNotEmpty() && name.all { it.code in 0x21..0x7E } && value.all { it == '\t' || it.code in 0x20..0x7E }

        /** The NSURLRequest for [request]; an address or header OkHttp would refuse becomes [AiRequestUnusable]. */
        @OptIn(BetaInteropApi::class)
        internal fun toNSURLRequest(request: AiRequest): NSURLRequest {
            AiUrl.parse(request.url) ?: throw AiRequestUnusable()
            val url = NSURL.URLWithString(request.url) ?: throw AiRequestUnusable()
            if (request.headers.any { (name, value) -> !headerOk(name, value) }) throw AiRequestUnusable()
            val ns = NSMutableURLRequest.requestWithURL(url, NSURLRequestReloadIgnoringLocalCacheData, REQUEST_IDLE_SECONDS)
            ns.setHTTPMethod(request.method)
            ns.setHTTPShouldHandleCookies(false)
            request.headers.forEach { (name, value) -> ns.setValue(value, forHTTPHeaderField = name) }
            request.body?.let { body ->
                ns.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")
                ns.setHTTPBody(NSString.create(string = body).dataUsingEncoding(NSUTF8StringEncoding))
            }
            return ns
        }

        private const val REQUEST_IDLE_SECONDS = 120.0
        private const val NO_OVERALL_DEADLINE_SECONDS = 7.0 * 24 * 3600
    }

    /** One request: the task, its delegate, and the bytes on their way from the delegate's queue to the reader. */
    private class Exchange(private val request: NSURLRequest) {
        private val condition = NSCondition()
        private val chunks = ArrayDeque<ByteArray>()
        private var response: NSHTTPURLResponse? = null
        private var done = false
        private var error: NSError? = null

        private val delegate = Delegate(this)
        private val session: NSURLSession = NSURLSession.sessionWithConfiguration(
            NSURLSessionConfiguration.ephemeralSessionConfiguration.apply {
                timeoutIntervalForRequest = REQUEST_IDLE_SECONDS
                timeoutIntervalForResource = NO_OVERALL_DEADLINE_SECONDS
                HTTPShouldSetCookies = false
                URLCache = null
            },
            delegate,
            NSOperationQueue().apply { maxConcurrentOperationCount = 1 }
        )
        private val task = session.dataTaskWithRequest(request)

        fun start() = task.resume()

        fun cancel() = task.cancel()

        /** Ends the session, which also lets go of the delegate it holds. */
        fun close() = session.invalidateAndCancel()

        /** Blocks until the status line and headers have arrived (or the request failed). */
        fun awaitResponse(): AiResponse {
            condition.lock()
            try {
                while (response == null && !done) condition.wait()
                val head = response ?: throw IOException(error?.localizedDescription ?: "No response")
                return Response(head, StreamSource(this).buffer())
            } finally {
                condition.unlock()
            }
        }

        /** Blocks until bytes arrive; -1 at the end of the body, an [IOException] if the transfer failed. */
        fun read(sink: Buffer, byteCount: Long): Long {
            condition.lock()
            try {
                while (chunks.isEmpty() && !done) condition.wait()
                if (chunks.isEmpty()) {
                    error?.let { throw IOException(it.localizedDescription) }
                    return -1
                }
                val chunk = chunks.removeFirst()
                val take = minOf(byteCount, chunk.size.toLong()).toInt()
                sink.write(chunk, 0, take)
                if (take < chunk.size) chunks.addFirst(chunk.copyOfRange(take, chunk.size))
                return take.toLong()
            } finally {
                condition.unlock()
            }
        }

        fun onResponse(head: NSHTTPURLResponse) = signal { response = head }
        fun onData(bytes: ByteArray) = signal { if (bytes.isNotEmpty()) chunks.addLast(bytes) }
        fun onComplete(failure: NSError?) = signal {
            error = failure
            done = true
        }

        private fun signal(change: () -> Unit) {
            condition.lock()
            try {
                change()
                condition.broadcast()
            } finally {
                condition.unlock()
            }
        }
    }

    private class StreamSource(private val exchange: Exchange) : Source {
        override fun read(sink: Buffer, byteCount: Long): Long = exchange.read(sink, byteCount)
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() = Unit
    }

    private class Response(private val head: NSHTTPURLResponse, override val source: BufferedSource) : AiResponse {
        override val code: Int = head.statusCode.toInt()
        override val contentType: String? = head.valueForHTTPHeaderField("Content-Type")

        override fun peekBody(byteCount: Long): String {
            val peek = source.peek()
            val buffer = Buffer()
            while (buffer.size < byteCount && peek.read(buffer, byteCount - buffer.size) != -1L) Unit
            return buffer.readUtf8()
        }

        override fun bodyString(): String = source.readUtf8()
    }

    private class Delegate(private val exchange: Exchange) : NSObject(), NSURLSessionDataDelegateProtocol {
        override fun URLSession(
            session: NSURLSession,
            dataTask: NSURLSessionDataTask,
            didReceiveResponse: NSURLResponse,
            completionHandler: (NSURLSessionResponseDisposition) -> Unit
        ) {
            (didReceiveResponse as? NSHTTPURLResponse)?.let(exchange::onResponse)
            completionHandler(NSURLSessionResponseAllow)
        }

        override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
            exchange.onData(didReceiveData.toByteArray())
        }

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            exchange.onComplete(didCompleteWithError)
        }

        /** Never follow: the 3xx itself becomes the answer, as with OkHttp's followRedirects(false). */
        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            willPerformHTTPRedirection: NSHTTPURLResponse,
            newRequest: NSURLRequest,
            completionHandler: (NSURLRequest?) -> Unit
        ) {
            completionHandler(null)
        }
    }
}

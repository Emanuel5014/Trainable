@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.remote.nextcloud

import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.data.repository.toNSData
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.IOException
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.downloadTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.uploadTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.Foundation.setHTTPShouldHandleCookies
import platform.Foundation.setValue
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Nextcloud over NSURLSession, held to Android's rules (OkHttpWebDavHttp + CleartextGuard): https only, a redirect is
 * followed only when it keeps the scheme (never https→http), 120 s without data, no cache or cookies. [wifiOnly] keeps
 * the call off mobile data and expensive networks. Network failures throw with the system's message.
 */
class IosWebDavHttp(private val wifiOnly: Boolean = false) : WebDavHttp {

    override suspend fun send(request: WebDavRequest): WebDavResponse {
        val url = NSURL.URLWithString(request.url) ?: throw IOException("Invalid URL")
        if (url.scheme?.lowercase() == "http") {
            throw IllegalStateException("CLEARTEXT communication to ${url.host} not permitted by network security policy")
        }
        val ns = NSMutableURLRequest.requestWithURL(url, NSURLRequestReloadIgnoringLocalCacheData, IDLE_SECONDS)
        ns.setHTTPMethod(request.method)
        ns.setHTTPShouldHandleCookies(false)
        request.headers.forEach { (name, value) -> ns.setValue(value, forHTTPHeaderField = name) }
        val config = NSURLSessionConfiguration.ephemeralSessionConfiguration.apply {
            timeoutIntervalForRequest = IDLE_SECONDS
            timeoutIntervalForResource = 7.0 * 24 * 3600
            HTTPShouldSetCookies = false
            URLCache = null
            allowsCellularAccess = !wifiOnly
            allowsExpensiveNetworkAccess = !wifiOnly
        }
        val session = NSURLSession.sessionWithConfiguration(config, SameSchemeRedirects(), null)
        try {
            return suspendCancellableCoroutine { continuation ->
                fun finish(data: NSData?, response: NSURLResponse?, error: NSError?) {
                    val http = response as? NSHTTPURLResponse
                    when {
                        error != null -> continuation.resumeWithException(IOException(error.localizedDescription))
                        http == null -> continuation.resumeWithException(IOException("No response"))
                        else -> continuation.resume(WebDavResponse(http.statusCode.toInt(),
                            NSHTTPURLResponse.localizedStringForStatusCode(http.statusCode), data?.toByteArray()?.decodeToString()))
                    }
                }
                val body = request.body
                val target = request.downloadTo
                val task: NSURLSessionTask = when {
                    body is WebDavBody.FilePath -> {
                        ns.setValue(body.contentType, forHTTPHeaderField = "Content-Type")
                        session.uploadTaskWithRequest(ns, fromFile = NSURL.fileURLWithPath(body.path),
                            completionHandler = { d: NSData?, r: NSURLResponse?, e: NSError? -> finish(d, r, e) })
                    }
                    target != null -> session.downloadTaskWithRequest(ns, completionHandler = { location: NSURL?, r: NSURLResponse?, e: NSError? ->
                        val code = (r as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                        if (e == null && location != null && code in 200..299) {
                            val files = NSFileManager.defaultManager
                            files.removeItemAtPath(target, null)
                            if (!files.moveItemAtURL(location, NSURL.fileURLWithPath(target), null)) {
                                continuation.resumeWithException(IOException("Could not save the download"))
                                return@downloadTaskWithRequest
                            }
                        }
                        finish(null, r, e)
                    })
                    else -> {
                        when (body) {
                            is WebDavBody.Text -> {
                                ns.setValue(body.contentType, forHTTPHeaderField = "Content-Type")
                                ns.setHTTPBody(body.text.encodeToByteArray().toNSData())
                            }
                            else -> if (request.method != "GET" && request.method != "DELETE") ns.setHTTPBody(NSData())
                        }
                        session.dataTaskWithRequest(ns, completionHandler = { d: NSData?, r: NSURLResponse?, e: NSError? -> finish(d, r, e) })
                    }
                }
                continuation.invokeOnCancellation { task.cancel() }
                task.resume()
            }
        } finally {
            session.finishTasksAndInvalidate()
        }
    }

    /** Like OkHttp with followSslRedirects(false): same-scheme hops only; otherwise the 3xx itself is the answer. */
    private class SameSchemeRedirects : NSObject(), NSURLSessionTaskDelegateProtocol {
        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, willPerformHTTPRedirection: NSHTTPURLResponse,
                                newRequest: NSURLRequest, completionHandler: (NSURLRequest?) -> Unit) {
            val from = task.originalRequest?.URL?.scheme?.lowercase()
            completionHandler(if (from != null && newRequest.URL?.scheme?.lowercase() == from) newRequest else null)
        }
    }

    private companion object {
        const val IDLE_SECONDS = 120.0
    }
}

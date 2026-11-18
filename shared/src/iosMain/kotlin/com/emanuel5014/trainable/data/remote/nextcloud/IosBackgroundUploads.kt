@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.remote.nextcloud

import com.emanuel5014.trainable.data.backup.BackgroundUploadSession
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.Foundation.NSURLSessionTaskStateRunning
import platform.Foundation.NSURLSessionTaskStateSuspended
import platform.Foundation.NSUserDomainMask
import platform.Foundation.setValue
import platform.Foundation.uploadTaskWithRequest
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * The one background URLSession: iOS finishes its uploads even with the app suspended or closed, and tells the app
 * when it next runs ([onFinished], possibly in a later process). Each upload has its own file in Application Support,
 * out of the backup workspace, whose exports are removed at every startup. The task keeps only the file's name,
 * because the container's absolute path can change between launches.
 */
object IosBackgroundUploads : BackgroundUploadSession {
    const val SESSION_ID = "com.emanuel5014.trainable.autobackup.upload"
    private const val IDLE_SECONDS = 120.0
    private const val RESOURCE_SECONDS = 7.0 * 24 * 3600

    /** Called for every finished upload: its path, and the HTTP status (null when the transfer itself failed). */
    var onFinished: (path: String, code: Int?) -> Unit = { _, _ -> }
    /** iOS delivered all the events it woke the app for. */
    var onEventsDelivered: () -> Unit = {}

    private val folder: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        val path = "$base/TrainableUploads"
        NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)
        path
    }

    private val delegate by lazy { UploadDelegate() }

    private val session: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_ID).apply {
            sessionSendsLaunchEvents = true
            discretionary = false
            timeoutIntervalForRequest = IDLE_SECONDS
            timeoutIntervalForResource = RESOURCE_SECONDS
            HTTPShouldSetCookies = false
            URLCache = null
        }
        NSURLSession.sessionWithConfiguration(config, delegate, NSOperationQueue())
    }

    /** Recreates the session so that iOS delivers what finished while the app was not running. */
    fun connect() { session }

    /** Removes files no upload needs any more (an upload that ended without the app hearing of it). */
    suspend fun removeOrphans() {
        val live = pendingPaths().toSet()
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(folder, null).orEmpty()
            .map { "$folder/$it" }.filter { it !in live }.forEach { NSFileManager.defaultManager.removeItemAtPath(it, null) }
    }

    override suspend fun pendingPaths(): List<String> = suspendCancellableCoroutine { continuation ->
        session.getAllTasksWithCompletionHandler { tasks ->
            continuation.resume(tasks.orEmpty().mapNotNull { item ->
                val task = item as NSURLSessionTask
                task.taskDescription?.takeIf { task.state == NSURLSessionTaskStateRunning || task.state == NSURLSessionTaskStateSuspended }
            }.map { "$folder/$it" })
        }
    }

    override fun keep(zipPath: String, name: String): String? {
        val target = "$folder/$name"
        val files = NSFileManager.defaultManager
        files.removeItemAtPath(target, null)
        return target.takeIf { files.moveItemAtPath(zipPath, it, null) }
    }

    override fun start(request: WebDavRequest, wifiOnly: Boolean): Boolean {
        val upload = request.body as? WebDavBody.FilePath ?: return false
        val ns = try { nsRequest(request, IDLE_SECONDS) } catch (_: Exception) { return false }
        ns.setValue(upload.contentType, forHTTPHeaderField = "Content-Type")
        ns.setAllowsCellularAccess(!wifiOnly)
        ns.setAllowsExpensiveNetworkAccess(!wifiOnly)
        val task = session.uploadTaskWithRequest(ns, fromFile = NSURL.fileURLWithPath(upload.path))
        task.taskDescription = upload.path.substringAfterLast('/')
        task.resume()
        return true
    }

    override fun remove(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    internal fun finished(name: String, code: Int?) = onFinished("$folder/$name", code)
}

private class UploadDelegate : NSObject(), NSURLSessionTaskDelegateProtocol {
    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, willPerformHTTPRedirection: NSHTTPURLResponse,
                            newRequest: NSURLRequest, completionHandler: (NSURLRequest?) -> Unit) {
        completionHandler(allowedRedirect(task, newRequest))
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        val name = task.taskDescription ?: return
        IosBackgroundUploads.finished(name, if (didCompleteWithError == null) (task.response as? NSHTTPURLResponse)?.statusCode?.toInt() else null)
    }

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
        IosBackgroundUploads.onEventsDelivered()
    }
}

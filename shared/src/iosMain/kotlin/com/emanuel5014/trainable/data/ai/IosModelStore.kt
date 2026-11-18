@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.repository.toByteArray
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionDownloadTaskResumeData
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.writeData
import platform.Foundation.writeToFile
import platform.darwin.NSObject

/**
 * The on-device model on iPhone (A5): Gemma E2B only (E4B failed every configuration on an iPhone 15 Pro). The file
 * lives in Application Support/ai_models, kept out of iCloud/iTunes backups like Android's no-backup storage; it
 * comes from a download (background URLSession, so it goes on while the app is away, resumable after a failure or a
 * cancel) or from a copy picked in Files, checked first like on Android ([ModelFileValidator]: header, version, size).
 */
class IosModelStore(
    private val directory: String = supportFolder("ai_models"),
    private val cacheDirectory: String = cachesFolder("litertlm"),
    private val supported: List<AiModelVariant> = listOf(AiModelVariant.E2B),
    private val check: (ByteArray, Long) -> ModelImportCheck = ModelFileValidator::check,
    private val availableBytes: (String) -> Long = ::volumeAvailableBytes,
    private val downloads: IosModelDownloads = IosModelDownloads.shared,
    private val urlFor: (AiModelVariant) -> String = { it.downloadUrl },
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow<AiModelStatus>(AiModelStatus.NotDownloaded)
    val status: StateFlow<AiModelStatus> = _status.asStateFlow()

    private val _importError = MutableStateFlow<ModelImportError?>(null)
    val importError: StateFlow<ModelImportError?> = _importError.asStateFlow()

    private var importJob: Job? = null

    init {
        ensureFolder(directory, excludeFromBackup = true)
        refresh()
        downloads.listener = { event -> onDownloadEvent(event) }
        downloads.resumeDataSink = { data -> data.writeToFile(resumeDataPath(variant), true) }
        // A download that went on while the app was closed reports back only to a session with the same identifier
        downloads.reconnect()
    }

    val variant: AiModelVariant get() = supported.first()

    fun modelPath(variant: AiModelVariant = this.variant): String = "$directory/${variant.fileName}"

    /** LiteRT-LM's compiled caches (can be rebuilt, so in Caches). */
    fun cacheDir(): String = cacheDirectory.also { ensureFolder(it, excludeFromBackup = false) }

    fun isDownloaded(variant: AiModelVariant = this.variant): Boolean = fileSize(modelPath(variant)) > 0

    /** Room for [bytes] while keeping the same 512 MB reserve as Android. */
    fun hasRoomFor(bytes: Long): Boolean = availableBytes(directory) >= bytes + MIN_FREE_SPACE

    fun clearImportError() {
        _importError.value = null
    }

    /** Downloads the model, resuming a broken or cancelled download when its resume data is still there. */
    fun startDownload(variant: AiModelVariant = this.variant) {
        if (_status.value is AiModelStatus.Downloading) return
        if (!hasRoomFor(variant.fileSizeBytes)) {
            _status.value = AiModelStatus.Error("Not enough space: ${(variant.fileSizeBytes + MIN_FREE_SPACE) / 1_000_000} MB needed")
            return
        }
        _status.value = AiModelStatus.Downloading(0f)
        downloads.start(urlFor(variant), resumeDataPath(variant))
    }

    /** Stops the download and keeps what was received for the next start. */
    fun cancelDownload() {
        importJob?.cancel()
        downloads.cancel(resumeDataPath(variant))
        refresh()
    }

    fun delete(variant: AiModelVariant = this.variant) {
        val manager = NSFileManager.defaultManager
        listOf(modelPath(variant), partPath(variant), resumeDataPath(variant)).forEach { manager.removeItemAtPath(it, null) }
        refresh()
    }

    /**
     * Copies the model file at [sourceUrl] (a file URL from the Files picker, security-scoped) after checking it. Errors
     * go to [importError]; [status] shows the copy as Downloading(importing = true).
     */
    fun import(sourceUrl: String) {
        if (_status.value is AiModelStatus.Downloading) {
            _importError.value = ModelImportError.Busy
            return
        }
        _importError.value = null
        importJob = scope.launch {
            val url = NSURL(string = sourceUrl)
            val scoped = url.startAccessingSecurityScopedResource()
            try {
                val path = url.path ?: run { _importError.value = ModelImportError.CannotRead; return@launch }
                val size = fileSize(path)
                val header = readHeader(path) ?: run { _importError.value = ModelImportError.CannotRead; return@launch }
                val target = when (val result = check(header, size)) {
                    is ModelImportCheck.Supported ->
                        result.variant.takeIf { it in supported }
                            ?: run { _importError.value = ModelImportError.UnsupportedModel; return@launch }
                    ModelImportCheck.NotLiteRtLm -> { _importError.value = ModelImportError.NotLiteRtLm; return@launch }
                    ModelImportCheck.UnsupportedModel -> { _importError.value = ModelImportError.UnsupportedModel; return@launch }
                }
                if (!hasRoomFor(size)) {
                    _importError.value = ModelImportError.NotEnoughSpace(size + MIN_FREE_SPACE)
                    return@launch
                }
                _status.value = AiModelStatus.Downloading(0f, importing = true)
                try {
                    withContext(Dispatchers.IO) { copyFile(path, partPath(target), size) { _status.value = AiModelStatus.Downloading(it, importing = true) } }
                    replace(partPath(target), modelPath(target))
                } catch (e: Exception) {
                    NSFileManager.defaultManager.removeItemAtPath(partPath(target), null)
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _importError.value = ModelImportError.CannotRead
                }
            } finally {
                if (scoped) url.stopAccessingSecurityScopedResource()
                refresh()
            }
        }
    }

    private fun onDownloadEvent(event: IosModelDownloads.Event) {
        when (event) {
            is IosModelDownloads.Event.Progress -> {
                val total = event.expected.takeIf { it > 0 } ?: variant.fileSizeBytes
                _status.value = AiModelStatus.Downloading((event.written.toFloat() / total).coerceIn(0f, 1f))
            }
            is IosModelDownloads.Event.Finished -> {
                // The system deletes its temporary file when the delegate returns: move it now, check, then put in place
                val part = partPath(variant)
                NSFileManager.defaultManager.removeItemAtPath(part, null)
                val moved = NSFileManager.defaultManager.moveItemAtPath(event.tempPath, part, null)
                val header = if (moved) readHeader(part) else null
                val ok = header != null && (check(header, fileSize(part)) as? ModelImportCheck.Supported)?.variant == variant
                if (ok) {
                    replace(part, modelPath(variant))
                    NSFileManager.defaultManager.removeItemAtPath(resumeDataPath(variant), null)
                    // The task still counts as running until didComplete: set the outcome here
                    _status.value = AiModelStatus.Ready
                } else {
                    NSFileManager.defaultManager.removeItemAtPath(part, null)
                    _status.value = AiModelStatus.Error("The downloaded file is not the expected model")
                }
            }
            is IosModelDownloads.Event.Failed -> {
                _status.value = if (event.canceled) {
                    if (isDownloaded()) AiModelStatus.Ready else AiModelStatus.NotDownloaded
                } else {
                    AiModelStatus.Error(event.message)
                }
            }
        }
    }

    private fun refresh() {
        if (_status.value is AiModelStatus.Downloading && downloads.isRunning) return
        _status.value = if (isDownloaded()) AiModelStatus.Ready else AiModelStatus.NotDownloaded
    }

    private fun partPath(variant: AiModelVariant) = "$directory/${variant.fileName}.part"
    private fun resumeDataPath(variant: AiModelVariant) = "$directory/${variant.fileName}.resume"

    companion object {
        const val MIN_FREE_SPACE = 512L * 1024 * 1024
        private const val CHUNK = 4 * 1024 * 1024

        private fun folder(kind: ULong, name: String): String {
            val base = NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true).first() as String
            return "$base/$name"
        }

        fun supportFolder(name: String) = folder(NSApplicationSupportDirectory, name)
        fun cachesFolder(name: String) = folder(NSCachesDirectory, name)

        internal fun ensureFolder(path: String, excludeFromBackup: Boolean) {
            NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)
            if (excludeFromBackup) NSURL.fileURLWithPath(path).setResourceValue(NSNumber(bool = true), NSURLIsExcludedFromBackupKey, null)
        }

        internal fun fileSize(path: String): Long {
            val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return 0
            return (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0
        }

        internal fun readHeader(path: String): ByteArray? {
            val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return null
            return try {
                handle.readDataOfLength(ModelFileValidator.HEADER_BYTES.toULong()).toByteArray()
            } finally {
                handle.closeFile()
            }
        }

        fun volumeAvailableBytes(path: String): Long = memScoped {
            val values = NSURL.fileURLWithPath(path).resourceValuesForKeys(listOf(NSURLVolumeAvailableCapacityForImportantUsageKey), null)
            (values?.get(NSURLVolumeAvailableCapacityForImportantUsageKey) as? NSNumber)?.longLongValue ?: 0L
        }

        /** Copies [from] to [to] in chunks, reporting each whole percent; stops when the coroutine is cancelled. */
        internal suspend fun copyFile(from: String, to: String, expected: Long, onProgress: (Float) -> Unit) {
            val manager = NSFileManager.defaultManager
            manager.removeItemAtPath(to, null)
            check(manager.createFileAtPath(to, null, null)) { "Cannot create $to" }
            val input = NSFileHandle.fileHandleForReadingAtPath(from) ?: error("Cannot read $from")
            val output = NSFileHandle.fileHandleForWritingAtPath(to) ?: error("Cannot write $to")
            var copied = 0L
            var lastPercent = 0
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val chunk: NSData = input.readDataOfLength(CHUNK.toULong())
                    val length = chunk.length.toLong()
                    if (length == 0L) break
                    output.writeData(chunk)
                    copied += length
                    if (copied > expected) error("Source longer than $expected bytes")
                    val percent = (copied * 100 / expected).toInt()
                    if (percent > lastPercent) {
                        lastPercent = percent
                        onProgress(copied.toFloat() / expected)
                    }
                }
            } finally {
                input.closeFile()
                output.closeFile()
            }
            if (copied != expected) error("Copied $copied of $expected bytes")
        }

        internal fun replace(from: String, to: String) {
            val manager = NSFileManager.defaultManager
            manager.removeItemAtPath(to, null)
            check(manager.moveItemAtPath(from, to, null)) { "Cannot move the model into place" }
        }
    }
}

/**
 * The model download as a background URLSession: one per process (the identifier must be unique), so the system can
 * finish a download while the app is suspended and hand the result over when the session is created again.
 */
class IosModelDownloads internal constructor(configuration: () -> NSURLSessionConfiguration) {

    sealed interface Event {
        data class Progress(val written: Long, val expected: Long) : Event
        data class Finished(val tempPath: String) : Event
        data class Failed(val message: String, val canceled: Boolean) : Event
    }

    /** Receives every event; called on the session's queue. */
    var listener: ((Event) -> Unit)? = null

    var isRunning: Boolean = false
        private set

    private var task: NSURLSessionDownloadTask? = null
    private val delegate = Delegate(this)
    private val session: NSURLSession by lazy {
        NSURLSession.sessionWithConfiguration(configuration(), delegate, NSOperationQueue().apply { maxConcurrentOperationCount = 1 })
    }

    /** Creates the session now, so the system delivers what a download did while the app was not running. */
    fun reconnect() {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            (downloadTasks?.firstOrNull() as? NSURLSessionDownloadTask)?.let { running ->
                task = running
                isRunning = true
            }
        }
    }

    /** Starts [url], resuming from [resumeDataPath] when a previous attempt left data there. */
    fun start(url: String, resumeDataPath: String) {
        val resume = NSData.dataWithContentsOfFile(resumeDataPath)
        NSFileManager.defaultManager.removeItemAtPath(resumeDataPath, null)
        val next = if (resume != null) session.downloadTaskWithResumeData(resume) else session.downloadTaskWithURL(NSURL(string = url))
        task = next
        isRunning = true
        next.resume()
    }

    /** Cancels, keeping what was received in [resumeDataPath]. */
    fun cancel(resumeDataPath: String) {
        val running = task ?: return
        running.cancelByProducingResumeData { data -> data?.writeToFile(resumeDataPath, true) }
    }

    private class Delegate(private val owner: IosModelDownloads) : NSObject(), NSURLSessionDownloadDelegateProtocol {
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long
        ) {
            owner.listener?.invoke(Event.Progress(totalBytesWritten, totalBytesExpectedToWrite))
        }

        override fun URLSession(session: NSURLSession, downloadTask: NSURLSessionDownloadTask, didFinishDownloadingToURL: NSURL) {
            val status = (downloadTask.response as? platform.Foundation.NSHTTPURLResponse)?.statusCode?.toInt() ?: 200
            if (status !in 200..299) {
                owner.listener?.invoke(Event.Failed("HTTP $status", canceled = false))
                return
            }
            didFinishDownloadingToURL.path?.let { owner.listener?.invoke(Event.Finished(it)) }
        }

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            owner.isRunning = false
            owner.task = null
            if (didCompleteWithError == null) return
            val canceled = didCompleteWithError.code == platform.Foundation.NSURLErrorCancelled
            // A broken download leaves resume data in the error: keep it for the next start
            (didCompleteWithError.userInfo[NSURLSessionDownloadTaskResumeData] as? NSData)?.let { data ->
                owner.resumeDataSink?.invoke(data)
            }
            owner.listener?.invoke(Event.Failed(didCompleteWithError.localizedDescription, canceled))
        }
    }

    /** Where resume data from a failed task goes (set by the store). */
    var resumeDataSink: ((NSData) -> Unit)? = null

    companion object {
        const val SESSION_ID = "com.emanuel5014.trainable.models"

        /** The app's one background session for the model. */
        val shared: IosModelDownloads by lazy {
            IosModelDownloads {
                NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_ID).apply {
                    setDiscretionary(false)
                    setSessionSendsLaunchEvents(true)
                }
            }
        }
    }
}

package com.emanuel5014.trainable.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.text.format.Formatter
import android.util.Log
import com.emanuel5014.trainable.BuildConfig
import com.emanuel5014.trainable.data.remote.GitHubAsset
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Android updater: finds the release to offer on GitHub, downloads its APK, checks it and hands it to Android's
 * installer. It holds the one [state] that the update dialog (shown by MainActivity, whether the check came from the
 * app start or from Settings) and the download notification read.
 *
 * The download runs in this object's own scope, kept alive by [UpdateDownloadService] (a foreground service with a
 * progress notification), so it survives rotations, leaving Settings and the app going to the background. A
 * dropped connection is retried and resumes where it stopped. The finished file is checked against the size and
 * SHA-256 GitHub lists, then written into a [PackageInstaller] session; Android answers in [UpdateInstallReceiver].
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    baseClient: OkHttpClient,
    private val preferences: UserPreferencesRepository
) {
    private val client = baseClient.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // GitHub sends null for an empty name or changelog: coerceInputValues turns it into the field's default
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checkMutex = Mutex()
    private val launchChecked = AtomicBoolean(false)
    private var downloadJob: Job? = null

    /** The installer session whose answer counts; answers of older sessions (abandoned) are ignored. */
    @Volatile
    private var activeSessionId: Int = -1

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val updateDir: File get() = File(context.cacheDir, UPDATE_DIR)

    /**
     * Release builds look for updates; debug and fast builds do not (their package name ends in ".debug", so a
     * release APK would install as a second app). A test feed given at build time turns it on in those builds.
     */
    val isEnabled: Boolean
        get() = BuildConfig.UPDATE_FEED_URL.isNotEmpty() || BuildConfig.BUILD_TYPE == "release"

    private val feedUrl: String
        get() = BuildConfig.UPDATE_FEED_URL.ifEmpty { GITHUB_RELEASES_URL }

    /** True when this phone takes beta updates (see [UpdateChannel.isBeta]). */
    suspend fun isBetaChannel(): Boolean =
        UpdateChannel.isBeta(preferences.updateChannelBeta.first(), BuildConfig.VERSION_NAME)

    /** The check at app start: once per process, opening the dialog only when there is something to offer. */
    suspend fun checkOnLaunch() {
        if (!launchChecked.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) { cleanUpOnLaunch() }
        check()
    }

    /**
     * Looks for an update and opens the dialog when there is one. While a download or an install is under way it
     * only reopens the dialog on it.
     */
    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext UpdateCheck.Disabled
        checkMutex.withLock {
            _state.value.let { current ->
                if (current.isBusy) {
                    _state.update { it.copy(dialogOpen = true) }
                    return@withLock UpdateCheck.Available(current.release!!)
                }
            }
            val result = fetch()
            when (result) {
                is UpdateCheck.Available -> {
                    val release = result.release
                    deleteStale(release.tagName)
                    val asset = UpdateFiles.apkAsset(release)
                    val apk = UpdateFiles.apkFile(updateDir, release.tagName)
                    // A file from an earlier try counts only if it still matches what GitHub lists now
                    val phase = if (asset != null && apk.isFile && UpdateFiles.matches(apk, asset)) {
                        UpdatePhase.ReadyToInstall(release)
                    } else {
                        UpdatePhase.Available(release)
                    }
                    _state.update { if (it.isBusy) it.copy(dialogOpen = true) else UpdateUiState(phase, dialogOpen = true) }
                }
                UpdateCheck.UpToDate -> {
                    deleteStale(null)
                    _state.update { if (it.isBusy) it else UpdateUiState() }
                }
                else -> Unit
            }
            result
        }
    }

    private suspend fun fetch(): UpdateCheck = try {
        val beta = isBetaChannel()
        val request = Request.Builder()
            .url(feedUrl)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Release list: HTTP ${response.code}")
                UpdateCheck.Failed(UpdateFiles.httpError(response.code, response.header("X-RateLimit-Remaining")))
            } else {
                val releases = json.decodeFromString<List<GitHubRelease>>(response.body.string())
                UpdateChannel.pick(releases, BuildConfig.VERSION_NAME, beta)
                    ?.let { UpdateCheck.Available(it) }
                    ?: UpdateCheck.UpToDate
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Log.w(TAG, "Release list: ${e.message}")
        UpdateCheck.Failed(UpdateError.NETWORK)
    } catch (e: Exception) {
        Log.w(TAG, "Release list unreadable", e)
        UpdateCheck.Failed(UpdateError.SERVER)
    }

    fun openDialog() {
        _state.update { if (it.phase == UpdatePhase.Idle) it else it.copy(dialogOpen = true) }
    }

    /** Hides the dialog. A download keeps going in its notification; an offer comes back at the next check. */
    fun dismissDialog() {
        _state.update { it.copy(dialogOpen = false) }
    }

    /**
     * Downloads (or resumes) the offered APK, checks it and opens Android's installer. Also the "retry" of a failed
     * step: an APK already complete on disk is checked again and installed without downloading it twice.
     */
    fun startDownload() {
        val current = _state.value
        val release = current.release ?: return
        if (current.isBusy || downloadJob?.isActive == true) return
        val asset = UpdateFiles.apkAsset(release)
        _state.value = UpdateUiState(UpdatePhase.Downloading(release, 0, asset?.size ?: 0), dialogOpen = true)
        downloadJob = scope.launch { runDownload(release) }
        UpdateDownloadService.start(context)
    }

    /** Stops the download and deletes what it got so far. */
    fun cancelDownload() {
        val release = _state.value.release
        downloadJob?.cancel()
        downloadJob = null
        if (release != null) {
            UpdateFiles.partFile(updateDir, release.tagName).delete()
            _state.update { it.copy(phase = UpdatePhase.Available(release)) }
        }
    }

    private suspend fun runDownload(release: GitHubRelease) {
        val asset = UpdateFiles.apkAsset(release) ?: return fail(release, UpdateError.NO_APK)
        val dir = updateDir.apply { mkdirs() }
        deleteStale(release.tagName)
        val apk = UpdateFiles.apkFile(dir, release.tagName)
        val part = UpdateFiles.partFile(dir, release.tagName)
        try {
            val complete = apk.isFile && (asset.size <= 0 || apk.length() == asset.size)
            if (!complete) {
                apk.delete()
                var attempt = 0
                while (true) {
                    try {
                        downloadOnce(release, asset, part)
                        break
                    } catch (e: IOException) {
                        if (e is UpdateFailure) throw e
                        currentCoroutineContext().ensureActive()
                        attempt++
                        Log.w(TAG, "Download attempt $attempt: ${e.message}")
                        if (attempt >= RETRY_DELAYS_MS.size + 1) throw UpdateFailure(UpdateError.NETWORK, e.message)
                        delay(RETRY_DELAYS_MS[attempt - 1])
                    }
                }
                if (!part.renameTo(apk)) throw UpdateFailure(UpdateError.STORAGE, null)
            }
            _state.update { it.copy(phase = UpdatePhase.Verifying(release)) }
            if (!UpdateFiles.matches(apk, asset)) {
                apk.delete()
                throw UpdateFailure(UpdateError.CORRUPT, null)
            }
            install(release, apk)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UpdateFailure) {
            fail(release, e.error, e.detail)
        } catch (e: Exception) {
            Log.e(TAG, "Update failed", e)
            val installing = _state.value.phase is UpdatePhase.Installing
            fail(release, if (installing) UpdateError.INSTALL_FAILED else UpdateError.STORAGE, e.message)
        }
    }

    /** One HTTP request for what is missing of [part]; throws IOException on a network problem (retried). */
    private suspend fun downloadOnce(release: GitHubRelease, asset: GitHubAsset, part: File) {
        var offset = if (part.isFile) part.length() else 0L
        if (asset.size > 0 && offset > asset.size) {
            part.delete()
            offset = 0
        }
        if (asset.size > 0 && offset == asset.size) return

        val needed = (asset.size - offset).coerceAtLeast(0) + asset.size + SPACE_MARGIN_BYTES
        if (asset.size > 0 && part.parentFile!!.usableSpace < needed) {
            throw UpdateFailure(UpdateError.STORAGE, Formatter.formatShortFileSize(context, needed))
        }

        val request = Request.Builder()
            .url(asset.downloadUrl)
            .apply { if (offset > 0) header("Range", "bytes=$offset-") }
            .build()
        val call = client.newCall(request)
        // A blocking read does not see the coroutine being cancelled: cancelling the call breaks it
        val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                when (UpdateFiles.resumeMode(response.code, offset)) {
                    UpdateFiles.ResumeMode.APPEND -> Unit
                    UpdateFiles.ResumeMode.FROM_START -> offset = 0
                    UpdateFiles.ResumeMode.RESTART -> {
                        part.delete()
                        throw IOException("Range not satisfiable, starting over")
                    }
                    UpdateFiles.ResumeMode.ERROR -> {
                        Log.w(TAG, "APK: HTTP ${response.code}")
                        // 5xx are worth retrying like a dropped connection; 4xx will not change
                        if (response.code >= 500) throw IOException("HTTP ${response.code}")
                        throw UpdateFailure(
                            UpdateFiles.httpError(response.code, response.header("X-RateLimit-Remaining")),
                            "HTTP ${response.code}"
                        )
                    }
                }
                val body = response.body
                val total = when {
                    asset.size > 0 -> asset.size
                    body.contentLength() > 0 -> offset + body.contentLength()
                    else -> 0L
                }
                var written = offset
                var lastEmit = 0L
                _state.update { it.copy(phase = UpdatePhase.Downloading(release, written, total)) }
                body.byteStream().use { input ->
                    FileOutputStream(part, offset > 0).use { output ->
                        val buffer = ByteArray(1 shl 16)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit >= PROGRESS_INTERVAL_MS) {
                                lastEmit = now
                                _state.update { it.copy(phase = UpdatePhase.Downloading(release, written, total)) }
                            }
                        }
                    }
                }
                _state.update { it.copy(phase = UpdatePhase.Downloading(release, written, total)) }
                if (asset.size > 0 && written < asset.size) throw IOException("Connection closed at $written of ${asset.size}")
            }
        } finally {
            cancelHandle?.dispose()
        }
    }

    /** Installs the APK already downloaded and checked; downloads it again when it is gone (cache cleared). */
    fun installNow() {
        val release = _state.value.release ?: return
        if (downloadJob?.isActive == true) return
        val apk = UpdateFiles.apkFile(updateDir, release.tagName)
        if (!apk.isFile) {
            _state.update { it.copy(phase = UpdatePhase.Available(release)) }
            startDownload()
            return
        }
        _state.update { it.copy(phase = UpdatePhase.Installing(release), dialogOpen = true) }
        downloadJob = scope.launch {
            try {
                install(release, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(release, UpdateError.INSTALL_FAILED, e.message)
            }
        }
    }

    /** Writes the APK into a new installer session and commits it; Android answers in [UpdateInstallReceiver]. */
    private fun install(release: GitHubRelease, apk: File) {
        _state.update { it.copy(phase = UpdatePhase.Installing(release)) }
        val installer = context.packageManager.packageInstaller
        // A session left open (installer closed without an answer) would hold a copy of the APK: drop it
        installer.mySessions.forEach { runCatching { installer.abandonSession(it.sessionId) } }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        activeSessionId = sessionId
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output, 1 shl 16)
                        session.fsync(output)
                    }
                }
                session.commit(UpdateInstallReceiver.statusSender(context, sessionId, release.tagName))
            }
        } catch (e: IOException) {
            runCatching { installer.abandonSession(sessionId) }
            val noSpace = e.message?.contains("ENOSPC") == true || e.message?.contains("space", ignoreCase = true) == true
            throw UpdateFailure(if (noSpace) UpdateError.INSTALL_STORAGE else UpdateError.INSTALL_FAILED, e.message)
        }
    }

    /** Android's answer about an installer session (see [UpdateInstallReceiver]). */
    fun onInstallerStatus(sessionId: Int, tag: String?, status: Int, message: String?, confirm: Intent?) {
        if (status == PackageInstaller.STATUS_SUCCESS) {
            // Android restarts the app to deliver this, so the state is empty: the update is in, drop its files
            UpdateNotifications.cancel(context)
            deleteStale(null)
            _state.value = UpdateUiState()
            return
        }
        val release = _state.value.release
        if (sessionId != activeSessionId || release == null || release.tagName != tag) {
            Log.i(TAG, "Ignoring installer status $status for session $sessionId")
            return
        }
        Log.i(TAG, "Installer status $status ($message)")
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                _state.update { it.copy(phase = UpdatePhase.Installing(release)) }
                if (confirm != null) showConfirmation(confirm, release)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // The user backed out of Android's dialog: the checked APK stays, one tap installs it
                UpdateNotifications.cancel(context)
                _state.update { it.copy(phase = UpdatePhase.ReadyToInstall(release)) }
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                fail(release, UpdateError.INSTALL_CONFLICT, message)
            PackageInstaller.STATUS_FAILURE_STORAGE -> fail(release, UpdateError.INSTALL_STORAGE, message)
            else -> fail(release, UpdateError.INSTALL_FAILED, message)
        }
    }

    /**
     * Opens Android's confirmation. Android lets an app open a screen only while it is on screen itself: in the
     * background a notification opens it instead, and the dialog waits with an "Install" button.
     */
    private fun showConfirmation(confirm: Intent, release: GitHubRelease) {
        if (isAppInForeground()) {
            try {
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not open the installer", e)
            }
        }
        _state.update { it.copy(dialogOpen = true) }
        UpdateNotifications.showReadyToInstall(context, release, confirm)
    }

    private fun fail(release: GitHubRelease, error: UpdateError, detail: String? = null) {
        Log.w(TAG, "Update failed: $error ($detail)")
        _state.value = UpdateUiState(UpdatePhase.Failed(release, error, detail), dialogOpen = true)
        if (!isAppInForeground()) UpdateNotifications.showFailed(context, release)
    }

    private fun isAppInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    /** Deletes the updater's files except those of [keepTag] (all of them when null). */
    private fun deleteStale(keepTag: String?) {
        val files = updateDir.listFiles()?.toList() ?: return
        UpdateFiles.stale(files, keepTag).forEach { it.delete() }
    }

    /** At app start: drops files of versions not newer than this one (installed by now) and the old updater's file. */
    private fun cleanUpOnLaunch() {
        File(context.cacheDir, LEGACY_APK_NAME).delete()
        updateDir.listFiles()?.forEach { file ->
            val tag = UpdateFiles.tagOf(file)
            if (tag == null || !AppVersion.isNewer(BuildConfig.VERSION_NAME, tag)) file.delete()
        }
    }

    /** A failure that retrying will not fix. Extends IOException only to travel through the same catch blocks. */
    private class UpdateFailure(val error: UpdateError, val detail: String?) : IOException(error.name)

    companion object {
        private const val TAG = "UpdateManager"

        // The newest releases first; 50 reach well past the last final release even with many betas and iOS builds
        private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Emanuel5014/Trainable/releases?per_page=50"

        private const val UPDATE_DIR = "updates"
        /** Where the updater before this one saved the APK; never cleaned up by it. */
        private const val LEGACY_APK_NAME = "app_update.apk"

        /** Waits before the 2nd, 3rd and 4th attempt after a dropped connection; each resumes where it stopped. */
        private val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000, 10_000)
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val SPACE_MARGIN_BYTES = 20L * 1024 * 1024
    }
}

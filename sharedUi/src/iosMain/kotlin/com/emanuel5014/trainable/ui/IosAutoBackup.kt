package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.backup.IosAutoBackupRun
import com.emanuel5014.trainable.data.backup.IosBackupScheduler
import com.emanuel5014.trainable.data.backup.IosLocalBackupFolder
import com.emanuel5014.trainable.data.backup.IosNetworkPath
import com.emanuel5014.trainable.data.backup.IosNextcloudVault
import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.data.backup.IosNextcloudAutoUpload
import com.emanuel5014.trainable.data.remote.nextcloud.IosBackgroundUploads
import com.emanuel5014.trainable.data.remote.nextcloud.IosWebDavHttp
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.time.Clock

/**
 * Automatic backups on iOS, called from Swift: the BGTaskScheduler task ([runScheduled] / [cancelScheduled]), the
 * catch-up when the app goes to the background ([onEnteredBackground]) and the wake-up for a finished upload
 * ([reconnectUploads]). One run at a time; nothing runs before the safe startup (a staged restore or a recovery is left
 * to the next normal launch, as for the timer App Intents).
 */
object IosAutoBackup {
    private const val WAKE_LIMIT_MILLIS = 25_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = Mutex()
    private var scheduledJob: Job? = null
    private val finishing = AtomicInt(0)
    private val wake = AtomicReference<(() -> Unit)?>(null)

    private fun now() = Clock.System.now().toEpochMilliseconds()

    internal val scheduler by lazy { IosBackupScheduler(IosAppGraph.preferences) { now() } }

    /**
     * At every launch, before anything else can finish: the network reading, so the first backup knows whether it is on
     * mobile data, and the upload session, so iOS delivers what finished while the app was not running.
     */
    fun start() {
        IosNetworkPath.start()
        IosBackgroundUploads.onFinished = ::finishUpload
        IosBackgroundUploads.onEventsDelivered = {
            scope.launch {
                withTimeoutOrNull(WAKE_LIMIT_MILLIS) { while (finishing.value > 0) delay(100) }
                endWake()
            }
        }
        IosBackgroundUploads.connect()
        scope.launch { IosBackgroundUploads.removeOrphans() }
    }

    private fun http(wifiOnly: Boolean) = NextcloudWebDav(IosWebDavHttp(wifiOnly))

    private suspend fun autoUpload(): IosNextcloudAutoUpload {
        val prefs = IosAppGraph.preferences
        val archiver = IosBackupArchiver()
        val webDav = http(prefs.nextcloudWifiOnly.first())
        val nextcloud = NextcloudBackupService(prefs, webDav, IosNextcloudVault(), archiver) { now() }
        return IosNextcloudAutoUpload(prefs, webDav, nextcloud, archiver, IosBackgroundUploads, scheduler) { now() }
    }

    private suspend fun run(): IosAutoBackupRun {
        val prefs = IosAppGraph.preferences
        val archiver = IosBackupArchiver()
        val nextcloud = NextcloudBackupService(prefs, http(prefs.nextcloudWifiOnly.first()), IosNextcloudVault(), archiver) { now() }
        return IosAutoBackupRun(prefs, archiver, IosLocalBackupFolder(prefs, archiver), nextcloud, autoUpload(), scheduler,
            restorePending = { IosBackups.manager.hasPendingRestoreOrRecovery() },
            expensiveNetwork = { IosNetworkPath.isExpensive() }) { now() }
    }

    /** The background task: [onDone] gets whether it finished without anything left to retry. */
    fun runScheduled(onDone: (Boolean) -> Unit) {
        val job = scope.launch {
            val ok = try {
                if (!running.tryLock()) false
                else try {
                    IosBackups.prepareTimerAction() && run().runScheduled()
                } finally { running.unlock() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            onDone(ok)
        }
        job.invokeOnCompletion { cause -> if (cause != null) onDone(false) }
        scheduledJob = job
    }

    /** iOS is ending the background task early. */
    fun cancelScheduled() {
        scheduledJob?.cancel()
    }

    /**
     * The app left the screen: run what is due in the few seconds iOS grants; the Nextcloud upload is handed to iOS and
     * nothing waits for it. Also keeps a background task asked for. When the time runs out the work is not cancelled: it
     * is suspended with the app and resumes on its next wake.
     */
    fun onEnteredBackground() {
        if (running.isLocked) return
        val app = UIApplication.sharedApplication
        var task: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
        fun end() {
            if (task != UIBackgroundTaskInvalid) {
                app.endBackgroundTask(task)
                task = UIBackgroundTaskInvalid
            }
        }
        task = app.beginBackgroundTaskWithName("Trainable automatic backup") { end() }
        scope.launch {
            try {
                if (running.tryLock()) try {
                    if (IosBackups.prepareTimerAction()) run().catchUp()
                } finally { running.unlock() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            } finally {
                launch(Dispatchers.Main) { end() }
            }
        }
    }

    /** An upload ended, possibly with the app closed since it started: remember it, trim the server, drop the file. */
    private fun finishUpload(path: String, code: Int?) {
        finishing.incrementAndGet()
        scope.launch {
            try {
                if (IosBackups.prepareTimerAction()) autoUpload().finish(path, code) else IosBackgroundUploads.remove(path)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                IosBackgroundUploads.remove(path)
            } finally {
                finishing.decrementAndGet()
            }
        }
    }

    /**
     * iOS woke the app for the upload session: [onDone] tells it when the app is done, after the events and the work they
     * start, or after a time limit, because iOS suspends the app when told.
     */
    fun reconnectUploads(onDone: () -> Unit) {
        wake.getAndSet(onDone)?.invoke()
        IosBackgroundUploads.connect()
        scope.launch {
            delay(WAKE_LIMIT_MILLIS)
            endWake()
        }
    }

    private fun endWake() {
        wake.getAndSet(null)?.invoke()
    }

    /** After a settings change (Android's rescheduleAutoBackup). */
    internal suspend fun reschedule() = scheduler.reschedule()
}

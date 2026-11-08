package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.backup.IosAutoBackupRun
import com.emanuel5014.trainable.data.backup.IosBackupScheduler
import com.emanuel5014.trainable.data.backup.IosLocalBackupFolder
import com.emanuel5014.trainable.data.backup.IosNetworkPath
import com.emanuel5014.trainable.data.backup.IosNextcloudVault
import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.data.remote.nextcloud.IosWebDavHttp
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.time.Clock

/**
 * Automatic backups on iOS, called from Swift: the BGTaskScheduler task ([runScheduled] / [cancelScheduled]) and the
 * catch-up when the app goes to the background ([onEnteredBackground]). One run at a time; nothing runs before the
 * safe startup (a staged restore or a recovery is left to the next normal launch, as for the timer App Intents).
 */
object IosAutoBackup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = Mutex()
    private var scheduledJob: Job? = null

    private fun now() = Clock.System.now().toEpochMilliseconds()

    internal val scheduler by lazy { IosBackupScheduler(IosAppGraph.preferences) { now() } }

    /** Starts the network reading early, so the first backup knows whether it is on mobile data. */
    fun start() = IosNetworkPath.start()

    private suspend fun run(backgroundUploads: Boolean): IosAutoBackupRun {
        val prefs = IosAppGraph.preferences
        val archiver = IosBackupArchiver()
        val http = IosWebDavHttp(prefs.nextcloudWifiOnly.first(), backgroundUploads)
        val nextcloud = NextcloudBackupService(prefs, NextcloudWebDav(http), IosNextcloudVault(), archiver) { now() }
        return IosAutoBackupRun(prefs, archiver, IosLocalBackupFolder(prefs, archiver), nextcloud, scheduler,
            restorePending = { IosBackups.manager.hasPendingRestoreOrRecovery() },
            expensiveNetwork = { IosNetworkPath.isExpensive() }) { now() }
    }

    /** The background task: [onDone] gets whether it finished without anything left to retry. */
    fun runScheduled(onDone: (Boolean) -> Unit) {
        val job = scope.launch {
            val ok = try {
                if (!running.tryLock()) false
                else try {
                    IosBackups.prepareTimerAction() && run(backgroundUploads = false).runScheduled()
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
     * The app left the screen: run an overdue backup in the few seconds iOS grants, the Nextcloud upload in the
     * background session so it finishes even when the app is suspended; otherwise make sure a background task is asked
     * for. When the time runs out the work is not cancelled: it is suspended with the app and resumes on its next wake.
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
                    if (IosBackups.prepareTimerAction()) {
                        val backup = run(backgroundUploads = true)
                        if (!backup.catchUp()) scheduler.reschedule()
                    }
                } finally { running.unlock() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            } finally {
                launch(Dispatchers.Main) { end() }
            }
        }
    }

    /** iOS woke the app for the background upload session: reconnect it so its events are delivered. */
    fun reconnectUploads() = IosWebDavHttp.reconnectBackgroundUploads()

    /** After a settings change (Android's rescheduleAutoBackup). */
    internal suspend fun reschedule() = scheduler.reschedule()
}

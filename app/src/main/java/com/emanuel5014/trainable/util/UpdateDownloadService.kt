package com.emanuel5014.trainable.util

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the app alive while [UpdateManager] downloads and checks an update, with the progress notification. It does
 * no work of its own: it follows the manager's state and stops when the download is over (installer open, failed,
 * cancelled).
 */
@AndroidEntryPoint
class UpdateDownloadService : Service() {

    @Inject
    lateinit var updateManager: UpdateManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            updateManager.cancelDownload()
            stop()
            return START_NOT_STICKY
        }
        val release = updateManager.state.value.release
        if (release == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                UpdateNotifications.PROGRESS_ID,
                UpdateNotifications.progress(this, release, 0, 0, verifying = false),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
        } catch (e: Exception) {
            // The download goes on without the service; it may stop if Android closes the app in the background
            Log.w(TAG, "Could not start in the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        if (watchJob == null) watchJob = scope.launch { follow() }
        return START_NOT_STICKY
    }

    private suspend fun follow() {
        val manager = getSystemService(NotificationManager::class.java)
        var lastPercent = -1
        var lastPost = 0L
        var lastWasVerifying = false
        updateManager.state.collect { state ->
            when (val phase = state.phase) {
                is UpdatePhase.Downloading -> {
                    val percent = phase.fraction?.let { (it * 100).toInt() } ?: -1
                    val now = System.currentTimeMillis()
                    if (percent != lastPercent && now - lastPost >= NOTIFICATION_INTERVAL_MS) {
                        lastPercent = percent
                        lastPost = now
                        lastWasVerifying = false
                        manager?.notify(
                            UpdateNotifications.PROGRESS_ID,
                            UpdateNotifications.progress(this, phase.release, phase.downloaded, phase.total, verifying = false)
                        )
                    }
                }
                is UpdatePhase.Verifying -> if (!lastWasVerifying) {
                    lastWasVerifying = true
                    manager?.notify(
                        UpdateNotifications.PROGRESS_ID,
                        UpdateNotifications.progress(this, phase.release, 0, 0, verifying = true)
                    )
                }
                else -> stop()
            }
        }
    }

    private fun stop() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Trainable:UpdateDownload")
            ?.apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
    }

    private fun releaseWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "UpdateDownloadService"
        const val ACTION_CANCEL = "com.emanuel5014.trainable.action.CANCEL_UPDATE_DOWNLOAD"
        private const val NOTIFICATION_INTERVAL_MS = 700L
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, UpdateDownloadService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start the download service", e)
            }
        }
    }
}

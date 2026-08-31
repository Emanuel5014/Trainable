package com.emanuel5014.trainable.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "ModelDownloadService"
private const val CHANNEL_ID = "ai_model_download_channel"
private const val NOTIFICATION_ID = 2004

@AndroidEntryPoint
class ModelDownloadService : Service() {

    @Inject lateinit var modelFileManager: ModelFileManager
    @Inject lateinit var modelDownloadManager: ModelDownloadManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeCall: Call? = null
    private var isCancelled: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val variantId = intent?.getStringExtra(EXTRA_VARIANT_ID)
        val variant = variantId?.let { AiModelVariant.fromId(it) } ?: AiModelVariant.E2B

        if (action == ACTION_CANCEL_DOWNLOAD) {
            Log.i(TAG, "Cancel requested for variant: ${variant.id}")
            cancelCurrentDownload(variant)
            return START_NOT_STICKY
        }

        if (action == ACTION_START_DOWNLOAD) {
            isCancelled = false
            acquireWakeLock()

            try {
                val initialNotification = buildProgressNotification(variant, 0)
                startForeground(NOTIFICATION_ID, initialNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground", e)
                releaseWakeLock()
                stopSelf()
                return START_NOT_STICKY
            }

            startDownload(variant)
        }

        return START_NOT_STICKY
    }

    private fun startDownload(variant: AiModelVariant) {
        isCancelled = false
        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            val target = File(modelFileManager.getModelFile(variant).parentFile, variant.fileName + ".part")
            try {
                if (modelFileManager.isDownloaded(variant)) {
                    modelDownloadManager.updateStatus(variant, AiModelStatus.Ready)
                    modelFileManager.notifyFilesChanged()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val request = Request.Builder()
                    .url(variant.downloadUrl)
                    .build()

                val call = client.newCall(request)
                activeCall = call

                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        if (!isCancelled) {
                            val error = "HTTP ${response.code}"
                            modelDownloadManager.updateStatus(variant, AiModelStatus.Error(error))
                        }
                        return@launch
                    }

                    val body = response.body ?: run {
                        if (!isCancelled) {
                            modelDownloadManager.updateStatus(variant, AiModelStatus.Error("Empty response"))
                        }
                        return@launch
                    }

                    val contentLength = body.contentLength()
                    var bytesWritten = 0L
                    var lastNotificationUpdateMs = 0L
                    var lastProgressPercent = -1

                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(1 shl 16)
                            while (true) {
                                if (isCancelled || !coroutineContext.isActive) {
                                    throw CancellationException("Download cancelled")
                                }
                                val read = input.read(buffer)
                                if (read == -1) break
                                if (isCancelled || !coroutineContext.isActive) {
                                    throw CancellationException("Download cancelled")
                                }
                                output.write(buffer, 0, read)
                                bytesWritten += read

                                if (contentLength > 0) {
                                    val progressFraction = bytesWritten.toFloat() / contentLength
                                    val progressPercent = (progressFraction * 100).toInt().coerceIn(0, 100)

                                    // Update download manager state for UI
                                    modelDownloadManager.updateStatus(variant, AiModelStatus.Downloading(progressFraction))

                                    // Throttle notification updates
                                    val now = System.currentTimeMillis()
                                    if (progressPercent != lastProgressPercent && now - lastNotificationUpdateMs > 600) {
                                        lastNotificationUpdateMs = now
                                        lastProgressPercent = progressPercent
                                        if (!isCancelled) {
                                            updateNotification(variant, progressPercent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isCancelled || !coroutineContext.isActive) {
                    throw CancellationException("Download cancelled")
                }

                val finalFile = modelFileManager.getModelFile(variant)
                if (finalFile.exists()) finalFile.delete()
                if (!target.renameTo(finalFile)) {
                    throw IllegalStateException("Could not finalize model file")
                }

                modelDownloadManager.updateStatus(variant, AiModelStatus.Ready)
                modelFileManager.notifyFilesChanged()
                showCompletedNotification(variant)
            } catch (e: CancellationException) {
                Log.i(TAG, "Download cancelled: ${e.message}")
                if (target.exists()) target.delete()
                modelDownloadManager.updateStatus(variant, AiModelStatus.NotDownloaded)
                modelFileManager.notifyFilesChanged()
            } catch (e: Exception) {
                if (target.exists()) target.delete()
                if (!isCancelled) {
                    Log.e(TAG, "Download error", e)
                    modelDownloadManager.updateStatus(variant, AiModelStatus.Error(e.message ?: "Download failed"))
                } else {
                    modelDownloadManager.updateStatus(variant, AiModelStatus.NotDownloaded)
                }
                modelFileManager.notifyFilesChanged()
            } finally {
                activeCall = null
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun cancelCurrentDownload(variant: AiModelVariant) {
        isCancelled = true
        try {
            activeCall?.cancel()
        } catch (_: Exception) {
        }
        downloadJob?.cancel()
        val target = File(modelFileManager.getModelFile(variant).parentFile, variant.fileName + ".part")
        if (target.exists()) target.delete()
        modelDownloadManager.updateStatus(variant, AiModelStatus.NotDownloaded)
        modelFileManager.notifyFilesChanged()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Trainable:ModelDownload")?.apply {
                acquire(30 * 60 * 1000L) // 30 min max timeout
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ai_download_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.ai_download_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(variant: AiModelVariant, progressPercent: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, ModelDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_VARIANT_ID, variant.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ai_download_notification_title))
            .setContentText(getString(R.string.ai_download_notification_progress, variant.displayName, progressPercent))
            .setSmallIcon(R.drawable.ic_app_logo)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.ai_download_cancel),
                cancelPendingIntent
            )
            .setSilent(true)
            .build()
    }

    private fun updateNotification(variant: AiModelVariant, progressPercent: Int) {
        val notification = buildProgressNotification(variant, progressPercent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletedNotification(variant: AiModelVariant) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.ai_download_notification_complete))
            .setContentText(variant.displayName)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isCancelled = true
        try {
            activeCall?.cancel()
        } catch (_: Exception) {
        }
        downloadJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "com.emanuel5014.trainable.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.emanuel5014.trainable.action.CANCEL_DOWNLOAD"
        const val EXTRA_VARIANT_ID = "extra_variant_id"
    }
}

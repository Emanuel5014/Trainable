package com.emanuel5014.trainable.webserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.data.local.dao.AnalyticsDao
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import javax.inject.Inject

private const val TAG = "LocalWebServerService"
private const val CHANNEL_ID = "trainable_web_server"
private const val NOTIFICATION_ID = 1001

@AndroidEntryPoint
class LocalWebServerService : Service() {

    @Inject lateinit var workoutDao: WorkoutDao
    @Inject lateinit var analyticsDao: AnalyticsDao
    @Inject lateinit var exerciseDao: ExerciseDao
    @Inject lateinit var userDao: UserDao
    @Inject lateinit var weightLogDao: WeightLogDao
    @Inject lateinit var userPrefsRepo: UserPreferencesRepository

    private var engine: EmbeddedServer<*, *>? = null
    private var serverIp: String = "127.0.0.1"
    private var serverPort: Int = 8080
    private var serverUrl: String = "http://127.0.0.1:8080"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        if (intent?.action == "STOP_SERVER") {
            Log.i(TAG, "Stop requested from notification")
            stopSelf()
            return START_NOT_STICKY
        }

        serverIp = intent?.getStringExtra("SERVER_IP") ?: "127.0.0.1"
        serverPort = intent?.getIntExtra("SERVER_PORT", 8080) ?: 8080
        serverUrl = intent?.getStringExtra("SERVER_URL") ?: "http://127.0.0.1:8080"

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (engine == null) {
            startServer()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        stopServer()
        sendBroadcast(Intent(WebServerManager.ACTION_SERVER_STOPPED))
        super.onDestroy()
    }

    private fun startServer() {
        try {
            engine = embeddedServer(Netty, port = serverPort) {
                configureServer(this@LocalWebServerService, workoutDao, analyticsDao, exerciseDao, userDao, weightLogDao, userPrefsRepo)
            }.start(wait = false)
            Log.i(TAG, "Ktor server started at $serverUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Ktor server", e)
        }
    }

    private fun stopServer() {
        try {
            engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
            engine = null
            Log.i(TAG, "Ktor server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trainable Web Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifica per il server web locale di Trainable"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocalWebServerService::class.java).apply {
            action = "STOP_SERVER"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trainable Web Server")
            .setContentText("Server attivo su $serverUrl")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Spegni",
                stopPendingIntent
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        fun stop(context: Context) {
            context.stopService(Intent(context, LocalWebServerService::class.java))
        }
    }
}

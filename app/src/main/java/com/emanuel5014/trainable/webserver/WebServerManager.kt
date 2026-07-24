package com.emanuel5014.trainable.webserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebServerManager"

data class WebServerState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val localIp: String = "",
    val url: String = ""
)

@Singleton
class WebServerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow(WebServerState())
    val state: StateFlow<WebServerState> = _state.asStateFlow()

    private val serverPort = 8080
    private var receiverRegistered = false
    private val pendingOperation = AtomicBoolean(false)

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "Server stopped by notification action")
            _state.value = WebServerState()
            pendingOperation.set(false)
        }
    }

    fun startServer() {
        synchronized(this) {
            if (isRunning() || pendingOperation.get()) return
            pendingOperation.set(true)
        }

        try {
            val ip = getLocalIpAddress()
            val url = "http://$ip:$serverPort"

            Log.i(TAG, "Starting server at $url")

            _state.value = WebServerState(
                isRunning = true,
                port = serverPort,
                localIp = ip,
                url = url
            )

            registerStopReceiver()

            val intent = Intent(context, LocalWebServerService::class.java).apply {
                putExtra("SERVER_IP", ip)
                putExtra("SERVER_PORT", serverPort)
                putExtra("SERVER_URL", url)
            }
            ContextCompat.startForegroundService(context, intent)
        } finally {
            synchronized(this) {
                pendingOperation.set(false)
            }
        }
    }

    fun stopServer() {
        synchronized(this) {
            if (!isRunning() || pendingOperation.get()) return
            pendingOperation.set(true)
        }

        try {
            Log.i(TAG, "Stopping server")
            context.stopService(Intent(context, LocalWebServerService::class.java))
            _state.value = WebServerState()
            unregisterStopReceiver()
        } finally {
            synchronized(this) {
                pendingOperation.set(false)
            }
        }
    }

    fun notifyServerStopped() {
        Log.i(TAG, "Server stopped notification received")
        _state.value = WebServerState()
        pendingOperation.set(false)
        unregisterStopReceiver()
    }

    fun isRunning(): Boolean = _state.value.isRunning

    private fun registerStopReceiver() {
        if (!receiverRegistered) {
            context.registerReceiver(
                stopReceiver,
                IntentFilter(ACTION_SERVER_STOPPED),
                Context.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    private fun unregisterStopReceiver() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(stopReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff,
                    ip shr 8 and 0xff,
                    ip shr 16 and 0xff,
                    ip shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get IP from WiFi", e)
        }

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get IP from network interfaces", e)
        }

        return "127.0.0.1"
    }

    companion object {
        const val ACTION_SERVER_STOPPED = "com.emanuel5014.trainable.SERVER_STOPPED"
    }
}

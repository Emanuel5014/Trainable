package com.emanuel5014.trainable.data.ai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class DeviceResourceMetrics(
    val cpuUsagePercent: Int = 0,
    val appRamUsedMb: Long = 0,
    val nativeHeapMb: Long = 0,
    val systemRamUsedGb: Float = 0f,
    val systemRamTotalGb: Float = 0f,
    val systemRamPercent: Int = 0,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val batteryPercent: Int? = null,
    val batteryTemperatureC: Float? = null,
    val isCharging: Boolean = false,
    val thermalStatus: String = "Normal",
    val isThermalThrottling: Boolean = false,
    val inferenceBackend: String = "CPU + GPU Vision",
    val tokensGenerated: Int = 0,
    val throughputTokPerSec: Float = 0f,
    val charsGenerated: Int = 0,
    val throughputCharsPerSec: Float = 0f
)

@Singleton
class AiResourceTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var lastCpuTimeMs: Long = 0
    private var lastCpuCheckWallMs: Long = 0
    private var currentCpuPercent: Int = 0

    fun captureMetrics(
        charsGenerated: Int = 0,
        elapsedSeconds: Int = 0
    ): DeviceResourceMetrics {
        // 1. CPU Usage % calculation
        val currentCpuTime = Process.getElapsedCpuTime()
        val currentWallTime = SystemClock.elapsedRealtime()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        if (lastCpuCheckWallMs > 0 && currentWallTime > lastCpuCheckWallMs) {
            val cpuDelta = currentCpuTime - lastCpuTimeMs
            val wallDelta = (currentWallTime - lastCpuCheckWallMs) * cores
            if (wallDelta > 0) {
                val usage = (cpuDelta.toFloat() / wallDelta.toFloat() * 100f).toInt().coerceIn(0, 100)
                currentCpuPercent = usage
            }
        }
        lastCpuTimeMs = currentCpuTime
        lastCpuCheckWallMs = currentWallTime

        // 2. Memory
        val runtime = Runtime.getRuntime()
        val appHeapUsedMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).coerceAtLeast(0)
        val nativeHeapMb = (Debug.getNativeHeapAllocatedSize() / (1024 * 1024)).coerceAtLeast(0)
        val totalAppRamMb = appHeapUsedMb + nativeHeapMb

        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalGb = (memInfo.totalMem / (1024f * 1024f * 1024f))
        val availGb = (memInfo.availMem / (1024f * 1024f * 1024f))
        val usedGb = (totalGb - availGb).coerceAtLeast(0f)
        val ramPercent = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt().coerceIn(0, 100) else 0

        // 3. Battery & Temperature
        var batteryLevel: Int? = null
        var batteryTempC: Float? = null
        var isCharging = false

        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level * 100) / scale
                }
                val rawTemp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (rawTemp > 0) {
                    batteryTempC = rawTemp / 10.0f
                }
                val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                isCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
            }
        } catch (_: Exception) {
        }

        // 4. Thermal State
        val thermalStatusStr: String
        var isThrottling = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> thermalStatusStr = "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> thermalStatusStr = "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> {
                    thermalStatusStr = "Moderate"
                    isThrottling = true
                }
                PowerManager.THERMAL_STATUS_SEVERE -> {
                    thermalStatusStr = "Severe"
                    isThrottling = true
                }
                PowerManager.THERMAL_STATUS_CRITICAL -> {
                    thermalStatusStr = "Critical"
                    isThrottling = true
                }
                PowerManager.THERMAL_STATUS_EMERGENCY -> {
                    thermalStatusStr = "Emergency"
                    isThrottling = true
                }
                PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                    thermalStatusStr = "Shutdown"
                    isThrottling = true
                }
                else -> thermalStatusStr = "Normal"
            }
        } else {
            thermalStatusStr = "Normal"
        }

        // 5. Throughput (approximate tokens: ~4 chars per token)
        val approxTokens = (charsGenerated / 4).coerceAtLeast(0)
        val validSeconds = elapsedSeconds.coerceAtLeast(1)
        val charsPerSec = if (charsGenerated > 0) charsGenerated.toFloat() / validSeconds else 0f
        val tokensPerSec = if (approxTokens > 0) approxTokens.toFloat() / validSeconds else 0f

        return DeviceResourceMetrics(
            cpuUsagePercent = currentCpuPercent,
            appRamUsedMb = totalAppRamMb,
            nativeHeapMb = nativeHeapMb,
            systemRamUsedGb = usedGb,
            systemRamTotalGb = totalGb,
            systemRamPercent = ramPercent,
            cpuCores = cores,
            batteryPercent = batteryLevel,
            batteryTemperatureC = batteryTempC,
            isCharging = isCharging,
            thermalStatus = thermalStatusStr,
            isThermalThrottling = isThrottling,
            inferenceBackend = "CPU + GPU Vision",
            tokensGenerated = approxTokens,
            throughputTokPerSec = tokensPerSec,
            charsGenerated = charsGenerated,
            throughputCharsPerSec = charsPerSec
        )
    }
}

package com.emanuel5014.trainable.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilityChecker @Inject constructor() {

    fun isSupported(context: Context): Boolean {
        return totalRamGb(context) >= MIN_RAM_GB && !isEmulator()
    }

    fun totalRamGb(context: Context): Double {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0.0
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT

    companion object {
        const val MIN_RAM_GB = 6
        const val MIN_FREE_STORAGE_BYTES = 3L * 1024 * 1024 * 1024
    }
}

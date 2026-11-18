package com.emanuel5014.trainable.ui.screens.exercise

import java.util.Locale

internal actual fun speedText(kmh: Float): String = String.format(Locale.getDefault(), "%.1f", kmh)

/** "1h 05m", "32m 10s", "45s". */
internal actual fun durationText(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> String.format(Locale.getDefault(), "%dh %02dm", h, m)
        m > 0 -> if (s > 0) String.format(Locale.getDefault(), "%dm %02ds", m, s) else "${m}m"
        else -> "${s}s"
    }
}

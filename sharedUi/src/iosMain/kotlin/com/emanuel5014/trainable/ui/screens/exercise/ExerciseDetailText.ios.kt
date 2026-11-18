package com.emanuel5014.trainable.ui.screens.exercise

import com.emanuel5014.trainable.platform.formatLocalDecimals

internal actual fun speedText(kmh: Float): String = formatLocalDecimals(kmh.toDouble(), 1)
internal actual fun durationText(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h ${m.toString().padStart(2, '0')}m"
        m > 0 -> if (s > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${m}m"
        else -> "${s}s"
    }
}

package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable

/** A rest as the sliders show it: "45s", "2m", "1m 30s". */
@Composable
fun formatRestTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes == 0 -> "${secs}s"
        secs == 0 -> "${minutes}m"
        else -> "${minutes}m ${secs}s"
    }
}

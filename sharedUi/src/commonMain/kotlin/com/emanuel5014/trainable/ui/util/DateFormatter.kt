package com.emanuel5014.trainable.ui.util

/** A day as the app writes it: "25/07/2026", or "25/07" in short. */
expect object DateFormatter {
    fun format(timestamp: Long): String

    fun formatShort(timestamp: Long): String
}

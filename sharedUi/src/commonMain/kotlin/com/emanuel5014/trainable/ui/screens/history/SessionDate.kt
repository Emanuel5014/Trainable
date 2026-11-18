package com.emanuel5014.trainable.ui.screens.history

/** A workout's day in the device's language: "05 Nov 2026", or "05 November 2026" with [longMonth]. */
internal expect fun formatSessionDate(timestampMillis: Long, longMonth: Boolean): String

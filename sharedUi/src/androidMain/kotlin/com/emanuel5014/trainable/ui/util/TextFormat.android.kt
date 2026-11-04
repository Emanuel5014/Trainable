package com.emanuel5014.trainable.ui.util

import java.text.DateFormat
import java.util.Date

actual fun formatShortTime(timestampMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestampMillis))

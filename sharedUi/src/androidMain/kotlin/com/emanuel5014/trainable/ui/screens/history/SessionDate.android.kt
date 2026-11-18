package com.emanuel5014.trainable.ui.screens.history

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun formatSessionDate(timestampMillis: Long, longMonth: Boolean): String =
    SimpleDateFormat(if (longMonth) "dd MMMM yyyy" else "dd MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))

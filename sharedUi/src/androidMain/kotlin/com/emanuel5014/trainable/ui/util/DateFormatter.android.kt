package com.emanuel5014.trainable.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual object DateFormatter {
    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    actual fun format(timestamp: Long): String {
        return formatter.format(Date(timestamp))
    }

    actual fun formatShort(timestamp: Long): String {
        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

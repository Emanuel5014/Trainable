package com.emanuel5014.trainable.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    fun format(timestamp: Long): String {
        return formatter.format(Date(timestamp))
    }

    fun formatShort(timestamp: Long): String {
        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

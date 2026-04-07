package com.example.gymtracking.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    fun format(timestamp: Long): String {
        return formatter.format(Date(timestamp))
    }
}

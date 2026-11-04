package com.emanuel5014.trainable.ui.screens.history

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

actual object MonthCalendar {
    actual fun currentMonthAndYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) to calendar.get(Calendar.YEAR)
    }

    actual fun monthRange(monthIndex: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    actual fun isWholeMonth(start: Long, end: Long, monthIndex: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = start
        val sMonth = cal.get(Calendar.MONTH)
        val sYear = cal.get(Calendar.YEAR)
        val sDay = cal.get(Calendar.DAY_OF_MONTH)

        cal.timeInMillis = end
        val eMonth = cal.get(Calendar.MONTH)
        val eYear = cal.get(Calendar.YEAR)
        val eDay = cal.get(Calendar.DAY_OF_MONTH)

        return sMonth == monthIndex && sYear == year && sDay == 1 &&
               eMonth == monthIndex && eYear == year && eDay == cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    actual fun shortMonthNames(): List<String> =
        DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths.filter { it.isNotEmpty() }
}

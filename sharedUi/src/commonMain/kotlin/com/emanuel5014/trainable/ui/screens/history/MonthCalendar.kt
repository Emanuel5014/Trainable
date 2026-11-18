package com.emanuel5014.trainable.ui.screens.history

/** The months of the history filter, in the phone's calendar and time zone. Months count from 0 (January). */
expect object MonthCalendar {
    fun currentMonthAndYear(): Pair<Int, Int>

    /** From the first millisecond of the month to the last, in local time. */
    fun monthRange(monthIndex: Int, year: Int): Pair<Long, Long>

    /** Whether [start]..[end] is exactly that month: its first day to its last. */
    fun isWholeMonth(start: Long, end: Long, monthIndex: Int, year: Int): Boolean

    /** "Jan", "Feb"... in the app's language. */
    fun shortMonthNames(): List<String>
}

package com.emanuel5014.trainable.ui.screens.analytics

/**
 * The words the analytics state writes into its cards outside of composition. Each app provides them: Android from
 * its resources (as the state always did), iOS from the shared strings.
 */
interface AnalyticsText {
    val compareVolume: String
    val compareSessions: String
    val trainingDays: String
    val thisWeek: String
    val thisMonth: String
    val previous: String
    fun timeRange(range: AnalyticsTimeRange): String
    val consistencyNoSessions: String
    fun consistencySummary(completed: Int, expected: Int): String
    val strengthNoData: String
    /** [value] is already formatted ("2,5"). */
    fun strengthPositive(value: String): String
    fun strengthNegative(value: String): String
    /** Day and month in numbers ("05/10"), for the ranges compared. */
    fun shortDate(timestampMillis: Long): String
}

/** The start of the period a category-volume card covers, in the phone's calendar and time zone. */
expect object AnalyticsCalendar {
    /** Monday of this week at midnight. */
    fun startOfWeek(): Long

    /** The first of this month at midnight. */
    fun startOfMonth(): Long

    /** The first of the month five months ago at midnight: six calendar months with this one. */
    fun startOfSixMonths(): Long
}

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsText
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsTimeRange
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.ui.util.fillPlaceholders
import org.jetbrains.compose.resources.stringResource

/** Resolve localized resources in composition; the state can fill them on any coroutine. */
@Composable
internal fun rememberIosAnalyticsText(): AnalyticsText {
    val text = IosAnalyticsText(
        compareVolume = stringResource(Res.string.compare_volume),
        compareSessions = stringResource(Res.string.compare_sessions),
        trainingDays = stringResource(Res.string.analytics_training_days),
        thisWeek = stringResource(Res.string.this_week),
        thisMonth = stringResource(Res.string.this_month),
        previous = stringResource(Res.string.previous_exercise),
        ranges = AnalyticsTimeRange.entries.associateWith { stringResource(it.labelResId) },
        consistencyNoSessions = stringResource(Res.string.analytics_consistency_no_sessions),
        consistency = stringResource(Res.string.analytics_consistency_summary),
        strengthNoData = stringResource(Res.string.analytics_strength_no_data),
        positive = stringResource(Res.string.analytics_strength_summary_positive),
        negative = stringResource(Res.string.analytics_strength_summary_negative)
    )
    return remember(text) { text }
}

private data class IosAnalyticsText(
    override val compareVolume: String,
    override val compareSessions: String,
    override val trainingDays: String,
    override val thisWeek: String,
    override val thisMonth: String,
    override val previous: String,
    val ranges: Map<AnalyticsTimeRange, String>,
    override val consistencyNoSessions: String,
    val consistency: String,
    override val strengthNoData: String,
    val positive: String,
    val negative: String
) : AnalyticsText {
    override fun timeRange(range: AnalyticsTimeRange): String = ranges.getValue(range)
    override fun consistencySummary(completed: Int, expected: Int): String = fillPlaceholders(consistency, completed, expected)
    override fun strengthPositive(value: String): String = fillPlaceholders(positive, value).replace("%%", "%")
    override fun strengthNegative(value: String): String = fillPlaceholders(negative, value).replace("%%", "%")
    override fun shortDate(timestampMillis: Long): String = DateFormatter.formatShort(timestampMillis)
}

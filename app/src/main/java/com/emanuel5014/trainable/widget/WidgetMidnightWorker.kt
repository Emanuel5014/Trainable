package com.emanuel5014.trainable.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Time from [now] to the next midnight in [zone], plus [offsetMinutes] so the new day has surely started. */
fun delayToNextMidnight(now: Instant, zone: TimeZone, offsetMinutes: Int = 1): Duration {
    val tomorrow = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
    return tomorrow.atStartOfDayIn(zone) - now + offsetMinutes.minutes
}

/**
 * Redraws the widgets just after midnight, so "today", the suggested routine and the week roll over without opening
 * the app, then plans the next night. An inexact one-time job: no exact-alarm permission needed.
 */
class WidgetMidnightWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.updateAllNow(applicationContext)
        // Appended after this run: REPLACE would cancel the work that is running now
        schedule(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_midnight"

        /** At app start KEEP leaves an already planned night alone; the worker itself appends the next one. */
        fun schedule(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val delay = delayToNextMidnight(Clock.System.now(), TimeZone.currentSystemDefault())
            val request = OneTimeWorkRequestBuilder<WidgetMidnightWorker>()
                .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}

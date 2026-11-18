package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock

/** How long a workout has run, always as hours, minutes and seconds: "0:42:07", "1:05:30". */
object WorkoutElapsedClock {
    fun format(elapsedMillis: Long): String {
        val total = (elapsedMillis / 1000L).coerceAtLeast(0L)
        val h = total / 3600L
        val m = ((total % 3600L) / 60L).toString().padStart(2, '0')
        val s = (total % 60L).toString().padStart(2, '0')
        return "$h:$m:$s"
    }
}

/**
 * The clock of a workout started at [startedAt], moving on the second while [isActive] (stopped while the screen
 * is not shown). Only the caller recomposes every second, so keep it in the smallest composable that shows it.
 */
@Composable
fun rememberWorkoutElapsed(startedAt: Long, isActive: Boolean = true): String {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(startedAt, isActive) {
        now = Clock.System.now().toEpochMilliseconds()
        while (isActive) {
            // On the tick of the workout's own second, so the shown seconds never skip one
            val sinceStart = (now - startedAt).coerceAtLeast(0L)
            delay(1000L - sinceStart % 1000L)
            now = Clock.System.now().toEpochMilliseconds()
        }
    }
    return WorkoutElapsedClock.format(now - startedAt)
}

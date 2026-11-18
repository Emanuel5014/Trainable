package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.ui.components.WorkoutElapsedClock
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutElapsedClockTest {
    @Test
    fun alwaysHoursMinutesSeconds() {
        assertEquals("0:00:00", WorkoutElapsedClock.format(0L))
        assertEquals("0:00:59", WorkoutElapsedClock.format(59_999L))
        assertEquals("0:42:07", WorkoutElapsedClock.format((42 * 60 + 7) * 1000L))
        assertEquals("1:05:30", WorkoutElapsedClock.format((3600 + 5 * 60 + 30) * 1000L))
        assertEquals("12:00:00", WorkoutElapsedClock.format(12 * 3600 * 1000L))
    }

    @Test
    fun aStartInTheFutureShowsZero() {
        assertEquals("0:00:00", WorkoutElapsedClock.format(-5_000L))
    }
}

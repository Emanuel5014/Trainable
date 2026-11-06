package com.emanuel5014.trainable.ui

import kotlin.test.*

private class FakeLiveActivities(var available: Boolean = true) : IosWorkoutLiveActivities {
    val timers = mutableMapOf<String, IosLiveTimer>()
    var updates = 0
    override fun isAvailable() = available
    override fun update(timer: IosLiveTimer) { timers[timer.kind] = timer; updates++ }
    override fun cancel(kind: String) { timers.remove(kind) }
    override fun cancelAll() { timers.clear() }
}

class IosLiveActivitiesTest {
    private val words = IosNotificationWords("Rest", "Go", "Warmup", "Ready", "Membership", "In %1\$d days",
        "Round %1\$d/%2\$d", "Complete", "Get ready")
    private fun timers(delivery: FakeNotificationDelivery, live: FakeLiveActivities, now: () -> Long = { 1_000_000L }) =
        IosWorkoutTimers(IosNotificationScheduler(delivery), { words }, now, { live })
    private fun rest(t: IosWorkoutTimers, seconds: Int, total: Int = seconds) = t.startOrUpdateTimerNotification(
        seconds, 1, "Squat", 2, 60f, 8, 8, "kg", total, true, "8", "60 kg × 8", null)

    @Test fun nextSetIsSplitIntoExerciseAndSetLineForEveryTemplate() {
        val it = IosNotificationWords("R", "G", "W", "Ready", "M", "%1\$d", "%1\$d/%2\$d", "C", "G",
            "Prossima: %1\$s - %2\$dª serie: %3\$s × %4\$d", "Prossima: %1\$s - %2\$dª serie: %3\$s × %4\$s",
            "Prossima: %1\$s - %2\$dª serie: %3\$s", "(ultima: %1\$d)")
        assertEquals("Panca - piana" to "2ª serie: 60 kg × 8 (ultima: 6)",
            it.nextSetParts("Panca - piana", 2, 60f, 8, 6, "kg", null, null, null))
        assertEquals("Squat" to "Set 3: 60 kg × 6-8 · RPE 8",
            words.nextSetParts("Squat", 3, 60f, 8, null, "kg", "6-8", "RPE 8", null))
        assertEquals("Plank" to "Set 1: 45s", words.nextSetParts("Plank", 1, 0f, 0, null, "kg", null, null, "45s"))
        assertEquals("" to "", words.nextSetParts(null, 1, 60f, 8, null, "kg", null, null, null))
    }
    @Test fun restActivityCarriesSplitTextAndThemeAccent() {
        val d = FakeNotificationDelivery(); val live = FakeLiveActivities(); val t = timers(d, live)
        IosLiveActivityStyle.accentArgb = 0xFF7AC6D2L
        try {
            rest(t, 30)
            val timer = live.timers.getValue(REST_NOTICE)
            assertEquals("Squat", timer.exercise); assertEquals("Set 2: 60 kg × 8 · 60 kg × 8", timer.setLine)
            assertEquals(0xFF7AC6D2L, timer.accentArgb)
            assertTrue(timer.detail.startsWith("Next: Squat - Set 2"))
        } finally { IosLiveActivityStyle.accentArgb = 0 }
    }
    @Test fun activityAndNotificationUseOneDeadlineEvenIfClockMovesBetweenCalls() {
        val delivery = FakeNotificationDelivery(); val live = FakeLiveActivities(); var now = 1_000_000L
        val timers = timers(delivery, live) { now++ }
        rest(timers, 30)
        assertEquals(delivery.notices[REST_NOTICE]?.at, live.timers[REST_NOTICE]?.endsAtMillis)
        assertEquals(1_030_000L, live.timers[REST_NOTICE]?.endsAtMillis)
    }
    @Test fun durationAdjustmentUpdatesOneLogicalActivityAndNextTimerGetsNewIdentity() {
        val d = FakeNotificationDelivery(); val live = FakeLiveActivities(); val t = timers(d, live)
        rest(t, 30)
        val first = live.timers.getValue(REST_NOTICE)
        rest(t, 50, 60)
        val adjusted = live.timers.getValue(REST_NOTICE)
        assertEquals(first.instanceId, adjusted.instanceId)
        assertEquals(1_050_000L, adjusted.endsAtMillis); assertEquals(60, adjusted.totalSeconds)
        assertTrue(adjusted.detail.contains("Squat")); assertTrue(adjusted.detail.contains("60 kg × 8"))
        t.cancelTimer(); assertTrue(live.timers.isEmpty())
        rest(t, 30)
        assertNotEquals(first.instanceId, live.timers.getValue(REST_NOTICE).instanceId)
    }
    @Test fun restAndWarmupAreIndependentAndFinishingOneDoesNotClearTheOther() {
        val d = FakeNotificationDelivery(); val live = FakeLiveActivities(); val t = timers(d, live)
        rest(t, 30); t.startOrUpdateWarmupTimerNotification(60, 60, true)
        assertEquals(setOf(REST_NOTICE, WARMUP_NOTICE), live.timers.keys)
        assertNotEquals(live.timers.getValue(REST_NOTICE).instanceId, live.timers.getValue(WARMUP_NOTICE).instanceId)
        t.cancelFinishAlarm(); t.showRestFinished()
        assertEquals(setOf(WARMUP_NOTICE), live.timers.keys); assertEquals(2, d.puts)
        t.cancelWarmupFinishAlarm(); t.showWarmupTimerFinished()
        assertTrue(live.timers.isEmpty()); assertEquals(2, d.puts)
    }
    @Test fun nativeCountdownDoesNotNeedPerSecondUpdatesOrLiveAndroidPath() {
        val d = FakeNotificationDelivery(); val live = FakeLiveActivities(); val t = timers(d, live)
        t.startOrUpdateWarmupTimerNotification(60, 60, true)
        t.startOrUpdateWarmupTimerNotification(58, 60, false)
        assertEquals(1, live.updates); assertEquals(1_060_000L, live.timers.getValue(WARMUP_NOTICE).endsAtMillis)
        assertFalse(t.isLiveNotificationSupported())
    }
    @Test fun liveActivityCanWorkWhenNotificationPermissionWasDenied() {
        val d = FakeNotificationDelivery().apply { canAuthorize = false; granted = false }
        val live = FakeLiveActivities(); val t = timers(d, live)
        assertTrue(t.hasNotificationPermission())
        rest(t, 30)
        assertTrue(d.notices.isEmpty()); assertEquals(1, live.timers.size)
    }
    @Test fun disabledLiveActivitiesKeepExistingNotificationDelivery() {
        val d = FakeNotificationDelivery(); val live = FakeLiveActivities(available = false); val t = timers(d, live)
        rest(t, 30)
        assertEquals(1, d.notices.size); assertTrue(live.timers.isEmpty())
        d.canAuthorize = false
        assertFalse(t.hasNotificationPermission())
    }
    @Test fun cancellingWhilePermissionDialogIsPendingClearsBothSurfaces() {
        val d = FakeNotificationDelivery().apply { defer = true }; val live = FakeLiveActivities(); val t = timers(d, live)
        rest(t, 30); t.cancelTimer(); d.resolve()
        assertTrue(d.notices.isEmpty()); assertTrue(live.timers.isEmpty())
    }
}

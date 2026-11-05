package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.util.notification.*
import kotlinx.datetime.TimeZone
import kotlin.test.*
import kotlin.time.Instant

internal class FakeNotificationDelivery : IosNotificationDelivery {
    override var canAuthorize = true
    var defer = false
    var granted = true
    val callbacks = mutableListOf<(Boolean) -> Unit>()
    val notices = mutableMapOf<String, IosNotice>()
    val removed = mutableListOf<String>()
    var prompts = 0
    var puts = 0
    override fun authorize(prompt: Boolean, result: (Boolean) -> Unit) {
        if (prompt) prompts++
        if (defer) callbacks += result else result(granted)
    }
    override fun put(notice: IosNotice) { notices[notice.id] = notice; puts++; notice.onScheduled() }
    override fun remove(ids: List<String>, delivered: Boolean) { removed += ids; ids.forEach(notices::remove) }
    fun resolve() { callbacks.toList().also { callbacks.clear() }.forEach { it(granted) } }
}

class IosNotificationsTest {
    private val words = IosNotificationWords("Rest", "Go", "Warmup", "Ready", "Membership", "In %1\$d days",
        "Round %1\$d/%2\$d", "Complete", "Get ready")
    private fun timers(delivery: FakeNotificationDelivery) = IosWorkoutTimers(IosNotificationScheduler(delivery), { words }, { 1_000_000L })
    private fun rest(t: IosWorkoutTimers, seconds: Int) = t.startOrUpdateTimerNotification(seconds, 1, "Squat", 2,
        60f, 8, 8, "kg", seconds, true, "8", "60 kg × 8", null)
    private fun emom(t: IosWorkoutTimers, elapsed: Int = 0, rounds: Int = 3, leadIn: Boolean = false) {
        t.showEmomNotification(EmomNotificationInfo(if (leadIn) "Get ready" else "EMOM", "Squat", null, 1_010_000,
            elapsed, rounds, emptyList()))
        t.scheduleEmomBoundaryAlarm(1_010_000)
    }
    @Test fun cancellationDuringPermissionPromptCannotResurrectAnAlert() {
        val d = FakeNotificationDelivery().apply { defer = true }; val t = timers(d)
        rest(t, 20); t.cancelTimer(); d.resolve()
        assertTrue(d.notices.isEmpty())
    }
    @Test fun onlyLatestAdjustmentSurvivesPermissionPrompt() {
        val d = FakeNotificationDelivery().apply { defer = true }; val t = timers(d)
        rest(t, 20); rest(t, 35); d.resolve()
        assertEquals(1, d.puts); assertEquals(1_035_000, d.notices[REST_NOTICE]?.at)
    }
    @Test fun independentTimersReplaceCancelAndNeverRepeatOnResume() {
        val d = FakeNotificationDelivery(); val t = timers(d)
        rest(t, 20); t.startOrUpdateWarmupTimerNotification(40, 40, true)
        assertEquals(2, d.notices.size)
        t.cancelFinishAlarm(); t.showRestFinished(); t.cancelWarmupFinishAlarm(); t.showWarmupTimerFinished()
        assertEquals(2, d.puts)
        t.cancelTimer(); assertEquals(setOf(WARMUP_NOTICE), d.notices.keys)
        t.cancelWarmupTimer(); assertTrue(d.notices.isEmpty())
    }
    @Test fun deniedPermissionPostsNothing() {
        val d = FakeNotificationDelivery().apply { canAuthorize = false; granted = false }; val t = timers(d)
        assertFalse(t.hasNotificationPermission()); assertFalse(t.isLiveNotificationSupported())
        rest(t, 20); assertTrue(d.notices.isEmpty())
    }
    @Test fun nonSchedulingLiveUpdatesCannotMoveTheDeadline() {
        val d = FakeNotificationDelivery(); val t = timers(d)
        t.startOrUpdateWarmupTimerNotification(30, 30, true)
        t.startOrUpdateWarmupTimerNotification(22, 30, false)
        assertEquals(1_030_000, d.notices[WARMUP_NOTICE]?.at); assertEquals(1, d.puts)
    }
    @Test fun emomPreSchedulesLeadInEveryRoundAndFinish() {
        val d = FakeNotificationDelivery(); val t = timers(d)
        emom(t, leadIn = true)
        assertEquals(listOf(1_010_000L, 1_070_000L, 1_130_000L, 1_190_000L), d.notices.values.map { it.at })
        assertEquals(listOf("Round 1/3", "Round 2/3", "Round 3/3", "Complete"), d.notices.values.map { it.title })
        t.showEmomFinished("Complete", "Done"); assertEquals(4, d.puts)
        t.cancelEmom(); assertTrue(d.notices.isEmpty())
    }
    @Test fun emomPauseCancelsAndResumeReplacesAllBoundaries() {
        val d = FakeNotificationDelivery(); val t = timers(d)
        emom(t); t.cancelEmomBoundaryAlarm(); assertTrue(d.notices.isEmpty())
        emom(t, elapsed = 60); assertEquals(2, d.notices.size)
        assertEquals("Round 3/3", d.notices["workout.emom.0"]?.title)
    }
    @Test fun veryLongEmomReservesSlotsAndRetainsFinish() {
        val d = FakeNotificationDelivery(); emom(timers(d), rounds = 120)
        assertEquals(60, d.notices.size)
        assertEquals("Complete", d.notices.values.last().title)
        assertEquals(1_010_000L + 119 * 60_000L, d.notices.values.last().at)
    }
    @Test fun stoppedEmomCannotReappearAfterGrant() {
        val d = FakeNotificationDelivery().apply { defer = true }; val t = timers(d)
        emom(t); t.cancelEmom(); d.resolve(); assertTrue(d.notices.isEmpty())
    }
    @Test fun calendarReminderUsesLocalNineAcrossDst() {
        val result = membershipNoticeAt(Instant.parse("2026-10-27T00:00:00Z").toEpochMilliseconds(), 3,
            Instant.parse("2026-10-20T00:00:00Z").toEpochMilliseconds(), TimeZone.of("Europe/Rome"))!!
        assertEquals(Instant.parse("2026-10-24T07:00:00Z").toEpochMilliseconds(), result.first)
        assertEquals(3, result.second)
    }
    @Test fun expiredMembershipDoesNotNotifyAndNearExpiryNotifiesOnceNow() {
        val now = Instant.parse("2026-11-05T13:00:00Z").toEpochMilliseconds()
        assertNull(membershipNoticeAt(Instant.parse("2026-11-04T00:00:00Z").toEpochMilliseconds(), 3, now, TimeZone.UTC))
        val near = membershipNoticeAt(Instant.parse("2026-11-06T00:00:00Z").toEpochMilliseconds(), 3, now, TimeZone.UTC)!!
        assertEquals(now + 1000, near.first); assertEquals(1, near.second)
    }
}

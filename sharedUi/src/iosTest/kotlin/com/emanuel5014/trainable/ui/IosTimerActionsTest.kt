package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.*

private class ActionLive : IosWorkoutLiveActivities {
    val timers = mutableMapOf<String, IosLiveTimer>()
    override fun isAvailable() = true
    override fun update(timer: IosLiveTimer) { timers[timer.kind] = timer }
    override fun cancel(kind: String) { timers.remove(kind) }
    override fun cancelAll() { timers.clear() }
}
private class ActionStore : IosTimerActionStore {
    var value = WorkoutSessionEntity(id = 7, planId = 1, timestamp = 100,
        restTimerEndTime = 1_030_000, totalRestSeconds = 30,
        warmupTimerEndTime = 1_060_000, totalWarmupSeconds = 60)
    var config = IosTimerControls(true, 45, 10, true, true, true)
    var writes = 0
    var loseRace = false
    var miniSetChanges = 0
    override suspend fun session(id: Int) = value.takeIf { it.id == id }
    override suspend fun controls() = config
    override suspend fun compareAndSet(kind: String, session: WorkoutSessionEntity, expectedEnd: Long, end: Long?, total: Int?): Boolean {
        if (loseRace) { value = value.copy(restTimerEndTime = 2_000_000); return false }
        val old = if (kind == REST_NOTICE) value.restTimerEndTime else value.warmupTimerEndTime
        if (value.isFinished || value.timestamp != session.timestamp || old != expectedEnd) return false
        value = if (kind == REST_NOTICE) value.copy(restTimerEndTime = end, totalRestSeconds = total)
            else value.copy(warmupTimerEndTime = end, totalWarmupSeconds = total)
        writes++
        return true
    }
    override fun miniSetChanged(sessionId: Int, end: Long?, now: Long) { miniSetChanges++ }
}

class IosTimerActionsTest {
    @BeforeTest fun reset() = IosLiveTimerSnapshots.clear()
    @AfterTest fun clean() = IosLiveTimerSnapshots.clear()
    private fun request(kind: String = REST_NOTICE) = IosLiveTimer(kind, "timer-A", "Rest", "Next: Squat - Set 2: 20 kg × 8",
        if (kind == REST_NOTICE) 1_030_000 else 1_060_000, if (kind == REST_NOTICE) 30 else 60,
        sessionId = 7, sessionStartedAtMillis = 100, finishedText = "Ready")

    @Test fun coldAddPersistsLatestConfiguredIncrementAndReschedulesOneDeadline() = runBlocking {
        val store = ActionStore(); val d = FakeNotificationDelivery(); val live = ActionLive()
        val p = IosTimerActionProcessor(store, IosNotificationScheduler(d), { live }, now = { 1_000_000 })
        assertTrue(p.perform(request(), "add"))
        assertEquals(1_075_000L, store.value.restTimerEndTime); assertEquals(75, store.value.totalRestSeconds)
        assertEquals(1_075_000L, d.notices[REST_NOTICE]?.at)
        assertEquals(1_075_000L, live.timers[REST_NOTICE]?.endsAtMillis)
        assertEquals(45, live.timers[REST_NOTICE]?.addSeconds)
        assertEquals(1_060_000L, store.value.warmupTimerEndTime)
    }
    @Test fun rapidTapsUsingOldPresentationApplyToLatestTimerInsteadOfLosingAnIncrement() = runBlocking {
        val s = ActionStore(); val p = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { null }, now = { 1_000_000 })
        assertTrue(p.perform(request(), "add")); assertTrue(p.perform(request(), "add"))
        assertEquals(1_120_000L, s.value.restTimerEndTime); assertEquals(120, s.value.totalRestSeconds)
        assertEquals(2, s.writes)
    }
    @Test fun subtractBeyondZeroClearsTimerAndDoesNotPostAnEndAlert() = runBlocking {
        val s = ActionStore().apply { config = config.copy(subtractSeconds = 45) }; val d = FakeNotificationDelivery()
        val live = ActionLive().also { it.update(request()) }
        val p = IosTimerActionProcessor(s, IosNotificationScheduler(d), { live }, now = { 1_000_000 })
        assertTrue(p.perform(request(), "subtract"))
        assertNull(s.value.restTimerEndTime); assertNull(s.value.totalRestSeconds)
        assertTrue(d.notices.isEmpty()); assertTrue(live.timers.isEmpty()); assertEquals(1, s.miniSetChanges)
    }
    @Test fun warmupSkipLeavesRestAndMiniSetUntouched() = runBlocking {
        val s = ActionStore(); val d = FakeNotificationDelivery()
        val p = IosTimerActionProcessor(s, IosNotificationScheduler(d), { null }, now = { 1_000_000 })
        assertTrue(p.perform(request(WARMUP_NOTICE), "skip"))
        assertNull(s.value.warmupTimerEndTime); assertNull(s.value.totalWarmupSeconds)
        assertEquals(1_030_000L, s.value.restTimerEndTime); assertEquals(0, s.miniSetChanges)
        assertTrue(d.notices.isEmpty())
    }
    @Test fun hiddenControlsCannotMutateStateAndPresentationIsRefreshed() = runBlocking {
        val s = ActionStore().apply { config = config.copy(showAdd = false) }; val live = ActionLive()
        val p = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { live }, now = { 1_000_000 })
        assertFalse(p.perform(request(), "add")); assertEquals(0, s.writes)
        assertEquals(false, live.timers[REST_NOTICE]?.showAdd)
    }
    @Test fun oldIdentityFinishedSessionAndReusedSessionIdCannotChangeData() = runBlocking {
        val s = ActionStore(); val p = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { null }, now = { 1_000_000 })
        IosLiveTimerSnapshots.remember(request().copy(instanceId = "new-timer"))
        assertFalse(p.perform(request(), "skip")); assertEquals(0, s.writes)
        IosLiveTimerSnapshots.clear(); s.value = s.value.copy(timestamp = 101)
        assertFalse(p.perform(request(), "add")); assertEquals(0, s.writes)
        s.value = s.value.copy(timestamp = 100, isFinished = true)
        assertFalse(p.perform(request(), "subtract")); assertEquals(0, s.writes)
    }
    @Test fun expiredAndUnrelatedDeadlinesNeverRevive() = runBlocking {
        val s = ActionStore(); val p = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { null }, now = { 1_030_001 })
        assertFalse(p.perform(request(), "add")); assertEquals(0, s.writes)
        val p2 = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { null }, now = { 1_000_000 })
        s.value = s.value.copy(restTimerEndTime = 1_060_000)
        assertFalse(p2.perform(request(), "skip")); assertEquals(0, s.writes)
    }
    @Test fun conditionalWriteDoesNotOverwriteANewerUiTimer() = runBlocking {
        val s = ActionStore().apply { loseRace = true }; val d = FakeNotificationDelivery()
        val p = IosTimerActionProcessor(s, IosNotificationScheduler(d), { null }, now = { 1_000_000 })
        assertFalse(p.perform(request(), "add")); assertEquals(2_000_000L, s.value.restTimerEndTime)
        assertEquals(0, s.writes); assertTrue(d.notices.isEmpty())
    }
    @Test fun disabledNotificationsAndUnknownActionCannotWrite() = runBlocking {
        val s = ActionStore().apply { config = config.copy(enabled = false) }
        val p = IosTimerActionProcessor(s, IosNotificationScheduler(FakeNotificationDelivery()), { null }, now = { 1_000_000 })
        assertFalse(p.perform(request(), "add")); assertFalse(p.perform(request(), "complete-workout")); assertEquals(0, s.writes)
    }
}

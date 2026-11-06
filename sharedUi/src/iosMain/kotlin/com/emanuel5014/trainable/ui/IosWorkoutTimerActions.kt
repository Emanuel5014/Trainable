package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.ui.screens.workout.PersistedMiniSet
import com.emanuel5014.trainable.ui.screens.workout.WorkoutModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.emanuel5014.trainable.util.notification.EmomAction
import kotlin.math.abs

/** Latest logical timer identity, also rejects actions from an old Dynamic Island presentation. */
internal object IosLiveTimerSnapshots {
    private val values = mutableMapOf<String, IosLiveTimer>()
    fun get(kind: String): IosLiveTimer? = values[kind]
    fun remember(timer: IosLiveTimer) { values[timer.kind] = timer }
    fun forget(kind: String, instance: String?) {
        if (instance == null || values[kind]?.instanceId == instance) values.remove(kind)
    }
    fun clear() = values.clear()
}

internal data class IosTimerControls(val enabled: Boolean, val addSeconds: Int, val subtractSeconds: Int,
    val showAdd: Boolean, val showSubtract: Boolean, val showSkip: Boolean) {
    fun allows(action: String): Boolean = enabled && when (action) {
        "add" -> showAdd
        "subtract" -> showSubtract
        "skip" -> showSkip
        else -> false
    }
    fun apply(timer: IosLiveTimer) = timer.copy(addSeconds = addSeconds, subtractSeconds = subtractSeconds,
        showAdd = showAdd, showSubtract = showSubtract, showSkip = showSkip)
}

internal interface IosTimerActionStore {
    suspend fun session(id: Int): WorkoutSessionEntity?
    suspend fun controls(): IosTimerControls
    suspend fun compareAndSet(kind: String, session: WorkoutSessionEntity, expectedEnd: Long, end: Long?, total: Int?): Boolean
    fun miniSetChanged(sessionId: Int, end: Long?, now: Long) {}
}

/** Uses the existing model while it is owned by navigation; otherwise updates only persisted timer columns. */
internal class IosTimerActionProcessor(
    private val store: IosTimerActionStore,
    private val scheduler: IosNotificationScheduler,
    private val live: () -> IosWorkoutLiveActivities?,
    private val modelFor: (Int) -> WorkoutModel? = { null },
    private val now: () -> Long = ::nowMillis
) {
    suspend fun perform(request: IosLiveTimer, action: String): Boolean {
        if (request.kind !in listOf(REST_NOTICE, WARMUP_NOTICE) || action !in listOf("add", "subtract", "skip")) return false
        if (request.sessionId < 0 || request.sessionStartedAtMillis <= 0) return false
        val current = IosLiveTimerSnapshots.get(request.kind) ?: request
        if (current.instanceId != request.instanceId || current.sessionId != request.sessionId) return false
        val controls = store.controls()
        if (!controls.allows(action)) {
            if (!controls.enabled) clear(current) else publish(controls.apply(current))
            return false
        }
        val session = store.session(current.sessionId)
        if (session == null || session.isFinished || session.timestamp != current.sessionStartedAtMillis) {
            clear(current)
            return false
        }
        val model = modelFor(current.sessionId)
        val active = model?.state?.value?.takeIf { it.sessionId == current.sessionId && !it.isFinished }
        val savedEnd = if (current.kind == REST_NOTICE) session.restTimerEndTime else session.warmupTimerEndTime
        val currentEnd = if (active != null) {
            if (current.kind == REST_NOTICE) active.restTimerEndTime else active.warmupTimerEndTime
        } else savedEnd
        if (currentEnd == null || currentEnd <= now()) { clear(current); return false }
        // The existing seconds-based notification contract floors the deadline by <1s when resuming/adjusting.
        if (active == null && abs(currentEnd - current.endsAtMillis) > 1_500) return false
        val delta = when (action) { "add" -> controls.addSeconds; "subtract" -> -controls.subtractSeconds; else -> 0 }
        if (model != null && active != null) {
            if (current.kind == REST_NOTICE) {
                if (action == "skip") model.skipRestTimer() else model.adjustRestTime(delta)
            } else {
                if (action == "skip") model.skipWarmupTimer() else model.adjustWarmupTime(delta)
            }
            val updated = model.state.value
            val end = if (current.kind == REST_NOTICE) updated.restTimerEndTime else updated.warmupTimerEndTime
            val total = if (end == null) null else if (current.kind == REST_NOTICE) updated.totalRestSeconds else updated.warmupTimerTotalSeconds
            // Wait for a database mutation before the intent returns. A queued model save may already have won;
            // compare-and-set never overwrites a subsequent UI change. Both writers carry the SAME target value.
            if (savedEnd != null) store.compareAndSet(current.kind, session, savedEnd, end, total)
            if (current.kind == REST_NOTICE) store.miniSetChanged(current.sessionId, end, now())
            return true
        }
        val targetEnd = currentEnd + delta * 1000L
        val remaining = ((targetEnd - now()) / 1000L).toInt().coerceAtLeast(0)
        val end = targetEnd.takeUnless { action == "skip" || (delta < 0 && remaining <= 0) }
        val oldTotal = if (current.kind == REST_NOTICE) session.totalRestSeconds else session.totalWarmupSeconds
        val total = if (end == null) null else ((oldTotal ?: current.totalSeconds).toLong() + delta)
            .coerceIn(0, Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(remaining)
        if (!store.compareAndSet(current.kind, session, currentEnd, end, total)) return false
        if (current.kind == REST_NOTICE) store.miniSetChanged(current.sessionId, end, now())
        if (end == null) clear(current) else {
            val updated = controls.apply(current.copy(endsAtMillis = end, totalSeconds = total!!))
            IosLiveTimerSnapshots.remember(updated)
            val body = updated.finishedText + if (updated.kind == REST_NOTICE && updated.detail.isNotEmpty()) " — ${updated.detail}" else ""
            scheduler.schedule(listOf(IosNotice(updated.kind, updated.title, body, end)))
            live()?.update(updated)
        }
        return true
    }

    private fun publish(timer: IosLiveTimer) { IosLiveTimerSnapshots.remember(timer); live()?.update(timer) }
    private fun clear(timer: IosLiveTimer) {
        IosLiveTimerSnapshots.forget(timer.kind, timer.instanceId)
        scheduler.cancel(listOf(timer.kind))
        live()?.cancel(timer.kind)
    }
}

/** LiveActivityIntent runs in the app process, including a cold background launch with no screen/model. */
object IosWorkoutTimerActions {
    private val lock = Mutex()
    private val models = mutableListOf<WorkoutModel>()
    private val emomTimers = mutableMapOf<WorkoutModel, IosWorkoutTimers>()
    internal fun bind(model: WorkoutModel, timers: IosWorkoutTimers? = null) {
        models += model
        if (timers != null) emomTimers[model] = timers
    }
    internal fun unbind(model: WorkoutModel) { models.remove(model); emomTimers.remove(model) }

    suspend fun perform(request: IosLiveTimer, action: String): Boolean = withContext(Dispatchers.Main) {
        lock.withLock {
            try {
                if (request.kind == EMOM_NOTICE) return@withLock performEmom(request, action)
                if (!prepareIosTimerAction()) return@withLock false
                IosTimerActionProcessor(IosAppTimerActionStore, IosNotifications.scheduler,
                    { IosWorkoutLiveActivityBridge.service },
                    { id -> models.lastOrNull { it.state.value.sessionId == id } }).perform(request, action)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { false }
        }
    }

    /**
     * An EMOM run only lives in the open workout (a reopened workout parks it, paused, before the next set), so
     * a button pressed after the app was terminated just removes the card.
     */
    private suspend fun performEmom(request: IosLiveTimer, action: String): Boolean {
        val model = models.lastOrNull { it.state.value.sessionId == request.sessionId }
        val timers = model?.let { emomTimers[it] }
        if (model == null || timers == null || timers.emomLiveInstance != request.instanceId || model.state.value.emomRun == null) {
            IosLiveTimerSnapshots.forget(EMOM_NOTICE, request.instanceId)
            IosWorkoutLiveActivityBridge.service?.cancel(EMOM_NOTICE)
            // The run comes back paused on reopen, so the remaining minute alerts would only mislead.
            if (model == null) IosNotifications.cancelEmom()
            return false
        }
        timers.sendEmom(emomActionOf(action))
        // Let the model handle the events on this (main) dispatcher before the intent returns.
        repeat(5) { yield() }
        delay(100)
        return true
    }
}

internal fun emomActionOf(key: String): EmomAction? = when (key) {
    "emom.done" -> EmomAction.DONE
    "emom.pause" -> EmomAction.PAUSE
    "emom.resume" -> EmomAction.RESUME
    "emom.stop" -> EmomAction.STOP
    else -> null
}

private object IosAppTimerActionStore : IosTimerActionStore {
    override suspend fun session(id: Int) = IosAppGraph.workouts.getSessionWithSets(id).first()?.session
    override suspend fun controls(): IosTimerControls = IosAppGraph.preferences.let { p ->
        val showTime = p.timerShowTimeButtons.first()
        IosTimerControls(p.timerNotificationsEnabled.first(), p.timerAddSeconds.first(), p.timerSubtractSeconds.first(),
            showTime && p.timerAddEnabled.first(), showTime && p.timerSubtractEnabled.first(), p.timerShowSkipButton.first())
    }
    override suspend fun compareAndSet(kind: String, session: WorkoutSessionEntity, expectedEnd: Long, end: Long?, total: Int?): Boolean {
        val dao = IosAppGraph.database.workoutDao()
        return if (kind == REST_NOTICE) dao.compareAndSetRestTimer(session.id, session.timestamp, expectedEnd, end, total) == 1
        else dao.compareAndSetWarmupTimer(session.id, session.timestamp, expectedEnd, end, total) == 1
    }
    override fun miniSetChanged(sessionId: Int, end: Long?, now: Long) {
        val store = IosWorkoutMiniSetStore()
        if (store.sessionId() != sessionId || store.pauseSeconds() >= 0) return
        val started = store.startedAt(now)
        store.save(PersistedMiniSet(sessionId, store.setId(), store.missing(), started, end ?: now,
            if (end == null) ((now - started) / 1000L).coerceIn(0, Int.MAX_VALUE.toLong()).toInt() else -1))
    }
}

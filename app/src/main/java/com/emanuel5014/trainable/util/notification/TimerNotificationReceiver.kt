package com.emanuel5014.trainable.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.ui.screens.dashboard.RestButton
import com.emanuel5014.trainable.ui.screens.dashboard.SavedTimers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerNotificationHelper: TimerNotificationHelper

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_SKIP = "com.emanuel5014.trainable.ACTION_SKIP"
        // The value keeps its old name because the manifest filter lists it; it adds whatever the settings say
        const val ACTION_ADD_30S = "com.emanuel5014.trainable.ACTION_ADD_30S"
        const val ACTION_SUBTRACT = "com.emanuel5014.trainable.ACTION_SUBTRACT"
        const val ACTION_DISMISS = "com.emanuel5014.trainable.ACTION_DISMISS"
        const val ACTION_TIMER_FINISHED = "com.emanuel5014.trainable.ACTION_TIMER_FINISHED"
        const val ACTION_REST_NEXT_SET = "com.emanuel5014.trainable.ACTION_REST_NEXT_SET"
        const val ACTION_REST_MORE = "com.emanuel5014.trainable.ACTION_REST_MORE"
        /** "Set done" on the live card during the set. */
        const val ACTION_LIVE_SET_DONE = "com.emanuel5014.trainable.ACTION_LIVE_SET_DONE"

        const val ACTION_WARMUP_SKIP = "com.emanuel5014.trainable.ACTION_WARMUP_SKIP"
        const val ACTION_WARMUP_ADD_30S = "com.emanuel5014.trainable.ACTION_WARMUP_ADD_30S"
        const val ACTION_WARMUP_SUBTRACT = "com.emanuel5014.trainable.ACTION_WARMUP_SUBTRACT"
        const val ACTION_WARMUP_DISMISS = "com.emanuel5014.trainable.ACTION_WARMUP_DISMISS"
        const val ACTION_WARMUP_FINISHED = "com.emanuel5014.trainable.ACTION_WARMUP_FINISHED"

        const val ACTION_EMOM_BOUNDARY = "com.emanuel5014.trainable.ACTION_EMOM_BOUNDARY"
        const val ACTION_EMOM_DONE = "com.emanuel5014.trainable.ACTION_EMOM_DONE"
        const val ACTION_EMOM_PAUSE = "com.emanuel5014.trainable.ACTION_EMOM_PAUSE"
        const val ACTION_EMOM_RESUME = "com.emanuel5014.trainable.ACTION_EMOM_RESUME"
        const val ACTION_EMOM_STOP = "com.emanuel5014.trainable.ACTION_EMOM_STOP"

        const val EXTRA_SESSION_ID = "extra_session_id"

        /** How long a boundary alarm keeps the receiver alive so the workout can act on it with the CPU awake. */
        private const val BOUNDARY_HOLD_MILLIS = 1500L

        /** How long a press on the live card waits for the open workout to answer before the card is drawn. */
        private const val LIVE_SETTLE_MILLIS = 400L

        val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
        val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
        val emomEvents = MutableSharedFlow<EmomAction>(extraBufferCapacity = 4)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1)
        
        when (intent.action) {
            ACTION_SKIP -> {
                timerEvents.tryEmit(TimerAction.SKIP)
                if (sessionId != -1) {
                    val pendingResult = goAsync()
                    scope.launch {
                        try {
                            workoutRepository.updateRestTimer(sessionId, null, null)
                            timerNotificationHelper.cancelTimer()
                            settleLive()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    timerNotificationHelper.cancelTimer()
                    holdForLive()
                }
            }
            ACTION_ADD_30S -> adjust(TimerAction.ADD, RestButton.ADD, sessionId)
            ACTION_SUBTRACT -> adjust(TimerAction.SUBTRACT, RestButton.SUBTRACT, sessionId)
            ACTION_DISMISS -> {
                timerEvents.tryEmit(TimerAction.DISMISS)
                timerNotificationHelper.cancelTimer()
                holdForLive()
            }
            ACTION_TIMER_FINISHED -> {
                timerEvents.tryEmit(TimerAction.FINISHED)
                timerNotificationHelper.showRestFinished()
                if (sessionId != -1) {
                    val pendingResult = goAsync()
                    scope.launch {
                        try {
                            workoutRepository.updateRestTimer(sessionId, null, null)
                            settleLive()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_REST_NEXT_SET -> forwardRest(TimerAction.NEXT_SET)
            ACTION_REST_MORE -> forwardRest(TimerAction.MORE_REST)
            ACTION_LIVE_SET_DONE -> forwardRest(TimerAction.LIVE_SET_DONE)

            ACTION_WARMUP_SKIP -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.SKIP)
                timerNotificationHelper.cancelWarmupTimer()
                holdForLive()
            }
            ACTION_WARMUP_ADD_30S -> adjustWarmup(WarmupTimerAction.ADD, RestButton.ADD, sessionId)
            ACTION_WARMUP_SUBTRACT -> adjustWarmup(WarmupTimerAction.SUBTRACT, RestButton.SUBTRACT, sessionId)
            ACTION_WARMUP_DISMISS -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.DISMISS)
                timerNotificationHelper.cancelWarmupTimer()
                holdForLive()
            }
            ACTION_WARMUP_FINISHED -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.FINISHED)
                timerNotificationHelper.showWarmupTimerFinished()
            }

            ACTION_EMOM_BOUNDARY -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        forwardEmom(EmomAction.BOUNDARY)
                        delay(BOUNDARY_HOLD_MILLIS)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_EMOM_DONE -> { forwardEmom(EmomAction.DONE); holdForLive() }
            ACTION_EMOM_PAUSE -> { forwardEmom(EmomAction.PAUSE); holdForLive() }
            ACTION_EMOM_RESUME -> { forwardEmom(EmomAction.RESUME); holdForLive() }
            ACTION_EMOM_STOP -> { forwardEmom(EmomAction.STOP); holdForLive() }
        }
    }

    /** −/+ go to the open workout; with the workout closed they change the rest saved on the session (see [SavedTimers]). */
    private fun adjust(action: TimerAction, button: RestButton, sessionId: Int) {
        if (timerEvents.subscriptionCount.value > 0) {
            timerEvents.tryEmit(action)
            holdForLive()
            return
        }
        adjustSaved(button, sessionId.takeIf { it != -1 } ?: timerNotificationHelper.workoutSessionId)
    }

    private fun adjustWarmup(action: WarmupTimerAction, button: RestButton, sessionId: Int) {
        if (warmupTimerEvents.subscriptionCount.value > 0) {
            warmupTimerEvents.tryEmit(action)
            holdForLive()
            return
        }
        adjustSaved(button, sessionId.takeIf { it != -1 } ?: timerNotificationHelper.workoutSessionId)
    }

    private fun adjustSaved(button: RestButton, sessionId: Int?) {
        if (sessionId == null) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                SavedTimers.adjust(workoutRepository, userPreferencesRepository, sessionId, button, System.currentTimeMillis())
                    ?.let { timerNotificationHelper.followSavedTimer(it) }
                timerNotificationHelper.flushLiveCard()
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** A finished-rest button acts only through the open workout; without one it writes nothing. */
    private fun forwardRest(action: TimerAction) {
        timerNotificationHelper.cancelCustomVibration()
        if (timerEvents.subscriptionCount.value == 0) {
            timerNotificationHelper.showOpenAppToContinue()
            return
        }
        timerEvents.tryEmit(action)
        holdForLive()
    }

    /** With the live card on, keeps the receiver alive while the open workout answers, then draws the card. */
    private fun holdForLive() {
        if (!timerNotificationHelper.isLive()) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                settleLive()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun settleLive() {
        if (!timerNotificationHelper.isLive()) return
        delay(LIVE_SETTLE_MILLIS)
        timerNotificationHelper.flushLiveCard()
    }

    private fun forwardEmom(action: EmomAction) {
        if (emomEvents.subscriptionCount.value == 0) {
            // No workout is alive to act on it: the run this notification belongs to is gone.
            timerNotificationHelper.cancelEmom()
            return
        }
        emomEvents.tryEmit(action)
    }
}

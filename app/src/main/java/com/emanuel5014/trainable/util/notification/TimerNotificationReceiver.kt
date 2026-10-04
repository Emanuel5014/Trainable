package com.emanuel5014.trainable.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emanuel5014.trainable.data.repository.WorkoutRepository
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_SKIP = "com.emanuel5014.trainable.ACTION_SKIP"
        const val ACTION_ADD_30S = "com.emanuel5014.trainable.ACTION_ADD_30S"
        const val ACTION_DISMISS = "com.emanuel5014.trainable.ACTION_DISMISS"
        const val ACTION_TIMER_FINISHED = "com.emanuel5014.trainable.ACTION_TIMER_FINISHED"

        const val ACTION_WARMUP_SKIP = "com.emanuel5014.trainable.ACTION_WARMUP_SKIP"
        const val ACTION_WARMUP_ADD_30S = "com.emanuel5014.trainable.ACTION_WARMUP_ADD_30S"
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

        val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
        val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
        val emomEvents = MutableSharedFlow<EmomAction>(extraBufferCapacity = 4)
    }

    enum class TimerAction { SKIP, ADD_30S, DISMISS, FINISHED }
    enum class WarmupTimerAction { SKIP, ADD_30S, DISMISS, FINISHED }

    /** What the EMOM notification or its round alarm asks of the workout. */
    enum class EmomAction { BOUNDARY, DONE, PAUSE, RESUME, STOP }

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
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    timerNotificationHelper.cancelTimer()
                }
            }
            ACTION_ADD_30S -> {
                timerEvents.tryEmit(TimerAction.ADD_30S)
            }
            ACTION_DISMISS -> {
                timerEvents.tryEmit(TimerAction.DISMISS)
                timerNotificationHelper.cancelTimer()
            }
            ACTION_TIMER_FINISHED -> {
                timerEvents.tryEmit(TimerAction.FINISHED)
                timerNotificationHelper.showRestFinished()
                if (sessionId != -1) {
                    val pendingResult = goAsync()
                    scope.launch {
                        try {
                            workoutRepository.updateRestTimer(sessionId, null, null)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_WARMUP_SKIP -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.SKIP)
                timerNotificationHelper.cancelWarmupTimer()
            }
            ACTION_WARMUP_ADD_30S -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.ADD_30S)
            }
            ACTION_WARMUP_DISMISS -> {
                warmupTimerEvents.tryEmit(WarmupTimerAction.DISMISS)
                timerNotificationHelper.cancelWarmupTimer()
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
            ACTION_EMOM_DONE -> forwardEmom(EmomAction.DONE)
            ACTION_EMOM_PAUSE -> forwardEmom(EmomAction.PAUSE)
            ACTION_EMOM_RESUME -> forwardEmom(EmomAction.RESUME)
            ACTION_EMOM_STOP -> forwardEmom(EmomAction.STOP)
        }
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

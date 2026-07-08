package com.emanuel5014.trainable.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        const val EXTRA_SESSION_ID = "extra_session_id"

        val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
        val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
    }

    enum class TimerAction { SKIP, ADD_30S, DISMISS, FINISHED }
    enum class WarmupTimerAction { SKIP, ADD_30S, DISMISS, FINISHED }

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
        }
    }
}

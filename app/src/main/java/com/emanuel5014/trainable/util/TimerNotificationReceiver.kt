package com.emanuel5014.trainable.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow

class TimerNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SKIP = "com.emanuel5014.trainable.ACTION_SKIP"
        const val ACTION_ADD_30S = "com.emanuel5014.trainable.ACTION_ADD_30S"
        const val ACTION_DISMISS = "com.emanuel5014.trainable.ACTION_DISMISS"
        const val ACTION_TIMER_FINISHED = "com.emanuel5014.trainable.ACTION_TIMER_FINISHED"

        val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
    }

    enum class TimerAction { SKIP, ADD_30S, DISMISS, FINISHED }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SKIP -> timerEvents.tryEmit(TimerAction.SKIP)
            ACTION_ADD_30S -> timerEvents.tryEmit(TimerAction.ADD_30S)
            ACTION_DISMISS -> timerEvents.tryEmit(TimerAction.DISMISS)
            ACTION_TIMER_FINISHED -> timerEvents.tryEmit(TimerAction.FINISHED)
        }
    }
}

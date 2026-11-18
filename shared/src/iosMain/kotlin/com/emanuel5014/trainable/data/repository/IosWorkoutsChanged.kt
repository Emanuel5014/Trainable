package com.emanuel5014.trainable.data.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A tick after every workout change (WorkoutPlatform.onWorkoutsChanged), for what shows workouts outside the app. */
object IosWorkoutsChanged {
    private val mutableTicks = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val ticks: SharedFlow<Unit> = mutableTicks.asSharedFlow()

    fun notifyChanged() {
        mutableTicks.tryEmit(Unit)
    }
}

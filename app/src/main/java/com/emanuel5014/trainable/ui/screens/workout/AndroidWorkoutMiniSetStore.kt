package com.emanuel5014.trainable.ui.screens.workout

import android.content.Context

/** Keeps the original file, keys, numeric types, defaults and asynchronous atomic editor writes. */
class AndroidWorkoutMiniSetStore(private val context: Context) : WorkoutMiniSetStore {
    private val prefs by lazy { context.getSharedPreferences("workout_mini_set", Context.MODE_PRIVATE) }

    override fun sessionId(): Int = prefs.getInt(MINI_SESSION, -1)
    override fun setId(): Int = prefs.getInt(MINI_SET, -1)
    override fun missing(): Int = prefs.getInt(MINI_MISSING, 0)
    override fun startedAt(default: Long): Long = prefs.getLong(MINI_STARTED, default)
    override fun endsAt(): Long = prefs.getLong(MINI_ENDS, 0L)
    override fun pauseSeconds(): Int = prefs.getInt(MINI_PAUSE, -1)

    override fun save(value: PersistedMiniSet) {
        prefs.edit()
            .putInt(MINI_SESSION, value.sessionId)
            .putInt(MINI_SET, value.setId)
            .putInt(MINI_MISSING, value.missing)
            .putLong(MINI_STARTED, value.startedAt)
            .putLong(MINI_ENDS, value.endsAt)
            .putInt(MINI_PAUSE, value.pauseSeconds)
            .apply()
    }

    override fun clear() { prefs.edit().clear().apply() }
}

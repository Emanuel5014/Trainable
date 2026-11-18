package com.emanuel5014.trainable.ui.screens.workout

/** The persisted mini-set, retaining Android's original numeric types and sentinel values. */
data class PersistedMiniSet(
    val sessionId: Int,
    val setId: Int,
    val missing: Int,
    val startedAt: Long,
    val endsAt: Long,
    val pauseSeconds: Int
)

/** Platform persistence for the mini-set that must survive leaving or closing the workout. */
interface WorkoutMiniSetStore {
    fun sessionId(): Int
    fun setId(): Int
    fun missing(): Int
    fun startedAt(default: Long): Long
    fun endsAt(): Long
    fun pauseSeconds(): Int
    /** Replaces all six fields together, matching Android's single SharedPreferences editor. */
    fun save(value: PersistedMiniSet)
    fun clear()
}

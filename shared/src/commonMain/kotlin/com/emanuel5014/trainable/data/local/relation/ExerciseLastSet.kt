package com.emanuel5014.trainable.data.local.relation

/**
 * A working set an exercise was done with in its last finished workout (not a deload): one row per set of that
 * workout. The quick workout picker shows the best one under each exercise ("100 kg × 5 · 4 days ago").
 */
data class ExerciseLastSet(
    val exerciseId: Int,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val seconds: Int?,
    val exerciseType: String?,
    val restSeconds: Int?,
    val timestamp: Long
)

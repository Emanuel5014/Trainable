package com.emanuel5014.trainable.data.report

data class PlanReport(
    val planId: Int,
    val planName: String,
    val planNote: String?,
    val startDate: Long,
    val endDate: Long?,
    val totalSessions: Int,
    val periodFirstSession: Long?,
    val periodLastSession: Long?,
    val exercises: List<ExerciseReport>,
    val weightUnit: String = "kg"
)

data class ExerciseReport(
    val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val isCurrentlyInPlan: Boolean,
    val sessions: List<ExerciseSessionEntry>,
    val summary: ExerciseSummary,
    val swapEvents: List<SwapEvent>
)

data class ExerciseSessionEntry(
    val date: Long,
    val sets: List<SetEntry>
)

data class SetEntry(
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val rpe: Int?,
    val isWarmup: Boolean,
    val note: String?
)

data class ExerciseSummary(
    val firstSessionDate: Long,
    val lastSessionDate: Long,
    val totalSets: Int,
    val maxWeight: Float,
    val maxWeightDate: Long,
    val maxVolume: Float,
    val maxVolumeDate: Long,
    val bestEstimatedOneRM: Float?
)

data class SwapEvent(
    val sessionDate: Long,
    val originalPlanExerciseId: Int,
    val originalExerciseName: String,
    val replacementExerciseName: String
)

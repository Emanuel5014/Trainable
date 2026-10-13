package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope

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

/** How an exercise of the report is measured, which decides its columns and its summary. */
enum class ReportExerciseKind { STRENGTH, TIME_AND_WEIGHT, TIME_AND_REPS, CARDIO }

data class ExerciseReport(
    val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val isCurrentlyInPlan: Boolean,
    val sessions: List<ExerciseSessionEntry>,
    val summary: ExerciseSummary,
    val swapEvents: List<SwapEvent>,
    val kind: ReportExerciseKind = ReportExerciseKind.STRENGTH,
    /** Changes made to the prescription of the exercise during a workout, oldest first. */
    val programEdits: List<ProgramEditEvent> = emptyList()
)

data class ExerciseSessionEntry(
    val date: Long,
    val sets: List<SetEntry>
)

data class SetEntry(
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val rpe: Float?,
    val isWarmup: Boolean,
    val note: String?,
    /** Planned %1RM / techniques this set was performed against, e.g. "75% · Pause 2″". */
    val prescription: String? = null,
    /** How long a timed or cardio set lasted. */
    val seconds: Int? = null,
    /** Distance of a cardio set, in km. */
    val distanceKm: Float? = null,
    /** Reps done in one go: all of them, or the first part of a set finished after short pauses. */
    val straightReps: Int = reps
)

data class ExerciseSummary(
    val firstSessionDate: Long,
    val lastSessionDate: Long,
    val totalSets: Int,
    val maxWeight: Float,
    val maxWeightDate: Long,
    val maxVolume: Float,
    val maxVolumeDate: Long,
    val bestEstimatedOneRM: Float?,
    /** Longest set (a hold, an interval or a cardio set). */
    val bestSeconds: Int = 0,
    val totalSeconds: Int = 0,
    /** Most reps in one set (Time & Reps: in one interval). */
    val bestReps: Int = 0,
    val totalReps: Int = 0,
    val totalKm: Float = 0f
)

data class SwapEvent(
    val sessionDate: Long,
    val originalPlanExerciseId: Int,
    val originalExerciseName: String,
    val replacementExerciseName: String
)

/**
 * The prescription of an exercise was changed during a workout: [before] and [after] are the blocks as one line each
 * ("70% 1×6 · 75% 4×5 Pause 2″"), [scope] says whether the routine was updated or the change stayed in that workout.
 */
data class ProgramEditEvent(
    val sessionDate: Long,
    val createdAt: Long,
    val week: Int?,
    val kind: ProgramEditKind,
    val scope: ProgramEditScope,
    val before: String,
    val after: String,
    /** The blocks that changed, one line each (see programChangeLines); empty when no block shows the change. */
    val changes: List<String> = emptyList()
)

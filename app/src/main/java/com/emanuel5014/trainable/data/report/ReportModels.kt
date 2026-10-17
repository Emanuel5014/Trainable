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
    val weightUnit: String = "kg",
    /** Weeks of a periodized program (1 for a plain routine) and the week the athlete is on. */
    val weeksCount: Int = 1,
    val currentWeek: Int = 1,
    /** What was trained week by week; empty for a plain routine. */
    val weeks: List<WeekSummary> = emptyList(),
    /** The workouts with a journal (rating, tags or note), newest first; empty with the workout journal off. */
    val journal: List<JournalEntry> = emptyList()
)

/** What the lifter said about one workout of the routine. */
data class JournalEntry(val date: Long, val rating: Int?, val tags: List<String>, val note: String?)

/** The workouts of one week of a periodized program. */
data class WeekSummary(val week: Int, val workouts: Int, val firstDate: Long?, val lastDate: Long?)

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
    val programEdits: List<ProgramEditEvent> = emptyList(),
    /** What the sheet asks of the exercise, to read next to what was done; null when it is not in the routine any more. */
    val program: ExerciseProgram? = null
)

/** What the sheet asks of an exercise: its blocks week by week, or the plain sets × reps (or time) of one without blocks. */
data class ExerciseProgram(
    val plain: PlainTarget? = null,
    /** One entry per week of the program, for an exercise with blocks. */
    val weeks: List<ProgramWeek> = emptyList()
)

/** The target of an exercise without blocks: [sets] of [reps] or of [seconds], or a cardio time and distance. */
data class PlainTarget(
    val sets: Int? = null,
    val reps: String? = null,
    val seconds: Int? = null,
    val distanceKm: Float? = null,
    val restSeconds: Int? = null
)

/** The prescription of one week: its blocks, or the week the exercise is skipped, or the earlier week it repeats. */
data class ProgramWeek(
    val week: Int,
    val excluded: Boolean = false,
    /** The week the blocks are copied from when this one is not written on the sheet yet. */
    val repeatsWeek: Int? = null,
    val blocks: List<ProgramBlock> = emptyList()
)

/** One block of a prescription as text ("75% 4×5 Pause 2″"), with the rest and the note the sheet gives it. */
data class ProgramBlock(val text: String, val restSeconds: Int? = null, val note: String? = null)

/** What a workout did of what the sheet asked for that week: working sets and reps done against the planned ones. */
data class SessionAdherence(
    val setsDone: Int,
    val setsPlanned: Int,
    val repsDone: Int,
    /** Null when a block has no fixed reps (as many as possible), so the total cannot be told. */
    val repsPlanned: Int?
)

data class ExerciseSessionEntry(
    val date: Long,
    val sets: List<SetEntry>,
    /** Week of the program the workout was done in (null for plain routines). */
    val week: Int? = null,
    val adherence: SessionAdherence? = null
)

data class SetEntry(
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val rpe: Float?,
    val isWarmup: Boolean,
    val note: String?,
    /** What the set was planned as (advanced programming only), one entry per badge: "75%", "MAX", "Pause 2″". */
    val planned: List<String> = emptyList(),
    /** How the set really went, one entry per badge: "2+1+1", "2 helped reps", the technique it was done with. */
    val performed: List<String> = emptyList(),
    /** How long a timed or cardio set lasted. */
    val seconds: Int? = null,
    /** Distance of a cardio set, in km. */
    val distanceKm: Float? = null,
    /** Reps done in one go: all of them, or the first part of a set finished after short pauses. */
    val straightReps: Int = reps,
    /** Reps the set was done against: "5", or a range "10–15"; "MAX" or null when there was no number to reach. */
    val targetReps: String? = null,
    /** Added on top of the sheet. */
    val isExtra: Boolean = false
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
 * The prescription of an exercise was changed during a workout: [before] and [after] are its blocks, one line each
 * ("75% 4×5 Pause 2″"; empty when there were none), [scope] says whether the routine was updated or the change
 * stayed in that workout.
 */
data class ProgramEditEvent(
    val sessionDate: Long,
    val createdAt: Long,
    val week: Int?,
    val kind: ProgramEditKind,
    val scope: ProgramEditScope,
    val before: List<String>,
    val after: List<String>,
    /** The blocks that changed, one line each (see programChangeLines); empty when no block shows the change. */
    val changes: List<String> = emptyList()
)

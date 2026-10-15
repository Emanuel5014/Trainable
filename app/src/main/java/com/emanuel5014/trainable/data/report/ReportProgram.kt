package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription

/**
 * What the sheet asks for, put next to what was done: the prescription of an exercise week by week, and how much of
 * it a workout got through. Kept apart from [ReportGenerator] so that it needs nothing of Android.
 */
internal object ReportProgram {

    /** The prescription of [planExercise] for each of the [weeksCount] weeks, or its plain target when it has no blocks (or advanced programming is off). */
    fun of(planExercise: PlanExerciseWithDetails, weeksCount: Int, advanced: Boolean, labels: PrescriptionLabels): ExerciseProgram {
        if (!advanced || !planExercise.isAdvanced) return ExerciseProgram(plain = plainTarget(planExercise.planExercise))

        val weeks = (1..weeksCount.coerceAtLeast(1)).map { week ->
            when (val resolved = planExercise.resolve(week)) {
                ResolvedPrescription.Excluded -> ProgramWeek(week, excluded = true)
                ResolvedPrescription.Legacy -> ProgramWeek(week)
                is ResolvedPrescription.Blocks -> ProgramWeek(
                    week = week,
                    repeatsWeek = resolved.sourceWeek.takeIf { resolved.isFallback },
                    blocks = resolved.blocks.map { block ->
                        ProgramBlock(
                            text = PrescriptionFormatter.summary(listOf(block), labels),
                            restSeconds = block.restSeconds?.takeIf { it > 0 },
                            note = block.note?.trim()?.takeIf { it.isNotEmpty() }
                        )
                    }
                )
            }
        }
        return ExerciseProgram(weeks = weeks)
    }

    /** Sets × reps of a strength exercise, sets × time of a timed one, time and distance of a cardio one. */
    fun plainTarget(exercise: PlanExerciseEntity): PlainTarget {
        val rest = exercise.recuperoTarget.takeIf { it > 0 }
        return when (exercise.exerciseType) {
            "cardio" -> PlainTarget(
                seconds = exercise.durataTargetSecondi?.takeIf { it > 0 },
                distanceKm = exercise.distanzaTargetKm?.takeIf { it > 0f },
                restSeconds = rest
            )
            TIME_AND_WEIGHT, TIME_AND_REPS -> PlainTarget(
                sets = exercise.serieTarget,
                seconds = exercise.durataTargetSecondi ?: exercise.repsTarget.filter { it.isDigit() }.toIntOrNull(),
                restSeconds = rest
            )
            else -> PlainTarget(
                sets = exercise.serieTarget,
                reps = exercise.repsDisplay.trim().ifEmpty { null },
                restSeconds = rest
            )
        }
    }

    /**
     * The working sets and reps of a workout against those the sheet gave that week, for an exercise with blocks. Null
     * when there is nothing to count against: no blocks, a week the exercise is skipped, or a block (as many reps as
     * it takes) that makes as many sets as it needs.
     */
    fun adherence(planExercise: PlanExerciseWithDetails?, advanced: Boolean, week: Int?, weeksCount: Int, sets: List<SetEntry>): SessionAdherence? {
        if (!advanced || planExercise == null || !planExercise.isAdvanced) return null
        val onWeek = week ?: if (weeksCount <= 1) 1 else return null
        val blocks = (planExercise.resolve(onWeek) as? ResolvedPrescription.Blocks)?.blocks ?: return null
        if (blocks.any { it.repMode == RepMode.TOTAL }) return null

        val planned = PrescriptionExpander.expand(blocks)
        val done = sets.filter { !it.isWarmup && !it.isExtra }
        return SessionAdherence(
            setsDone = done.size,
            setsPlanned = planned.size,
            repsDone = done.sumOf { it.reps },
            repsPlanned = planned.takeIf { all -> all.all { it.targetReps != null } }?.sumOf { it.targetReps ?: 0 }
        )
    }

    /** Workouts, first and last day of each of the [weeksCount] weeks, from the (week, day) of every workout done. */
    fun weekSummaries(weeksCount: Int, workouts: List<Pair<Int?, Long>>): List<WeekSummary> =
        if (weeksCount <= 1) emptyList() else (1..weeksCount).map { week ->
            val days = workouts.filter { it.first == week }.map { it.second }
            WeekSummary(week, days.size, days.minOrNull(), days.maxOrNull())
        }
}

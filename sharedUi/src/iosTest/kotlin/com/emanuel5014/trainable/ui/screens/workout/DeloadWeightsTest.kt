package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** A running workout marked as a deload: which weights go to the deload share and back. */
class DeloadWeightsTest {
    private val bench = ExerciseEntity(1, "Flat Bench Press", "Chest")

    private fun set(n: Int, weight: Float, previous: Float?, done: Boolean = false, warmup: Boolean = false) =
        WorkoutSetState(setNumber = n, weight = weight, reps = 8, previousWeight = previous, isCompleted = done, isWarmup = warmup)

    private fun exercise(vararg sets: WorkoutSetState, cardio: Boolean = false) =
        WorkoutExerciseState(exercise = bench, planDetails = null, executionOrder = 0, sets = sets.toList(), isCardio = cardio)

    private fun apply(list: List<WorkoutExerciseState>, deload: Boolean, only: Int? = null) =
        DeloadWeights.apply(list, deload, percent = 60, unit = "kg", increment = 2.5f, onlyIndex = only)

    @Test
    fun setsAtLastTimesWeightGoToTheDeloadShare() {
        val result = apply(listOf(exercise(set(1, 100f, 100f), set(2, 100f, 100f))), deload = true)
        assertEquals(listOf(60f, 60f), result.exercises.single().sets.map { it.weight })
        assertEquals(listOf(0), result.changedIndices)
    }

    @Test
    fun typedWeightsDoneSetsAndWarmUpsAreLeftAlone() {
        val result = apply(
            listOf(exercise(set(1, 100f, 100f, done = true), set(2, 40f, 40f, warmup = true), set(3, 90f, 100f), set(4, 100f, 100f))),
            deload = true
        )
        assertEquals(listOf(100f, 40f, 90f, 60f), result.exercises.single().sets.map { it.weight })
    }

    @Test
    fun turningItOffGivesTheWeightsBack() {
        val on = apply(listOf(exercise(set(1, 100f, 100f), set(2, 100f, 100f))), deload = true).exercises
        // The lifter changed the second set meanwhile: that one stays
        val edited = listOf(on.single().copy(sets = on.single().sets.mapIndexed { i, s -> if (i == 1) s.copy(weight = 65f) else s }))
        val off = apply(edited, deload = false)
        assertEquals(listOf(100f, 65f), off.exercises.single().sets.map { it.weight })
    }

    @Test
    fun noHistoryCardioAndPercentExercisesAreUntouched() {
        val noHistory = exercise(set(1, 50f, null))
        val cardio = exercise(set(1, 100f, 100f), cardio = true)
        val advanced = exercise(set(1, 100f, 100f)).copy(blocks = listOf(PrescriptionBlock()))
        val list = listOf(noHistory, cardio, advanced)
        val result = apply(list, deload = true)
        result.exercises.forEachIndexed { i, ex -> assertSame(list[i], ex) }
        assertEquals(emptyList(), result.changedIndices)
    }

    @Test
    fun onlyTheExerciseAskedForChanges() {
        val list = listOf(exercise(set(1, 100f, 100f)), exercise(set(1, 80f, 80f)))
        val result = apply(list, deload = true, only = 1)
        assertEquals(100f, result.exercises[0].sets.single().weight)
        assertEquals(47.5f, result.exercises[1].sets.single().weight)
        assertEquals(listOf(1), result.changedIndices)
    }
}

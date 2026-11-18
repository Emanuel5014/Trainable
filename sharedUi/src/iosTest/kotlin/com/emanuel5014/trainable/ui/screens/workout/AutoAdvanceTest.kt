package com.emanuel5014.trainable.ui.screens.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutoAdvanceTest {
    private fun timed(vararg done: Boolean) =
        AutoAdvance.Exercise(runsOnSetTimer = true, sets = done.map { AutoAdvance.Set(done = it, emom = false) })

    private fun strength(vararg done: Boolean) =
        AutoAdvance.Exercise(runsOnSetTimer = false, sets = done.map { AutoAdvance.Set(done = it, emom = false) })

    @Test
    fun nextSetOfTheSameExercise() {
        val exercises = listOf(timed(true, false, false))
        assertEquals(AutoAdvance.Target(0, 1), AutoAdvance.next(exercises, 0))
    }

    @Test
    fun exerciseOverMovesToTheNextTimedOne() {
        val exercises = listOf(timed(true, true), timed(false, false))
        assertEquals(AutoAdvance.Target(1, 0), AutoAdvance.next(exercises, 0))
    }

    @Test
    fun skipsExercisesAlreadyDone() {
        val exercises = listOf(timed(true), timed(true, true), timed(true, false))
        assertEquals(AutoAdvance.Target(2, 1), AutoAdvance.next(exercises, 0))
    }

    @Test
    fun stopsBeforeAStrengthExercise() {
        assertNull(AutoAdvance.next(listOf(timed(true), strength(false)), 0))
    }

    @Test
    fun stopsWhenTheCurrentExerciseIsNotTimed() {
        assertNull(AutoAdvance.next(listOf(strength(true, false)), 0))
    }

    @Test
    fun stopsAtTheEndOfTheWorkout() {
        assertNull(AutoAdvance.next(listOf(timed(true), timed(true, true)), 1))
    }

    @Test
    fun neverLooksBackToEarlierExercises() {
        // An earlier exercise left unfinished is the lifter's to pick up, not the chain's
        assertNull(AutoAdvance.next(listOf(timed(false), timed(true)), 1))
    }

    @Test
    fun stopsBeforeAnEmomRound() {
        val exercises = listOf(
            AutoAdvance.Exercise(true, listOf(AutoAdvance.Set(done = true, emom = false), AutoAdvance.Set(done = false, emom = true)))
        )
        assertNull(AutoAdvance.next(exercises, 0))
    }

    @Test
    fun noExerciseAtThatIndex() {
        assertNull(AutoAdvance.next(emptyList(), 0))
    }

    @Test
    fun onTimeStartsFromZero() {
        assertEquals(0, AutoAdvance.startSeconds(lateMillis = 0L, targetSeconds = 30))
        assertEquals(0, AutoAdvance.startSeconds(lateMillis = -500L, targetSeconds = 30))
    }

    @Test
    fun lateStartKeepsTimeWithTheClock() {
        assertEquals(12, AutoAdvance.startSeconds(lateMillis = 12_900L, targetSeconds = 30))
    }

    @Test
    fun tooLateStopsTheChain() {
        assertNull(AutoAdvance.startSeconds(lateMillis = 30_000L, targetSeconds = 30))
        assertNull(AutoAdvance.startSeconds(lateMillis = 600_000L, targetSeconds = 30))
    }
}

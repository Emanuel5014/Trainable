package com.emanuel5014.trainable.ui.screens.workout

/**
 * "Auto-advance timed sets" (issue #65): a timed set stopped at its target is logged, the rest runs, and the next
 * timed set starts on its own. These are the pure decisions behind that chain; [WorkoutModel] does the work.
 */
internal object AutoAdvance {
    /** Seconds of the get-ready countdown when a set follows with no rest in between (a superset round). */
    const val LEAD_IN_SECONDS = 3

    /** Wait after a set is logged before looking for the next one: a superset moves to its next exercise 300 ms after the tick. */
    const val SETTLE_MILLIS = 400L

    /** What the chain needs to know about one exercise of the workout. */
    data class Exercise(val runsOnSetTimer: Boolean, val sets: List<Set>)

    data class Set(val done: Boolean, val emom: Boolean)

    data class Target(val exerciseIndex: Int, val setIndex: Int)

    /**
     * The set the chain starts next, or null where it stops and waits for the lifter. It is the first set left of
     * the exercise on screen; once that exercise is over, the first set left of the next exercise that still has
     * one. Either way the exercise must run on the set timer (timed, not cardio) and the set must not be an EMOM
     * round, which has its own minute clock.
     */
    fun next(exercises: List<Exercise>, currentIndex: Int): Target? {
        val current = exercises.getOrNull(currentIndex) ?: return null
        val exerciseIndex = if (current.sets.any { !it.done }) currentIndex
            else (currentIndex + 1 until exercises.size).firstOrNull { i -> exercises[i].sets.any { !it.done } } ?: return null
        val exercise = exercises[exerciseIndex]
        if (!exercise.runsOnSetTimer) return null
        val setIndex = exercise.sets.indexOfFirst { !it.done }
        if (exercise.sets[setIndex].emom) return null
        return Target(exerciseIndex, setIndex)
    }

    /**
     * Where the set timer starts when the rest ended [lateMillis] ago (the app was asleep): the seconds already gone,
     * so the set keeps time with the clock. Null when those seconds already reach the set's [targetSeconds]: the
     * set was not done while the app slept, and logging it (and the ones after it) would be made up, so the chain stops.
     */
    fun startSeconds(lateMillis: Long, targetSeconds: Int): Int? {
        val late = (lateMillis.coerceAtLeast(0L) / 1000L).toInt()
        return late.takeIf { it < targetSeconds }
    }
}

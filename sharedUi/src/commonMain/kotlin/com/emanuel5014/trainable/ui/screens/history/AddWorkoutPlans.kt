package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.entity.isSystemPlan

/** What a routine is made of, to tell routines apart at a glance: how many exercises and which muscle groups. */
data class PlanSummary(
    val exerciseCount: Int,
    /** Muscle groups in the order the exercises come, without repeats. */
    val muscleGroups: List<String>
)

/** A routine as the "add workout" sheet lists it. */
data class AddWorkoutPlan(
    val plan: WorkoutPlanEntity,
    val summary: PlanSummary?,
    /** Muscle groups already in the user's language, which is also what a search matches against. */
    val muscleLabels: List<String> = emptyList(),
    /** When a workout of this routine was last done, null if never. */
    val lastDone: Long? = null
)

/** The routines of the "add workout" sheet split the way the user thinks of them: the ones in use, and the old ones. */
data class AddWorkoutGroups(
    val active: List<AddWorkoutPlan>,
    val archived: List<AddWorkoutPlan>
) {
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

object AddWorkoutPlans {

    /** Below this many routines the list is short enough that a search field would only be noise. */
    const val SEARCH_MIN_ROUTINES = 6

    /**
     * Splits [plans] into active and archived routines, keeping only those that match [query] (name or muscle group).
     * Active routines keep the order the user gave them in Routines. Archived ones come most recently done first,
     * since an old routine is usually picked to log a workout that was really done, and the ones never done last.
     * System plans (cardio, quick, custom workout) are not routines and are left out.
     */
    fun group(plans: List<AddWorkoutPlan>, query: String): AddWorkoutGroups {
        val needle = query.trim()
        val matching = plans.filter { !it.plan.isSystemPlan && it.matches(needle) }
        val (archived, active) = matching.partition { it.plan.isArchived }
        return AddWorkoutGroups(
            active = active.sortedBy { it.plan.ordine },
            archived = archived.sortedWith(
                compareByDescending<AddWorkoutPlan> { it.lastDone ?: Long.MIN_VALUE }.thenBy { it.plan.ordine }
            )
        )
    }

    /** Number of routines the sheet can list, i.e. everything but system plans. */
    fun routineCount(plans: List<AddWorkoutPlan>): Int = plans.count { !it.plan.isSystemPlan }

    /** The latest workout time of each routine, from the workouts of the history. */
    fun lastDoneByPlan(sessions: List<Pair<Int, Long>>): Map<Int, Long> =
        sessions.groupBy({ it.first }, { it.second }).mapValues { (_, times) -> times.max() }

    private fun AddWorkoutPlan.matches(needle: String): Boolean =
        needle.isEmpty() ||
            plan.nome.contains(needle, ignoreCase = true) ||
            muscleLabels.any { it.contains(needle, ignoreCase = true) }
}

package com.emanuel5014.trainable.domain.session

/**
 * What a workout is called (issue #75). A name the user gave it wins; a workout of a routine goes by the routine's name.
 * A workout with no routine (the app's own Quick / Custom / Cardio plans) is named after what it holds, so the same
 * workout gets the same name wherever it was created and the name follows when exercises are added or removed:
 * only cardio exercises makes it a cardio workout, anything else (or nothing yet) a quick workout.
 */
sealed interface WorkoutTitle {
    /** The user's own name for the workout, or its routine's name. */
    data class Named(val name: String) : WorkoutTitle

    data object Quick : WorkoutTitle

    data object Cardio : WorkoutTitle

    companion object {
        /**
         * [note] is the workout's own name (`noteSessione`), [planName] its plan's, [isSystemPlan] whether that plan is
         * one of the app's own. [hasStrength]: any set that is not cardio; [hasCardio]: any cardio set.
         */
        fun of(note: String?, planName: String, isSystemPlan: Boolean, hasStrength: Boolean, hasCardio: Boolean): WorkoutTitle {
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { return Named(it) }
            if (!isSystemPlan) return Named(planName)
            return if (hasCardio && !hasStrength) Cardio else Quick
        }
    }
}

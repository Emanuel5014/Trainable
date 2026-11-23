package com.emanuel5014.trainable.domain.session

/**
 * The workout's own name, kept in `WorkoutSessionEntity.noteSessione`. Null means the workout has no name of its own
 * and goes by its routine's name, so renaming the routine renames its past workouts too.
 */
object SessionName {

    /**
     * What to save after the user edited the name field, which was filled with [shown] (what the workout is called now)
     * while [current] is what is saved. Null keeps the workout following its routine ([planName]): when the field is
     * left empty, holds the routine's name, or was not touched and the workout had no name of its own. Only a name the
     * user really typed breaks the link (issue #74: saving the date or the duration must not).
     */
    fun toSave(typed: String, shown: String, current: String?, planName: String): String? {
        val name = typed.trim()
        if (name == shown.trim()) return ownName(current, planName)
        return name.takeIf { it.isNotEmpty() && it != planName.trim() }
    }

    /** [note] as the workout's own name, or null when it is empty or only repeats the routine's name. */
    fun ownName(note: String?, planName: String): String? =
        note?.trim()?.takeIf { it.isNotEmpty() && it != planName.trim() }
}

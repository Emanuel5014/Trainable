package com.emanuel5014.trainable.domain.exercise

/**
 * The main implement of an exercise. [key] is what the database and the routine files hold, so it never changes once
 * shipped.
 */
enum class Equipment(val key: String) {
    BARBELL("barbell"),
    DUMBBELL("dumbbell"),
    CABLE("cable"),
    MACHINE("machine"),
    SMITH_MACHINE("smith_machine"),
    BODYWEIGHT("bodyweight"),
    LANDMINE("landmine"),
    OTHER("other");

    companion object {
        /** The equipment stored as [key]; null for a key this version does not know. */
        fun fromKey(key: String): Equipment? = entries.firstOrNull { it.key == key }
    }
}

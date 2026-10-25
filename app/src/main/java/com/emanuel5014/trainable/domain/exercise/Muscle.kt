package com.emanuel5014.trainable.domain.exercise

/**
 * The muscles an exercise can target. The order of the entries is the canonical one: stored lists and chips follow it.
 * [key] is what the database and the routine files hold, so it never changes once shipped.
 */
enum class Muscle(val key: String) {
    CHEST("chest"),
    LATS("lats"),
    TRAPS("traps"),
    UPPER_BACK("upper_back"),
    LOWER_BACK("lower_back"),
    FRONT_DELTS("front_delts"),
    SIDE_DELTS("side_delts"),
    REAR_DELTS("rear_delts"),
    BICEPS("biceps"),
    TRICEPS("triceps"),
    FOREARMS("forearms"),
    ABS("abs"),
    OBLIQUES("obliques"),
    QUADS("quads"),
    HAMSTRINGS("hamstrings"),
    GLUTES("glutes"),
    CALVES("calves"),
    ADDUCTORS("adductors");

    companion object {
        /** The muscle stored as [key]; null for a key this version does not know (a file or a backup of a newer one). */
        fun fromKey(key: String): Muscle? = entries.firstOrNull { it.key == key }
    }
}

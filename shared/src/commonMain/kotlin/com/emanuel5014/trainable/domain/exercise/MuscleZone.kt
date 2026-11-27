package com.emanuel5014.trainable.domain.exercise

/**
 * The muscles of the filter grouped by area of the body, in the order the filter shows them. Each area is named and
 * shaped after the exercise category it matches ([categoryKey], the stored category name), so the filter reads like the
 * pickers. Every [Muscle] belongs to exactly one area.
 */
enum class MuscleZone(val categoryKey: String, val muscles: List<Muscle>) {
    CHEST("Chest", listOf(Muscle.UPPER_CHEST, Muscle.MIDDLE_CHEST, Muscle.LOWER_CHEST)),
    BACK("Back", listOf(Muscle.LATS, Muscle.TRAPS, Muscle.UPPER_BACK, Muscle.LOWER_BACK)),
    SHOULDERS("Shoulders", listOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.REAR_DELTS)),
    ARMS("Arms", listOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS)),
    CORE("Core", listOf(Muscle.ABS, Muscle.OBLIQUES)),
    LEGS("Legs", listOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.ADDUCTORS, Muscle.ABDUCTORS));

    companion object {
        /** The area [muscle] belongs to. */
        fun of(muscle: Muscle): MuscleZone = entries.first { muscle in it.muscles }
    }
}

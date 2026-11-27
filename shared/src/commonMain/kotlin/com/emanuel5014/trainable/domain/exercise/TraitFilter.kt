package com.emanuel5014.trainable.domain.exercise

/**
 * The muscles and equipment an exercise list is narrowed down to. Inside a group any choice passes ("biceps or triceps");
 * the two groups must both pass. An exercise with nothing set in an active group does not pass it.
 */
data class TraitFilter(val muscles: Set<Muscle> = emptySet(), val equipment: Set<Equipment> = emptySet()) {

    val isActive: Boolean get() = muscles.isNotEmpty() || equipment.isNotEmpty()

    /** How many choices are on: the badge of the filter button. */
    val activeCount: Int get() = muscles.size + equipment.size

    fun matches(traits: ExerciseTraits): Boolean =
        (muscles.isEmpty() || traits.muscles.any { it in muscles }) &&
            (equipment.isEmpty() || traits.equipment in equipment)

    fun toggle(muscle: Muscle): TraitFilter = copy(muscles = muscles.toggled(muscle))

    fun toggle(equipment: Equipment): TraitFilter = copy(equipment = this.equipment.toggled(equipment))

    /** Whether every muscle of [zone] is chosen. */
    fun hasWhole(zone: MuscleZone): Boolean = muscles.containsAll(zone.muscles)

    /** Chooses every muscle of [zone], or none of them when they were all chosen already. */
    fun toggle(zone: MuscleZone): TraitFilter =
        copy(muscles = if (hasWhole(zone)) muscles - zone.muscles.toSet() else muscles + zone.muscles)

    companion object {
        val NONE = TraitFilter()
    }
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item

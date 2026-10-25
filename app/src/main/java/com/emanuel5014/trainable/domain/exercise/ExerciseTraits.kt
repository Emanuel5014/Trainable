package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/**
 * The two columns of an exercise as they are stored: null is "the user set nothing" (a preset then has the values of the
 * catalog), an empty string is "the user chose none", anything else is the user's own value.
 */
data class StoredTraits(val muscles: String?, val equipment: String?)

/** The target muscles (up to [MAX_MUSCLES], in canonical order) and the equipment of an exercise, as the app uses them. */
data class ExerciseTraits(val muscles: Set<Muscle> = emptySet(), val equipment: Equipment? = null) {

    val isEmpty: Boolean get() = muscles.isEmpty() && equipment == null

    companion object {
        const val MAX_MUSCLES = 3

        val EMPTY = ExerciseTraits()

        /** Traits from stored or imported keys: unknown and repeated keys are dropped, and at most 3 muscles are kept. */
        fun fromKeys(muscles: Iterable<String>, equipment: String?): ExerciseTraits = ExerciseTraits(
            muscles = muscles.mapNotNull(Muscle::fromKey).distinct().sortedBy { it.ordinal }.take(MAX_MUSCLES).toSet(),
            equipment = equipment?.let(Equipment::fromKey)
        )

        /**
         * What exercise [exerciseId] has, given the raw [muscles] and [equipment] columns. A preset (an id below
         * [ExerciseEntity.FIRST_CUSTOM_ID]) falls back to its entry in [catalog] for a column that is null or holds nothing
         * this version understands; a custom exercise falls back to nothing. An empty column means the user chose none.
         */
        fun of(
            exerciseId: Int,
            muscles: String?,
            equipment: String?,
            catalog: Map<Int, ExerciseTraits> = ExerciseTraitsCatalog.entries
        ): ExerciseTraits {
            val shipped = if (exerciseId < ExerciseEntity.FIRST_CUSTOM_ID) catalog[exerciseId] ?: EMPTY else EMPTY
            return ExerciseTraits(
                muscles = musclesOf(muscles, shipped.muscles),
                equipment = equipmentOf(equipment, shipped.equipment)
            )
        }

        /** What [exercise] has: [of] on the columns of its row. */
        fun of(exercise: ExerciseEntity, catalog: Map<Int, ExerciseTraits> = ExerciseTraitsCatalog.entries): ExerciseTraits =
            of(exercise.id, exercise.muscles, exercise.equipment, catalog)

        /**
         * The column values to store when the user saved [chosen] for exercise [exerciseId]. A preset stores only what
         * differs from its catalog entry, so saving it for another reason (a rename) never freezes its traits. A custom
         * exercise stores what it has, null for nothing.
         */
        fun forStorage(
            exerciseId: Int,
            chosen: ExerciseTraits,
            catalog: Map<Int, ExerciseTraits> = ExerciseTraitsCatalog.entries
        ): StoredTraits {
            if (exerciseId >= ExerciseEntity.FIRST_CUSTOM_ID) {
                return StoredTraits(
                    muscles = chosen.muscles.takeIf { it.isNotEmpty() }?.let(::encode),
                    equipment = chosen.equipment?.key
                )
            }
            val shipped = catalog[exerciseId] ?: EMPTY
            val picked = chosen.equipment
            return StoredTraits(
                muscles = if (chosen.muscles == shipped.muscles) null else encode(chosen.muscles),
                equipment = when (picked) {
                    shipped.equipment -> null
                    null -> ""
                    else -> picked.key
                }
            )
        }

        private fun musclesOf(column: String?, shipped: Set<Muscle>): Set<Muscle> = when {
            column == null -> shipped
            column.isEmpty() -> emptySet()
            else -> fromKeys(column.split(','), null).muscles.ifEmpty { shipped }
        }

        private fun equipmentOf(column: String?, shipped: Equipment?): Equipment? = when {
            column == null -> shipped
            column.isEmpty() -> null
            else -> Equipment.fromKey(column) ?: shipped
        }

        private fun encode(muscles: Set<Muscle>): String = muscles.sortedBy { it.ordinal }.joinToString(",") { it.key }
    }
}

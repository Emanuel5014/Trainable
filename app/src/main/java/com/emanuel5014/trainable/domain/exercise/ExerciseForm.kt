package com.emanuel5014.trainable.domain.exercise

/**
 * What the exercise form collects. [category] is the stored (English) category name; [description] is blank when none.
 * [muscles] and [equipment] are what the form shows and gives back, so for a preset the traits it has now, not the columns.
 */
data class ExerciseForm(
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val muscles: Set<Muscle> = emptySet(),
    val equipment: Equipment? = null
)

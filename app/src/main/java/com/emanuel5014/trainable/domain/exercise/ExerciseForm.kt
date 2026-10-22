package com.emanuel5014.trainable.domain.exercise

/** What the exercise form collects. [category] is the stored (English) category name; [description] is blank when none. */
data class ExerciseForm(val name: String = "", val category: String = "", val description: String = "")

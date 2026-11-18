package com.emanuel5014.trainable.data.repository

/** The user's own image or GIF of an exercise. The database keeps only the file name; each platform stores the file. */
interface ExerciseMediaFiles {
    /** Copies the picked file at [source] (the platform's address of it) in as media of [exerciseId]. Blocking. */
    fun import(source: String, exerciseId: Int): ExerciseMediaImport

    /** Deletes a stored file; nothing when [fileName] is null. Blocking. */
    fun delete(fileName: String?)
}

sealed interface ExerciseMediaImport {
    data class Saved(val fileName: String) : ExerciseMediaImport
    data object TooLarge : ExerciseMediaImport
    data object Unsupported : ExerciseMediaImport
}

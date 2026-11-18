package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What adding or swapping an exercise remembers between uses: the rest confirmed last time and the type each exercise was
 * last added with. Both are read while the lifter browses the list, so step 2 opens on them.
 */
interface ExerciseAddMemory {
    /** Null until the stored value is read. */
    val lastRestSeconds: StateFlow<Int?>

    fun rememberRest(seconds: Int)

    /** Empty until the stored value is read. */
    val types: StateFlow<Map<Int, String>>

    fun rememberType(exerciseId: Int, type: String)
}

/** Nothing remembered: the defaults every time. */
object NoExerciseAddMemory : ExerciseAddMemory {
    override val lastRestSeconds: StateFlow<Int?> = MutableStateFlow(null)
    override fun rememberRest(seconds: Int) {}
    override val types: StateFlow<Map<Int, String>> = MutableStateFlow(emptyMap())
    override fun rememberType(exerciseId: Int, type: String) {}
}

/** The platform's [ExerciseAddMemory], obtained where the sheet is (Android scopes its ViewModels to the screen). */
val LocalExerciseAddMemory = staticCompositionLocalOf<@Composable () -> ExerciseAddMemory> { { NoExerciseAddMemory } }

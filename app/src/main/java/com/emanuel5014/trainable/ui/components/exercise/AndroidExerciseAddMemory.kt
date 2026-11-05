package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * The remembered rest and types on Android: the two ViewModels the swap sheet always used, obtained where the sheet is
 * (so they are scoped to the screen that shows it, as before).
 */
val AndroidExerciseAddMemory: @Composable () -> ExerciseAddMemory = {
    val rest: RememberedRestViewModel = hiltViewModel()
    val types: RememberedExerciseTypesViewModel = hiltViewModel()
    remember(rest, types) {
        object : ExerciseAddMemory {
            override val lastRestSeconds: StateFlow<Int?> get() = rest.lastRestSeconds
            override fun rememberRest(seconds: Int) = rest.remember(seconds)
            override val types: StateFlow<Map<Int, String>> get() = types.types
            override fun rememberType(exerciseId: Int, type: String) = types.remember(exerciseId, type)
        }
    }
}

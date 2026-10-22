package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock

/**
 * The flags of the workout screen that open dialogs and sheets or switch the bottom hub between its modes.
 * They live here, not as locals of the screen, so the pieces of the screen can take them as one parameter.
 */
class WorkoutScreenUiState internal constructor(initialSessionName: String) {
    var isEditingValues by mutableStateOf(false)

    /** Index of a set being edited in place in the bottom hub, done or still to do (none by default); it resets with the exercise. */
    internal var editingSetState: MutableState<Int?> = mutableStateOf(null)
    var editingSetIndex: Int?
        get() = editingSetState.value
        set(value) { editingSetState.value = value }

    /** The set whose technique-as-done dialog is open (exercise index, set index). */
    var techniqueDialogFor by mutableStateOf<Pair<Int, Int>?>(null)
    var lastEditedIndex by mutableIntStateOf(0)
    var showSwapExerciseSheet by mutableStateOf(false)
    var showPlateSheet by mutableStateOf(false)

    // Changing the program of an exercise: the exercise (index) whose blocks are open in the editor, the new blocks
    // waiting for the answer to "update the routine or only this workout?", the exercise and block the load editor
    // is open for, and the exercise whose list of changes is shown
    var programEditFor by mutableStateOf<Int?>(null)
    var pendingProgram by mutableStateOf<Pair<Int, List<PrescriptionBlock>>?>(null)
    var loadSheetFor by mutableStateOf<Pair<Int, Int?>?>(null)
    var programHistoryFor by mutableStateOf<Int?>(null)

    var showAddExerciseSheet by mutableStateOf(false)
    var isAddingAfterCurrent by mutableStateOf(false)
    var showCancelDialog by mutableStateOf(false)
    var showFinishDialog by mutableStateOf(false)
    var showRenameDialog by mutableStateOf(false)
    var newSessionName by mutableStateOf(initialSessionName)
    var showWarmupTimerDialog by mutableStateOf(false)
    var warmupTimerDuration by mutableStateOf(120)

    /** Exercises whose EMOM sets the lifter logs by hand instead of with the minute clock. */
    var emomManualExercises by mutableStateOf(emptySet<Int>())

    /** The exercise (id, name) whose own image or GIF is open in the viewer. */
    var mediaViewer by mutableStateOf<Pair<Int, String>?>(null)
}

@Composable
fun rememberWorkoutScreenUiState(currentExerciseIndex: Int, planName: String): WorkoutScreenUiState {
    val ui = remember { WorkoutScreenUiState(planName) }
    // The set being edited in the hub is forgotten when the exercise changes
    ui.editingSetState = remember(currentExerciseIndex) { mutableStateOf<Int?>(null) }
    return ui
}

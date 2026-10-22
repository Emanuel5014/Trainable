package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.ui.components.journal.FinishWorkoutSheet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.ExercisePrescriptionSheet
import com.emanuel5014.trainable.ui.components.LoadPercentSheet
import com.emanuel5014.trainable.ui.components.ProgramEditHistoryDialog
import com.emanuel5014.trainable.ui.components.ProgramEditScopeDialog
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope
import com.emanuel5014.trainable.ui.components.PlateCalculatorSheet
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.components.RestoreExerciseDialog
import com.emanuel5014.trainable.ui.components.SwapExerciseBottomSheet
import com.emanuel5014.trainable.ui.components.RestSlider
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/** The dialogs that can open before the workout has loaded (technique as done, media viewer, detail, rename, new 1RM, restore). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WorkoutEarlyDialogs(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    exerciseMedia: Map<Int, String>,
    pickMedia: (Int) -> Unit,
    detailExerciseId: Int?,
    onDetailDismiss: () -> Unit
) {
    ui.techniqueDialogFor?.let { (exIndex, setIndex) ->
        state.exercises.getOrNull(exIndex)?.sets?.getOrNull(setIndex)?.let { set ->
            PerformedTechniqueDialog(
                planned = set.prescription?.techniques.orEmpty(),
                performed = set.performedTechniques,
                onDismiss = { ui.techniqueDialogFor = null },
                onSave = { techniques, applyToFollowing ->
                    viewModel.setPerformedTechniques(exIndex, setIndex, techniques, applyToFollowing)
                    ui.techniqueDialogFor = null
                }
            )
        }
    }

    ui.mediaViewer?.let { (exerciseId, exerciseName) ->
        val mediaFile = exerciseMedia[exerciseId]
        // The file disappears when the user removes it from the viewer: close it then
        LaunchedEffect(mediaFile) { if (mediaFile == null) ui.mediaViewer = null }
        if (mediaFile != null) {
            ExerciseMediaViewer(
                fileName = mediaFile,
                exerciseName = exerciseName,
                onDismiss = { ui.mediaViewer = null },
                onReplace = { pickMedia(exerciseId) },
                onRemove = { viewModel.removeExerciseMedia(exerciseId) }
            )
        }
    }

    detailExerciseId?.let { ExerciseDetailDialog(exerciseId = it, onDismiss = onDetailDismiss) }

    if (ui.showRenameDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { ui.showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_workout)) },
            text = {
                com.emanuel5014.trainable.ui.components.GymInputField(
                    value = ui.newSessionName,
                    onValueChange = { ui.newSessionName = it },
                    label = stringResource(R.string.routine_name),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSessionName(ui.newSessionName)
                    ui.showRenameDialog = false
                }) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ui.showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    state.oneRepMaxSuggestions.firstOrNull()?.let { suggestion ->
        val fmt = { kg: Float -> WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, state.weightUnit), state.weightUnit) }
        AlertDialog(
            onDismissRequest = { viewModel.resolveOneRepMaxSuggestion(accept = false) },
            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Primary) },
            title = { Text(stringResource(R.string.new_one_rep_max_title), fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    stringResource(
                        R.string.new_one_rep_max_message,
                        fmt(suggestion.weightKg), suggestion.reps, suggestion.exerciseName,
                        fmt(suggestion.currentKg), fmt(suggestion.suggestedKg)
                    )
                )
            },
            confirmButton = {
                GymButton(
                    onClick = { viewModel.resolveOneRepMaxSuggestion(accept = true) },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.save_new_one_rep_max).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolveOneRepMaxSuggestion(accept = false) }) {
                    Text(stringResource(R.string.not_now).uppercase(), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    // The routine has exercises the user deleted from the exercise list: offer to add them back
    if (state.deletedExercises.isNotEmpty()) {
        RestoreExerciseDialog(
            exerciseNames = state.deletedExercises.map { ExerciseTranslations.translate(it.nome, languageCode) },
            onRestore = { viewModel.restoreDeletedExercises() },
            onSkip = { viewModel.dismissDeletedExercises() },
            onDismiss = { viewModel.dismissDeletedExercises() }
        )
    }
}

/** The dialogs and sheets of the loaded workout: warm-up timer, plate calculator, program editing, swap and add exercise, cancel, finish. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WorkoutDialogs(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    currentExState: WorkoutExerciseState?,
    activeSet: WorkoutSetState?,
    activeSetIndex: Int,
    plateExercises: Map<Int, Float?>,
    languageCode: String,
    availableExercises: List<ExerciseEntity>,
    categories: List<String>,
    safeNavigateBack: () -> Unit
) {
    if (ui.showWarmupTimerDialog) {
        AlertDialog(
            onDismissRequest = { ui.showWarmupTimerDialog = false },
            title = {
                Text(stringResource(R.string.warmup_timer), fontWeight = FontWeight.Black)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.warmup_timer_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val haptic = LocalHapticFeedback.current
                    RestSlider(
                        value = ui.warmupTimerDuration,
                        onValueChange = { ui.warmupTimerDuration = it },
                        hapticEnabled = true,
                        haptic = haptic
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startWarmupTimer(ui.warmupTimerDuration)
                    ui.showWarmupTimerDialog = false
                }) {
                    Text(stringResource(R.string.start).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ui.showWarmupTimerDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (ui.showPlateSheet && activeSet != null) {
        currentExState?.let { exState ->
            val exerciseId = exState.exercise.id
            PlateCalculatorSheet(
                exerciseName = ExerciseTranslations.translate(exState.exercise.nome, languageCode),
                weightKg = activeSet.weight,
                barKg = plateExercises[exerciseId],
                weightUnit = state.weightUnit,
                plates = state.availablePlates,
                onApply = { newWeightKg ->
                    viewModel.updateSetWeight(state.currentExerciseIndex, activeSetIndex, newWeightKg)
                },
                onBarChange = { barKg -> viewModel.setPlateCalculator(exerciseId, true, barKg) },
                onDisable = { viewModel.setPlateCalculator(exerciseId, false, plateExercises[exerciseId]) },
                onDismiss = { ui.showPlateSheet = false }
            )
        }
    }

    // --- Changing the program of an exercise during the workout ---
    ui.programEditFor?.let { exerciseIndex ->
        val exercise = state.exercises.getOrNull(exerciseIndex)
        if (exercise == null || !exercise.isAdvanced) {
            ui.programEditFor = null
        } else {
            val editedName = ExerciseTranslations.translate(exercise.exercise.nome, languageCode)
            ExercisePrescriptionSheet(
                exerciseId = exercise.exercise.id,
                exerciseName = editedName,
                initialBlocks = exercise.blocks,
                subtitle = stringResource(R.string.program_edit_subtitle),
                onDismiss = { ui.programEditFor = null },
                onSave = { blocks ->
                    val canUpdateRoutine = exercise.planDetails != null && exercise.swappedExerciseId == null
                    when {
                        blocks == exercise.blocks -> ui.programEditFor = null
                        // An exercise that is not in the routine can only be a variation of this workout
                        !canUpdateRoutine -> {
                            viewModel.applyProgramEdit(exerciseIndex, blocks, ProgramEditScope.WORKOUT)
                            ui.programEditFor = null
                        }
                        else -> ui.pendingProgram = exerciseIndex to blocks
                    }
                },
                onRemove = null
            )
        }
    }

    ui.pendingProgram?.let { (exerciseIndex, blocks) ->
        val exercise = state.exercises.getOrNull(exerciseIndex)
        if (exercise == null) {
            ui.pendingProgram = null
        } else {
            ProgramEditScopeDialog(
                exerciseName = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
                week = state.programWeek,
                onChoose = { scope ->
                    viewModel.applyProgramEdit(exerciseIndex, blocks, scope)
                    ui.pendingProgram = null
                    ui.programEditFor = null
                },
                onDismiss = { ui.pendingProgram = null }
            )
        }
    }

    ui.loadSheetFor?.let { (exerciseIndex, blockIndex) ->
        val exercise = state.exercises.getOrNull(exerciseIndex)
        val toDo = exercise?.let { ProgramEditing.percentBlocksToDo(it.sets, it.blocks) }.orEmpty()
        if (exercise == null || toDo.isEmpty()) {
            ui.loadSheetFor = null
        } else {
            LoadPercentSheet(
                exerciseName = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
                blocks = exercise.blocks,
                activeBlockIndex = blockIndex,
                blocksToDo = toDo,
                setsToDoByBlock = exercise.sets.filter { !it.isCompleted && !it.isExtra }
                    .groupingBy { it.prescription?.blockIndex ?: -1 }.eachCount(),
                oneRepMaxKg = exercise.oneRepMaxKg,
                weightUnit = state.weightUnit,
                roundingIncrement = state.loadRoundingIncrement,
                programWeek = state.programWeek,
                canUpdatePlan = exercise.planDetails != null && exercise.swappedExerciseId == null,
                hapticEnabled = state.hapticEnabled,
                onApply = { blockIndices, delta, scope ->
                    viewModel.shiftLoad(exerciseIndex, blockIndices, delta, scope)
                    ui.loadSheetFor = null
                },
                onDismiss = { ui.loadSheetFor = null }
            )
        }
    }

    ui.programHistoryFor?.let { exerciseIndex ->
        val exercise = state.exercises.getOrNull(exerciseIndex)
        if (exercise == null || exercise.programEdits.isEmpty()) {
            ui.programHistoryFor = null
        } else {
            ProgramEditHistoryDialog(
                exerciseName = ExerciseTranslations.translate(exercise.exercise.nome, languageCode),
                edits = exercise.programEdits,
                weightUnit = state.weightUnit,
                onDismiss = { ui.programHistoryFor = null }
            )
        }
    }

    if (ui.showSwapExerciseSheet) {
        currentExState?.let { exState ->
            SwapExerciseBottomSheet(
                // A cardio exercise has no sets or reps to carry over
                currentSets = exState.sets.size.takeIf { it > 0 } ?: 3,
                currentReps = if (exState.isCardio) "8" else exState.currentReps.repsTarget,
                currentRepsMax = if (exState.isCardio) null else exState.currentReps.repsMax,
                availableExercises = availableExercises,
                languageCode = languageCode,
                onExerciseSelected = { newExercise, sets, reps, rest, exerciseType, durataTargetSec ->
                    viewModel.swapExercise(
                        state.currentExerciseIndex, newExercise.id, sets, reps.repsTarget, rest, exerciseType, durataTargetSec,
                        repsMax = reps.repsMax
                    )
                    ui.showSwapExerciseSheet = false
                },
                onAddCustomExercise = { nome, categoria, onCreated ->
                    viewModel.addCustomExercise(nome, categoria, onCreated)
                },
                onDismiss = { ui.showSwapExerciseSheet = false },
                categories = categories,
                onCardioExerciseSelected = { newExercise, durationMinutes, rest, sets ->
                    viewModel.swapToCardioExercise(state.currentExerciseIndex, newExercise.id, durationMinutes, rest, sets)
                    ui.showSwapExerciseSheet = false
                },
                onAdvancedExerciseSelected = { newExercise, blocks, rest ->
                    val (sets, reps) = com.emanuel5014.trainable.domain.prescription.PrescriptionExpander.legacyTargets(blocks)
                    viewModel.swapExercise(state.currentExerciseIndex, newExercise.id, sets, reps, rest, blocks = blocks)
                    ui.showSwapExerciseSheet = false
                },
                initialBlocks = exState.blocks,
                currentExerciseId = exState.exercise.id
            )
        }
    }

    if (ui.showAddExerciseSheet) {
        SwapExerciseBottomSheet(
            currentSets = 3,
            currentReps = "8",
            availableExercises = availableExercises,
            languageCode = languageCode,
            onExerciseSelected = { exercise, sets, reps, rest, exerciseType, durataTargetSec ->
                if (ui.isAddingAfterCurrent) {
                    viewModel.addExerciseAfterCurrent(exercise, sets, reps.repsTarget, rest, exerciseType = exerciseType, durataTargetSecondi = durataTargetSec, repsMax = reps.repsMax)
                } else {
                    viewModel.addExerciseToActiveSession(exercise, sets, reps.repsTarget, rest, exerciseType = exerciseType, durataTargetSecondi = durataTargetSec, repsMax = reps.repsMax)
                }
                ui.isAddingAfterCurrent = false
                ui.showAddExerciseSheet = false
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                viewModel.addCustomExercise(nome, categoria, onCreated)
            },
            onDismiss = {
                ui.isAddingAfterCurrent = false
                ui.showAddExerciseSheet = false
            },
            isAdding = true,
            categories = categories,
            onCardioExerciseSelected = { exercise, durationMinutes, rest, sets ->
                if (ui.isAddingAfterCurrent) {
                    viewModel.addExerciseAfterCurrent(exercise, 1, "1", rest, durationMinutes, cardioSetsCount = sets)
                } else {
                    viewModel.addExerciseToActiveSession(exercise, 1, "1", rest, durationMinutes, cardioSetsCount = sets)
                }
                ui.isAddingAfterCurrent = false
                ui.showAddExerciseSheet = false
            },
            onAdvancedExerciseSelected = { exercise, blocks, rest ->
                val (sets, reps) = com.emanuel5014.trainable.domain.prescription.PrescriptionExpander.legacyTargets(blocks)
                if (ui.isAddingAfterCurrent) {
                    viewModel.addExerciseAfterCurrent(exercise, sets, reps, rest, blocks = blocks)
                } else {
                    viewModel.addExerciseToActiveSession(exercise, sets, reps, rest, blocks = blocks)
                }
                ui.isAddingAfterCurrent = false
                ui.showAddExerciseSheet = false
            }
        )
    }

    if (ui.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { ui.showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_workout_title)) },
            text = { Text(stringResource(R.string.cancel_workout_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.cancelWorkout { safeNavigateBack() }
                        ui.showCancelDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { ui.showCancelDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (ui.showFinishDialog && state.journalEnabled && state.journalPromptAtFinish) {
        FinishWorkoutSheet(
            title = stringResource(R.string.finish_workout_title),
            message = stringResource(
                if (state.completedExercises == state.totalExercises) R.string.finish_workout_message
                else R.string.finish_workout_message_incomplete
            ),
            journal = state.journalDraft,
            onJournalChange = viewModel::updateJournalDraft,
            hapticEnabled = state.hapticEnabled,
            onFinish = {
                viewModel.finishWorkout()
                ui.showFinishDialog = false
            },
            onCancel = { ui.showFinishDialog = false }
        )
    } else if (ui.showFinishDialog) {
        AlertDialog(
            onDismissRequest = { ui.showFinishDialog = false },
            title = { Text(stringResource(R.string.finish_workout_title)) },
            text = {
                Text(
                    stringResource(
                        if (state.completedExercises == state.totalExercises) R.string.finish_workout_message
                        else R.string.finish_workout_message_incomplete
                    )
                )
            },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.finishWorkout()
                        ui.showFinishDialog = false
                    },
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.finish_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { ui.showFinishDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }
}

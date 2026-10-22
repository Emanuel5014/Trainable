package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.remember
import com.emanuel5014.trainable.ui.components.BadgeShape
import com.emanuel5014.trainable.ui.components.SetNumberBadge
import com.emanuel5014.trainable.ui.components.motion.RollingText
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import androidx.compose.ui.platform.LocalContext
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.ui.components.HubSheetState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.components.hubScaled
import com.emanuel5014.trainable.ui.components.HubSlot
import com.emanuel5014.trainable.ui.components.HubPanel
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseNavigation
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.PlateCalculatorEnableChip
import com.emanuel5014.trainable.ui.components.PlateChip
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.components.RestTimerSection
import com.emanuel5014.trainable.ui.components.TimerAdjustButton
import com.emanuel5014.trainable.ui.components.WeightRepsInput
import com.emanuel5014.trainable.ui.components.WeightTimeInput
import com.emanuel5014.trainable.ui.components.TimeRepsInput
import com.emanuel5014.trainable.ui.components.setValueText
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.ui.components.techniqueLabel
import androidx.compose.foundation.layout.BoxScope

/** The bottom hub of the workout: the card of the active set, its editor, the rest card and the way to the next exercise. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BoxScope.WorkoutHub(
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    currentExState: WorkoutExerciseState?,
    activeSet: WorkoutSetState?,
    activeSetIndex: Int,
    isExerciseCompleted: Boolean,
    setBeingEdited: WorkoutSetState?,
    usesEmomClock: (WorkoutExerciseState) -> Boolean,
    hubSheet: HubSheetState,
    onHubHeightChanged: (Int) -> Unit,
    languageCode: String,
    prescriptionLabels: PrescriptionLabels,
    plateExercises: Map<Int, Float?>
) {
    val context = LocalContext.current
    val isCardioActive = currentExState?.isCardio == true
    val isEmomActive = currentExState != null && usesEmomClock(currentExState)
    // While the minute clock runs, its screen has everything the lifter needs.
    val showBottomHub = (!isCardioActive || currentExState.isCardioCompleted || (!state.cardioTimerRunning && !state.cardioTimerPaused)) &&
        !(isEmomActive && state.emomRun != null)
    val isResting = state.remainingRestSeconds > 0
    val hubMode = when {
        // The pause of a mini-set is over: how many reps did it have?
        state.miniSet?.awaitingReps == true -> HubMode.MiniSet
        // A set being edited wins over the rest card, which stays visible in a slim form
        setBeingEdited != null && !isCardioActive && !isEmomActive -> HubMode.EditingSet
        isResting -> HubMode.Resting
        // Cardio and the EMOM clock have their own screens: the hub only navigates.
        isCardioActive || isEmomActive -> HubMode.Cardio
        activeSet == null || activeSet.isCompleted -> HubMode.Completed
        ui.isEditingValues -> HubMode.Editing
        else -> HubMode.Logging
    }
    // These ask the lifter to type something: the hub opens by itself and goes back to the strip afterwards
    val hubNeedsInput = hubMode == HubMode.MiniSet || hubMode == HubMode.EditingSet || hubMode == HubMode.Editing
    // Logging and the two editors share one panel, so changing between them does not swap the content of the hub
    val hubKey = if (hubMode == HubMode.Editing || hubMode == HubMode.EditingSet) HubMode.Logging else hubMode

    HubPanel(
        sheet = hubSheet,
        visible = showBottomHub,
        locked = hubNeedsInput,
        hapticEnabled = state.hapticEnabled,
        horizontalPadding = com.emanuel5014.trainable.ui.theme.ResponsiveSize.horizontalPadding,
        onHeightChanged = onHubHeightChanged,
        modifier = Modifier.align(Alignment.BottomCenter)
    ) { expansion ->
            AnimatedVisibility(
                visible = state.warmupTimerEnabled && state.warmupTimerRemaining > 0,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                val warmupProgress = if (state.warmupTimerTotalSeconds > 0)
                    1f - (state.warmupTimerRemaining.toFloat() / state.warmupTimerTotalSeconds.toFloat()) else 0f
                val e = expansion()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(lerp(14.dp, 18.dp, e)))
                        .background(Tertiary)
                        .padding(lerp(9.dp, 16.dp, e))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularWavyProgressIndicator(
                                    progress = { warmupProgress },
                                    modifier = Modifier.size(lerp(32.dp, 48.dp, e)),
                                    color = OnTertiary,
                                    trackColor = OnTertiary.copy(alpha = 0.2f),
                                    stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                                    trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = OnTertiary,
                                    modifier = Modifier.size(lerp(14.dp, 20.dp, e))
                                )
                            }
                            Spacer(modifier = Modifier.width(lerp(10.dp, 14.dp, e)))
                            Column {
                                HubSlot(expansion = expansion, from = 0.3f, to = 0.55f) {
                                    Text(
                                        text = stringResource(R.string.warmup_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnTertiary.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val wMinutes = state.warmupTimerRemaining / 60
                                val wSeconds = state.warmupTimerRemaining % 60
                                Text(
                                    text = String.format("%d:%02d", wMinutes, wSeconds),
                                    style = MaterialTheme.typography.titleLarge.hubScaled(lerp(0.78f, 1f, e)),
                                    color = OnTertiary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (state.showTimerSubtract) {
                                TimerAdjustButton(
                                    label = "−${state.timerSubtractSeconds}s",
                                    onClick = { viewModel.adjustWarmupTime(-state.timerSubtractSeconds) }
                                )
                            }
                            if (state.showTimerAdd) {
                                TimerAdjustButton(
                                    label = "+${state.timerAddSeconds}s",
                                    onClick = { viewModel.adjustWarmupTime(state.timerAddSeconds) }
                                )
                            }
                            FilledIconButton(
                                onClick = { viewModel.skipWarmupTimer() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = OnTertiary,
                                    contentColor = Tertiary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = hubKey,
                label = "HubTransition",
                transitionSpec = {
                    (fadeIn() + expandVertically(expandFrom = Alignment.Bottom))
                        .togetherWith(fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom))
                }
            ) { mode ->
                when (mode) {
                    HubMode.Cardio -> {
                        currentExState?.let { ex ->
                            CardioHubSection(
                                exerciseState = ex,
                                state = state,
                                viewModel = viewModel,
                                languageCode = languageCode,
                                onFinishWorkout = { ui.showFinishDialog = true },
                                onAddExercise = {
                                    ui.isAddingAfterCurrent = false
                                    ui.showAddExerciseSheet = true
                                },
                                expansion = expansion
                            )
                        }
                    }
                    HubMode.Resting -> {
                        Column {
                            RestTimerSection(
                                remainingSeconds = state.remainingRestSeconds,
                                totalRestSeconds = state.totalRestSeconds,
                                onAddTime = { viewModel.adjustRestTime(state.timerAddSeconds) },
                                onSubtractTime = { viewModel.adjustRestTime(-state.timerSubtractSeconds) },
                                addSeconds = state.timerAddSeconds,
                                subtractSeconds = state.timerSubtractSeconds,
                                showAdd = state.showTimerAdd,
                                showSubtract = state.showTimerSubtract,
                                onSkip = { viewModel.skipRestTimer() },
                                expansion = expansion
                            )
                            val miniSet = state.miniSet
                            val offer = state.miniSetOffer
                            if (miniSet != null) {
                                val miniSetNumber = state.exercises.getOrNull(miniSet.exerciseIndex)?.sets?.getOrNull(miniSet.setIndex)?.setNumber ?: 0
                                HubSlot(expansion = expansion, from = 0.45f, to = 0.7f, gap = 8.dp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    MiniSetPauseLabel(miniSetNumber)
                                }
                            } else if (offer != null) {
                                HubSlot(expansion = expansion, from = 0.45f, to = 0.7f, gap = 8.dp) {
                                    MiniSetOfferButton(missing = offer.missing, onClick = { viewModel.startMiniSet(offer.exerciseIndex, offer.setIndex) })
                                }
                            }
                            // The rest is the time to load the bar for the next set
                            HubSlot(expansion = expansion, from = 0.45f, to = 0.7f, gap = 8.dp) {
                                ActiveSetPlateChip(currentExState, activeSet, state, plateExercises) { ui.showPlateSheet = true }
                            }
                        }
                    }
                    HubMode.Logging, HubMode.Editing, HubMode.EditingSet -> {
                        // Logging, editing the active set and editing a tapped set are one panel: the card stays, the editor opens under it
                        SetPanel(
                            hubMode = hubMode,
                            expansion = expansion,
                            state = state,
                            ui = ui,
                            viewModel = viewModel,
                            currentExState = currentExState,
                            activeSet = activeSet,
                            activeSetIndex = activeSetIndex,
                            isResting = isResting,
                            prescriptionLabels = prescriptionLabels,
                            plateExercises = plateExercises
                        )
                    }
                    HubMode.MiniSet -> {
                        state.miniSet?.let { run ->
                            val miniSetSet = state.exercises.getOrNull(run.exerciseIndex)?.sets?.getOrNull(run.setIndex)
                            if (miniSetSet != null) {
                                MiniSetEntry(
                                    setNumber = miniSetSet.setNumber,
                                    soFar = miniSetSet.repParts.ifEmpty { listOf(miniSetSet.reps) }.joinToString("+"),
                                    initialReps = run.missing,
                                    hapticEnabled = state.hapticEnabled,
                                    onLog = { viewModel.logMiniSet(it) },
                                    onEnd = { viewModel.endMiniSet() }
                                )
                            }
                        }
                    }
                    HubMode.Completed -> {
                        val isLastExercise = state.currentExerciseIndex == state.exercises.size - 1
                        val allowModify = state.isQuickWorkout || state.inlineExerciseModificationsEnabled
                        // On the strip the exercise buttons are a little lower and Add and the mini-set offer are gone
                        val navHeight = hubNavHeight(expansion())

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // The last set of the workout has no rest to offer it in
                            state.miniSetOffer?.let { offer ->
                                HubSlot(expansion = expansion, from = 0.55f, to = 0.8f, gapBelow = 8.dp) {
                                    MiniSetOfferButton(missing = offer.missing, onClick = { viewModel.startMiniSet(offer.exerciseIndex, offer.setIndex) })
                                }
                            }
                            if (allowModify) {
                                HubSlot(expansion = expansion, from = 0.55f, to = 0.8f, gapBelow = 8.dp) {
                                    GymButton(
                                        onClick = {
                                            ui.isAddingAfterCurrent = false
                                            ui.showAddExerciseSheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = SurfaceContainerHigh,
                                        contentColor = Primary
                                    ) {
                                        Icon(Icons.Rounded.KeyboardDoubleArrowRight, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.add_exercise).uppercase(),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Previous Exercise Button
                                if (state.currentExerciseIndex > 0) {
                                    val prevName = state.exercises[state.currentExerciseIndex - 1].exercise.nome
                                    HubPreviousButton(
                                        previousName = ExerciseTranslations.translate(prevName, languageCode),
                                        expansion = expansion,
                                        onClick = { viewModel.previousExercise() },
                                        nameStyle = MaterialTheme.typography.bodyMedium,
                                        nameWeight = FontWeight.Black,
                                        height = navHeight
                                    )
                                }

                                // Next / Finish Button
                                if (!isLastExercise) {
                                    val nextName = state.exercises[state.currentExerciseIndex + 1].exercise.nome
                                    val translatedNext = ExerciseTranslations.translate(nextName, languageCode)
                                    GymButton(
                                        onClick = { viewModel.nextExercise() },
                                        modifier = Modifier.weight(1f),
                                        height = navHeight
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.next_exercise_btn).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OnPrimary.copy(alpha = 0.75f),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = translatedNext,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = OnPrimary,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null
                                        )
                                    }
                                } else if (state.isQuickWorkout) {
                                    // Quick workout last exercise finish button
                                    GymButton(
                                        onClick = { ui.showFinishDialog = true },
                                        modifier = Modifier.weight(1f),
                                        height = navHeight
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.finish_workout).uppercase(),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                } else {
                                    // Guided / Quick workout last exercise finish button
                                    GymButton(
                                        onClick = { ui.showFinishDialog = true },
                                        modifier = Modifier.weight(1f),
                                        height = navHeight
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.finish_workout).uppercase(),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // The first thing to leave when the hub shrinks: the strip keeps only the main action
            if ((!isExerciseCompleted && !isEmomActive) || isResting) {
                HubSlot(expansion = expansion, from = 0.7f, to = 0.95f, gap = 16.dp) {
                    ExerciseNavigation(
                        onPrevious = { viewModel.previousExercise() },
                        onNext = {
                            if (state.currentExerciseIndex == state.exercises.size - 1) {
                                ui.showFinishDialog = true
                            } else {
                                viewModel.nextExercise()
                            }
                        },
                        hasPrevious = state.currentExerciseIndex > 0,
                        hasNext = state.currentExerciseIndex < state.exercises.size - 1,
                        previousName = if (state.currentExerciseIndex > 0) state.exercises[state.currentExerciseIndex - 1].exercise.nome else null,
                        nextName = if (state.currentExerciseIndex < state.exercises.size - 1) state.exercises[state.currentExerciseIndex + 1].exercise.nome else null,
                        languageCode = languageCode
                    )
                }
            }
    }
}

/**
 * The editor of a set in the bottom hub, opened under the card of the set: its weight and reps (or time), the way it felt,
 * and what else the lifter logs about it. It serves both the active set and a set tapped in the list; what differs between the two comes in as
 * callbacks and slots: [aboveInputs] and [afterPercentChip] sit around the weight and reps wheels of a strength set,
 * [afterExtras] under the extras row, [primaryAction] is the button at the bottom.
 */
@Composable
internal fun SetEditorPanel(
    exercise: WorkoutExerciseState?,
    set: WorkoutSetState,
    setIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onRpeChange: (Float?) -> Unit,
    primaryAction: @Composable () -> Unit,
    aboveInputs: @Composable () -> Unit = {},
    afterPercentChip: @Composable () -> Unit = {},
    afterExtras: @Composable () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (exercise?.isTimeAndReps == true) {
            TimeRepsInput(
                seconds = set.timeSeconds ?: exercise.timeTargetSeconds ?: TimeAndReps.DEFAULT_SECONDS,
                reps = set.reps,
                onSecondsChange = onSecondsChange,
                onRepsChange = onRepsChange
            )
            RpeInput(state, set.rpe, onRpeChange)
        } else if (exercise?.isTimeAndWeight == true) {
            WeightTimeInput(
                weight = set.weight,
                seconds = set.timeSeconds ?: exercise.timeTargetSeconds ?: 45,
                onWeightChange = onWeightChange,
                onSecondsChange = onSecondsChange,
                weightUnit = state.weightUnit
            )
            RpeInput(state, set.rpe, onRpeChange)
        } else {
            aboveInputs()
            WeightRepsInput(
                weight = set.weight,
                reps = set.reps,
                onWeightChange = onWeightChange,
                onRepsChange = onRepsChange,
                weightUnit = state.weightUnit
            )
            SetPercentChip(exercise, set) {
                ui.loadSheetFor = state.currentExerciseIndex to set.prescription?.blockIndex
            }
            afterPercentChip()
            RpeInput(state, set.rpe, onRpeChange)
            if (state.setPerformanceEnabled) {
                SetExtrasRow(
                    assisted = set.assistedReps,
                    partial = set.partialReps,
                    onAssistedChange = { viewModel.updateSetAssistedReps(state.currentExerciseIndex, setIndex, it) },
                    onPartialChange = { viewModel.updateSetPartialReps(state.currentExerciseIndex, setIndex, it) },
                    hapticEnabled = state.hapticEnabled,
                    techniqueChanged = set.performedTechniques != null,
                    onEditTechnique = if (set.prescription?.techniques.isNullOrEmpty()) null else {
                        { ui.techniqueDialogFor = state.currentExerciseIndex to setIndex }
                    }
                )
            }
            afterExtras()
        }
        primaryAction()
    }
}

/** What the editor of the hub shows: the set, where it stands in the list, and whether it was tapped in the list. */
private data class EditorTarget(val set: WorkoutSetState, val index: Int, val tapped: Boolean)

/**
 * The card of the set being worked on and, when the lifter edits it, the editor opened under the same card: the card
 * keeps its place and turns into the title of the editor.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetPanel(
    hubMode: HubMode,
    expansion: () -> Float,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    currentExState: WorkoutExerciseState?,
    activeSet: WorkoutSetState?,
    activeSetIndex: Int,
    isResting: Boolean,
    prescriptionLabels: PrescriptionLabels,
    plateExercises: Map<Int, Float?>
) {
    val editing = hubMode == HubMode.Editing || hubMode == HubMode.EditingSet
    val editsTappedSet = hubMode == HubMode.EditingSet
    val editedIndex = ui.editingSetIndex ?: ui.lastEditedIndex
    val shownIndex = if (editsTappedSet) editedIndex else activeSetIndex
    val shownSet = if (editsTappedSet) currentExState?.sets?.getOrNull(editedIndex) else activeSet

    // The editor keeps what it showed while it closes, so its values do not jump to those of the active set
    val lastEditor = remember { arrayOfNulls<EditorTarget>(1) }
    val editor = if (editing && shownSet != null) {
        EditorTarget(shownSet, shownIndex, editsTappedSet).also { lastEditor[0] = it }
    } else {
        lastEditor[0]
    }

    Column {
        if (shownSet != null) {
            SetCard(
                set = shownSet,
                exercise = currentExState,
                state = state,
                isActiveSet = !editsTappedSet,
                expansion = expansion,
                prescriptionLabels = prescriptionLabels,
                onClick = if (editing) null else ({ ui.isEditingValues = true }),
                trailing = {
                    if (editing) {
                        // The rest card is hidden while a tapped set is edited: keep the countdown in sight
                        if (editsTappedSet && isResting) RestTimerPill(state.remainingRestSeconds)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHighest)
                                .clickable {
                                    if (editsTappedSet) ui.editingSetIndex = null else ui.isEditingValues = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.collapse))
                        }
                    } else {
                        val e = expansion()
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        LogSetButton(
                            onClick = {
                                if (shownSet.isAmrap) {
                                    ui.isEditingValues = true
                                } else if (currentExState?.isTimed == true) {
                                    viewModel.skipTimerAndLogSet(
                                        state.currentExerciseIndex,
                                        activeSetIndex,
                                        shownSet.weight,
                                        shownSet.timeSeconds ?: currentExState.timeTargetSeconds ?: 45
                                    )
                                } else {
                                    viewModel.toggleSetComplete(state.currentExerciseIndex, activeSetIndex)
                                }
                            },
                            modifier = Modifier.width(120.dp).height(lerp(44.dp, 48.dp, e)),
                            compact = true
                        )
                    }
                }
            )
        }
        AnimatedVisibility(
            visible = editing,
            enter = expandVertically(WorkoutMotion.spatial()) + fadeIn(WorkoutMotion.effects()),
            exit = shrinkVertically(WorkoutMotion.spatial()) + fadeOut(WorkoutMotion.effects())
        ) {
            if (editor != null) {
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    if (editor.tapped) {
                        SetEditorPanel(
                            exercise = currentExState,
                            set = editor.set,
                            setIndex = editor.index,
                            state = state,
                            ui = ui,
                            viewModel = viewModel,
                            onWeightChange = { viewModel.editSetWeight(state.currentExerciseIndex, editor.index, it) },
                            onRepsChange = { viewModel.editSetReps(state.currentExerciseIndex, editor.index, it) },
                            onSecondsChange = { viewModel.editSetSeconds(state.currentExerciseIndex, editor.index, it) },
                            onRpeChange = { viewModel.editSetRpe(state.currentExerciseIndex, editor.index, it) },
                            afterExtras = {
                                // A set already done can still be finished after a short pause
                                if (state.setPerformanceEnabled && editor.set.isCompleted && !editor.set.isEmom) {
                                    MiniSetOfferButton(missing = 0, onClick = {
                                        ui.editingSetIndex = null
                                        viewModel.startMiniSet(state.currentExerciseIndex, editor.index)
                                    })
                                }
                            },
                            primaryAction = {
                                GymButton(
                                    onClick = { ui.editingSetIndex = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.done).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        )
                    } else {
                        SetEditorPanel(
                            exercise = currentExState,
                            set = editor.set,
                            setIndex = editor.index,
                            state = state,
                            ui = ui,
                            viewModel = viewModel,
                            onWeightChange = { newW -> viewModel.updateSetWeight(state.currentExerciseIndex, editor.index, newW) },
                            onRepsChange = { newR -> viewModel.updateSetReps(state.currentExerciseIndex, editor.index, newR) },
                            onSecondsChange = { newT -> viewModel.updateSetTimeSeconds(state.currentExerciseIndex, editor.index, newT) },
                            onRpeChange = { viewModel.updateSetRpe(state.currentExerciseIndex, editor.index, it) },
                            aboveInputs = {
                                if (editor.set.isAmrap) {
                                    Text(
                                        text = stringResource(R.string.amrap_enter_reps),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                editor.set.repsRange?.let { range ->
                                    Text(
                                        text = stringResource(R.string.reps_goal, range.text),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            afterPercentChip = {
                                if (state.plateCalculatorEnabled && currentExState != null) {
                                    val exerciseId = currentExState.exercise.id
                                    if (exerciseId in plateExercises) {
                                        PlateChip(
                                            weightKg = editor.set.weight,
                                            barKg = plateExercises[exerciseId],
                                            weightUnit = state.weightUnit,
                                            plates = state.availablePlates,
                                            onClick = { ui.showPlateSheet = true }
                                        )
                                    } else {
                                        PlateCalculatorEnableChip(onClick = {
                                            viewModel.setPlateCalculator(exerciseId, true, null)
                                            ui.showPlateSheet = true
                                        })
                                    }
                                }
                            },
                            primaryAction = {
                                LogSetButton(onClick = {
                                    if (currentExState?.isTimed == true) {
                                        viewModel.skipTimerAndLogSet(
                                            state.currentExerciseIndex,
                                            editor.index,
                                            editor.set.weight,
                                            editor.set.timeSeconds ?: currentExState.timeTargetSeconds ?: 45
                                        )
                                    } else {
                                        viewModel.toggleSetComplete(state.currentExerciseIndex, editor.index)
                                    }
                                    ui.isEditingValues = false
                                })
                            }
                        )
                    }
                }
            }
        }
        if (!editing && shownSet != null) {
            HubSlot(expansion = expansion, from = 0.45f, to = 0.7f, gap = 8.dp) {
                ActiveSetPlateChip(currentExState, shownSet, state, plateExercises) { ui.showPlateSheet = true }
            }
        }
    }
}

/** The card of a set in the hub: its number, what it asks, and what the lifter can do with it ([trailing]). */
@Composable
private fun SetCard(
    set: WorkoutSetState,
    exercise: WorkoutExerciseState?,
    state: WorkoutState,
    isActiveSet: Boolean,
    expansion: () -> Float,
    prescriptionLabels: PrescriptionLabels,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    val context = LocalContext.current
    // The card keeps its place all the way down to the strip: what goes around it leaves first
    val e = expansion()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(lerp(18.dp, 24.dp, e)))
            .background(SurfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(lerp(8.dp, 12.dp, e)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetNumberBadge(
            number = set.setNumber,
            shape = when {
                set.isCompleted -> BadgeShape.Done
                isActiveSet -> BadgeShape.Active
                else -> BadgeShape.Pending
            },
            containerColor = when {
                set.isCompleted -> Tertiary
                isActiveSet -> Primary
                else -> SurfaceContainerHighest
            },
            contentColor = when {
                set.isCompleted -> OnTertiary
                isActiveSet -> OnPrimary
                else -> OnSurfaceVariant
            },
            size = lerp(36.dp, 48.dp, e)
        )
        Spacer(modifier = Modifier.width(lerp(10.dp, 16.dp, e)))
        Column(modifier = Modifier.weight(1f)) {
            HubSlot(expansion = expansion, from = 0.3f, to = 0.6f) {
                Text(
                    text = if (isActiveSet) stringResource(R.string.active_set) else stringResource(R.string.adjust_set_number, set.setNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
            RollingText(
                text = setValueText(
                    kind = exercise?.setKind ?: SetLogKind.STRENGTH,
                    weightKg = set.weight,
                    reps = set.reps,
                    seconds = if (exercise?.isTimed == true) set.timeSeconds ?: exercise.timeTargetSeconds ?: 45 else null,
                    weightUnit = state.weightUnit,
                    repsText = if (set.isAmrap) stringResource(R.string.max_label) else null
                ),
                style = MaterialTheme.typography.titleLarge.hubScaled(lerp(0.82f, 1f, e)),
                fontWeight = FontWeight.ExtraBold
            )
            val p = set.prescription
            val detail = listOfNotNull(
                p?.let { PrescriptionFormatter.intensity(it.intensityType, it.intensityValue, prescriptionLabels) },
                set.repsRange?.let { stringResource(R.string.reps_goal, it.text) }
            ) + p?.techniques.orEmpty().map { techniqueLabel(context, it) } +
                listOfNotNull(if (set.isExtra) stringResource(R.string.extra_badge) else null)
            if (detail.isNotEmpty()) {
                HubSlot(expansion = expansion, from = 0.25f, to = 0.5f) {
                    Text(
                        text = detail.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        trailing()
    }
}

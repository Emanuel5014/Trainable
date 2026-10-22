package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.ui.components.SegmentedShapes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListState
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity
import com.emanuel5014.trainable.ui.components.ExerciseMediaCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.GymButton
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.components.SetLogRow
import com.emanuel5014.trainable.ui.components.setPerformanceBadges
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.ui.components.rpeBadgeText
import com.emanuel5014.trainable.ui.components.techniqueLabel

/** The sets of a strength or timed exercise, grouped by block, with the large media card and the room for the hub below. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColumnScope.SetList(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    targetActiveSetIndex: Int,
    exerciseListState: LazyListState,
    expandedSetIndex: Int?,
    onExpandedSetIndexChange: (Int?) -> Unit,
    largeMediaFile: String?,
    largeMediaVisible: Boolean,
    freeMediaPx: Int,
    hubHeightPx: Int,
    isExerciseCompleted: Boolean,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    prescriptionLabels: PrescriptionLabels
) {
    val localDensity = LocalDensity.current
    val context = LocalContext.current
    // The sets of a block are one segmented list: where each stands in its block decides its corners
    val positions = remember(targetExState.sets, targetExState.isAdvanced) {
        SegmentedShapes.positions(
            targetExState.sets.map { if (targetExState.isAdvanced) it.isExtra to it.prescription?.blockIndex else null }
        )
    }
    // Once the sets have come in, those scrolled into view later do not
    var entered by remember(targetIndex) { mutableStateOf(false) }
    LaunchedEffect(targetIndex) {
        delay(STAGGERED_ROWS * STAGGER_MS + 450)
        entered = true
    }
    // Sets List
    LazyColumn(
        state = exerciseListState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(targetExState.sets) { index, set ->
            val isActive = index == targetActiveSetIndex && !set.isCompleted
            val position = positions.getOrNull(index)
            val corners = SegmentedShapes.corners(position?.index ?: 0, position?.count ?: 1, isActive)
            // The first sets of an exercise come in one after the other, a little apart, when it appears
            val animateIn = remember { !entered }
            val enter = remember { Animatable(if (animateIn && index < STAGGERED_ROWS) 0f else 1f) }
            LaunchedEffect(Unit) {
                if (enter.value < 1f) {
                    delay(index * STAGGER_MS)
                    enter.animateTo(1f, WorkoutMotion.spatial())
                }
            }
            Column(
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - enter.value) * 24.dp.toPx()
                    alpha = enter.value.coerceIn(0f, 1f)
                }
            ) {
                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text(stringResource(R.string.remove_set)) },
                        text = { Text(stringResource(R.string.remove_set_confirm)) },
                        confirmButton = {
                            TextButton(onClick = {
                                ui.editingSetIndex = null
                                viewModel.removeSetFromExercise(targetIndex, index)
                                showDeleteConfirm = false
                            }) {
                                Text(stringResource(R.string.delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text(stringResource(R.string.cancel).uppercase())
                            }
                        },
                        containerColor = SurfaceContainerHigh,
                        titleContentColor = OnSurface,
                        textContentColor = OnSurfaceVariant
                    )
                }

                val haptic = LocalHapticFeedback.current
                val isExpanded = targetExState.isTimed && (expandedSetIndex == index) && !set.isCompleted

                if (targetExState.isAdvanced) {
                    val previous = targetExState.sets.getOrNull(index - 1)
                    val p = set.prescription
                    val startsGroup = index == 0 ||
                        previous?.isExtra != set.isExtra ||
                        previous?.prescription?.blockIndex != p?.blockIndex
                    if (startsGroup) {
                        val block = p?.let { targetExState.blocks.getOrNull(it.blockIndex) }
                        // The load of a block that still has sets to do can be moved from its title
                        val loadEditable = block != null && p.blockIndex in
                            ProgramEditing.percentBlocksToDo(targetExState.sets, targetExState.blocks)
                        Column(
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (loadEditable) Modifier.clickable { ui.loadSheetFor = targetIndex to p!!.blockIndex }
                                        else Modifier
                                    )
                                    .background(Primary.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        block != null -> stringResource(
                                            R.string.block_header, p.blockIndex + 1,
                                            (listOf(PrescriptionFormatter.headline(block, prescriptionLabels)) +
                                                block.techniques.map { techniqueLabel(context, it) }).joinToString(" · ")
                                        ).uppercase()
                                        set.isExtra -> stringResource(R.string.extra_badge)
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (loadEditable) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        contentDescription = stringResource(R.string.load_sheet_title),
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (block?.repMode == RepMode.TOTAL) {
                                val total = block.totalReps ?: 0
                                val done = targetExState.sets
                                    .filter { it.prescription?.blockIndex == p.blockIndex && it.isCompleted }
                                    .sumOf { it.reps }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.LinearWavyProgressIndicator(
                                        progress = { if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier.weight(1f),
                                        color = Primary,
                                        trackColor = SurfaceContainerHighest
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.total_reps_progress, done, total),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            block?.note?.let { note ->
                                Text(note, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }

                val setPrescription = set.prescription
                SetLogRow(
                    corners = corners,
                    repsLabel = if (set.isAmrap) stringResource(R.string.max_label) else null,
                    intensityBadge = setPrescription?.let { PrescriptionFormatter.intensity(it.intensityType, it.intensityValue, prescriptionLabels) }
                        ?.takeIf { setPrescription.intensityType == IntensityType.PERCENT || setPrescription.intensityType == IntensityType.RPE },
                    badges = buildList {
                        // The goal of a rep range stays visible until the set is done
                        if (!set.isCompleted) set.repsRange?.let { add(it.text) }
                        if (set.isAmrap && set.isCompleted) add(stringResource(R.string.max_label))
                        setPrescription?.techniques?.forEach { add(techniqueLabel(context, it)) }
                        set.rpe?.let { add(rpeBadgeText(context, it, state.rpeScale)) }
                        if (set.isExtra) add(stringResource(R.string.extra_badge))
                        // How the set really went: parts after short pauses, help, partials, technique as done
                        addAll(setPerformanceBadges(context, set.repParts, set.pauses, set.assistedReps, set.partialReps, set.performedTechniques))
                    },
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps,
                    note = set.note,
                    isWarmup = set.isWarmup,
                    isCompleted = set.isCompleted,
                    onToggleComplete = { 
                        if (set.isAmrap && !set.isCompleted && index == targetActiveSetIndex) {
                            // AMRAP: reps must be entered before logging
                            ui.isEditingValues = true
                        } else if (targetExState.isTimed && !set.isCompleted) {
                            viewModel.skipTimerAndLogSet(
                                targetIndex,
                                index,
                                set.weight,
                                set.timeSeconds ?: targetExState.timeTargetSeconds ?: 45
                            )
                        } else {
                            viewModel.toggleSetComplete(targetIndex, index) 
                        }
                    },
                    onNoteChange = { newNote ->
                        viewModel.updateSetNote(targetIndex, index, newNote)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirm = true
                    },
                    onEditValues = {
                        ui.editingSetIndex = null
                        ui.isEditingValues = !ui.isEditingValues
                    },
                    // A tap on the card edits that set in place, done or still to do; the square
                    // on the right is what checks and unchecks it
                    onEdit = {
                        ui.editingSetIndex = if (ui.editingSetIndex == index) null else index
                        ui.isEditingValues = false
                    },
                    isEditing = ui.editingSetIndex == index && targetIndex == state.currentExerciseIndex,
                    isActive = isActive,
                    weightUnit = state.weightUnit,
                    previousNote = set.previousNote,
                    timeSeconds = set.timeSeconds,
                    isExpanded = isExpanded,
                    isTimeAndReps = set.isTimeAndReps,
                    onToggleExpanded = if (targetExState.isTimed && !set.isCompleted) {
                        { onExpandedSetIndexChange(if (expandedSetIndex == index) null else index) }
                    } else null,
                    expandedContent = if (isExpanded) {
                        {
                            val targetSeconds = set.timeSeconds ?: targetExState.timeTargetSeconds ?: 45
                            val isCurrentActive = index == targetActiveSetIndex
                            val timerSeconds = if (isCurrentActive) state.setTimerSeconds else 0
                            val isRunning = isCurrentActive && state.setTimerRunning
                            val isPaused = isCurrentActive && state.setTimerPaused
                            val isTargetReached = targetSeconds > 0 && timerSeconds >= targetSeconds
                            val timerColor = if (isTargetReached) Color(0xFF4CAF50) else Primary
                            val timerTrackColor = if (isTargetReached) Color(0xFF4CAF50).copy(alpha = 0.25f) else Primary.copy(alpha = 0.2f)
                            val progress = if (targetSeconds > 0) (timerSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f) else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceContainerHighest.copy(alpha = 0.5f))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(130.dp)
                                    ) {
                                        CircularWavyProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(130.dp),
                                            color = timerColor,
                                            trackColor = timerTrackColor,
                                            stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                                            trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke,
                                            gapSize = 8.dp,
                                            wavelength = 28.dp,
                                            amplitude = { p ->
                                                if (!isRunning) 0f
                                                else if (p <= 0.1f) 0f
                                                else 1f
                                            }
                                        )
                                        if (state.autoStopTimeWeightAtTarget && isTargetReached) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .background(timerColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ThumbUp,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        } else {
                                            CardioToggleButton(
                                                isRunning = isRunning,
                                                isPaused = isPaused,
                                                onToggle = {
                                                    if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    when {
                                                        isRunning -> viewModel.pauseSetTimer()
                                                        else -> viewModel.startSetTimer()
                                                    }
                                                },
                                                modifier = Modifier.size(80.dp),
                                                activeColor = timerColor,
                                                onActiveColor = if (isTargetReached) Color.White else OnPrimary
                                            )
                                        }
                                    }

                                    val curFormatted = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60)
                                    val tarFormatted = String.format("%02d:%02d", targetSeconds / 60, targetSeconds % 60)
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = curFormatted,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isTargetReached) timerColor else if (isRunning) Primary else OnSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "/ $tarFormatted",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = OnSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    if (set.isTimeAndReps) {
                                        // What was done in the interval: counted while the clock runs, fixed up once it stops
                                        CountStepper(
                                            value = set.reps,
                                            onValueChange = { viewModel.updateSetReps(targetIndex, index, it) },
                                            min = 0,
                                            max = TimeAndReps.MAX_REPS,
                                            hapticEnabled = state.hapticEnabled,
                                            big = true,
                                            label = stringResource(R.string.reps_done_label)
                                        )
                                    }

                                    if (isRunning || isPaused || timerSeconds > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledIconButton(
                                                onClick = {
                                                    if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.resetSetTimer()
                                                },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = SurfaceContainerHigh,
                                                    contentColor = OnSurfaceVariant
                                                ),
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Replay,
                                                    contentDescription = stringResource(R.string.time_weight_reset_timer),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            GymButton(
                                                onClick = {
                                                    if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.finishSetTimer(targetIndex, index, set.weight)
                                                },
                                                containerColor = timerColor,
                                                contentColor = if (isTargetReached) Color.White else OnPrimary,
                                                modifier = Modifier.fillMaxWidth(0.72f),
                                                height = 42
                                            ) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    stringResource(R.string.time_weight_finish_set),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else null
                )
            }
        }
        if (targetExState.isAdvanced) {
            item {
                GymButton(
                    onClick = { viewModel.addExtraSet(targetIndex) },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    height = 48
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_extra_set).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
         if (state.isQuickWorkout || state.inlineExerciseModificationsEnabled) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymButton(
                        onClick = { viewModel.addSetToExercise(targetIndex) },
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = Primary
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_set), fontWeight = FontWeight.ExtraBold)
                    }
                    if (!isExerciseCompleted) {
                        val isLastExercise = targetIndex == state.exercises.size - 1
                        GymButton(
                            onClick = {
                                ui.isAddingAfterCurrent = !state.isQuickWorkout
                                ui.showAddExerciseSheet = true
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = Primary
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (state.isQuickWorkout) {
                                        if (isLastExercise) R.string.add_exercise else R.string.next_exercise_wrk
                                    } else {
                                        R.string.add_exercise
                                    }
                                ).uppercase(),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
        if (largeMediaFile != null) {
            item(key = MEDIA_CARD_KEY) {
                val targetHeight = if (largeMediaVisible) with(localDensity) { freeMediaPx.toDp() } else 0.dp
                val cardHeight by animateDpAsState(targetHeight, label = "exerciseMediaCardHeight")
                if (cardHeight > 0.dp) {
                    ExerciseMediaCard(
                        fileName = largeMediaFile,
                        onOpen = {
                            ui.mediaViewer = targetExState.exercise.id to
                                ExerciseTranslations.translate(targetExState.exercise.nome, languageCode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                    )
                }
            }
        }
        item(key = "hub_spacer") {
            // The list ends above the hub, whatever step it is on: with the large media card
            // exactly above it, otherwise with a little air
            val hubSpacer = when {
                largeMediaVisible -> with(localDensity) { hubHeightPx.toDp() }
                hubHeightPx > 0 -> with(localDensity) { hubHeightPx.toDp() } + 16.dp
                com.emanuel5014.trainable.ui.theme.ResponsiveSize.isShortHeight -> 220.dp
                else -> 280.dp
            }
            Spacer(modifier = Modifier.height(hubSpacer)) // Space for the dynamic hub
        }
    }
}

/** How many sets come in one after the other, and how long each one waits for the one before. */
private const val STAGGERED_ROWS = 6
private const val STAGGER_MS = 25L

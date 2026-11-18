package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import com.emanuel5014.trainable.sharedui.resources.ai_scan_new_will_be_created
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ai.hasValidReps
import com.emanuel5014.trainable.data.ai.repsDraft
import com.emanuel5014.trainable.data.ai.typeId
import com.emanuel5014.trainable.data.ai.withType
import com.emanuel5014.trainable.data.ai.withRepsDraft
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.domain.prescription.WeekShift
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_scan_custom_badge
import com.emanuel5014.trainable.sharedui.resources.ai_scan_needs_review
import com.emanuel5014.trainable.sharedui.resources.ai_scan_read_as
import com.emanuel5014.trainable.sharedui.resources.ai_scan_remove_exercise
import com.emanuel5014.trainable.sharedui.resources.cardio_duration_slider
import com.emanuel5014.trainable.sharedui.resources.rest_seconds
import com.emanuel5014.trainable.sharedui.resources.scan_one_rep_max_found
import com.emanuel5014.trainable.sharedui.resources.scan_programmed_hint
import com.emanuel5014.trainable.sharedui.resources.sets
import com.emanuel5014.trainable.ui.components.AdvancedPrescriptionEditor
import com.emanuel5014.trainable.ui.components.ExerciseTypeSelector
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.OneRepMaxBinding
import com.emanuel5014.trainable.ui.components.Slider
import com.emanuel5014.trainable.ui.components.TargetSecondsSlider
import com.emanuel5014.trainable.ui.components.WeekEditing
import com.emanuel5014.trainable.ui.components.exercise.RepsEditor
import com.emanuel5014.trainable.ui.components.exercise.TimeAndRepsEditor
import com.emanuel5014.trainable.ui.screens.onboarding.JoinedPosition
import com.emanuel5014.trainable.ui.screens.onboarding.joinedShape
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.util.sortedByKeys
import org.jetbrains.compose.resources.stringResource

/**
 * One exercise of the scan review: a compact line (number, name, "4 × 8–12 · 1:30", chips) that opens in place
 * to edit it. Only one row is open at a time, so the photo of the sheet above stays in view.
 */
@Composable
fun ScanReviewRow(
    entry: ScannedExerciseEntry,
    number: Int,
    position: JoinedPosition,
    expanded: Boolean,
    seen: Boolean,
    languageCode: String,
    onToggle: () -> Unit,
    onUpdate: (ScannedExerciseEntry) -> Unit,
    onRemove: () -> Unit,
    onChangeExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = ExerciseTranslations.translate(
        if (entry.exerciseId != null) entry.matchedName ?: entry.rawName else entry.rawName,
        languageCode
    )
    val flagged = entry.needsReview || !entry.hasValidReps
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onRemove()
        }
    }

    // A closed row can be swiped left to remove it (an open one has its own button and sliders to drag)
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !expanded,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(joinedShape(position))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.ai_scan_remove_exercise),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(joinedShape(position))
            .background(SurfaceContainerHigh)
            // The open row is outlined: the fields inside keep the usual contrast with the row
            .then(if (expanded) Modifier.border(1.5.dp, Primary.copy(alpha = 0.6f), joinedShape(position)) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberBadge(number = number, matched = entry.exerciseId != null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.exerciseId == null) {
                        RowChip(stringResource(Res.string.ai_scan_custom_badge), Primary.copy(alpha = 0.15f), Primary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = scanEntrySummary(entry),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (flagged) {
                        RowChip(
                            stringResource(Res.string.ai_scan_needs_review),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            if (seen && !flagged) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = OnSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ScanEntryEditor(
                entry = entry,
                number = number,
                title = title,
                onUpdate = onUpdate,
                onRemove = onRemove,
                onChangeExercise = onChangeExercise
            )
        }
    }
    }
}

/** "4 × 8–12 · 1:30": what the row holds, with "?" for reps that cannot be saved yet. */
internal fun scanEntrySummary(entry: ScannedExerciseEntry): String {
    val rest = " · ${restClock(entry.restSeconds)}"
    return when {
        entry.isCardio -> "${entry.cardioMinutes ?: 20} min"
        entry.isAdvanced -> "${entry.sets} × ${entry.reps}$rest"
        entry.isTimeAndReps -> "${entry.sets} × ${entry.reps} · ${entry.timeSeconds ?: TimeAndReps.DEFAULT_SECONDS} s$rest"
        entry.isTimeAndWeight -> "${entry.sets} × ${entry.timeSeconds ?: entry.reps.filter { it.isDigit() }.toIntOrNull() ?: 45} s$rest"
        else -> {
            val target = entry.repsDraft().target
            val reps = target?.range?.let { "${it.min}–${it.max}" } ?: target?.repsTarget ?: "?"
            "${entry.sets} × $reps$rest"
        }
    }
}

private fun restClock(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NumberBadge(number: Int, matched: Boolean) {
    // MaterialShapes.toShape() is @Composable in this material3: not wrapped in remember
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(if (matched) Primary else Primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (matched) OnPrimary else Primary
        )
    }
}

@Composable
private fun RowChip(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** The open part of a row: which exercise it is, its type and its prescription, then the rest. */
@Composable
private fun ScanEntryEditor(
    entry: ScannedExerciseEntry,
    number: Int,
    title: String,
    onUpdate: (ScannedExerciseEntry) -> Unit,
    onRemove: () -> Unit,
    onChangeExercise: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // The reps editor reports reps and then sets in one go: each change must build on the previous one,
    // not on the entry this composition was given
    val latest = remember { arrayOf(entry) }
    latest[0] = entry
    val update: (ScannedExerciseEntry) -> Unit = { latest[0] = it; onUpdate(it) }
    var setsText by remember(entry.rawName, number, entry.isAdvanced) { mutableStateOf(entry.sets.toString()) }
    val onSetsChange: (String) -> Unit = { value ->
        setsText = value
        value.toIntOrNull()?.let { update(latest[0].copy(sets = it)) }
    }
    val environment = com.emanuel5014.trainable.ui.components.LocalPrescriptionEnvironment.current()
    // While the row is open the editor keeps every mode's values (8–12 → Fisso → Intervallo gives 8–12 back);
    // they go when the row closes. A new type rebuilds it from the row.
    var repsDraft by remember(entry.rawName, number, entry.typeId) { mutableStateOf(entry.repsDraft()) }
    var advancedWeeksCount by remember(entry.rawName, number) { mutableIntStateOf(entry.blocksByWeek.keys.maxOrNull() ?: 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Which exercise it is, as a field like "Modifica esercizio" in the routine: tap to pick another
        ExerciseField(
            title = title,
            subtitle = if (entry.exerciseId == null) {
                stringResource(Res.string.ai_scan_new_will_be_created)
            } else {
                entry.suggestedCategory
            },
            onClick = onChangeExercise
        )

        entry.readReps?.let { read ->
            Text(
                text = stringResource(Res.string.ai_scan_read_as, read),
                style = MaterialTheme.typography.bodyMedium,
                // Red only while the row still needs the user; once fixed it is just a note
                color = if (entry.needsReview || !entry.hasValidReps) MaterialTheme.colorScheme.error else OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (entry.isProgrammed && !entry.isAdvanced) {
            Text(
                text = stringResource(Res.string.scan_programmed_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        entry.oneRepMaxKg?.let { kg ->
            Text(
                text = stringResource(Res.string.scan_one_rep_max_found, com.emanuel5014.trainable.ui.components.formatWeight(kg, "kg")),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        if (entry.isCardio) {
            CardioDurationSlider(
                valueMinutes = entry.cardioMinutes ?: 20,
                onValueChange = { update(latest[0].copy(cardioMinutes = it)) },
                haptic = haptic
            )
        } else {
            ExerciseTypeSelector(
                selectedType = entry.typeId,
                showAdvanced = LocalAdvancedProgramming.current,
                onTypeSelected = { type -> latest[0].withType(type)?.let(update) },
                modifier = Modifier.fillMaxWidth(),
                compact = true
            )

            when {
                entry.isAdvanced -> {
                    fun withBlocks(blocks: Map<Int, List<PrescriptionBlock>>): ScannedExerciseEntry {
                        val firstWeek = blocks.filterValues { it.isNotEmpty() }.sortedByKeys().values.firstOrNull()
                        val (sets, reps) = firstWeek?.let { PrescriptionExpander.legacyTargets(it) } ?: (latest[0].sets to latest[0].reps)
                        return latest[0].copy(blocksByWeek = blocks, sets = sets.coerceIn(1, 30), reps = reps)
                    }
                    AdvancedPrescriptionEditor(
                        blocksByWeek = entry.blocksByWeek,
                        onBlocksByWeekChange = { update(withBlocks(it)) },
                        exerciseId = entry.exerciseId,
                        exerciseName = title,
                        oneRepMax = OneRepMaxBinding(
                            kg = entry.oneRepMaxKg ?: entry.exerciseId?.let { environment.oneRepMaxes[it] },
                            estimatedKg = entry.exerciseId?.let { environment.estimatedOneRepMaxes[it] },
                            onChange = { kg, _ -> update(latest[0].copy(oneRepMaxKg = kg)) }
                        ),
                        weeks = WeekEditing(
                            weeksCount = advancedWeeksCount,
                            onWeeksCountChange = { advancedWeeksCount = it },
                            onDeleteWeek = { week ->
                                update(withBlocks(WeekShift.removeWeek(latest[0].blocksByWeek, week)))
                                advancedWeeksCount = (advancedWeeksCount - 1).coerceAtLeast(1)
                            },
                            persistedWeeksCount = 0
                        ),
                        // Each row scans on its own; the routine scan and its draft are untouched by this
                        aiScanKey = "program_scan_${number - 1}"
                    )
                }
                entry.isTimeAndReps -> {
                    var goalText by remember(entry.rawName, number) { mutableStateOf(entry.reps) }
                    TimeAndRepsEditor(
                        setsText = setsText,
                        onSetsChange = onSetsChange,
                        repsGoalText = goalText,
                        onRepsGoalChange = { value ->
                            goalText = value
                            TimeAndReps.parseGoal(value)?.let { update(latest[0].copy(reps = it.toString())) }
                        },
                        seconds = entry.timeSeconds ?: TimeAndReps.DEFAULT_SECONDS,
                        onSecondsChange = { update(latest[0].copy(timeSeconds = it)) },
                        hapticEnabled = true,
                        haptic = haptic,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                entry.isTimeAndWeight -> {
                    GymInputField(
                        value = setsText,
                        onValueChange = onSetsChange,
                        label = stringResource(Res.string.sets),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TargetSecondsSlider(
                        valueSeconds = entry.timeSeconds ?: entry.reps.filter { it.isDigit() }.toIntOrNull() ?: 45,
                        onValueChange = { sec -> update(latest[0].copy(timeSeconds = sec, reps = "${sec}s")) },
                        hapticEnabled = true,
                        haptic = haptic,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> RepsEditor(
                    draft = repsDraft,
                    onDraftChange = { draft ->
                        repsDraft = draft
                        update(latest[0].withRepsDraft(draft))
                    },
                    setsText = setsText,
                    onSetsChange = onSetsChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        RestSlider(
            value = entry.restSeconds,
            onValueChange = { update(latest[0].copy(restSeconds = it)) },
            haptic = haptic
        )

        // Removing is set apart at the bottom, in red, so it is never mistaken for closing the row
        HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onRemove)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(Res.string.ai_scan_remove_exercise),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** The exercise of an open row, outlined with a chevron: name, then category or "new, will be created". */
@Composable
private fun ExerciseField(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 1)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}

@Composable
private fun RestSlider(value: Int, onValueChange: (Int) -> Unit, haptic: HapticFeedback) {
    StepSlider(
        label = stringResource(Res.string.rest_seconds),
        valueText = restClock(value),
        steps = listOf(0, 30, 60, 90, 120, 180, 240, 300),
        value = value,
        onValueChange = onValueChange,
        haptic = haptic
    )
}

@Composable
private fun CardioDurationSlider(valueMinutes: Int, onValueChange: (Int) -> Unit, haptic: HapticFeedback) {
    StepSlider(
        label = stringResource(Res.string.cardio_duration_slider),
        valueText = "$valueMinutes min",
        steps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120),
        value = valueMinutes,
        onValueChange = onValueChange,
        haptic = haptic
    )
}

/** A slider that snaps to [steps], showing the label and the current value above it. */
@Composable
private fun StepSlider(
    label: String,
    valueText: String,
    steps: List<Int>,
    value: Int,
    onValueChange: (Int) -> Unit,
    haptic: HapticFeedback
) {
    val currentIndex = remember(value, steps) {
        steps.indexOf(steps.minByOrNull { kotlin.math.abs(it - value) } ?: steps.first()).coerceAtLeast(0)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            Text(text = valueText, style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { raw ->
                val index = kotlin.math.round(raw).toInt().coerceIn(0, steps.size - 1)
                if (index != currentIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[index])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

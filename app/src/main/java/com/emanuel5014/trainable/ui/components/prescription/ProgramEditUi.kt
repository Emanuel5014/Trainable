package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.domain.prescription.BlockChange
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.ProgramDiff
import com.emanuel5014.trainable.domain.prescription.LoadShift
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import java.text.DateFormat
import java.util.Date

/** The pill an exercise wears once its prescription was changed during the workout. */
@Composable
fun ProgramEditPill(edit: SessionPrescriptionEditEntity, modifier: Modifier = Modifier) {
    val updatedPlan = edit.editScope == ProgramEditScope.PLAN
    PrescriptionPill(
        text = stringResource(if (updatedPlan) R.string.program_edit_pill_plan else R.string.program_edit_pill_workout),
        modifier = modifier,
        containerColor = Tertiary.copy(alpha = 0.15f),
        contentColor = Tertiary
    )
}

/** The line under the weight and reps of a set with a %1RM: how much it asks for, and the way into the load editor. */
@Composable
fun LoadPercentChip(percentText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    M3Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Primary.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.change_percent_button, percentText),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Asked once the prescription of an exercise was changed in a workout: replace the week of the routine, or keep the
 * change as a variation of this workout only (the routine stays as it is).
 */
@Composable
fun ProgramEditScopeDialog(
    exerciseName: String,
    week: Int?,
    onChoose: (ProgramEditScope) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_edit_scope_title), fontWeight = FontWeight.ExtraBold) },
        text = {
            // Scrolls when a small screen or large text leaves less room than the two options need
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.program_edit_scope_message, exerciseName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                ScopeOption(
                    title = stringResource(R.string.program_edit_scope_plan),
                    description = if (week != null) stringResource(R.string.program_edit_scope_plan_week, week)
                    else stringResource(R.string.program_edit_scope_plan_all),
                    highlighted = true,
                    onClick = { onChoose(ProgramEditScope.PLAN) }
                )
                ScopeOption(
                    title = stringResource(R.string.program_edit_scope_workout),
                    description = stringResource(R.string.program_edit_scope_workout_desc),
                    highlighted = false,
                    onClick = { onChoose(ProgramEditScope.WORKOUT) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold, color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = Shapes.extraLarge
    )
}

@Composable
private fun ScopeOption(title: String, description: String, highlighted: Boolean, onClick: () -> Unit) {
    M3Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (highlighted) Primary.copy(alpha = 0.12f) else Surface,
        border = BorderStroke(1.dp, if (highlighted) Primary.copy(alpha = 0.4f) else OnSurfaceVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) Primary else OnSurface
            )
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
}

/** What a set of blocks says as one line, in the user's units and language ("70% 1×6 · 75% 4×5 Pause 2″"). */
fun programSummary(context: android.content.Context, weightUnit: String, blocks: List<PrescriptionBlock>): String =
    if (blocks.isEmpty()) "-" else PrescriptionFormatter.summary(blocks, prescriptionLabels(context, weightUnit))

/**
 * What an edit changed, one line per block: "Block 2: 75% 3×3 Pause 1″ → 72.5% 3×3 Pause 1″", or the block that was
 * added or taken out. Empty when only something the blocks do not show (a note, a rest) changed.
 */
fun programChangeLines(context: android.content.Context, weightUnit: String, before: List<PrescriptionBlock>, after: List<PrescriptionBlock>): List<String> {
    val labels = prescriptionLabels(context, weightUnit)
    fun text(block: PrescriptionBlock) = PrescriptionFormatter.summary(listOf(block), labels)
    return ProgramDiff.changes(before, after).map { change ->
        when (change.kind) {
            BlockChange.Kind.CHANGED -> context.getString(R.string.load_preview_line, change.blockNumber, text(change.before!!), text(change.after!!))
            BlockChange.Kind.ADDED -> context.getString(R.string.program_edit_block_added, change.blockNumber, text(change.after!!))
            BlockChange.Kind.REMOVED -> context.getString(R.string.program_edit_block_removed, change.blockNumber, text(change.before!!))
        }
    }
}

/**
 * Quick editor of the percentage of the load of the blocks still to do, opened from the workout. The new percentage is
 * the one of the block the active set belongs to; the same number of points can move every block still to do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadPercentSheet(
    exerciseName: String,
    blocks: List<PrescriptionBlock>,
    activeBlockIndex: Int?,
    blocksToDo: List<Int>,
    setsToDoByBlock: Map<Int, Int>,
    oneRepMaxKg: Float?,
    weightUnit: String,
    roundingIncrement: Float,
    programWeek: Int?,
    canUpdatePlan: Boolean,
    hapticEnabled: Boolean,
    onApply: (blockIndices: Set<Int>, delta: Float, scope: ProgramEditScope) -> Unit,
    onDismiss: () -> Unit
) {
    val focusBlock = activeBlockIndex?.takeIf { it in blocksToDo } ?: blocksToDo.firstOrNull() ?: return
    val original = blocks.getOrNull(focusBlock)?.intensityValue ?: return
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var target by remember { mutableStateOf(original) }
    var shiftAll by remember { mutableStateOf(false) }
    var toPlan by remember { mutableStateOf(false) }

    val delta = LoadShift.clamp(target) - original
    val affected = if (shiftAll) blocksToDo.toSet() else setOf(focusBlock)
    val labels = rememberPrescriptionLabels(weightUnit)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.cardPadding, spacing = Spacing.large) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                    Text(
                        text = stringResource(R.string.load_sheet_title).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)
                        ),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.load_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                // The percentage and, right under it, the bar it makes
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    NumberStepper(
                        value = target,
                        step = LoadShift.STEP_PERCENT,
                        range = LoadShift.MIN_PERCENT..LoadShift.MAX_PERCENT,
                        label = stringResource(R.string.load_new_percent),
                        suffix = "%",
                        onValueChange = { target = it },
                        onTick = {
                            if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                    )
                    Text(
                        text = if (oneRepMaxKg != null && oneRepMaxKg > 0f) {
                            stringResource(
                                R.string.load_weight_result,
                                labels.weight(LoadCalculator.weightForPercent(oneRepMaxKg, LoadShift.clamp(target), weightUnit, roundingIncrement)),
                                stringResource(R.string.one_rep_max_value, labels.weight(oneRepMaxKg))
                            )
                        } else stringResource(R.string.load_one_rep_max_missing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (oneRepMaxKg != null && oneRepMaxKg > 0f) Primary else Error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(-5f, -2.5f, 2.5f, 5f).forEach { step ->
                            FilterChip(
                                selected = kotlin.math.abs(delta - step) < 0.001f,
                                onClick = { target = LoadShift.clamp(original + step) },
                                label = {
                                    Text(
                                        text = (if (step > 0) "+" else "−") + PrescriptionFormatter.number(kotlin.math.abs(step)),
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                                    selectedLabelColor = Primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (blocksToDo.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        SectionTitle(stringResource(R.string.load_apply_to))
                        ChoiceRow(
                            selected = !shiftAll,
                            title = stringResource(R.string.load_apply_block),
                            description = stringResource(R.string.load_apply_block_desc, focusBlock + 1, setsToDoByBlock[focusBlock] ?: 0),
                            onClick = { shiftAll = false }
                        )
                        ChoiceRow(
                            selected = shiftAll,
                            title = stringResource(R.string.load_apply_all),
                            description = stringResource(
                                R.string.load_apply_all_desc,
                                (if (delta >= 0) "+" else "−") + PrescriptionFormatter.number(kotlin.math.abs(delta))
                            ),
                            onClick = { shiftAll = true }
                        )
                    }
                }

                // What will change, block by block
                if (delta != 0f) {
                    M3Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            affected.sorted().forEach { index ->
                                val from = blocks.getOrNull(index)?.intensityValue ?: return@forEach
                                val to = LoadShift.clamp(from + delta)
                                val weights = oneRepMaxKg?.takeIf { it > 0f }?.let { max ->
                                    " (" + labels.weight(LoadCalculator.weightForPercent(max, from, weightUnit, roundingIncrement)) +
                                        " → " + labels.weight(LoadCalculator.weightForPercent(max, to, weightUnit, roundingIncrement)) + ")"
                                }.orEmpty()
                                Text(
                                    text = stringResource(
                                        R.string.load_preview_line, index + 1,
                                        PrescriptionFormatter.number(from) + "%", PrescriptionFormatter.number(to) + "%"
                                    ) + weights,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                            Text(
                                text = stringResource(R.string.load_done_stay),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                if (canUpdatePlan) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        SectionTitle(stringResource(R.string.load_save_where))
                        ConnectedToggleRow(
                            options = listOf(stringResource(R.string.scope_short_workout), stringResource(R.string.scope_short_plan)),
                            selectedIndex = if (toPlan) 1 else 0,
                            onSelect = { toPlan = it == 1 },
                            stackWhenTextIsLarge = true
                        )
                        Text(
                            text = if (toPlan) {
                                if (programWeek != null) stringResource(R.string.program_edit_scope_plan_week, programWeek)
                                else stringResource(R.string.program_edit_scope_plan_all)
                            } else stringResource(R.string.program_edit_scope_workout_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
            SheetFormFooter(horizontalPadding = ResponsiveSize.cardPadding) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                    GymButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    GymButton(
                        onClick = {
                            onApply(affected, delta, if (toPlan && canUpdatePlan) ProgramEditScope.PLAN else ProgramEditScope.WORKOUT)
                        },
                        enabled = delta != 0f,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.apply).uppercase(), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun ChoiceRow(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    M3Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh,
        border = if (selected) BorderStroke(1.dp, Primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = Primary)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = if (selected) Primary else OnSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}

/**
 * Everything that changed in the prescription of an exercise during its workout, oldest first: what it was, what it
 * became and whether the routine was updated as well.
 */
@Composable
fun ProgramEditHistoryDialog(
    exerciseName: String,
    edits: List<SessionPrescriptionEditEntity>,
    weightUnit: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val time = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.program_edit_history_title), fontWeight = FontWeight.ExtraBold)
                Text(exerciseName, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                edits.forEach { edit ->
                    M3Surface(shape = RoundedCornerShape(20.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                PrescriptionPill(
                                    text = stringResource(
                                        if (edit.editKind == ProgramEditKind.LOAD) R.string.program_edit_kind_load else R.string.program_edit_kind_program
                                    ),
                                    containerColor = Primary.copy(alpha = 0.12f),
                                    contentColor = Primary
                                )
                                PrescriptionPill(
                                    text = if (edit.editScope == ProgramEditScope.PLAN) {
                                        edit.programWeek?.let { stringResource(R.string.program_edit_plan_week, it) }
                                            ?: stringResource(R.string.program_edit_plan_updated)
                                    } else stringResource(R.string.program_edit_scope_workout),
                                    containerColor = Tertiary.copy(alpha = 0.15f),
                                    contentColor = Tertiary
                                )
                            }
                            Text(
                                text = time.format(Date(edit.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                            val lines = programChangeLines(context, weightUnit, edit.before, edit.after)
                            if (lines.isEmpty()) {
                                EditLine(stringResource(R.string.program_edit_after), programSummary(context, weightUnit, edit.after))
                            } else {
                                lines.forEach { line ->
                                    Text(line, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                                }
                                // The whole program is worth showing once its shape changed, not for a moved percentage
                                if (edit.before.size != edit.after.size) {
                                    EditLine(stringResource(R.string.program_edit_full_program), programSummary(context, weightUnit, edit.after))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = Shapes.extraLarge
    )
}

@Composable
private fun EditLine(label: String, text: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.Black)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.Bold)
    }
}

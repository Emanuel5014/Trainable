package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.flow.map

/** Week handling for periodized programs; pass null to [AdvancedPrescriptionEditor] for a single-week context (a session). */
data class WeekEditing(
    val weeksCount: Int,
    /** Called with the new week count (add a week, or grow to fit an imported program). */
    val onWeeksCountChange: (Int) -> Unit,
    val currentWeek: Int? = null,
    /** Weeks in which the exercise is skipped; null hides the "skip this week" switch. */
    val excludedWeeks: Set<Int>? = null,
    val onExcludedWeeksChange: (Set<Int>) -> Unit = {},
    /** Deletes a week (called after the user confirmed); null hides the delete action. */
    val onDeleteWeek: ((Int) -> Unit)? = null,
    val initialWeek: Int = 1,
    /** Weeks up to this number already exist in the routine, so deleting them affects every exercise; later ones are draft-only. */
    val persistedWeeksCount: Int = 0
)

/** Where the 1RM the percentages refer to comes from; the default reads and writes the exercise's own 1RM. */
data class OneRepMaxBinding(
    val kg: Float?,
    val estimatedKg: Float?,
    val onChange: (kg: Float, source: String) -> Unit
)

/**
 * The advanced (%1RM / blocks / techniques) prescription editor shared by every screen that can
 * create such exercises: routine builder, add/swap during a workout, session editing and the
 * AI scan review. Weeks, the 1RM card and the AI program scan are opt-in through the parameters.
 */
@Composable
fun AdvancedPrescriptionEditor(
    blocksByWeek: Map<Int, List<PrescriptionBlock>>,
    onBlocksByWeekChange: (Map<Int, List<PrescriptionBlock>>) -> Unit,
    exerciseId: Int?,
    exerciseName: String?,
    modifier: Modifier = Modifier,
    weeks: WeekEditing? = null,
    oneRepMax: OneRepMaxBinding? = null,
    showAiButton: Boolean = true,
    /** See [AiProgramScanButton]: needed when several editors are on screen at once. */
    aiScanKey: String? = null
) {
    val environment: PrescriptionEnvironmentViewModel = hiltViewModel()
    val weightUnit by environment.weightUnit.collectAsState()
    val roundingIncrement by environment.roundingIncrement.collectAsState()
    val storedMaxes by environment.oneRepMaxes.collectAsState()
    val storedEstimates by environment.estimatedOneRepMaxes.collectAsState()
    val aiAvailable by environment.aiAvailable.collectAsState()

    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val binding = oneRepMax ?: OneRepMaxBinding(
        kg = exerciseId?.let { storedMaxes[it] },
        estimatedKg = exerciseId?.let { storedEstimates[it] },
        onChange = { kg, source -> exerciseId?.let { environment.saveOneRepMax(it, kg, source) } }
    )
    val canEditOneRepMax = oneRepMax != null || exerciseId != null

    val weeksCount = weeks?.weeksCount ?: 1
    var selectedWeek by remember { mutableIntStateOf((weeks?.initialWeek ?: 1).coerceIn(1, weeksCount)) }
    LaunchedEffect(weeksCount) { if (selectedWeek > weeksCount) selectedWeek = weeksCount }
    val week = if (weeks == null) 1 else selectedWeek

    var showOneRepMaxDialog by remember { mutableStateOf(false) }
    var weekToDelete by remember { mutableStateOf<Int?>(null) }

    val excluded = weeks?.excludedWeeks
    val isWeekExcluded = excluded != null && week in excluded
    val weekBlocks = blocksByWeek[week].orEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canEditOneRepMax) {
            OneRepMaxCard(
                oneRepMaxKg = binding.kg,
                estimatedKg = binding.estimatedKg,
                weightUnit = weightUnit,
                onEdit = { showOneRepMaxDialog = true },
                onUseEstimate = { kg ->
                    binding.onChange(LoadCalculatorRounding.roundEstimate(kg), OneRepMaxEntity.SOURCE_ESTIMATED)
                }
            )
        }

        if (weeks != null) {
            WeekSelector(
                weeksCount = weeksCount,
                selectedWeek = selectedWeek,
                onWeekSelected = { selectedWeek = it },
                currentWeek = weeks.currentWeek?.takeIf { weeksCount > 1 },
                dimmedWeeks = excluded.orEmpty(),
                hapticEnabled = hapticEnabled,
                onAddWeek = if (weeksCount < MAX_WEEKS) {
                    {
                        weeks.onWeeksCountChange(weeksCount + 1)
                        selectedWeek = weeksCount + 1
                    }
                } else null
            )

            if (weeksCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (selectedWeek > 1 && !isWeekExcluded) {
                        TextButton(onClick = {
                            val source = PrescriptionResolver.resolve(blocksByWeek, emptySet(), selectedWeek - 1)
                            if (source is ResolvedPrescription.Blocks) {
                                onBlocksByWeekChange(blocksByWeek + (selectedWeek to source.blocks))
                            }
                        }) {
                            Text(
                                text = stringResource(R.string.copy_previous_week, selectedWeek - 1).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Primary
                            )
                        }
                    }
                    if (weeks.onDeleteWeek != null) {
                        TextButton(onClick = { weekToDelete = selectedWeek }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.height(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.delete_week).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Error
                            )
                        }
                    }
                }
                if (excluded != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.skip_this_week),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isWeekExcluded,
                            onCheckedChange = { skip ->
                                weeks.onExcludedWeeksChange(if (skip) excluded + week else excluded - week)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                        )
                    }
                }
            }
        }

        if (isWeekExcluded) {
            PrescriptionPill(text = stringResource(R.string.not_in_week, week))
        } else {
            if (weeks != null && weekBlocks.isEmpty()) {
                val fallback = PrescriptionResolver.resolve(blocksByWeek, emptySet(), week)
                if (fallback is ResolvedPrescription.Blocks && fallback.isFallback) {
                    Text(
                        text = stringResource(R.string.week_repeats, fallback.sourceWeek),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            key(week) {
                PrescriptionBlocksEditor(
                    blocks = weekBlocks,
                    onBlocksChange = { onBlocksByWeekChange(blocksByWeek + (week to it)) },
                    oneRepMaxKg = binding.kg,
                    weightUnit = weightUnit,
                    roundingIncrement = roundingIncrement,
                    hapticEnabled = hapticEnabled,
                    footerActions = if (showAiButton && aiAvailable) {
                        {
                            AiProgramScanButton(
                                exerciseName = exerciseName,
                                singleWeek = weeks == null,
                                currentOneRepMaxKg = binding.kg,
                                weightUnit = weightUnit,
                                modifier = Modifier.weight(1f),
                                scanKey = aiScanKey,
                                onApply = { application ->
                                    val merged = if (weeks == null) {
                                        application.weeks.getValue(1).let { blocksByWeek + (1 to it) }
                                    } else {
                                        blocksByWeek + application.weeks
                                    }
                                    val needed = application.weeks.keys.maxOrNull() ?: 1
                                    if (weeks != null && needed > weeksCount) weeks.onWeeksCountChange(needed.coerceAtMost(MAX_WEEKS))
                                    onBlocksByWeekChange(merged)
                                    application.oneRepMaxKg?.let { binding.onChange(it, OneRepMaxEntity.SOURCE_MANUAL) }
                                }
                            )
                        }
                    } else null
                )
            }
        }
    }

    if (showOneRepMaxDialog) {
        OneRepMaxDialog(
            title = stringResource(R.string.edit_one_rep_max),
            initialKg = binding.kg,
            weightUnit = weightUnit,
            onDismiss = { showOneRepMaxDialog = false },
            onConfirm = { kg, source ->
                binding.onChange(kg, source)
                showOneRepMaxDialog = false
            }
        )
    }

    weekToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { weekToDelete = null },
            title = { Text(stringResource(R.string.delete_week_title, target), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = {
                Text(
                    stringResource(if ((weeks?.persistedWeeksCount ?: 0) < target) R.string.delete_week_message_local else R.string.delete_week_message),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                GymButton(
                    onClick = {
                        weekToDelete = null
                        weeks?.onDeleteWeek?.invoke(target)
                    },
                    containerColor = Error.copy(alpha = 0.12f),
                    contentColor = Error,
                    height = 48
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { weekToDelete = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    height = 48
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = Shapes.extraLarge
        )
    }
}

private const val MAX_WEEKS = 52

private object LoadCalculatorRounding {
    /** An estimate is shown to the kg; a stored 1RM lands on a real plate increment. */
    fun roundEstimate(kg: Float): Float =
        com.emanuel5014.trainable.domain.prescription.LoadCalculator.roundTo(kg, 0.5f)
}

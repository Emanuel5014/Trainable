package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.add
import com.emanuel5014.trainable.sharedui.resources.add_block
import com.emanuel5014.trainable.sharedui.resources.add_block_short
import com.emanuel5014.trainable.sharedui.resources.added_load
import com.emanuel5014.trainable.sharedui.resources.block_note
import com.emanuel5014.trainable.sharedui.resources.block_rest
import com.emanuel5014.trainable.sharedui.resources.custom_technique_hint
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.duplicate
import com.emanuel5014.trainable.sharedui.resources.intensity
import com.emanuel5014.trainable.sharedui.resources.intensity_bodyweight
import com.emanuel5014.trainable.sharedui.resources.intensity_none
import com.emanuel5014.trainable.sharedui.resources.intensity_percent
import com.emanuel5014.trainable.sharedui.resources.intensity_rpe
import com.emanuel5014.trainable.sharedui.resources.intensity_weight
import com.emanuel5014.trainable.sharedui.resources.move_down
import com.emanuel5014.trainable.sharedui.resources.move_up
import com.emanuel5014.trainable.sharedui.resources.one_rep_max_missing
import com.emanuel5014.trainable.sharedui.resources.rep_mode_amrap
import com.emanuel5014.trainable.sharedui.resources.rep_mode_fixed
import com.emanuel5014.trainable.sharedui.resources.rep_mode_total
import com.emanuel5014.trainable.sharedui.resources.reps
import com.emanuel5014.trainable.sharedui.resources.reps_hint
import com.emanuel5014.trainable.sharedui.resources.same_as_exercise
import com.emanuel5014.trainable.sharedui.resources.sets
import com.emanuel5014.trainable.sharedui.resources.technique_bands
import com.emanuel5014.trainable.sharedui.resources.technique_chains
import com.emanuel5014.trainable.sharedui.resources.technique_competition
import com.emanuel5014.trainable.sharedui.resources.technique_custom
import com.emanuel5014.trainable.sharedui.resources.technique_deficit
import com.emanuel5014.trainable.sharedui.resources.technique_emom
import com.emanuel5014.trainable.sharedui.resources.technique_feet_up
import com.emanuel5014.trainable.sharedui.resources.technique_pause
import com.emanuel5014.trainable.sharedui.resources.technique_pause_seconds
import com.emanuel5014.trainable.sharedui.resources.technique_tempo
import com.emanuel5014.trainable.sharedui.resources.technique_tempo_value
import com.emanuel5014.trainable.sharedui.resources.techniques
import com.emanuel5014.trainable.sharedui.resources.tempo_hint
import com.emanuel5014.trainable.sharedui.resources.volume
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.PrescriptionNotationParser
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter

private val BLOCK_REST_OPTIONS = listOf(60, 90, 120, 150, 180, 240, 300)

/** Human-readable planned load for a block ("172.5 kg", "RPE 8", "BW +5 kg"), null when free. */
fun blockLoadLabel(
    block: PrescriptionBlock,
    labels: PrescriptionLabels,
    oneRepMaxKg: Float?,
    weightUnit: String,
    roundingIncrement: Float
): String? = when (block.intensityType) {
    IntensityType.PERCENT -> {
        val percent = block.intensityValue
        if (percent != null && oneRepMaxKg != null && oneRepMaxKg > 0f) {
            labels.weight(LoadCalculator.weightForPercent(oneRepMaxKg, percent, weightUnit, roundingIncrement))
        } else null
    }
    IntensityType.NONE -> null
    else -> PrescriptionFormatter.intensity(block, labels)
}

/**
 * Editor for an advanced prescription: an ordered list of expandable block cards
 * (intensity · volume · techniques · rest). [footerActions] sit next to "Add block" (e.g. the AI scan button).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrescriptionBlocksEditor(
    blocks: List<PrescriptionBlock>,
    onBlocksChange: (List<PrescriptionBlock>) -> Unit,
    oneRepMaxKg: Float?,
    weightUnit: String,
    roundingIncrement: Float,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    footerActions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    val labels = rememberPrescriptionLabels(weightUnit)
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    fun update(transform: (MutableList<PrescriptionBlock>) -> Unit) {
        onBlocksChange(blocks.toMutableList().also(transform))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEachIndexed { index, block ->
            PrescriptionBlockCard(
                index = index,
                block = block,
                expanded = expandedIndex == index,
                labels = labels,
                oneRepMaxKg = oneRepMaxKg,
                weightUnit = weightUnit,
                roundingIncrement = roundingIncrement,
                hapticEnabled = hapticEnabled,
                isFirst = index == 0,
                isLast = index == blocks.lastIndex,
                onToggle = { expandedIndex = if (expandedIndex == index) null else index },
                onChange = { updated -> update { it[index] = updated } },
                onMoveUp = {
                    update { list -> list.add(index - 1, list.removeAt(index)) }
                    expandedIndex = index - 1
                },
                onMoveDown = {
                    update { list -> list.add(index + 1, list.removeAt(index)) }
                    expandedIndex = index + 1
                },
                onDuplicate = {
                    update { it.add(index + 1, block) }
                    expandedIndex = index + 1
                },
                onDelete = {
                    update { it.removeAt(index) }
                    expandedIndex = null
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymButton(
                onClick = {
                    val last = blocks.lastOrNull()
                    val next = when {
                        last == null -> PrescriptionBlock(sets = 3, reps = "5", intensityType = IntensityType.PERCENT, intensityValue = 70f)
                        last.intensityType == IntensityType.PERCENT ->
                            last.copy(intensityValue = ((last.intensityValue ?: 70f) + 5f).coerceAtMost(100f), techniques = last.techniques)
                        else -> last
                    }
                    update { it.add(next) }
                    expandedIndex = blocks.size
                },
                modifier = Modifier.weight(1f),
                containerColor = Primary.copy(alpha = 0.12f),
                contentColor = Primary,
                height = 48
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (footerActions != null) Res.string.add_block_short else Res.string.add_block).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            footerActions?.invoke(this)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrescriptionBlockCard(
    index: Int,
    block: PrescriptionBlock,
    expanded: Boolean,
    labels: PrescriptionLabels,
    oneRepMaxKg: Float?,
    weightUnit: String,
    roundingIncrement: Float,
    hapticEnabled: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onChange: (PrescriptionBlock) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val prescriptionText = LocalPrescriptionText.current
    val haptic = LocalHapticFeedback.current
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "block_chevron")
    val load = blockLoadLabel(block, labels, oneRepMaxKg, weightUnit, roundingIncrement)
    val tick = { if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Surface(
        shape = Shapes.medium,
        color = if (expanded) SurfaceContainerHighest else SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = PrescriptionFormatter.headline(block, labels),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (block.techniques.isNotEmpty()) {
                        Text(
                            text = block.techniques.joinToString(" · ") { prescriptionText.technique(it) },
                            style = MaterialTheme.typography.labelSmall,
                            color = Tertiary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (block.intensityType == IntensityType.PERCENT && load != null) {
                    Text(load, style = MaterialTheme.typography.titleSmall, color = Primary, fontWeight = FontWeight.ExtraBold)
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).rotate(chevronRotation)
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Intensity
                    SectionLabel(stringResource(Res.string.intensity))
                    val intensityOptions = listOf(
                        IntensityType.PERCENT to Res.string.intensity_percent,
                        IntensityType.RPE to Res.string.intensity_rpe,
                        IntensityType.WEIGHT to Res.string.intensity_weight,
                        IntensityType.BODYWEIGHT to Res.string.intensity_bodyweight,
                        IntensityType.NONE to Res.string.intensity_none
                    )
                    ConnectedToggleRow(
                        options = intensityOptions.map { stringResource(it.second) },
                        selectedIndex = intensityOptions.indexOfFirst { it.first == block.intensityType },
                        onSelect = { i ->
                            tick()
                            val type = intensityOptions[i].first
                            val value = when (type) {
                                block.intensityType -> block.intensityValue
                                IntensityType.PERCENT -> 70f
                                IntensityType.RPE -> 8f
                                IntensityType.WEIGHT -> oneRepMaxKg?.let { LoadCalculator.weightForPercent(it, 70f, weightUnit, roundingIncrement) } ?: 0f
                                IntensityType.BODYWEIGHT -> 0f
                                IntensityType.NONE -> null
                            }
                            onChange(block.copy(intensityType = type, intensityValue = value))
                        }
                    )
                    when (block.intensityType) {
                        IntensityType.PERCENT -> Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberStepper(
                                value = block.intensityValue ?: 70f,
                                step = 2.5f,
                                range = 5f..120f,
                                label = stringResource(Res.string.intensity_percent),
                                suffix = "%",
                                onValueChange = { onChange(block.copy(intensityValue = it)) },
                                onTick = tick,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = load?.let { "= $it" } ?: stringResource(Res.string.one_rep_max_missing),
                                style = if (load != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                                color = if (load != null) Primary else OnSurfaceVariant,
                                fontWeight = if (load != null) FontWeight.ExtraBold else FontWeight.Normal,
                                modifier = if (load != null) Modifier else Modifier.width(96.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        IntensityType.RPE -> NumberStepper(
                            value = block.intensityValue ?: 8f,
                            step = 0.5f,
                            range = 5f..10f,
                            label = stringResource(Res.string.intensity_rpe),
                            onValueChange = { onChange(block.copy(intensityValue = it)) },
                            onTick = tick
                        )
                        IntensityType.WEIGHT, IntensityType.BODYWEIGHT -> NumberStepper(
                            value = WeightUnitConverter.convertDisplay(block.intensityValue ?: 0f, weightUnit),
                            step = roundingIncrement,
                            range = 0f..1000f,
                            label = if (block.intensityType == IntensityType.BODYWEIGHT) {
                                "${stringResource(Res.string.added_load)} ($weightUnit)"
                            } else "${stringResource(Res.string.intensity_weight)} ($weightUnit)",
                            onValueChange = { onChange(block.copy(intensityValue = WeightUnitConverter.convertStorage(it, weightUnit))) },
                            onTick = tick
                        )
                        IntensityType.NONE -> Unit
                    }

                    // Volume
                    SectionLabel(stringResource(Res.string.volume))
                    val modeOptions = listOf(
                        RepMode.FIXED to Res.string.rep_mode_fixed,
                        RepMode.AMRAP to Res.string.rep_mode_amrap,
                        RepMode.TOTAL to Res.string.rep_mode_total
                    )
                    ConnectedToggleRow(
                        options = modeOptions.map { stringResource(it.second) },
                        selectedIndex = modeOptions.indexOfFirst { it.first == block.repMode },
                        onSelect = { i ->
                            tick()
                            val mode = modeOptions[i].first
                            onChange(
                                when (mode) {
                                    RepMode.FIXED -> block.copy(repMode = mode, reps = block.reps.ifBlank { "5" }, totalReps = null)
                                    RepMode.AMRAP -> block.copy(repMode = mode, reps = "", totalReps = null)
                                    RepMode.TOTAL -> block.copy(repMode = mode, sets = 1, reps = "", totalReps = block.totalReps ?: 20)
                                }
                            )
                        }
                    )
                    when (block.repMode) {
                        RepMode.FIXED -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IntTextField(
                                value = block.sets,
                                label = stringResource(Res.string.sets),
                                onValueChange = { onChange(block.copy(sets = it.coerceIn(1, 30))) },
                                modifier = Modifier.weight(1f)
                            )
                            var repsText by remember(index) { mutableStateOf(block.reps) }
                            GymInputField(
                                value = repsText,
                                onValueChange = { raw ->
                                    val cleaned = raw.filter { it.isDigit() || it == '-' }
                                    repsText = cleaned
                                    val list = cleaned.split("-").mapNotNull { it.trim().toIntOrNull() }
                                    onChange(block.copy(reps = cleaned, sets = if (list.size > 1) list.size else block.sets))
                                },
                                label = stringResource(Res.string.reps),
                                supportingText = stringResource(Res.string.reps_hint),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        RepMode.AMRAP -> IntTextField(
                            value = block.sets,
                            label = stringResource(Res.string.sets),
                            onValueChange = { onChange(block.copy(sets = it.coerceIn(1, 30))) }
                        )
                        RepMode.TOTAL -> IntTextField(
                            value = block.totalReps ?: 20,
                            label = stringResource(Res.string.rep_mode_total),
                            onValueChange = { onChange(block.copy(totalReps = it.coerceIn(1, 300))) }
                        )
                    }

                    // Techniques
                    SectionLabel(stringResource(Res.string.techniques))
                    TechniquesPicker(
                        techniques = block.techniques,
                        onChange = { tick(); onChange(block.copy(techniques = it)) },
                        onTick = tick
                    )

                    // Rest override
                    SectionLabel(stringResource(Res.string.block_rest))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SelectableChip(
                            label = stringResource(Res.string.same_as_exercise),
                            selected = block.restSeconds == null,
                            onClick = { tick(); onChange(block.copy(restSeconds = null)) }
                        )
                        BLOCK_REST_OPTIONS.forEach { seconds ->
                            SelectableChip(
                                label = formatRestTime(seconds),
                                selected = block.restSeconds == seconds,
                                onClick = { tick(); onChange(block.copy(restSeconds = seconds)) }
                            )
                        }
                    }

                    var noteText by remember(index) { mutableStateOf(block.note.orEmpty()) }
                    GymInputField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            onChange(block.copy(note = it.ifBlank { null }))
                        },
                        label = stringResource(Res.string.block_note),
                        singleLine = false,
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onMoveUp, enabled = !isFirst) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(Res.string.move_up))
                        }
                        IconButton(onClick = onMoveDown, enabled = !isLast) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(Res.string.move_down))
                        }
                        IconButton(onClick = onDuplicate) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(Res.string.duplicate))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(Res.string.delete), tint = Error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechniquesPicker(
    techniques: List<Technique>,
    onChange: (List<Technique>) -> Unit,
    onTick: () -> Unit
) {
    val pause = techniques.filterIsInstance<Technique.Pause>().firstOrNull()
    val tempo = techniques.filterIsInstance<Technique.Tempo>().firstOrNull()
    val customs = techniques.filterIsInstance<Technique.Custom>()
    var showCustomField by remember { mutableStateOf(false) }

    fun toggle(technique: Technique, present: Boolean) {
        onChange(if (present) techniques.filterNot { it::class == technique::class } else techniques + technique)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SelectableChip(
                label = pause?.let { stringResource(Res.string.technique_pause_seconds, it.seconds) } ?: stringResource(Res.string.technique_pause),
                selected = pause != null,
                onClick = { toggle(Technique.Pause(2), pause != null) }
            )
            listOf(
                Technique.Chains to Res.string.technique_chains,
                Technique.Bands to Res.string.technique_bands,
                Technique.FeetUp to Res.string.technique_feet_up,
                Technique.Competition to Res.string.technique_competition,
                Technique.Deficit to Res.string.technique_deficit,
                Technique.Emom to Res.string.technique_emom
            ).forEach { (technique, label) ->
                val present = technique in techniques
                SelectableChip(label = stringResource(label), selected = present, onClick = { toggle(technique, present) })
            }
            SelectableChip(
                label = tempo?.let { stringResource(Res.string.technique_tempo_value, it.pattern) } ?: stringResource(Res.string.technique_tempo),
                selected = tempo != null,
                onClick = { toggle(Technique.Tempo("3-1-1"), tempo != null) }
            )
            customs.forEach { custom ->
                InputChip(
                    selected = true,
                    onClick = { onChange(techniques - custom) },
                    label = { Text(custom.text) },
                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(InputChipDefaults.IconSize)) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Tertiary.copy(alpha = 0.15f),
                        selectedLabelColor = Tertiary,
                        selectedTrailingIconColor = Tertiary
                    )
                )
            }
            SelectableChip(
                label = "+ " + stringResource(Res.string.technique_custom),
                selected = showCustomField,
                onClick = { showCustomField = !showCustomField }
            )
        }

        if (pause != null) {
            NumberStepper(
                value = pause.seconds.toFloat(),
                step = 1f,
                range = 1f..10f,
                label = stringResource(Res.string.technique_pause),
                suffix = "″",
                onValueChange = { seconds ->
                    onChange(techniques.map { if (it is Technique.Pause) Technique.Pause(seconds.toInt()) else it })
                },
                onTick = onTick
            )
        }
        if (tempo != null) {
            var tempoText by remember { mutableStateOf(tempo.pattern) }
            GymInputField(
                value = tempoText,
                onValueChange = { raw ->
                    tempoText = raw.uppercase().filter { it.isDigit() || it == '-' || it == 'X' }
                    if (tempoText.isNotBlank()) {
                        onChange(techniques.map { if (it is Technique.Tempo) Technique.Tempo(tempoText) else it })
                    }
                },
                label = stringResource(Res.string.technique_tempo),
                supportingText = stringResource(Res.string.tempo_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }
        if (showCustomField) {
            var customText by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GymInputField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = stringResource(Res.string.technique_custom),
                    placeholder = stringResource(Res.string.custom_technique_hint),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = {
                        val text = customText.trim()
                        if (text.isNotEmpty()) {
                            onChange(techniques + Technique.Custom(text))
                            customText = ""
                            showCustomField = false
                        }
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Primary, contentColor = OnPrimary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.15f),
            selectedLabelColor = Primary
        )
    )
}

/** Connected expressive toggle group (single choice). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedToggleRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** With very large text the options go one under the other instead of being cut short on one line. */
    stackWhenTextIsLarge: Boolean = false
) {
    if (stackWhenTextIsLarge && LocalDensity.current.fontScale >= LARGE_TEXT_SCALE) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { index, label ->
                ToggleButton(
                    checked = index == selectedIndex,
                    onCheckedChange = { if (index != selectedIndex) onSelect(index) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant,
                        checkedContainerColor = Primary,
                        checkedContentColor = OnPrimary
                    )
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = { if (index != selectedIndex) onSelect(index) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant,
                    checkedContainerColor = Primary,
                    checkedContentColor = OnPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** − [ value ] + with a typed field in the middle for exact values. */
@Composable
fun NumberStepper(
    value: Float,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String = "",
    onTick: () -> Unit = {}
) {
    var text by remember { mutableStateOf(PrescriptionFormatter.number(value)) }
    LaunchedEffect(value) {
        if (text.replace(',', '.').toFloatOrNull() != value) text = PrescriptionFormatter.number(value)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = {
                onTick()
                onValueChange(LoadCalculator.roundTo(value - step, step).coerceIn(range.start, range.endInclusive))
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceContainerHigh, contentColor = Primary)
        ) { Icon(Icons.Rounded.Remove, contentDescription = null) }
        GymInputField(
            value = text,
            onValueChange = { raw ->
                text = raw.filter { it.isDigit() || it == '.' || it == ',' }
                text.replace(',', '.').toFloatOrNull()?.let { onValueChange(it.coerceIn(range.start, range.endInclusive)) }
            },
            label = if (suffix.isNotEmpty() && !label.contains(suffix)) "$label ($suffix)" else label,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            textAlign = TextAlign.Center
        )
        FilledTonalIconButton(
            onClick = {
                onTick()
                onValueChange(LoadCalculator.roundTo(value + step, step).coerceIn(range.start, range.endInclusive))
            },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SurfaceContainerHigh, contentColor = Primary)
        ) { Icon(Icons.Rounded.Add, contentDescription = null) }
    }
}

@Composable
private fun IntTextField(
    value: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }
    GymInputField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() }.take(3)
            text.toIntOrNull()?.let(onValueChange)
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

private const val LARGE_TEXT_SCALE = 1.4f

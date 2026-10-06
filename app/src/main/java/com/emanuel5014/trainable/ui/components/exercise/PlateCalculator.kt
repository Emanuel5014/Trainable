package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt

// Plate colours follow the competition standard (red, blue, yellow, green, white, black, silver)
private val PlatePalette = listOf(
    Color(0xFFD32F2F),
    Color(0xFF1976D2),
    Color(0xFFF9A825),
    Color(0xFF388E3C),
    Color(0xFFF5F5F5),
    Color(0xFF37474F),
    Color(0xFF9E9E9E),
    Color(0xFF7B1FA2),
    Color(0xFF00897B)
)
private val SteelColor = Color(0xFF5F5F66)
private val SteelTipColor = Color(0xFFB4B4BC)

private fun plateColor(plate: Float, unit: String): Color {
    val choices = PlateCalculator.plateChoices(unit)
    val index = choices.indexOfFirst { abs(it - plate) < 0.001f }
    return PlatePalette[(if (index >= 0) index else 6).coerceAtMost(PlatePalette.lastIndex)]
}

/** Heavier plates are taller and thicker, but never so small that they cannot be tapped. */
private fun plateHeight(plate: Float, heaviest: Float): Dp =
    (28f + 52f * sqrt((plate / heaviest).coerceIn(0f, 1f))).dp

private fun plateThickness(plate: Float, heaviest: Float): Dp =
    (6f + 12f * sqrt((plate / heaviest).coerceIn(0f, 1f))).dp

private fun format(value: Float): String = WeightUnitConverter.format(value)

private fun displayOf(kg: Float, unit: String): Float =
    round(WeightUnitConverter.convertDisplay(kg, unit) * 100f) / 100f

/** Weight of the bar in the display unit, falling back to the usual Olympic bar. */
private fun barDisplay(barKg: Float?, unit: String): Float =
    barKg?.let { displayOf(it, unit) } ?: PlateCalculator.defaultBar(unit)

/** "2×20 + 5 + 1,25": identical plates are grouped, heaviest first. */
private fun groupPlates(plates: List<Float>): String =
    plates.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.key }
        .joinToString(" + ") { (plate, count) -> if (count > 1) "$count×${format(plate)}" else format(plate) }

/**
 * One line that says what to load on each side of the bar for [weightKg], e.g. "2×20 + 5 per side · bar 20 kg".
 * Tapping it opens the calculator.
 */
@Composable
fun PlateChip(
    weightKg: Float,
    barKg: Float?,
    weightUnit: String,
    plates: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bar = barDisplay(barKg, weightUnit)
    val total = displayOf(weightKg, weightUnit)
    val result = remember(total, bar, plates) { PlateCalculator.solve(total, bar, plates) }
    val barText = "${format(bar)} $weightUnit"
    val loaded = groupPlates(result.platesPerSide)
    val text = when {
        result.belowBar -> stringResource(R.string.plate_chip_below_bar, barText)
        result.isExact && result.platesPerSide.isEmpty() -> stringResource(R.string.plate_chip_bar_only, barText)
        // The usual bar is taken for granted; any other bar is worth a mention
        result.isExact && abs(bar - PlateCalculator.defaultBar(weightUnit)) < 0.01f ->
            stringResource(R.string.plate_chip_per_side_plain, loaded)
        result.isExact -> stringResource(R.string.plate_chip_per_side, loaded, barText)
        else -> stringResource(R.string.plate_chip_short, loaded.ifEmpty { "–" }, "${format(result.missing)} $weightUnit")
    }
    val short = !result.belowBar && !result.isExact
    val accent = when {
        short -> Error
        result.belowBar -> OnSurfaceVariant
        else -> Primary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (result.belowBar) SurfaceContainerHigh else accent.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                maxLines = if (short) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Shown where the weight is edited for an exercise that does not use the calculator yet. */
@Composable
fun PlateCalculatorEnableChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.plate_calculator_use),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Bottom sheet that works out the plates for a weight and lets the lifter change them. Everything shown is in
 * [weightUnit]; only [onApply] and [onBarChange] hand back kg, the unit weights are stored in.
 *
 * @param plates the plates the gym has, in [weightUnit].
 * @param onApply sets the weight of the set (kg) to what is on the bar.
 * @param onBarChange remembers the weight of the bar (kg) for this exercise.
 * @param onDisable turns the calculator off for this exercise.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorSheet(
    exerciseName: String,
    weightKg: Float,
    barKg: Float?,
    weightUnit: String,
    plates: List<Float>,
    onApply: (Float) -> Unit,
    onBarChange: (Float) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    var bar by remember { mutableFloatStateOf(barDisplay(barKg, weightUnit)) }
    var total by remember { mutableFloatStateOf(displayOf(weightKg, weightUnit)) }
    // null while the plates are the suggested ones, a list once a plate is added or removed by hand
    var manual by remember { mutableStateOf<List<Float>?>(null) }
    var showBarDialog by remember { mutableStateOf(false) }

    val suggested = remember(total, bar, plates) { PlateCalculator.solve(total, bar, plates) }
    val shown = manual ?: suggested.platesPerSide
    val loadedTotal = PlateCalculator.total(bar, shown)
    val step = (plates.minOrNull() ?: 1f).times(2f).coerceAtLeast(0.5f)
    val maxTotal = if (weightUnit == "lb") 2200f else 1000f
    val presets = PlateCalculator.barPresets(weightUnit)

    fun editByHand(updated: List<Float>) {
        manual = updated
        total = PlateCalculator.total(bar, updated)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ResponsiveSize.cardPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.plate_calculator).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                NumberStepper(
                    value = total,
                    step = step,
                    range = 0f..maxTotal,
                    label = stringResource(R.string.plate_total_weight),
                    onValueChange = {
                        total = it
                        manual = null
                    },
                    suffix = weightUnit
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetLabel(stringResource(R.string.plate_bar))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val custom = presets.none { abs(it - bar) < 0.001f }
                        presets.forEach { preset ->
                            SheetChip(
                                label = "${format(preset)} $weightUnit",
                                selected = abs(preset - bar) < 0.001f,
                                onClick = {
                                    bar = preset
                                    manual = null
                                    onBarChange(WeightUnitConverter.convertStorage(preset, weightUnit))
                                }
                            )
                        }
                        SheetChip(
                            label = if (custom) "${format(bar)} $weightUnit" else stringResource(R.string.plate_bar_other),
                            selected = custom,
                            onClick = { showBarDialog = true }
                        )
                    }
                }

                PlateBarbell(
                    plates = shown,
                    weightUnit = weightUnit,
                    onRemove = { index -> editByHand(shown.filterIndexed { i, _ -> i != index }) }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (manual == null && suggested.belowBar) {
                        Text(
                            text = stringResource(R.string.plate_lighter_than_bar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val perSide = (loadedTotal - bar) / 2f
                        Text(
                            text = stringResource(R.string.plate_per_side, "${format(perSide)} $weightUnit"),
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (shown.isEmpty()) stringResource(R.string.plate_no_plates) else groupPlates(shown),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    if (manual == null && !suggested.belowBar && !suggested.isExact) {
                        Text(
                            text = stringResource(R.string.plate_cannot_load, "${format(suggested.missing)} $weightUnit"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (manual != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.plate_edited),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                            TextButton(onClick = { manual = null }) {
                                Text(stringResource(R.string.plate_reset), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SheetLabel(stringResource(R.string.plate_add_hint))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plates.forEach { plate ->
                            PlateAddChip(
                                plate = plate,
                                weightUnit = weightUnit,
                                onClick = { editByHand((shown + plate).sortedDescending()) }
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.plate_tap_to_remove),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            // Pinned under the scrolling part so it is always in reach, even on a short screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ResponsiveSize.cardPadding)
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GymButton(
                    onClick = {
                        onApply(WeightUnitConverter.convertStorage(loadedTotal, weightUnit))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.plate_apply, "${format(loadedTotal)} $weightUnit").uppercase(),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                TextButton(
                    onClick = {
                        onDisable()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.plate_turn_off),
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showBarDialog) {
        BarWeightDialog(
            initial = bar,
            weightUnit = weightUnit,
            onConfirm = { value ->
                bar = value
                manual = null
                onBarChange(WeightUnitConverter.convertStorage(value, weightUnit))
                showBarDialog = false
            },
            onDismiss = { showBarDialog = false }
        )
    }
}

@Composable
private fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
    )
}

@Composable
private fun SheetChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun PlateAddChip(plate: Float, weightUnit: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(plateColor(plate, weightUnit))
                    .border(BorderStroke(1.dp, OutlineVariant), CircleShape)
            )
            Text(
                text = "+ ${format(plate)}",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** One side of the bar: the plates heaviest first against the collar, on a bar that runs on. A tap removes a plate. */
@Composable
private fun PlateBarbell(
    plates: List<Float>,
    weightUnit: String,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val heaviest = PlateCalculator.plateChoices(weightUnit).first()
    val removeLabel = stringResource(R.string.plate_remove)
    val lead = 20.dp
    val stop = 8.dp

    Surface(shape = Shapes.medium, color = SurfaceContainer, modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val slot = if (plates.isEmpty()) 0.dp else ((maxWidth - lead - stop - 16.dp) / plates.size).coerceIn(14.dp, 30.dp)
            val showLabels = slot >= 24.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .drawBehind {
                            val thickness = 10.dp.toPx()
                            drawRect(
                                color = SteelColor,
                                topLeft = Offset(0f, (size.height - thickness) / 2f),
                                size = Size(size.width, thickness)
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(lead))
                    Box(
                        modifier = Modifier
                            .width(stop)
                            .height(44.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SteelTipColor)
                    )
                    plates.forEachIndexed { index, plate ->
                        Box(
                            modifier = Modifier
                                .width(slot)
                                .height(84.dp)
                                .clickable(role = Role.Button, onClickLabel = removeLabel) { onRemove(index) }
                                .semantics { contentDescription = "${format(plate)} $weightUnit" },
                            contentAlignment = Alignment.Center
                        ) {
                            val shape = RoundedCornerShape(3.dp)
                            Box(
                                modifier = Modifier
                                    .width(minOf(plateThickness(plate, heaviest), slot - 2.dp))
                                    .height(plateHeight(plate, heaviest))
                                    .clip(shape)
                                    .background(plateColor(plate, weightUnit))
                                    .border(BorderStroke(1.dp, OutlineVariant), shape)
                            )
                        }
                    }
                }
                if (showLabels) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Spacer(modifier = Modifier.width(lead + stop))
                        plates.forEach { plate ->
                            Text(
                                text = format(plate),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.width(slot)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarWeightDialog(
    initial: Float,
    weightUnit: String,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val value = text.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f && it <= 500f }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = { Text(stringResource(R.string.plate_bar_dialog_title)) },
        text = {
            GymInputField(
                value = text,
                onValueChange = { raw -> text = raw.filter { it.isDigit() || it == '.' || it == ',' } },
                label = stringResource(R.string.weight_label_unit, weightUnit),
                placeholder = format(initial),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

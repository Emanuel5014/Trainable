package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import com.emanuel5014.trainable.ui.components.motion.WheelLens
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun WeightRepsInput(
    weight: Float,
    reps: Int,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    weightUnit: String = "kg"
) {
    var showCustomWeightDialog by remember { mutableStateOf(false) }
    var customWeightText by remember { mutableStateOf("") }
    var showCustomRepsDialog by remember { mutableStateOf(false) }
    var customRepsText by remember { mutableStateOf("") }

    val displayWeight = remember(weight, weightUnit) {
        val converted = WeightUnitConverter.convertDisplay(weight, weightUnit)
        (kotlin.math.round(converted * 100) / 100f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val baseWeightRange = remember(weightUnit) {
                if (weightUnit == "lb") (0..2000).map { it * 1f } else (0..1000).map { it * 0.5f }
            }
            val weightRange = remember(displayWeight, baseWeightRange) {
                val isInRange = baseWeightRange.any { kotlin.math.abs(it - displayWeight) < 0.001f }
                if (!isInRange && displayWeight > 0) {
                    (baseWeightRange + displayWeight).sorted()
                } else {
                    baseWeightRange
                }
            }

            WheelPickerBox(
                label = stringResource(R.string.weight_label_unit, weightUnit.uppercase()),
                value = displayWeight,
                range = weightRange,
                onValueChange = { 
                    val kgWeight = WeightUnitConverter.convertStorage(it, weightUnit)
                    onWeightChange(kgWeight) 
                },
                onRequestCustom = { customWeightText = ""; showCustomWeightDialog = true },
                format = { WeightUnitConverter.format(it) }
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            WheelPickerBox(
                label = stringResource(R.string.reps),
                value = reps.toFloat(),
                range = (0..100).map { it.toFloat() },
                onValueChange = { onRepsChange(it.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                onRequestCustom = { customRepsText = ""; showCustomRepsDialog = true },
                format = { it.toInt().toString() }
            )
        }
    }

    if (showCustomWeightDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showCustomWeightDialog = false },
            title = { Text(stringResource(R.string.custom_weight), color = OnSurface) },
            text = {
                OutlinedTextField(
                    value = customWeightText,
                    onValueChange = { customWeightText = it.replace(',', '.') },
                    label = { Text(stringResource(R.string.weight_label_unit, weightUnit)) },
                    placeholder = { Text(WeightUnitConverter.format(displayWeight), color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customWeightText.toFloatOrNull()?.let { 
                            val kgWeight = WeightUnitConverter.convertStorage(it, weightUnit)
                            onWeightChange(kgWeight) 
                        }
                        showCustomWeightDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWeightDialog = false }) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh
        )
    }

    if (showCustomRepsDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showCustomRepsDialog = false },
            title = { Text(stringResource(R.string.custom_reps), color = OnSurface) },
            text = {
                OutlinedTextField(
                    value = customRepsText,
                    onValueChange = { customRepsText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.reps)) },
                    placeholder = { Text(reps.toString(), color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customRepsText.toIntOrNull()?.let { 
                            onRepsChange(it) 
                        }
                        showCustomRepsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRepsDialog = false }) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun <T> WheelPickerBox(
    label: String,
    value: T,
    range: List<T>,
    onValueChange: (T) -> Unit,
    format: (T) -> String,
    modifier: Modifier = Modifier,
    /** Opens the dialog to type a value the wheel does not offer: from the pencil and from a tap on the chosen value. */
    onRequestCustom: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val currentIndex = remember(range, value) {
        range.indexOfFirst { areEqual(it, value) }.coerceAtLeast(0)
    }

    val listState = key(range) {
        rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value, listState) {
        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centeredIndex = layoutInfo.visibleItemsInfo.minByOrNull {
            kotlin.math.abs(it.offset + it.size / 2 - viewportCenter)
        }?.index ?: -1
        val targetIndex = range.indexOfFirst { areEqual(it, value) }
        if (targetIndex != -1 && targetIndex != centeredIndex && !listState.isScrollInProgress) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs(it.offset + it.size / 2 - viewportCenter)
            }?.index
        }
            .debounce(50L)
            .distinctUntilChanged()
            .collect { index ->
                if (index != null && index in range.indices) {
                    val newValue = range[index]
                    if (!areEqual(newValue, value)) {
                        currentOnValueChange(newValue)
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
    }

    // A little bounce of the lens each time a value settles
    val bounce = remember { Animatable(1f) }
    var firstValue by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (!firstValue) {
            bounce.snapTo(1.04f)
            bounce.animateTo(1f, WorkoutMotion.spatial())
        }
        firstValue = false
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onRequestCustom, modifier = Modifier.size(48.dp).testTag("wheel_edit")) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.custom),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    }
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            )

            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 36.dp)
            ) {
                items(
                    count = range.size,
                    key = { index -> range[index].toString() }
                ) { index ->
                    val item = range[index]
                    val isSelected = currentIndex == index

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                // The chosen value opens the field to type one; any other one comes to the centre
                                if (isSelected) {
                                    onRequestCustom()
                                } else {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                }
                            }
                            .graphicsLayer {
                                // The rows shrink and fade the further they are from the centre line
                                val info = listState.layoutInfo
                                val row = info.visibleItemsInfo.firstOrNull { it.index == index }
                                val distance = if (row == null || row.size == 0) WHEEL_FAR_ROWS else {
                                    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                    abs(row.offset + row.size / 2f - centre) / row.size
                                }
                                val rowScale = WheelLens.scale(distance)
                                scaleX = rowScale
                                scaleY = rowScale
                                alpha = WheelLens.alpha(distance)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format(item),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isSelected) Primary else OnSurface,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

/** Distance, in rows, past which a row is as small and faint as the lens makes it. */
private const val WHEEL_FAR_ROWS = 2f

private fun <T> areEqual(a: T, b: T): Boolean =
    if (a is Float && b is Float) kotlin.math.abs(a - b) < 0.001f else a == b

@OptIn(FlowPreview::class)
@Composable
fun WeightTimeInput(
    weight: Float,
    seconds: Int,
    onWeightChange: (Float) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    weightUnit: String = "kg"
) {
    var showCustomWeightDialog by remember { mutableStateOf(false) }
    var customWeightText by remember { mutableStateOf("") }
    var showCustomSecondsDialog by remember { mutableStateOf(false) }
    var customSecondsText by remember { mutableStateOf("") }

    val displayWeight = remember(weight, weightUnit) {
        val converted = WeightUnitConverter.convertDisplay(weight, weightUnit)
        (kotlin.math.round(converted * 100) / 100f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val baseWeightRange = remember(weightUnit) {
                if (weightUnit == "lb") (0..2000).map { it * 1f } else (0..1000).map { it * 0.5f }
            }
            val weightRange = remember(displayWeight, baseWeightRange) {
                val isInRange = baseWeightRange.any { kotlin.math.abs(it - displayWeight) < 0.001f }
                if (!isInRange && displayWeight > 0) {
                    (baseWeightRange + displayWeight).sorted()
                } else {
                    baseWeightRange
                }
            }

            WheelPickerBox(
                label = stringResource(R.string.weight_label_unit, weightUnit.uppercase()),
                value = displayWeight,
                range = weightRange,
                onValueChange = { 
                    val kgWeight = WeightUnitConverter.convertStorage(it, weightUnit)
                    onWeightChange(kgWeight) 
                },
                onRequestCustom = { customWeightText = ""; showCustomWeightDialog = true },
                format = { WeightUnitConverter.format(it) }
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            val baseSecondsRange = remember {
                (5..600 step 5).map { it.toFloat() }
            }
            val secondsRange = remember(seconds, baseSecondsRange) {
                val secF = seconds.toFloat()
                if (!baseSecondsRange.contains(secF) && seconds > 0) {
                    (baseSecondsRange + secF).sorted()
                } else {
                    baseSecondsRange
                }
            }

            WheelPickerBox(
                label = stringResource(R.string.set_duration_label),
                value = seconds.toFloat(),
                range = secondsRange,
                onValueChange = { onSecondsChange(it.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                onRequestCustom = { customSecondsText = ""; showCustomSecondsDialog = true },
                format = { "${it.toInt()}s" }
            )
        }
    }

    if (showCustomWeightDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showCustomWeightDialog = false },
            title = { Text(stringResource(R.string.custom_weight), color = OnSurface) },
            text = {
                OutlinedTextField(
                    value = customWeightText,
                    onValueChange = { customWeightText = it.replace(',', '.') },
                    label = { Text(stringResource(R.string.weight_label_unit, weightUnit)) },
                    placeholder = { Text(WeightUnitConverter.format(displayWeight), color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customWeightText.toFloatOrNull()?.let { 
                            val kgWeight = WeightUnitConverter.convertStorage(it, weightUnit)
                            onWeightChange(kgWeight) 
                        }
                        showCustomWeightDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWeightDialog = false }) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh
        )
    }

    if (showCustomSecondsDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showCustomSecondsDialog = false },
            title = { Text(stringResource(R.string.set_duration_label), color = OnSurface) },
            text = {
                OutlinedTextField(
                    value = customSecondsText,
                    onValueChange = { customSecondsText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.target_seconds)) },
                    placeholder = { Text(seconds.toString(), color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = Shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        cursorColor = Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customSecondsText.toIntOrNull()?.let { onSecondsChange(it) }
                        showCustomSecondsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSecondsDialog = false }) {
                    Text(stringResource(R.string.cancel), color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh
        )
    }
}

/** Interval and reps of a Time & Reps set (no weight): the duration wheel goes in 5 s steps, the reps one up to 300. */
@Composable
fun TimeRepsInput(
    seconds: Int,
    reps: Int,
    onSecondsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customSeconds by remember { mutableStateOf(false) }
    var customReps by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val baseSecondsRange = remember { (5..600 step 5).map { it.toFloat() } }
            val secondsRange = remember(seconds, baseSecondsRange) {
                val secF = seconds.toFloat()
                if (!baseSecondsRange.contains(secF) && seconds > 0) (baseSecondsRange + secF).sorted() else baseSecondsRange
            }
            WheelPickerBox(
                label = stringResource(R.string.set_duration_label),
                value = seconds.toFloat(),
                range = secondsRange,
                onValueChange = { onSecondsChange(it.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                onRequestCustom = { customSeconds = true },
                format = { "${it.toInt()}s" }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            val repsRange = remember(reps) {
                val base = (0..TIME_REPS_WHEEL_MAX).map { it.toFloat() }
                if (reps > TIME_REPS_WHEEL_MAX) base + reps.toFloat() else base
            }
            WheelPickerBox(
                label = stringResource(R.string.reps),
                value = reps.toFloat(),
                range = repsRange,
                onValueChange = { onRepsChange(it.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                onRequestCustom = { customReps = true },
                format = { it.toInt().toString() }
            )
        }
    }

    if (customSeconds) {
        CustomNumberDialog(
            title = stringResource(R.string.set_duration_label),
            label = stringResource(R.string.target_seconds),
            current = seconds,
            onDismiss = { customSeconds = false },
            onConfirm = { if (it > 0) onSecondsChange(it) }
        )
    }
    if (customReps) {
        CustomNumberDialog(
            title = stringResource(R.string.custom_reps),
            label = stringResource(R.string.reps),
            current = reps,
            onDismiss = { customReps = false },
            onConfirm = onRepsChange
        )
    }
}

private const val TIME_REPS_WHEEL_MAX = 300

@Composable
private fun CustomNumberDialog(
    title: String,
    label: String,
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(title, color = OnSurface) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { char -> char.isDigit() }.take(4) },
                label = { Text(label) },
                placeholder = { Text(current.toString(), color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = Shapes.large,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, cursorColor = Primary),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toIntOrNull()?.let(onConfirm)
                onDismiss()
            }) {
                Text(stringResource(R.string.ok), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = OnSurfaceVariant)
            }
        },
        containerColor = SurfaceContainerHigh
    )
}

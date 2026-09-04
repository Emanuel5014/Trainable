package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
                format = { WeightUnitConverter.format(it) }
            )
            TextButton(
                onClick = { 
                    customWeightText = ""
                    showCustomWeightDialog = true 
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            WheelPickerBox(
                label = stringResource(R.string.reps),
                value = reps.toFloat(),
                range = (0..100).map { it.toFloat() },
                onValueChange = { onRepsChange(it.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                format = { it.toInt().toString() }
            )
            TextButton(
                onClick = { 
                    customRepsText = ""
                    showCustomRepsDialog = true 
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }

    if (showCustomWeightDialog) {
        AlertDialog(
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
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
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
                    .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format(item),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isSelected) Primary else OnSurface.copy(alpha = 0.3f),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = if (isSelected) 24.sp else 18.sp
                        )
                    }
                }
            }
        }
    }
}

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
                format = { WeightUnitConverter.format(it) }
            )
            TextButton(
                onClick = { 
                    customWeightText = ""
                    showCustomWeightDialog = true 
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
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
                format = { "${it.toInt()}s" }
            )
            TextButton(
                onClick = { 
                    customSecondsText = ""
                    showCustomSecondsDialog = true 
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(R.string.custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }

    if (showCustomWeightDialog) {
        AlertDialog(
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

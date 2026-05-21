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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                label = "WEIGHT (${weightUnit.uppercase()})",
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
                    customWeightText = if (displayWeight > 0) WeightUnitConverter.format(displayWeight) else ""
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
                    "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
        
        WheelPickerBox(
            label = "REPS",
            value = reps.toFloat(),
            range = (0..100).map { it.toFloat() },
            onValueChange = { onRepsChange(it.toInt()) },
            modifier = Modifier.weight(1f),
            format = { it.toInt().toString() }
        )
    }

    if (showCustomWeightDialog) {
        AlertDialog(
            onDismissRequest = { showCustomWeightDialog = false },
            title = { Text("Custom Weight", color = OnSurface) },
            text = {
                OutlinedTextField(
                    value = customWeightText,
                    onValueChange = { customWeightText = it.replace(',', '.') },
                    label = { Text("Weight (${weightUnit})") },
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
                    Text("OK", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWeightDialog = false }) {
                    Text("Cancel", color = OnSurfaceVariant)
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
        range.indexOfFirst { item ->
            if (item is Float && value is Float) {
                kotlin.math.abs(item - value) < 0.001f
            } else {
                item == value
            }
        }.coerceAtLeast(0)
    }

    val listState = key(range) {
        rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        val targetIndex = range.indexOfFirst { item ->
            if (item is Float && value is Float) {
                kotlin.math.abs(item - value) < 0.001f
            } else {
                item == value
            }
        }
        if (targetIndex != -1 && targetIndex != listState.firstVisibleItemIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(50L)
            .distinctUntilChanged()
            .collect { index ->
                if (index in range.indices) {
                    val newValue = range[index]
                    val isSame = if (newValue is Float && value is Float) {
                        kotlin.math.abs(newValue - value) < 0.001f
                    } else {
                        newValue == value
                    }
                    if (!isSame) {
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

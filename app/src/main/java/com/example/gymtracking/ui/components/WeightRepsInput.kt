package com.example.gymtracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.gymtracking.data.repository.dataStore
import com.example.gymtracking.data.repository.UserPreferencesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracking.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun WeightRepsInput(
    weight: Float,
    reps: Int,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WheelPickerBox(
            label = "WEIGHT (KG)",
            value = weight,
            range = (0..1000).map { it * 0.5f },
            onValueChange = { onWeightChange(it) },
            modifier = Modifier.weight(1f),
            format = { String.format("%.1f", it) }
        )
        
        WheelPickerBox(
            label = "REPS",
            value = reps.toFloat(),
            range = (0..100).map { it.toFloat() },
            onValueChange = { onRepsChange(it.toInt()) },
            modifier = Modifier.weight(1f),
            format = { it.toInt().toString() }
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
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = range.indexOf(value).coerceAtLeast(0)
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        val targetIndex = range.indexOf(value)
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
                    if (newValue != value) {
                        onValueChange(newValue)
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
            fontWeight = FontWeight.Bold,
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
                items(range.size) { index ->
                    val item = range[index]
                    val isSelected = range.indexOf(value) == index
                    
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
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = if (isSelected) 24.sp else 18.sp
                        )
                    }
                }
            }
        }
    }
}

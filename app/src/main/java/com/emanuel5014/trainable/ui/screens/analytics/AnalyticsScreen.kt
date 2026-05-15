package com.emanuel5014.trainable.ui.screens.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.analytics.AnalyticsLineChart
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExercisePicker by remember { mutableStateOf(false) }
    var showChartPicker by remember { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var draggedWidgetId by remember { mutableStateOf<String?>(null) }
    var recentlyMovedWidgetId by remember { mutableStateOf<String?>(null) }
    var displacedWidgetId by remember { mutableStateOf<String?>(null) }
    var displacedWidgetOffset by remember { mutableStateOf(0f) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val moveThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val swapNudgePx = with(LocalDensity.current) { 28.dp.toPx() }
    val hasBodyWeightWidget = uiState.widgets.any { it is AnalyticsWidget.BodyWeight }
    val weightUnit = uiState.weightUnit
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Scaffold(containerColor = Surface) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenHeader(
                            title = stringResource(R.string.analytics_title),
                            subtitle = stringResource(R.string.analytics_subtitle),
                            icon = Icons.Rounded.Insights
                        )
                    }
                    AnalyticsHeaderFabMenu(
                        expanded = fabMenuExpanded,
                        hasBodyWeightWidget = hasBodyWeightWidget,
                        onExpandedChange = { fabMenuExpanded = it },
                        onAddBodyWeight = {
                            viewModel.addBodyWeightChart()
                            fabMenuExpanded = false
                        },
                        onAddExercise = {
                            showChartPicker = true
                            fabMenuExpanded = false
                        },
                        modifier = Modifier.padding(end = Spacing.CardPadding, top = Spacing.small)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        ExerciseCarouselSection(
                            selectedExercises = uiState.personalBests.filter { it.exerciseId in uiState.selectedExerciseIds },
                            weightUnit = weightUnit,
                            onEditClick = { showExercisePicker = true }
                        )
                    }
                    
                    items(uiState.widgets, key = { it.id }) { widget ->
                        var dragAccumulator by remember(widget.id) { mutableStateOf(0f) }
                        var dragOffsetY by remember(widget.id) { mutableStateOf(0f) }
                        val isDragging = draggedWidgetId == widget.id
                        val isRecentlyMoved = recentlyMovedWidgetId == widget.id
                        val displacedOffsetTarget = if (displacedWidgetId == widget.id) displacedWidgetOffset else 0f
                        val displacedAnimatedOffset by animateFloatAsState(
                            targetValue = displacedOffsetTarget,
                            label = "widget_displaced_offset"
                        )
                        val animatedOffsetY by animateFloatAsState(
                            targetValue = (if (isDragging) dragOffsetY else 0f) + displacedAnimatedOffset,
                            label = "widget_drag_offset"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = when {
                                isDragging -> 1.02f
                                isRecentlyMoved -> 1.01f
                                else -> 1f
                            },
                            label = "widget_drag_scale"
                        )
                        val dragModifier = Modifier
                            .graphicsLayer {
                                translationY = animatedOffsetY
                                scaleX = animatedScale
                                scaleY = animatedScale
                            }
                            .pointerInput(widget.id, uiState.widgets.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedWidgetId = widget.id
                                        dragAccumulator = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        draggedWidgetId = null
                                        displacedWidgetId = null
                                        displacedWidgetOffset = 0f
                                        dragAccumulator = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedWidgetId = null
                                        displacedWidgetId = null
                                        displacedWidgetOffset = 0f
                                        dragAccumulator = 0f
                                        dragOffsetY = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragAccumulator += dragAmount.y
                                    dragOffsetY += dragAmount.y

                                    val currentIndex = uiState.widgets.indexOfFirst { it.id == widget.id }
                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                    if (dragAccumulator > moveThresholdPx && currentIndex < uiState.widgets.lastIndex) {
                                        displacedWidgetId = uiState.widgets.getOrNull(currentIndex + 1)?.id
                                        displacedWidgetOffset = -swapNudgePx
                                        scope.launch {
                                            delay(30)
                                            displacedWidgetOffset = 0f
                                        }
                                        viewModel.moveWidget(widget.id, up = false)
                                        recentlyMovedWidgetId = widget.id
                                        scope.launch {
                                            delay(220)
                                            if (recentlyMovedWidgetId == widget.id) recentlyMovedWidgetId = null
                                        }
                                        dragAccumulator = 0f
                                        dragOffsetY -= moveThresholdPx
                                    } else if (dragAccumulator < -moveThresholdPx && currentIndex > 0) {
                                        displacedWidgetId = uiState.widgets.getOrNull(currentIndex - 1)?.id
                                        displacedWidgetOffset = swapNudgePx
                                        scope.launch {
                                            delay(30)
                                            displacedWidgetOffset = 0f
                                        }
                                        viewModel.moveWidget(widget.id, up = true)
                                        recentlyMovedWidgetId = widget.id
                                        scope.launch {
                                            delay(220)
                                            if (recentlyMovedWidgetId == widget.id) recentlyMovedWidgetId = null
                                        }
                                        dragAccumulator = 0f
                                        dragOffsetY += moveThresholdPx
                                    }
                                }
                            }

                        when (widget) {
                            is AnalyticsWidget.BodyWeight -> {
                                BodyWeightChartSection(
                                    modifier = dragModifier,
                                    isDragging = isDragging,
                                    isRecentlyMoved = isRecentlyMoved,
                                    bodyWeightHistory = widget.history,
                                    bodyWeightInput = uiState.bodyWeightInput,
                                    weightUnit = weightUnit,
                                    onBodyWeightInputChanged = viewModel::onBodyWeightInputChanged,
                                    onSubmitWeight = viewModel::submitWeight,
                                    onRemove = { viewModel.removeWidget(widget.id) }
                                )
                            }
                            is AnalyticsWidget.Exercise -> {
                                ExerciseChartSection(
                                    modifier = dragModifier,
                                    isDragging = isDragging,
                                    isRecentlyMoved = isRecentlyMoved,
                                    exerciseName = widget.exerciseName,
                                    history = widget.history,
                                    weightUnit = weightUnit,
                                    onRemove = { viewModel.removeWidget(widget.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExercisePicker) {
            ExercisePickerBottomSheet(
                allExercises = uiState.personalBests,
                selectedIds = uiState.selectedExerciseIds,
                onDismiss = { showExercisePicker = false },
                onToggleSelection = viewModel::toggleExerciseSelection,
                onClearAll = viewModel::clearExerciseSelection
            )
        }

        if (showChartPicker) {
            ExercisePickerBottomSheet(
                allExercises = uiState.personalBests,
                selectedIds = emptySet(), // Not used for this picker
                onDismiss = { showChartPicker = false },
                onToggleSelection = { id ->
                    viewModel.addExerciseChart(id)
                    showChartPicker = false
                },
                onClearAll = {}
            )
        }
    }
}

@Composable
fun ExerciseCarouselSection(
    selectedExercises: List<PersonalBestUiModel>,
    weightUnit: String,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.CardPadding, end = Spacing.CardPadding, bottom = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.analytics_max_progress),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(
                onClick = onEditClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.analytics_edit_selection))
            }
        }

        if (selectedExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(Spacing.medium)
                    .clip(RoundedCornerShape(Spacing.medium))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        stringResource(R.string.analytics_select_exercises_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.large)
                    )
                    TextButton(onClick = onEditClick) {
                        Text(stringResource(R.string.analytics_choose_exercises))
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { selectedExercises.size })
            
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = Spacing.CardPadding),
                    pageSpacing = Spacing.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) { page ->
                    val exercise = selectedExercises[page]
                    
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val pageOffset = (
                                        (pagerState.currentPage - page) + pagerState
                                            .currentPageOffsetFraction
                                        ).absoluteValue
                                
                                alpha = lerp(
                                    start = 0.5f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                                
                                scaleY = lerp(
                                    start = 0.85f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                            }
                    ) {
                        ExerciseCarouselItem(exercise = exercise, weightUnit = weightUnit)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .padding(top = Spacing.medium)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(selectedExercises.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width by animateDpAsState(if (isSelected) 16.dp else 6.dp, label = "indicator_width")
                            val color by animateColorAsState(if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.3f), label = "indicator_color")
                            
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHeaderFabMenu(
    expanded: Boolean,
    hasBodyWeightWidget: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddBodyWeight: () -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { onExpandedChange(!expanded) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = SurfaceContainerHigh,
                contentColor = Primary
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.Clear else Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_body_weight)) },
                leadingIcon = { Icon(Icons.Rounded.Insights, contentDescription = null) },
                enabled = !hasBodyWeightWidget,
                onClick = {
                    if (!hasBodyWeightWidget) {
                        onAddBodyWeight()
                    }
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_choose_exercises)) },
                leadingIcon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) },
                onClick = {
                    onAddExercise()
                    onExpandedChange(false)
                }
            )
        }
    }
}

@Composable
fun BodyWeightChartSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    bodyWeightHistory: List<AnalyticsChartPoint>,
    bodyWeightInput: String,
    weightUnit: String,
    onBodyWeightInputChanged: (String) -> Unit,
    onSubmitWeight: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "body_weight_card_elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.CardPadding, vertical = Spacing.medium)
            .border(
                width = if (isDragging || isRecentlyMoved) 1.dp else 0.dp,
                color = Primary.copy(alpha = if (isDragging) 0.5f else 0.22f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_body_weight),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                WidgetControls(onRemove)
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))

            if (bodyWeightHistory.isNotEmpty()) {
                val latestWeight = bodyWeightHistory.lastOrNull()?.value
                val firstWeight = bodyWeightHistory.firstOrNull()?.value
                val weightChangePercent = if (latestWeight != null && firstWeight != null && firstWeight != 0f) {
                    ((latestWeight - firstWeight) / firstWeight) * 100f
                } else null
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = latestWeight?.let { WeightUnitConverter.formatWithUnit(it, weightUnit) } ?: "-",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                        if (weightChangePercent != null && weightChangePercent != 0f) {
                            Text(
                                text = String.format("%+.1f%%", weightChangePercent),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (weightChangePercent < 0) Tertiary else Primary
                            )
                        }
                    }
                }
                
                AnalyticsLineChart(
                    points = bodyWeightHistory,
                    modifier = Modifier.fillMaxWidth(),
                    lineColor = Primary,
                    fillColor = Primary.copy(alpha = 0.14f)
                )
                
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = bodyWeightInput,
                    onValueChange = onBodyWeightInputChanged,
                    label = { Text(stringResource(R.string.analytics_todays_weight)) },
                    suffix = { Text(weightUnit) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                IconButton(
                    onClick = onSubmitWeight,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun ExerciseChartSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    exerciseName: String,
    history: List<AnalyticsChartPoint>,
    weightUnit: String,
    onRemove: (() -> Unit)? = null
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "exercise_card_elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.CardPadding, vertical = Spacing.medium)
            .border(
                width = if (isDragging || isRecentlyMoved) 1.dp else 0.dp,
                color = Primary.copy(alpha = if (isDragging) 0.5f else 0.22f),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                WidgetControls(onRemove)
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))

            if (history.isNotEmpty()) {
                val latest = history.last().value
                val first = history.first().value
                val changePercent = if (first != 0f) ((latest - first) / first) * 100f else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = WeightUnitConverter.formatWithUnit(latest, weightUnit),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                        Text(
                            text = stringResource(R.string.analytics_estimated_1rm_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (changePercent != 0f) {
                            Text(
                                text = String.format("%+.1f%%", changePercent),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (changePercent > 0) Primary else Tertiary
                            )
                        }
                    }
                }

                AnalyticsLineChart(
                    points = history,
                    modifier = Modifier.fillMaxWidth(),
                    lineColor = Primary,
                    fillColor = Primary.copy(alpha = 0.14f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.analytics_no_data_range),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetControls(
    onRemove: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Rounded.Clear,
                    contentDescription = stringResource(R.string.analytics_remove),
                    tint = Error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ExerciseCarouselItem(exercise: PersonalBestUiModel, weightUnit: String) {
    val displayWeight = WeightUnitConverter.convertDisplay(exercise.maxWeightKg, weightUnit)
    val estimated1RM = calculateEpley1RM(displayWeight, exercise.reps)
    
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Primary.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = exercise.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = WeightUnitConverter.format(displayWeight),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = weightUnit,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.analytics_record_reps, exercise.reps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary)
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.analytics_estimated_1rm),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = WeightUnitConverter.formatWithUnit(estimated1RM, weightUnit),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    allExercises: List<PersonalBestUiModel>,
    selectedIds: Set<Int>,
    onDismiss: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredExercises = remember(searchQuery, allExercises) {
        if (searchQuery.isBlank()) {
            allExercises
        } else {
            allExercises.filter {
                it.exerciseName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .padding(top = 24.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.analytics_choose_exercises),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                if (selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = stringResource(R.string.analytics_clear))
                    }
                }
            }
            
            Text(
                text = stringResource(R.string.analytics_selected_count, selectedIds.size),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedIds.isNotEmpty()) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selectedIds.isNotEmpty()) FontWeight.ExtraBold else FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text(stringResource(R.string.search_exercises)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.analytics_clear_search))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            HorizontalDivider(color = SurfaceContainerHigh)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                items(filteredExercises) { exercise ->
                    val isSelected = exercise.exerciseId in selectedIds
                    ExercisePickerItem(
                        exercise = exercise,
                        isSelected = isSelected,
                        onToggle = { onToggleSelection(exercise.exerciseId) },
                        enabled = true
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisePickerItem(
    exercise: PersonalBestUiModel,
    isSelected: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = exercise.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary
                )
            }
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSelected) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

fun calculateEpley1RM(weight: Float, reps: Int): Float {
    return if (reps > 0) {
        if (reps == 1) weight
        else weight * (1f + reps / 30f)
    } else 0f
}

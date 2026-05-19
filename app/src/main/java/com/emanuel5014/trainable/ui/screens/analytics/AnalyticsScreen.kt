package com.emanuel5014.trainable.ui.screens.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.DateRange
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.analytics.AnalyticsLineChart
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight

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
    val haptic = LocalHapticFeedback.current
    val moveThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val swapNudgePx = with(LocalDensity.current) { 28.dp.toPx() }
    val hasBodyWeightWidget = uiState.widgets.any { it is AnalyticsWidget.BodyWeight }
    val hasCalendarWidget = uiState.widgets.any { it is AnalyticsWidget.Calendar }
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                        hasCalendarWidget = hasCalendarWidget,
                        onExpandedChange = { fabMenuExpanded = it },
                        onAddBodyWeight = {
                            viewModel.addBodyWeightChart()
                            fabMenuExpanded = false
                        },
                        onAddCalendar = {
                            viewModel.addCalendarChart()
                            fabMenuExpanded = false
                        },
                        onAddExercise = {
                            showChartPicker = true
                            fabMenuExpanded = false
                        },
                        modifier = Modifier.padding(end = ResponsiveSize.cardPadding, top = Spacing.small)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
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
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "widget_displaced_offset"
                            )
                            val animatedOffsetY by animateFloatAsState(
                                targetValue = (if (isDragging) dragOffsetY else 0f) + displacedAnimatedOffset,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "widget_drag_offset"
                            )
                            val animatedScale by animateFloatAsState(
                                targetValue = when {
                                    isDragging -> 1.04f
                                    isRecentlyMoved -> 1.02f
                                    else -> 1f
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "widget_drag_scale"
                            )
                            val dragModifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = animatedOffsetY
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    clip = !isDragging
                                    rotationZ = if (isDragging) 1.2f else 0f
                                }
                                .pointerInput(widget.id, uiState.widgets.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                is AnalyticsWidget.Calendar -> {
                                    WorkoutCalendarSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyMoved,
                                        workoutDates = widget.workoutDates,
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

                    // Top gradient fade to smoothly hide items when scrolling up
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Surface, Color.Transparent)
                                )
                            )
                    )
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
                .padding(start = ResponsiveSize.cardPadding, end = ResponsiveSize.cardPadding, bottom = Spacing.medium),
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
                    contentPadding = PaddingValues(horizontal = ResponsiveSize.horizontalPadding),
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
    hasCalendarWidget: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddBodyWeight: () -> Unit,
    onAddCalendar: () -> Unit,
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
                text = {
                    Text(
                        if (Locale.getDefault().language == "it") "Calendario Attività"
                        else "Workout Calendar"
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                enabled = !hasCalendarWidget,
                onClick = {
                    if (!hasCalendarWidget) {
                        onAddCalendar()
                    }
                    onExpandedChange(false)
                }
            )
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
            .padding(horizontal = ResponsiveSize.cardPadding, vertical = Spacing.medium)
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
            .padding(horizontal = ResponsiveSize.cardPadding, vertical = Spacing.medium)
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

@Composable
fun WorkoutCalendarSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    workoutDates: List<Long>,
    onRemove: (() -> Unit)? = null
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "calendar_card_elevation"
    )

    // Today's date to highlight today
    val today = LocalDate.now()

    // 42 days grid calculations (6 weeks of 7 days)
    val days = remember(currentMonth) {
        val list = mutableListOf<LocalDate>()
        val firstDay = currentMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val leadingDays = firstDayOfWeek - 1
        
        // Start from Monday of the week containing the 1st of the month
        val startDate = firstDay.minusDays(leadingDays.toLong())
        
        for (i in 0 until 42) {
            list.add(startDate.plusDays(i.toLong()))
        }
        list
    }

    // Group workouts in the system's timezone to LocalDate format for extremely fast contains() lookup
    val workoutLocalDates = remember(workoutDates) {
        workoutDates.map {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    // Total workouts in the selected month
    val workoutsInMonthCount = remember(currentMonth, workoutLocalDates) {
        workoutLocalDates.count { date ->
            date.year == currentMonth.year && date.month == currentMonth.month
        }
    }

    val monthDisplayName = remember(currentMonth) {
        val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        // Capitalize first letter of month
        val capitalizedMonthName = monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$capitalizedMonthName ${currentMonth.year}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ResponsiveSize.cardPadding, vertical = Spacing.medium)
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
            // Header: Month Title & Left/Right Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.workout_logs).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = monthDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Primary
                    )
                    Text(
                        text = if (Locale.getDefault().language == "it") {
                            "$workoutsInMonthCount allenamenti questo mese"
                        } else {
                            "$workoutsInMonthCount workouts this month"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Left & Right Navigation Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    WidgetControls(onRemove)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Day of Week Header Row (Lun, Mar, Mer, Gio, Ven, Sab, Dom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val dayHeaders = if (Locale.getDefault().language == "it") {
                    listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
                } else {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                }
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // 6 Rows of 7 Days Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(6) { weekIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        repeat(7) { dayIndex ->
                            val cellDate = days[weekIndex * 7 + dayIndex]
                            val isCurrentMonth = cellDate.month == currentMonth.month
                            val hasWorkout = workoutLocalDates.contains(cellDate)
                            val isToday = cellDate == today

                            val cellModifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        hasWorkout -> Modifier.background(Primary)
                                        isToday -> Modifier.border(1.5.dp, Primary, CircleShape)
                                        else -> Modifier
                                    }
                                )

                            Box(
                                modifier = cellModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellDate.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (hasWorkout || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        hasWorkout -> OnPrimary
                                        !isCurrentMonth -> OnSurfaceVariant.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

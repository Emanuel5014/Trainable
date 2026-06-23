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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.BarChart
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.math.abs
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.analytics.AnalyticsLineChart
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
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
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        haptic = haptic,
        scope = scope,
        onMove = { id, up ->
            viewModel.moveWidget(id, up)
        }
    )
    val context = LocalContext.current
    val themeMode by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.THEME_MODE] ?: 0 }
    }.collectAsState(initial = 0)
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val hasBodyWeightWidget = uiState.widgets.any { it is AnalyticsWidget.BodyWeight }
    val hasCalendarWidget = uiState.widgets.any { it is AnalyticsWidget.Calendar }
    val hasCategoryVolumeWidget = uiState.widgets.any { it is AnalyticsWidget.CategoryVolume }
    val hasTimePeriodComparisonWidget = uiState.widgets.any { it is AnalyticsWidget.TimePeriodComparison }
    val weightUnit = uiState.weightUnit
    var showVolumeSettings by remember { mutableStateOf<String?>(null) }
    var showAddVolumeDialog by remember { mutableStateOf(false) }
    var showCategoryVolumeSettings by remember { mutableStateOf(false) }
    var showTimePeriodComparisonSettings by remember { mutableStateOf(false) }
    var showAddCategoryVolumeDialog by remember { mutableStateOf(false) }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    val autoScrollThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    val autoScrollSpeed = 15f
    
    // Hide navbar during drag
    LaunchedEffect(dragDropState.draggedWidgetId != null) {
        BottomBarManager.isVisibleOverride = dragDropState.draggedWidgetId == null
    }

    LaunchedEffect(dragDropState.draggedWidgetId) {
        val draggedId = dragDropState.draggedWidgetId
        if (draggedId != null) {
            while (true) {
                val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
                if (viewportHeight > 0f) {
                    val fingerY = dragDropState.fingerY
                    
                    if (fingerY < autoScrollThreshold) {
                        val ratio = (1f - (fingerY / autoScrollThreshold)).coerceIn(0f, 1f)
                        val speed = autoScrollSpeed * ratio
                        if (speed > 0.5f) {
                            lazyListState.scrollBy(-speed)
                            dragDropState.onDrag(fingerY)
                        }
                    } else if (fingerY > viewportHeight - autoScrollThreshold) {
                        val distanceToBottom = viewportHeight - fingerY
                        val ratio = (1f - (distanceToBottom / autoScrollThreshold)).coerceIn(0f, 1f)
                        val speed = autoScrollSpeed * ratio
                        if (speed > 0.5f) {
                            lazyListState.scrollBy(speed)
                            dragDropState.onDrag(fingerY)
                        }
                    }
                }
                delay(16)
            }
        }
    }

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
                        hasCategoryVolumeWidget = hasCategoryVolumeWidget,
                        hasTimePeriodComparisonWidget = hasTimePeriodComparisonWidget,
                        onExpandedChange = { fabMenuExpanded = it },
                        onAddBodyWeight = {
                            viewModel.addBodyWeightChart()
                            fabMenuExpanded = false
                        },
                        onAddCalendar = {
                            viewModel.addCalendarChart()
                            fabMenuExpanded = false
                        },
                        onAddVolume = {
                            showAddVolumeDialog = true
                            fabMenuExpanded = false
                        },
                        onAddExercise = {
                            showChartPicker = true
                            fabMenuExpanded = false
                        },
                        onAddCategoryVolume = {
                            showAddCategoryVolumeDialog = true
                            fabMenuExpanded = false
                        },
                        onAddTimePeriodComparison = {
                            viewModel.addTimePeriodComparison(AnalyticsTimeRange.OneMonth)
                            fabMenuExpanded = false
                        },
                        modifier = Modifier.padding(end = ResponsiveSize.cardPadding, top = Spacing.small)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val item = lazyListState.layoutInfo.visibleItemsInfo.find { info ->
                                            info.index > 0 && offset.y >= info.offset && offset.y <= info.offset + info.size
                                        }
                                        if (item != null) {
                                            val widgetId = item.key as String
                                            val itemRelativeOffset = offset - Offset(0f, item.offset.toFloat())
                                            dragDropState.onDragStart(
                                                absoluteInitialY = offset.y,
                                                itemRelativeY = itemRelativeOffset.y,
                                                widgetId = widgetId
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        dragDropState.onDragEnd()
                                    },
                                    onDragCancel = {
                                        dragDropState.onDragEnd()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragDropState.onDrag(change.position.y)
                                    }
                                )
                            },
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
                            val isDragging = dragDropState.draggedWidgetId == widget.id
                            val isRecentlyDropped = dragDropState.recentlyDroppedWidgetId == widget.id
                            
                            val translationY = if (isDragging) {
                                dragDropState.dragTranslationY(widget.id)
                            } else if (isRecentlyDropped) {
                                dragDropState.dropAnimatable.value
                            } else {
                                0f
                            }
                            
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.05f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "widget_drag_scale"
                            )
                            val animatedAlpha by animateFloatAsState(
                                targetValue = if (dragDropState.draggedWidgetId != null && !isDragging) 0.6f else 1f,
                                label = "widget_drag_alpha"
                            )

                            val dragModifier = Modifier
                                .then(if (isDragging) Modifier else Modifier.animateItem())
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    this.translationY = translationY
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = animatedAlpha
                                    shadowElevation = if (isDragging && isDark) 16.dp.toPx() else 0f
                                    clip = false
                                }

                            when (widget) {
                                is AnalyticsWidget.BodyWeight -> {
                                    BodyWeightChartSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        bodyWeightHistory = widget.history,
                                        bodyWeightInput = uiState.bodyWeightInput,
                                        weightUnit = weightUnit,
                                        onBodyWeightInputChanged = viewModel::onBodyWeightInputChanged,
                                        onSubmitWeight = viewModel::submitWeight,
                                        onRemove = { viewModel.removeWidget(widget.id) },
                                        onDeleteWeight = viewModel::deleteWeightLog
                                    )
                                }
                                is AnalyticsWidget.Calendar -> {
                                    WorkoutCalendarSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        workoutDates = widget.workoutDates,
                                        onRemove = { viewModel.removeWidget(widget.id) }
                                    )
                                }
                                is AnalyticsWidget.Exercise -> {
                                    ExerciseChartSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        exerciseName = widget.exerciseName,
                                        history = widget.history,
                                        weightUnit = weightUnit,
                                        onRemove = { viewModel.removeWidget(widget.id) }
                                    )
                                }
                                is AnalyticsWidget.Volume -> {
                                    VolumeChartSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        planName = widget.planName,
                                        timeRange = widget.timeRange,
                                        history = widget.history,
                                        weightUnit = weightUnit,
                                        onRemove = { viewModel.removeWidget(widget.id) },
                                        onEdit = { showVolumeSettings = widget.id }
                                    )
                                }
                                is AnalyticsWidget.CategoryVolume -> {
                                    CategoryVolumeChartSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        history = widget.history,
                                        timeRange = widget.timeRange,
                                        startDate = widget.startDate,
                                        weightUnit = weightUnit,
                                        onRemove = { viewModel.removeWidget(widget.id) },
                                        onEdit = { showCategoryVolumeSettings = true }
                                    )
                                }
                                is AnalyticsWidget.TimePeriodComparison -> {
                                    TimePeriodComparisonSection(
                                        modifier = dragModifier,
                                        isDragging = isDragging,
                                        isRecentlyMoved = isRecentlyDropped,
                                        period1Name = widget.period1Name,
                                        period2Name = widget.period2Name,
                                        period1DateRange = widget.period1DateRange,
                                        period2DateRange = widget.period2DateRange,
                                        period1Metrics = widget.period1Metrics,
                                        period2Metrics = widget.period2Metrics,
                                        period1Exercises = widget.period1Exercises,
                                        period2Exercises = widget.period2Exercises,
                                        weightUnit = weightUnit,
                                        summaryParts = widget.summaryParts,
                                        onRemove = { viewModel.removeWidget(widget.id) },
                                        onEdit = { showTimePeriodComparisonSettings = true }
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
            val existingExerciseIds = uiState.widgets
                .filterIsInstance<AnalyticsWidget.Exercise>()
                .map { it.exerciseId }
                .toSet()
            ExercisePickerBottomSheet(
                allExercises = uiState.personalBests,
                selectedIds = existingExerciseIds,
                onDismiss = { showChartPicker = false },
                onToggleSelection = { id ->
                    if (id !in existingExerciseIds) {
                        viewModel.addExerciseChart(id)
                    }
                },
                onClearAll = {
                    viewModel.removeAllExerciseCharts()
                }
            )
        }

        if (showVolumeSettings != null) {
            val widgetId = showVolumeSettings!!
            val widget = uiState.widgets.find { it.id == widgetId } as? AnalyticsWidget.Volume
            if (widget != null) {
                VolumeSettingsBottomSheet(
                    allPlans = uiState.allPlans,
                    currentPlanId = widget.planId,
                    currentTimeRange = widget.timeRange,
                    onDismiss = { showVolumeSettings = null },
                    onConfirm = { planId, timeRange ->
                        viewModel.updateVolumeChart(widgetId, planId, timeRange)
                        showVolumeSettings = null
                    }
                )
            }
        }

        if (showAddVolumeDialog) {
            val activePlan = uiState.allPlans.find { it.nome == uiState.activePlanName }
            val planId = activePlan?.id ?: uiState.allPlans.firstOrNull()?.id ?: -1
            VolumeSettingsBottomSheet(
                allPlans = uiState.allPlans,
                currentPlanId = planId,
                currentTimeRange = AnalyticsTimeRange.OneMonth,
                onDismiss = { showAddVolumeDialog = false },
                onConfirm = { selectedPlanId, selectedTimeRange ->
                    viewModel.addVolumeChart(selectedPlanId, selectedTimeRange)
                    showAddVolumeDialog = false
                }
            )
        }

        if (showAddCategoryVolumeDialog) {
            CategoryVolumeSettingsBottomSheet(
                currentTimeRange = AnalyticsTimeRange.OneWeek,
                onDismiss = { showAddCategoryVolumeDialog = false },
                onConfirm = { timeRange ->
                    viewModel.addCategoryVolumeChart(timeRange)
                    showAddCategoryVolumeDialog = false
                }
            )
        }

        if (showCategoryVolumeSettings) {
            val currentWidget = uiState.widgets.find { it is AnalyticsWidget.CategoryVolume } as? AnalyticsWidget.CategoryVolume
            CategoryVolumeSettingsBottomSheet(
                currentTimeRange = currentWidget?.timeRange ?: AnalyticsTimeRange.OneWeek,
                onDismiss = { showCategoryVolumeSettings = false },
                onConfirm = { timeRange ->
                    viewModel.updateCategoryVolumeChart(timeRange)
                    showCategoryVolumeSettings = false
                }
            )
        }

        if (showTimePeriodComparisonSettings) {
            val currentWidget = uiState.widgets.find { it is AnalyticsWidget.TimePeriodComparison } as? AnalyticsWidget.TimePeriodComparison
            TimePeriodComparisonSettingsBottomSheet(
                currentTimeRange = currentWidget?.timeRange ?: AnalyticsTimeRange.OneMonth,
                onDismiss = { showTimePeriodComparisonSettings = false },
                onConfirm = { range ->
                    viewModel.updateTimePeriodComparison(range)
                    showTimePeriodComparisonSettings = false
                }
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
    hasCategoryVolumeWidget: Boolean,
    hasTimePeriodComparisonWidget: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddBodyWeight: () -> Unit,
    onAddCalendar: () -> Unit,
    onAddVolume: () -> Unit,
    onAddExercise: () -> Unit,
    onAddCategoryVolume: () -> Unit,
    onAddTimePeriodComparison: () -> Unit,
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
                text = { Text(stringResource(R.string.analytics_workout_calendar)) },
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
                text = { Text(stringResource(R.string.analytics_category_volume_widget)) },
                leadingIcon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                enabled = !hasCategoryVolumeWidget,
                onClick = {
                    if (!hasCategoryVolumeWidget) {
                        onAddCategoryVolume()
                    }
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_total_volume)) },
                leadingIcon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) },
                onClick = {
                    onAddVolume()
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_add_1rm_graphic)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                onClick = {
                    onAddExercise()
                    onExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.analytics_time_period_comparison)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null) },
                enabled = !hasTimePeriodComparisonWidget,
                onClick = {
                    if (!hasTimePeriodComparisonWidget) {
                        onAddTimePeriodComparison()
                    }
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
    onSubmitWeight: (Long) -> Unit,
    onRemove: (() -> Unit)? = null,
    onDeleteWeight: ((Int) -> Unit)? = null
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
                
                var showHistory by remember { mutableStateOf(false) }
                val recentEntries = bodyWeightHistory.takeLast(5).reversed()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showHistory = !showHistory }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.analytics_body_weight_history),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showHistory) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(visible = showHistory) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recentEntries.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHighest.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateFormatter.formatShort(entry.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = WeightUnitConverter.formatWithUnit(entry.value, weightUnit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnSurface
                                    )
                                    if (onDeleteWeight != null && entry.id > 0) {
                                        IconButton(
                                            onClick = { onDeleteWeight(entry.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Clear,
                                                contentDescription = stringResource(R.string.analytics_remove),
                                                tint = Error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.small))
            }
            
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis()
            )
            val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showDatePicker = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = SurfaceContainerHighest,
                        contentColor = Primary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Rounded.DateRange,
                        contentDescription = stringResource(R.string.analytics_select_date),
                        modifier = Modifier.size(20.dp)
                    )
                }
                OutlinedTextField(
                    value = bodyWeightInput,
                    onValueChange = onBodyWeightInputChanged,
                    label = { Text(DateFormatter.formatShort(selectedDate)) },
                    suffix = { Text(weightUnit) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                IconButton(
                    onClick = { onSubmitWeight(selectedDate) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.save))
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = Surface,
                        titleContentColor = OnSurface,
                        headlineContentColor = OnSurface,
                        weekdayContentColor = OnSurfaceVariant,
                        subheadContentColor = OnSurfaceVariant,
                        yearContentColor = OnSurface,
                        currentYearContentColor = Primary,
                        selectedYearContentColor = OnPrimary,
                        selectedDayContentColor = OnPrimary,
                        selectedDayContainerColor = Primary,
                        todayContentColor = Primary,
                        todayDateBorderColor = Primary,
                        dayContentColor = OnSurface,
                        dayInSelectionRangeContentColor = OnSurface,
                        dayInSelectionRangeContainerColor = Primary.copy(alpha = 0.12f)
                    )
                ) {
                    DatePicker(state = datePickerState)
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
    onRemove: (() -> Unit)?,
    onEdit: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = Primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (onRemove != null) {
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
    onClearAll: () -> Unit,
    onConfirmAdd: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                    .height(if (onConfirmAdd != null) 300.dp else 350.dp)
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

            if (onConfirmAdd != null) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.small)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedIds.isNotEmpty()) Primary else SurfaceContainerHigh)
                        .clickable(enabled = selectedIds.isNotEmpty()) {
                            onConfirmAdd()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.add).uppercase(),
                        color = if (selectedIds.isNotEmpty()) OnPrimary else OnSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.small))
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

@Composable
fun VolumeChartSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    planName: String,
    timeRange: AnalyticsTimeRange,
    history: List<AnalyticsChartPoint>,
    weightUnit: String,
    onRemove: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "volume_card_elevation"
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
                Column {
                    Text(
                        text = stringResource(R.string.analytics_total_volume),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
                WidgetControls(onRemove, onEdit)
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
                            text = stringResource(timeRange.labelResId),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSettingsBottomSheet(
    allPlans: List<WorkoutPlanEntity>,
    currentPlanId: Int,
    currentTimeRange: AnalyticsTimeRange,
    onDismiss: () -> Unit,
    onConfirm: (Int, AnalyticsTimeRange) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPlanId by remember { mutableStateOf(currentPlanId) }
    var selectedTimeRange by remember { mutableStateOf(currentTimeRange) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_volume_settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            Text(
                text = stringResource(R.string.training_plans),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(allPlans) { plan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedPlanId == plan.id) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedPlanId = plan.id }
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, Primary, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (selectedPlanId == plan.id) Primary else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Text(
                            text = plan.nome,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedPlanId == plan.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = stringResource(R.string.analytics_time_range),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                AnalyticsTimeRange.values().forEach { range ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTimeRange == range) Primary else SurfaceContainerHigh)
                            .clickable { selectedTimeRange = range }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(range.labelResId),
                            color = if (selectedTimeRange == range) OnPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary)
                    .clickable { onConfirm(selectedPlanId, selectedTimeRange) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = OnPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryVolumeSettingsBottomSheet(
    currentTimeRange: AnalyticsTimeRange,
    onDismiss: () -> Unit,
    onConfirm: (AnalyticsTimeRange) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTimeRange by remember { mutableStateOf(currentTimeRange) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_category_volume),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            Text(
                text = stringResource(R.string.analytics_time_range),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                AnalyticsTimeRange.values().forEach { range ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTimeRange == range) Primary else SurfaceContainerHigh)
                            .clickable { selectedTimeRange = range }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(range.labelResId),
                            color = if (selectedTimeRange == range) OnPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary)
                    .clickable { onConfirm(selectedTimeRange) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = OnPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
                        text = stringResource(R.string.analytics_workouts_this_month, workoutsInMonthCount),
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
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
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
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
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
                val dayHeaders = listOf(
                    stringResource(R.string.analytics_day_mon),
                    stringResource(R.string.analytics_day_tue),
                    stringResource(R.string.analytics_day_wed),
                    stringResource(R.string.analytics_day_thu),
                    stringResource(R.string.analytics_day_fri),
                    stringResource(R.string.analytics_day_sat),
                    stringResource(R.string.analytics_day_sun)
                )
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

@Composable
fun CategoryVolumeChartSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    history: List<CategoryVolumeRow>,
    timeRange: AnalyticsTimeRange,
    startDate: Long,
    weightUnit: String,
    onRemove: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "category_volume_card_elevation"
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
                Column {
                    Text(
                        text = stringResource(R.string.analytics_category_volume),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val endDate = System.currentTimeMillis()
                    val dateRangeText = if (timeRange == AnalyticsTimeRange.All) {
                        stringResource(R.string.analytics_time_range_all)
                    } else {
                        "${DateFormatter.formatShort(startDate)} - ${DateFormatter.formatShort(endDate)}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                WidgetControls(onRemove, onEdit)
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))

            if (history.isNotEmpty()) {
                val maxVolume = history.maxOfOrNull { it.volume } ?: 1f
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    history.forEach { row ->
                        val progress = if (maxVolume > 0f) row.volume / maxVolume else 0f
                        
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "category_volume_progress"
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${row.volume.toInt()} ${stringResource(R.string.sets).lowercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                         .fillMaxWidth(animatedProgress)
                                         .fillMaxHeight()
                                         .clip(RoundedCornerShape(5.dp))
                                         .background(
                                             Brush.horizontalGradient(
                                                 colors = listOf(
                                                     Primary.copy(alpha = 0.7f),
                                                     Primary
                                                 )
                                             )
                                         )
                                 )
                            }
                        }
                    }
                }
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
fun TimePeriodComparisonSection(
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isRecentlyMoved: Boolean = false,
    period1Name: String,
    period2Name: String,
    period1DateRange: String,
    period2DateRange: String,
    period1Metrics: PeriodComparisonMetrics,
    period2Metrics: PeriodComparisonMetrics,
    period1Exercises: List<PeriodExerciseComparison>,
    period2Exercises: List<PeriodExerciseComparison>,
    weightUnit: String,
    summaryParts: List<SummaryPart> = emptyList(),
    onRemove: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else if (isRecentlyMoved) 8.dp else 2.dp,
        label = "comparison_card_elevation"
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
                    text = stringResource(R.string.analytics_time_period_comparison),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                WidgetControls(onRemove, onEdit)
            }

            // Summary row
            if (summaryParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    summaryParts.forEach { part ->
                        val isPositive = part.deltaPercent > 0.01f
                        val isNegative = part.deltaPercent < -0.01f
                        val badgeBgColor = when {
                            isPositive -> Primary.copy(alpha = 0.12f)
                            isNegative -> Error.copy(alpha = 0.12f)
                            else -> OnSurfaceVariant.copy(alpha = 0.08f)
                        }
                        val badgeContentColor = when {
                            isPositive -> Primary
                            isNegative -> Error
                            else -> OnSurfaceVariant
                        }
                        val icon = when {
                            isPositive -> Icons.Rounded.ArrowUpward
                            isNegative -> Icons.Rounded.ArrowDownward
                            else -> Icons.Rounded.Remove
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = badgeContentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${String.format("%+.0f", part.deltaPercent)}% ${part.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeContentColor,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Period headers with date ranges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = period1Name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = period1DateRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = period2Name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Tertiary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = period2DateRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Metrics with visual bars
            ComparisonMetricBar(
                label = stringResource(R.string.compare_volume),
                value1 = period1Metrics.volume,
                value2 = period2Metrics.volume,
                format = { WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(it, weightUnit), weightUnit) }
            )
            ComparisonMetricBar(
                label = stringResource(R.string.compare_sessions),
                value1 = period1Metrics.sessionCount.toFloat(),
                value2 = period2Metrics.sessionCount.toFloat(),
                format = { String.format("%.0f", it) }
            )
            ComparisonMetricBar(
                label = stringResource(R.string.compare_sets),
                value1 = period1Metrics.setCount.toFloat(),
                value2 = period2Metrics.setCount.toFloat(),
                format = { String.format("%.0f", it) }
            )
            ComparisonMetricBar(
                label = stringResource(R.string.analytics_training_days),
                value1 = period1Metrics.trainingDays.toFloat(),
                value2 = period2Metrics.trainingDays.toFloat(),
                format = { String.format("%.0f", it) }
            )
            ComparisonMetricBar(
                label = stringResource(R.string.compare_avg_weight),
                value1 = period1Metrics.avgWeight,
                value2 = period2Metrics.avgWeight,
                format = { WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(it, weightUnit), weightUnit) }
            )

            if (period1Exercises.isNotEmpty() || period2Exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                HorizontalDivider(color = SurfaceContainerHighest)
                Spacer(modifier = Modifier.height(Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.compare_exercises_detail),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "1RM",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.small))

                val allExerciseNames = (period1Exercises.map { it.exerciseName } + period2Exercises.map { it.exerciseName }).distinct()
                val sortedExercises = allExerciseNames
                    .map { name ->
                        val p1 = period1Exercises.find { it.exerciseName == name }?.max1rm ?: 0f
                        val p2 = period2Exercises.find { it.exerciseName == name }?.max1rm ?: 0f
                        val diffPercent = if (p2 != 0f) ((p1 - p2) / p2) * 100f else 0f
                        Triple(name, p1, diffPercent)
                    }
                    .sortedByDescending { it.third }
                    .take(5)
                sortedExercises.forEach { (name, value1, _) ->
                    val p2 = period2Exercises.find { it.exerciseName == name }?.max1rm ?: 0f
                    ComparisonExerciseRow(
                        exerciseName = name,
                        value1 = value1,
                        value2 = p2,
                        weightUnit = weightUnit
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonMetricBar(
    label: String,
    value1: Float,
    value2: Float,
    format: (Float) -> String
) {
    val diff = value1 - value2
    val diffPercent = if (value2 != 0f) (diff / value2) * 100f else 0f

    val maxValue = maxOf(value1, value2, 0.01f)
    val progress1 = (value1 / maxValue).coerceIn(0f, 1f)
    val progress2 = (value2 / maxValue).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            val isNeutral = abs(diffPercent) < 0.1f
            val isPositive = diffPercent >= 0.1f
            val badgeBgColor = when {
                isPositive -> Primary.copy(alpha = 0.12f)
                isNeutral -> OnSurfaceVariant.copy(alpha = 0.08f)
                else -> Error.copy(alpha = 0.12f)
            }
            val badgeContentColor = when {
                isPositive -> Primary
                isNeutral -> OnSurfaceVariant
                else -> Error
            }
            val icon = when {
                isPositive -> Icons.Rounded.ArrowUpward
                isNeutral -> Icons.Rounded.Remove
                else -> Icons.Rounded.ArrowDownward
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeContentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = String.format("%+.1f%%", diffPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeContentColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        // Side-by-side formatted values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = format(value1),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Text(
                text = stringResource(R.string.compare_vs),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = format(value2),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Tertiary
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        // Stacked progress bars with gradients
        // Bar 1: Primary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress1)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary.copy(alpha = 0.7f), Primary)
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Bar 2: Tertiary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress2)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Tertiary.copy(alpha = 0.7f), Tertiary)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    label: String,
    value1: String,
    value2: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = value2,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Tertiary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ComparisonExerciseRow(
    exerciseName: String,
    value1: Float,
    value2: Float,
    weightUnit: String
) {
    val diff = value1 - value2
    val diffPercent = if (value2 != 0f) (diff / value2) * 100f else 0f

    val isNeutral = abs(diffPercent) < 0.1f
    val isPositive = !isNeutral && diff > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${WeightUnitConverter.format(WeightUnitConverter.convertDisplay(value1, weightUnit))}${weightUnit}",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            val badgeBgColor = when {
                isPositive -> Primary.copy(alpha = 0.12f)
                isNeutral -> OnSurfaceVariant.copy(alpha = 0.08f)
                else -> Error.copy(alpha = 0.12f)
            }
            val badgeContentColor = when {
                isPositive -> Primary
                isNeutral -> OnSurfaceVariant
                else -> Error
            }
            val icon = when {
                isPositive -> Icons.Rounded.ArrowUpward
                isNeutral -> Icons.Rounded.Remove
                else -> Icons.Rounded.ArrowDownward
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeContentColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isNeutral) "0.0%" else String.format("%+.1f%%", diffPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeContentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePeriodComparisonSettingsBottomSheet(
    currentTimeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth,
    onDismiss: () -> Unit,
    onConfirm: (AnalyticsTimeRange) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTimeRange by remember { mutableStateOf(currentTimeRange) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.analytics_time_period_comparison),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = Spacing.medium)
            )

            Text(
                text = stringResource(R.string.analytics_time_range),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                AnalyticsTimeRange.values().forEach { range ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTimeRange == range) Primary else SurfaceContainerHigh)
                            .clickable { selectedTimeRange = range }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(range.labelResId),
                            color = if (selectedTimeRange == range) OnPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary)
                    .clickable { onConfirm(selectedTimeRange) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = OnPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

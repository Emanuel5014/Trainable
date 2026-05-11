package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.WorkoutShareCard
import com.emanuel5014.trainable.ui.components.captureViewToBitmap
import com.emanuel5014.trainable.util.UriMigrationHelper
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.ShareUtils
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.emanuel5014.trainable.ui.components.CardioInputForm
import com.emanuel5014.trainable.ui.components.AddCardioDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    navController: NavController? = null,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val context = LocalContext.current
    
    val listState = rememberLazyListState()

    // Resolve colors at the top of the Composable
    val surfaceColor = Surface
    val onSurfaceColor = OnSurface
    val onSurfaceVariantColor = OnSurfaceVariant
    val primaryColor = Primary
    val errorColor = Error
    val surfaceContainerHighColor = SurfaceContainerHigh
    val surfaceContainerHighestColor = SurfaceContainerHighest

    var expandedSessionId by remember { mutableStateOf<Int?>(null) }
    var sessionToEdit by remember { mutableStateOf<SessionWithDetails?>(null) }
    var sessionToDelete by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    val swipeActionsEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true }
    }.collectAsState(initial = true)

    var isNavigating by remember { mutableStateOf(false) }
    var showCardioDialog by remember { mutableStateOf(false) }
    var showAddWorkoutSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { sessionId ->
            navController?.navigate(EditWorkoutSession(sessionId))
        }
    }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var sessionToShare by remember { mutableStateOf<Pair<SessionWithDetails, String>?>(null) }

    LaunchedEffect(Unit) {
        isNavigating = false
    }

    val fabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.canScrollForward
        }
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = surfaceColor,
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    FloatingActionButtonMenu(
                        modifier = Modifier.padding(bottom = 72.dp).offset(x = 12.dp).zIndex(10f),
                        expanded = fabMenuExpanded,
                        button = {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    if (fabMenuExpanded) TooltipAnchorPosition.Start else TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(stringResource(if (fabMenuExpanded) R.string.close else R.string.add)) } },
                                state = rememberTooltipState()
                            ) {
                                ToggleFloatingActionButton(
                                    checked = fabMenuExpanded,
                                    onCheckedChange = { fabMenuExpanded = !fabMenuExpanded }
                                ) {
                                    val imageVector by remember {
                                        derivedStateOf {
                                            if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add
                                        }
                                    }
                                    Icon(
                                        painter = rememberVectorPainter(imageVector),
                                        contentDescription = null,
                                        modifier = Modifier.animateIcon({ checkedProgress })
                                    )
                                }
                            }
                        }
                    ) {
                        FloatingActionButtonMenuItem(
                            onClick = { 
                                fabMenuExpanded = false
                                showCardioDialog = true
                            },
                            icon = { Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null) },
                            text = { Text(text = stringResource(R.string.add_cardio)) }
                        )
                        FloatingActionButtonMenuItem(
                            onClick = { 
                                fabMenuExpanded = false
                                showAddWorkoutSheet = true
                            },
                            icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null) },
                            text = { Text(text = stringResource(R.string.add_workout)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ScreenHeader(
                    titleContent = {
                        Text(
                            text = if (uiState.isSelectionMode) {
                                "${uiState.selectedSessionIds.size} ${stringResource(R.string.selected)}"
                            } else {
                                stringResource(R.string.history_title)
                            },
                            style = if (uiState.isSelectionMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            color = onSurfaceColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    },
                    subtitle = if (uiState.isSelectionMode) null else stringResource(R.string.workout_logs),
                    icon = if (uiState.isSelectionMode) null else Icons.Rounded.History,
                    actions = if (uiState.isSelectionMode) {
                        {
                            if (!swipeActionsEnabled) {
                                if (uiState.selectedSessionIds.size == 1) {
                                    GymIconButton(
                                        icon = Icons.Rounded.Edit,
                                        onClick = { 
                                            val sessionId = uiState.selectedSessionIds.first()
                                            sessionToEdit = uiState.sessions.find { it.session.id == sessionId }
                                        },
                                        containerColor = surfaceContainerHighColor,
                                        contentColor = primaryColor
                                    )
                                }
                                GymIconButton(
                                    icon = Icons.Rounded.DeleteSweep,
                                    onClick = { showBulkDeleteDialog = true },
                                    containerColor = surfaceContainerHighColor,
                                    contentColor = errorColor
                                )
                            }
                            GymIconButton(
                                icon = Icons.Rounded.Close,
                                onClick = { viewModel.clearSelection() },
                                containerColor = surfaceContainerHighColor
                            )
                        }
                    } else {
                        {
                            val hasActiveFilters = uiState.selectedPlanId != null || uiState.startDate != null || uiState.endDate != null
                            GymIconButton(
                                icon = if (hasActiveFilters) Icons.Rounded.FilterListOff else Icons.Rounded.FilterList,
                                onClick = { showFilterSheet = true },
                                containerColor = if (hasActiveFilters) primaryColor.copy(alpha = 0.1f) else surfaceContainerHighColor,
                                contentColor = if (hasActiveFilters) primaryColor else onSurfaceColor
                            )
                        }
                    },
                    titleInRow = uiState.isSelectionMode
                )

                if (uiState.isLoading && uiState.sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GymLoadingIndicator()
                    }
                } else if (uiState.sessions.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = stringResource(R.string.no_history_yet),
                        description = stringResource(R.string.no_history_description_screen),
                        modifier = Modifier.weight(1f)
                    )
                } else if (uiState.filteredSessions.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.FilterListOff,
                        title = stringResource(R.string.no_results_filters),
                        description = stringResource(R.string.try_adjust_filters),
                        action = {
                            GymButton(onClick = { viewModel.setFilters(null, null, null) }) {
                                Text(stringResource(R.string.clear_filters).uppercase(), fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                    itemsIndexed(uiState.filteredSessions, key = { _, s -> s.session.id }) { index, sessionDetails ->
                        val session = sessionDetails.session
                        val planName = sessionDetails.plan.nome
                        val isExpanded = expandedSessionId == session.id

                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.targetValue) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                if (swipeActionsEnabled && !uiState.isSelectionMode) {
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            sessionToDelete = session
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            sessionToEdit = sessionDetails
                                        }
                                        else -> {}
                                    }
                                }
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = swipeActionsEnabled && !uiState.isSelectionMode,
                            enableDismissFromEndToStart = swipeActionsEnabled && !uiState.isSelectionMode,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection

                                val color by animateColorAsState(
                                    when (direction) {
                                        SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.6f)
                                        SwipeToDismissBoxValue.StartToEnd -> Primary.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    },
                                    label = "bg_color"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(Shapes.extraLarge)
                                        .background(color)
                                        .padding(horizontal = 28.dp),
                                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(
                                            Icons.Rounded.DeleteSweep,
                                            contentDescription = "Delete",
                                            tint = Error,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                        Icon(
                                            Icons.Rounded.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            SessionHistoryCard(
                                session = session,
                                planName = planName,
                                isExpanded = isExpanded,
                                details = if (isExpanded) uiState.selectedSession else null,
                                languageCode = languageCode,
                                weightUnit = uiState.weightUnit,
                                onShareClick = { details ->
                                    sessionToShare = details to planName
                                },
                                swipeActionsEnabled = swipeActionsEnabled,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = uiState.selectedSessionIds.contains(session.id),
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSessionSelection(session.id)
                                    } else {
                                        expandedSessionId = if (isExpanded) null else session.id
                                        if (!isExpanded) {
                                            viewModel.loadSessionDetails(session.id)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isSelectionMode && !swipeActionsEnabled) {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleSessionSelection(session.id)
                                    }
                                }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
            }
        }

        if (sessionToShare != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GymLoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.preparing_share),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Box(
                        modifier = Modifier
                            .alpha(0f)
                            .zIndex(-100f)
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    setContent {
                                        WorkoutShareCard(
                                            sessionDetails = sessionToShare!!.first,
                                            planName = sessionToShare!!.second,
                                            languageCode = languageCode,
                                            weightUnit = uiState.weightUnit
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                view.post {
                                    val bitmap = captureViewToBitmap(view)
                                    ShareUtils.shareBitmap(context, bitmap)
                                    sessionToShare = null
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_session)) },
                text = { Text(stringResource(R.string.delete_session_message)) },
                confirmButton = {
                    GymButton(
                        onClick = {
                            viewModel.deleteSelectedSessions()
                            showBulkDeleteDialog = false
                        },
                        containerColor = errorColor.copy(alpha = 0.1f),
                        contentColor = errorColor
                    ) {
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { showBulkDeleteDialog = false },
                        containerColor = Color.Transparent,
                        contentColor = onSurfaceVariantColor
                    ) {
                        Text(stringResource(R.string.cancel).uppercase())
                    }
                },
                containerColor = surfaceContainerHighColor,
                titleContentColor = onSurfaceColor,
                textContentColor = onSurfaceVariantColor
            )
        }

        if (sessionToDelete != null) {
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(stringResource(R.string.delete_session)) },
                text = { Text(stringResource(R.string.delete_session_message)) },
                confirmButton = {
                    GymButton(
                        onClick = {
                            sessionToDelete?.let { viewModel.deleteSession(it.id) }
                            sessionToDelete = null
                        },
                        containerColor = errorColor.copy(alpha = 0.1f),
                        contentColor = errorColor
                    ) {
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { sessionToDelete = null },
                        containerColor = Color.Transparent,
                        contentColor = onSurfaceVariantColor
                    ) {
                        Text(stringResource(R.string.cancel).uppercase())
                    }
                },
                containerColor = surfaceContainerHighColor,
                titleContentColor = onSurfaceColor,
                textContentColor = onSurfaceVariantColor
            )
        }

        if (sessionToEdit != null) {
            AlertDialog(
                onDismissRequest = { 
                    sessionToEdit = null
                },
                title = { Text(stringResource(R.string.edit_session)) },
                text = { Text(stringResource(R.string.edit_session_message)) },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.padding(bottom = Spacing.small)
                    ) {
                        GymButton(
                            onClick = { sessionToEdit = null },
                            containerColor = surfaceContainerHighColor,
                            contentColor = onSurfaceVariantColor
                        ) {
                            Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
                        }
                        GymButton(
                            onClick = {
                                sessionToEdit?.let { details ->
                                    isNavigating = true
                                    navController?.navigate(EditWorkoutSession(sessionId = details.session.id))
                                }
                                sessionToEdit = null
                            },
                            containerColor = primaryColor.copy(alpha = 0.15f),
                            contentColor = primaryColor
                        ) {
                            Text(stringResource(R.string.edit).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {},
                containerColor = surfaceContainerHighColor,
                titleContentColor = onSurfaceColor,
                textContentColor = onSurfaceVariantColor
            )
        }

        if (showFilterSheet) {
            HistoryFilterBottomSheet(
                selectedPlanId = uiState.selectedPlanId,
                availablePlans = uiState.availablePlans,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onPlanSelected = { planId ->
                    viewModel.setFilters(planId, uiState.startDate, uiState.endDate)
                },
                onDateRangeSelected = { start, end ->
                    viewModel.setFilters(uiState.selectedPlanId, start, end)
                },
                onDateClick = { showDatePicker = true },
                onClearDate = {
                    viewModel.setFilters(uiState.selectedPlanId, null, null)
                },
                onClearAll = {
                    viewModel.setFilters(null, null, null)
                },
                onDismiss = { showFilterSheet = false }
            )
        }
        if (showDatePicker) {

        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.setFilters(uiState.selectedPlanId, start, end)
                        }
                        showDatePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text(stringResource(R.string.apply_filters).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = onSurfaceVariantColor
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = surfaceContainerHighColor
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = {
                    Text(
                        text = stringResource(R.string.select_date_range),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                headline = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dateRangePickerState.selectedStartDateMillis != null) {
                                DateFormatter.formatShort(dateRangePickerState.selectedStartDateMillis!!)
                            } else "Start",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text("-", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = if (dateRangePickerState.selectedEndDateMillis != null) {
                                DateFormatter.formatShort(dateRangePickerState.selectedEndDateMillis!!)
                            } else "End",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = surfaceContainerHighColor,
                    selectedDayContainerColor = primaryColor,
                    todayDateBorderColor = primaryColor,
                    dayInSelectionRangeContainerColor = primaryColor.copy(alpha = 0.1f)
                )
            )
        }
    }

    if (showAddWorkoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddWorkoutSheet = false },
            sheetState = bottomSheetState,
            containerColor = surfaceColor,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            AddWorkoutFromPlanContent(
                plans = uiState.availablePlans,
                onPlanSelected = { planId ->
                    viewModel.addManualWorkout(planId)
                    showAddWorkoutSheet = false
                },
                onEmptyWorkoutClick = {
                    viewModel.addEmptyWorkout()
                    showAddWorkoutSheet = false
                }
            )
        }
    }

    if (showCardioDialog) {
        AddCardioDialog(
            onDismiss = { showCardioDialog = false },
            onConfirm = { categoria, distanza, durataSecondi ->
                viewModel.saveCardioWorkout(categoria, distanza, durataSecondi)
                showCardioDialog = false
            }
        )
    }
}
}

// AddCardioDialog was moved to CardioInputForm.kt as a reusable component

@Composable
fun AddWorkoutFromPlanContent(
    plans: List<WorkoutPlanEntity>,
    onPlanSelected: (Int) -> Unit,
    onEmptyWorkoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.add_workout),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = OnSurface
        )
        
        Text(
            text = stringResource(R.string.select_plan_to_add),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
        
        val filteredPlans = remember(plans) { 
            plans.filter { 
                val nameLower = it.nome.lowercase()
                nameLower != "cardio" && nameLower != "custom workout"
            } 
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GymCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEmptyWorkoutClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AddBox,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.add_empty_workout),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = stringResource(R.string.custom_workout),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filteredPlans) { plan ->
                GymCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlanSelected(plan.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (plan.imageUri != null) {
                                val context = LocalContext.current
                                val model = remember(plan.imageUri) {
                                    UriMigrationHelper.fixPath(plan.imageUri, context) ?: plan.imageUri
                                }
                                AsyncImage(
                                    model = model,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Notes,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.nome,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            if (!plan.note.isNullOrBlank()) {
                                Text(
                                    text = plan.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = OnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionHistoryCard(
    session: WorkoutSessionEntity,
    planName: String,
    isExpanded: Boolean,
    details: SessionWithDetails?,
    languageCode: String = "en",
    weightUnit: String = "kg",
    onShareClick: (SessionWithDetails) -> Unit = {},
    swipeActionsEnabled: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val mainIcon = if (planName == "Cardio") Icons.AutoMirrored.Rounded.DirectionsRun else Icons.Rounded.FitnessCenter
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = mainIcon,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = DateFormatter.format(session.timestamp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = when (planName) {
                                "Cardio" -> stringResource(R.string.add_cardio).replace(stringResource(R.string.add) + " ", "")
                                "Custom Workout" -> stringResource(R.string.custom_workout)
                                else -> planName
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (isSelectionMode) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Primary,
                                uncheckedColor = OnSurfaceVariant
                            )
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            color = SurfaceContainerHighest, 
                            thickness = 1.dp,
                            modifier = Modifier.weight(1f)
                        )
                        if (details != null && details.sets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(16.dp))
                            GymIconButton(
                                icon = Icons.Rounded.Share,
                                onClick = { onShareClick(details) },
                                containerColor = Primary.copy(alpha = 0.1f),
                                contentColor = Primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (details == null) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            GymLoadingIndicator(size = 24.dp)
                        }
                    } else if (details.sets.isEmpty() && details.cardio.isEmpty()) {
                        Text(stringResource(R.string.no_exercises_in_session_detail), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    } else {
                        val sortedSets = details.sets.sortedWith(
                            compareBy(
                                { it.setLog.ordineEsercizio },
                                { it.setLog.numeroSerie }
                            )
                        )
                        val groupedSets = sortedSets.groupBy { it.exercise.id }
                        
                        groupedSets.forEach { (_, sets) ->
                            val exerciseName = ExerciseTranslations.translate(sets.first().exercise.nome, languageCode)
                            
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Text(
                                    text = exerciseName.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                sets.forEach { setWithEx ->
                                    val set = setWithEx.setLog
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stringResource(R.string.set_number, set.numeroSerie),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = OnSurfaceVariant
                                            )
                                            Text(
                                                text = WeightUnitConverter.formatWithUnit(
                                                    WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                                                    weightUnit
                                                ) + " × ${set.repsEffettive}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OnSurface
                                            )
                                        }
                                        if (!set.note.isNullOrBlank()) {
                                            Row(
                                                modifier = Modifier.padding(top = 2.dp, start = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.Notes,
                                                    contentDescription = null,
                                                    tint = OnSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = set.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = OnSurfaceVariant,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        details.cardio.forEach { cardio ->
                            val icon = when (cardio.categoria.lowercase()) {
                                "corsa", "run" -> Icons.AutoMirrored.Rounded.DirectionsRun
                                "bici", "bike", "cycling" -> Icons.AutoMirrored.Rounded.DirectionsBike
                                "camminata", "walk" -> Icons.AutoMirrored.Rounded.DirectionsWalk
                                else -> Icons.AutoMirrored.Rounded.DirectionsRun
                            }
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cardio.categoria.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cardio.distanza} km",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = run {
                                            val h = cardio.durataSecondi / 3600
                                            val m = (cardio.durataSecondi % 3600) / 60
                                            val s = cardio.durataSecondi % 60
                                            if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSetDialog(
    set: SetLogEntity,
    weightUnit: String = "kg",
    isNewSet: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (SetLogEntity) -> Unit,
    onDelete: (SetLogEntity) -> Unit
) {
    var weight by remember {
        mutableStateOf(
            if (isNewSet) ""
            else WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit).toString()
        )
    }
    var reps by remember { mutableStateOf(set.repsEffettive.toString()) }
    var note by remember { mutableStateOf(set.note ?: "") }

    val isValid = weight.isNotBlank() && reps.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_exercise_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GymInputField(
                    value = weight,
                    onValueChange = { newValue ->
                        weight = newValue.replace(",", ".")
                    },
                    label = "Weight ($weightUnit)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = if (!isNewSet && weight.isNotEmpty()) {
                        {
                            IconButton(onClick = { weight = "" }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Clear",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null
                )
                GymInputField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = stringResource(R.string.reps),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                GymInputField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.routine_notes),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (!isNewSet) {
                    GymButton(
                        onClick = { onDelete(set) },
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error,
                        height = 48,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                GymButton(
                    onClick = onDismiss,
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant,
                    height = 48,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.cancel).uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                GymButton(
                    onClick = {
                        if (!isValid) return@GymButton
                        val displayWeight = weight.toFloatOrNull() ?: 0f
                        val storageWeight = WeightUnitConverter.convertStorage(displayWeight, weightUnit)
                        val updatedSet = set.copy(
                            pesoSollevato = storageWeight,
                            repsEffettive = reps.toIntOrNull() ?: 0,
                            note = note.ifBlank { null }
                        )
                        onConfirm(updatedSet)
                    },
                    enabled = isValid,
                    height = 48,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.save).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        dismissButton = {},
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}


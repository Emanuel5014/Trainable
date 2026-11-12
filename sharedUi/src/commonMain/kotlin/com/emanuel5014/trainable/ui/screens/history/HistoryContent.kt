package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.sharedui.resources.add
import com.emanuel5014.trainable.sharedui.resources.add_cardio
import com.emanuel5014.trainable.sharedui.resources.add_workout
import com.emanuel5014.trainable.sharedui.resources.apply_filters
import com.emanuel5014.trainable.sharedui.resources.assisted_reps_label
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.clear_filters
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.delete_session
import com.emanuel5014.trainable.sharedui.resources.delete_session_message
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.edit_exercise_title
import com.emanuel5014.trainable.sharedui.resources.edit_session
import com.emanuel5014.trainable.sharedui.resources.edit_session_message
import com.emanuel5014.trainable.sharedui.resources.history_title
import com.emanuel5014.trainable.sharedui.resources.mini_set_parts_label
import com.emanuel5014.trainable.sharedui.resources.no_history_description_screen
import com.emanuel5014.trainable.sharedui.resources.no_history_yet
import com.emanuel5014.trainable.sharedui.resources.no_results_filters
import com.emanuel5014.trainable.sharedui.resources.no_routine
import com.emanuel5014.trainable.sharedui.resources.partial_reps_label
import com.emanuel5014.trainable.sharedui.resources.parts_error
import com.emanuel5014.trainable.sharedui.resources.pause_field_hint
import com.emanuel5014.trainable.sharedui.resources.pause_field_label
import com.emanuel5014.trainable.sharedui.resources.performed_technique_badge
import com.emanuel5014.trainable.sharedui.resources.performed_technique_none
import com.emanuel5014.trainable.sharedui.resources.performed_technique_title
import com.emanuel5014.trainable.sharedui.resources.reps
import com.emanuel5014.trainable.sharedui.resources.routine_notes
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.select_date_range
import com.emanuel5014.trainable.sharedui.resources.selected
import com.emanuel5014.trainable.sharedui.resources.set_duration_label
import com.emanuel5014.trainable.sharedui.resources.try_adjust_filters
import com.emanuel5014.trainable.sharedui.resources.weight_label_unit
import com.emanuel5014.trainable.sharedui.resources.workout_logs
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.glass.GlassSource
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import com.emanuel5014.trainable.data.local.entity.encodePerformed
import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.data.local.entity.isInParts
import com.emanuel5014.trainable.domain.prescription.SetPerformance
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.asKind
import com.emanuel5014.trainable.data.local.entity.kind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.relation.toJournal
import com.emanuel5014.trainable.ui.components.journal.JournalSheet
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.components.CardioAddFlow
import com.emanuel5014.trainable.ui.components.CardioEntryMode
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.ExerciseTypeSelector
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.components.NavBarAction
import com.emanuel5014.trainable.ui.components.NavBarActionEffect
import com.emanuel5014.trainable.ui.components.NavBarMenuItem
import com.emanuel5014.trainable.ui.components.NavBarPage
import com.emanuel5014.trainable.ui.components.navBarBottomClearance
import com.emanuel5014.trainable.ui.components.floatingNavFabLift
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.navigation.CompareSessions
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.DateFormatter
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import com.emanuel5014.trainable.ui.screens.routines.routineRowCorners
import com.emanuel5014.trainable.ui.screens.routines.toShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryContent(
    state: HistoryState,
    languageCode: String,
    share: HistoryShareUi,
    onEditSession: (sessionId: Int) -> Unit,
    onCompareSessions: (sessionId1: Int, sessionId2: Int) -> Unit,
    isVisible: Boolean = true
) {
    val uiState by state.uiState.collectAsState()
    val advancedProgramming = com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming.current
    // When each routine was last done, to tell routines apart in the add-workout sheet
    val lastDoneByPlan = remember(uiState.sessions) {
        AddWorkoutPlans.lastDoneByPlan(
            uiState.sessions.filter { it.session.isFinished }.map { it.session.planId to it.session.timestamp }
        )
    }

    // The workout sheet that is open, if any (its session id)
    var openSessionId by remember { mutableStateOf<Int?>(null) }
    // La pagina resta composta quando si cambia tab (beyondViewportPageCount),
    // quindi la selezione sopravvivrebbe allo spostamento: la si resetta qui,
    // sia al cambio tab che all'uscita verso le schermate di dettaglio.
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            state.clearSelection()
            openSessionId = null
        }
    }
    DisposableEffect(Unit) {
        onDispose { state.clearSelection() }
    }
    
    val listState = rememberLazyListState()

    // Quando si completa un workout la query è ORDER BY timestamp DESC,
    // quindi il più recente è in cima (index 0): se la lista cresce,
    // torna in cima automaticamente invece di costringere a scrollare a mano.
    var previousListSize by remember { mutableStateOf<Int?>(null) }
    val newestSessionId = uiState.filteredSessions.firstOrNull()?.session?.id
    LaunchedEffect(uiState.filteredSessions.size, newestSessionId) {
        val currentSize = uiState.filteredSessions.size
        val prevSize = previousListSize
        if (prevSize != null && currentSize > prevSize) {
            if (listState.firstVisibleItemIndex > 6) {
                listState.scrollToItem(0)
            } else {
                listState.animateScrollToItem(0)
            }
        }
        previousListSize = currentSize
    }

    // The list's months (computed from the filtered list, so their totals follow the filters)
    val months = remember(uiState.filteredSessions) {
        historyMonths(uiState.filteredSessions, TimeZone.currentSystemDefault())
    }
    val currentYear = remember(months) { Clock.System.todayIn(TimeZone.currentSystemDefault()).year }

    // Resolve colors at the top of the Composable
    val surfaceColor = Surface
    val onSurfaceColor = OnSurface
    val onSurfaceVariantColor = OnSurfaceVariant
    val primaryColor = Primary
    val errorColor = Error
    val surfaceContainerHighColor = SurfaceContainerHigh
    val surfaceContainerHighestColor = SurfaceContainerHighest

    var sessionToEdit by remember { mutableStateOf<SessionWithDetails?>(null) }
    var sessionToDelete by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)

    val swipeActionsEnabled by state.swipeActionsEnabled.collectAsState(initial = true)

    var isNavigating by remember { mutableStateOf(false) }
    var showCardioDialog by remember { mutableStateOf(false) }
    var showAddWorkoutSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var sessionToJournal by remember { mutableStateOf<SessionWithDetails?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.navigationEvent) {
        state.navigationEvent.collect { sessionId ->
            onEditSession(sessionId)
        }
    }

    LaunchedEffect(state.compareNavigationEvent) {
        state.compareNavigationEvent.collect { (sessionId1, sessionId2) ->
            onCompareSessions(sessionId1, sessionId2)
        }
    }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isNavigating = false
    }

    val fabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.canScrollForward
        }
    }

    PlatformBackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    // With the expressive navbar the add menu opens from the button next to it instead of floating here
    val showOwnFab = LocalNavBarStyle.current != NavBarStyle.Expressive
    val bottomClearance = navBarBottomClearance()
    val addLabel = stringResource(Res.string.add)
    val addCardioLabel = stringResource(Res.string.add_cardio)
    val addWorkoutLabel = stringResource(Res.string.add_workout)
    NavBarActionEffect(
        page = NavBarPage.History,
        action = if (uiState.isSelectionMode) null else NavBarAction.Menu(
            icon = Icons.Rounded.Add,
            label = addLabel,
            items = listOf(
                NavBarMenuItem(Icons.AutoMirrored.Rounded.DirectionsRun, addCardioLabel) { showCardioDialog = true },
                NavBarMenuItem(Icons.AutoMirrored.Rounded.Assignment, addWorkoutLabel) { showAddWorkoutSheet = true }
            )
        )
    )

    val actionGlass = rememberActionGlassSource()
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = surfaceColor,
            floatingActionButton = {
                if (showOwnFab && !uiState.isSelectionMode) {
                    FloatingActionButtonMenu(
                        modifier = Modifier.padding(bottom = 60.dp + floatingNavFabLift()).offset(x = 12.dp).zIndex(10f),
                        expanded = fabMenuExpanded,
                        button = {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    if (fabMenuExpanded) TooltipAnchorPosition.Start else TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { LocalPlatformGlass.current.NotAnOverlay(); Text(stringResource(if (fabMenuExpanded) Res.string.close else Res.string.add)) } },
                                state = rememberTooltipState()
                            ) {
                                val onToggle = {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    fabMenuExpanded = !fabMenuExpanded
                                }
                                if (actionGlass != null) {
                                    // Material's toggle button casts a shadow of its morphing shape through a clear
                                    // fill, so the glass gets a plain button of its own
                                    GlassMenuToggle(
                                        expanded = fabMenuExpanded,
                                        onClick = onToggle,
                                        glass = actionGlass
                                    )
                                } else {
                                    ToggleFloatingActionButton(
                                        checked = fabMenuExpanded,
                                        onCheckedChange = { onToggle() }
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
                        }
                    ) {
                        FloatingActionButtonMenuItem(
                            onClick = { 
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                fabMenuExpanded = false
                                showCardioDialog = true
                            },
                            modifier = Modifier.actionGlass(actionGlass, MaterialTheme.colorScheme.primaryContainer),
                            containerColor = actionGlassContainer(actionGlass, MaterialTheme.colorScheme.primaryContainer),
                            icon = { Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null) },
                            text = { Text(text = stringResource(Res.string.add_cardio)) }
                        )
                        FloatingActionButtonMenuItem(
                            onClick = { 
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                fabMenuExpanded = false
                                showAddWorkoutSheet = true
                            },
                            modifier = Modifier.actionGlass(actionGlass, MaterialTheme.colorScheme.primaryContainer),
                            containerColor = actionGlassContainer(actionGlass, MaterialTheme.colorScheme.primaryContainer),
                            icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null) },
                            text = { Text(text = stringResource(Res.string.add_workout)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .actionGlassSource(actionGlass)
                    .padding(paddingValues)
            ) {
                ScreenHeader(
                    titleContent = {
                        androidx.compose.animation.AnimatedContent(
                            targetState = uiState.isSelectionMode,
                            transitionSpec = {
                                val direction = if (targetState) 1 else -1
                                (androidx.compose.animation.slideInHorizontally { width -> direction * width / 2 } + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.EaseOutExpo)))
                                    .togetherWith(androidx.compose.animation.slideOutHorizontally { width -> -direction * width / 2 } + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.EaseOutExpo)))
                                    .using(androidx.compose.animation.SizeTransform(clip = true))
                            },
                            label = "title_anim"
                        ) { isSelection ->
                            val historyStyle = if (isSelection) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall
                            val responsiveFs = ResponsiveSize.responsiveFontSize(historyStyle.fontSize)
                            Text(
                                text = if (isSelection) {
                                    "${uiState.selectedSessionIds.size} ${stringResource(Res.string.selected)}"
                                } else {
                                    stringResource(Res.string.history_title)
                                },
                                style = historyStyle.copy(fontSize = responsiveFs),
                                color = onSurfaceColor,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (isSelection) 0.sp else (-1).sp,
                                lineHeight = if (isSelection) responsiveFs * 1.2f else responsiveFs * 1.1f,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    },
                    subtitle = if (uiState.isSelectionMode) null else stringResource(Res.string.workout_logs),
                    icon = if (uiState.isSelectionMode) null else Icons.Rounded.History,
                    actions = if (uiState.isSelectionMode) {
                        {
                            if (uiState.selectedSessionIds.size == 2) {
                                GymIconButton(
                                    icon = Icons.AutoMirrored.Rounded.CompareArrows,
                                    onClick = { state.compareSelectedSessions() },
                                    containerColor = Primary.copy(alpha = 0.1f),
                                    contentColor = Primary
                                )
                            }
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
                                onClick = { state.clearSelection() },
                                containerColor = surfaceContainerHighColor
                            )
                        }
                    } else {
                        {
                            val hasActiveFilters = uiState.filters.isActive(uiState.journalEnabled)
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

                val noRoutineLabel = stringResource(Res.string.no_routine)
                ActiveFiltersBar(
                    filters = uiState.filters,
                    planName = uiState.filters.planId?.let { id ->
                        if (id == NO_ROUTINE_PLAN_ID) noRoutineLabel
                        else uiState.availablePlans.firstOrNull { it.id == id }?.nome ?: noRoutineLabel
                    },
                    tags = uiState.availableTags,
                    journalEnabled = uiState.journalEnabled,
                    onFiltersChange = state::setFilters
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading && uiState.sessions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GymLoadingIndicator()
                        }
                    } else if (uiState.sessions.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = stringResource(Res.string.no_history_yet),
                            description = stringResource(Res.string.no_history_description_screen)
                        )
                    } else if (uiState.filteredSessions.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.FilterListOff,
                            title = stringResource(Res.string.no_results_filters),
                            description = stringResource(Res.string.try_adjust_filters),
                            action = {
                                GymButton(onClick = { state.setFilters(HistoryFilters()) }) {
                                    Text(stringResource(Res.string.clear_filters).uppercase(), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = ResponsiveSize.horizontalPadding, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {

                        months.forEachIndexed { monthIndex, month ->
                            item(key = "m-${month.year}-${month.month}", contentType = "month") {
                                Column {
                                    if (monthIndex > 0) Spacer(modifier = Modifier.height(10.dp))
                                    HistoryMonthHeader(month = month, weightUnit = uiState.weightUnit, currentYear = currentYear, languageCode = languageCode)
                                }
                            }

                            itemsIndexed(month.sessions, key = { _, s -> s.session.id }, contentType = { _, _ -> "row" }) { index, sessionDetails ->
                                val session = sessionDetails.session
                                val planName = sessionName(sessionDetails.session.noteSessione, sessionDetails.plan.nome)
                                val rowShape = routineRowCorners(index, month.sessions.size).toShape()
                                val summary = remember(sessionDetails) { historyRowSummary(sessionDetails) }

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
                                                .clip(rowShape)
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
                                    HistoryRow(
                                        sessionDetails = sessionDetails,
                                        planName = planName,
                                        summary = summary,
                                        shape = rowShape,
                                        weightUnit = uiState.weightUnit,
                                        languageCode = languageCode,
                                        exercises = uiState.exercises,
                                        showJournal = uiState.journalEnabled,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedSessionIds.contains(session.id),
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                state.toggleSessionSelection(session.id)
                                            } else {
                                                openSessionId = session.id
                                                state.loadSessionDetails(session.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.isSelectionMode) {
                                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                openSessionId = null
                                                state.toggleSessionSelection(session.id)
                                            }
                                        },
                                        onTagClick = state::filterByTag
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(bottomClearance)) }
                    }
                }
                
                // Top gradient fade to smoothly hide items when scrolling up
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(surfaceColor, Color.Transparent)
                            )
                        )
                )
            }
            }
        }

        // Preparing and previewing the image of a shared workout, where the platform can share one
        share.Overlay(languageCode, uiState.weightUnit, advancedProgramming, uiState.exercises)

        // The opened workout. It closes when its workout leaves the shown list (deleted or filtered out), when
        // selection starts or when the page is left. Composed before the dialogs and the journal sheet so they
        // open over it.
        val openDetails = openSessionId?.let { id -> uiState.filteredSessions.firstOrNull { it.session.id == id } }
        LaunchedEffect(openSessionId, openDetails != null) {
            if (openSessionId != null && openDetails == null) openSessionId = null
        }
        LaunchedEffect(uiState.isSelectionMode) {
            if (uiState.isSelectionMode) openSessionId = null
        }
        if (openDetails != null) {
            val openPlanName = sessionName(openDetails.session.noteSessione, openDetails.plan.nome)
            val details = uiState.selectedSession?.takeIf { it.session.id == openSessionId }
            WorkoutDetailSheet(
                sessionDetails = openDetails,
                details = details,
                planName = openPlanName,
                languageCode = languageCode,
                exercises = uiState.exercises,
                weightUnit = uiState.weightUnit,
                showJournal = uiState.journalEnabled,
                showEdit = !swipeActionsEnabled,
                onShare = if (!share.available || details == null || details.sets.isEmpty()) null else {
                    {
                        // Closed first, so the share preview is not hidden behind the sheet
                        openSessionId = null
                        scope.launch {
                            val planExercises = state.loadPlanExercises(details.session.planId)
                            share.share(details, openPlanName, planExercises)
                        }
                    }
                },
                onEdit = { sessionToEdit = openDetails },
                onEditJournal = { sessionToJournal = openDetails },
                onTagClick = { tagId ->
                    // Closed first: the workout carries the tag, so it stays in the filtered list and the sheet would cover it
                    openSessionId = null
                    state.filterByTag(tagId)
                },
                onRestoreExercise = state::restoreExercise,
                onDismiss = { openSessionId = null }
            )
        }

        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                title = { Text(stringResource(Res.string.delete_session)) },
                text = { Text(stringResource(Res.string.delete_session_message)) },
                confirmButton = {
                    GymButton(
                        onClick = {
                            state.deleteSelectedSessions()
                            showBulkDeleteDialog = false
                        },
                        containerColor = errorColor.copy(alpha = 0.1f),
                        contentColor = errorColor
                    ) {
                        Text(stringResource(Res.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { showBulkDeleteDialog = false },
                        containerColor = Color.Transparent,
                        contentColor = onSurfaceVariantColor
                    ) {
                        Text(stringResource(Res.string.cancel).uppercase())
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
                title = { Text(stringResource(Res.string.delete_session)) },
                text = { Text(stringResource(Res.string.delete_session_message)) },
                confirmButton = {
                    GymButton(
                        onClick = {
                            sessionToDelete?.let { state.deleteSession(it.id) }
                            sessionToDelete = null
                        },
                        containerColor = errorColor.copy(alpha = 0.1f),
                        contentColor = errorColor
                    ) {
                        Text(stringResource(Res.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { sessionToDelete = null },
                        containerColor = Color.Transparent,
                        contentColor = onSurfaceVariantColor
                    ) {
                        Text(stringResource(Res.string.cancel).uppercase())
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
                title = { Text(stringResource(Res.string.edit_session)) },
                text = { Text(stringResource(Res.string.edit_session_message)) },
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
                            Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                        GymButton(
                            onClick = {
                                sessionToEdit?.let { details ->
                                    isNavigating = true
                                    openSessionId = null
                                    onEditSession(details.session.id)
                                }
                                sessionToEdit = null
                            },
                            containerColor = primaryColor.copy(alpha = 0.15f),
                            contentColor = primaryColor
                        ) {
                            Text(stringResource(Res.string.edit).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                },
                dismissButton = {},
                containerColor = surfaceContainerHighColor,
                titleContentColor = onSurfaceColor,
                textContentColor = onSurfaceVariantColor
            )
        }

        sessionToJournal?.let { target ->
            JournalSheet(
                initial = target.toJournal(),
                onSave = {
                    state.saveJournal(target.session.id, it)
                    sessionToJournal = null
                },
                onDismiss = { sessionToJournal = null },
                hapticEnabled = hapticEnabled
            )
        }

        if (showFilterSheet) {
            HistoryFilterBottomSheet(
                selectedPlanId = uiState.filters.planId,
                availablePlans = uiState.availablePlans,
                hasUnassignedSessions = uiState.sessions.any { it.plan.isSystemPlan },
                startDate = uiState.filters.startDate,
                endDate = uiState.filters.endDate,
                onPlanSelected = { planId ->
                    state.setFilters(uiState.filters.copy(planId = planId))
                },
                onDateRangeSelected = { start, end ->
                    state.setFilters(uiState.filters.copy(startDate = start, endDate = end))
                },
                onDateClick = { showDatePicker = true },
                onClearDate = {
                    state.setFilters(uiState.filters.copy(startDate = null, endDate = null))
                },
                onClearAll = {
                    state.setFilters(HistoryFilters())
                },
                onDismiss = { showFilterSheet = false },
                journalEnabled = uiState.journalEnabled,
                ratingEnabled = uiState.journalRatingEnabled,
                availableTags = uiState.availableTags,
                journalFilters = uiState.filters.journal,
                onJournalFiltersChanged = { state.setFilters(uiState.filters.copy(journal = it)) }
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
                            state.setFilters(uiState.filters.copy(startDate = start, endDate = end))
                        }
                        showDatePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text(stringResource(Res.string.apply_filters).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = onSurfaceVariantColor
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = surfaceContainerHighColor
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 560.dp).fillMaxHeight(0.85f),
                title = {
                    Text(
                        text = stringResource(Res.string.select_date_range),
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
            AddWorkoutSheetContent(
                plans = uiState.availablePlans,
                summaries = uiState.planSummaries,
                lastDoneByPlan = lastDoneByPlan,
                languageCode = languageCode,
                onPlanSelected = { planId ->
                    state.addManualWorkout(planId)
                    showAddWorkoutSheet = false
                },
                onEmptyWorkoutClick = {
                    state.addEmptyWorkout()
                    showAddWorkoutSheet = false
                }
            )
        }
    }

    if (showCardioDialog) {
        // Picker (cardio only) then the entry sheet; the workout lands on the day and time chosen there
        CardioAddFlow(
            mode = CardioEntryMode.ADD_FROM_HISTORY,
            exercises = uiState.exercises.values.toList(),
            languageCode = languageCode,
            loadSuggestions = { state.cardioSuggestions() },
            onAddCustomExercise = { name, category, onCreated -> state.addCustomExercise(name, category, onCreated) },
            isNameOfHiddenNonCardio = { state.isNameOfHiddenNonCardio(it) },
            onSave = { exercise, entry ->
                state.saveCardioWorkout(
                    exercise,
                    entry.distanceKm,
                    entry.seconds,
                    entry.timestamp ?: Clock.System.now().toEpochMilliseconds(),
                    entry.note
                )
                showCardioDialog = false
            },
            onDismiss = { showCardioDialog = false }
        )
    }
}
}


@Composable
fun EditSetDialog(
    set: SetLogEntity,
    weightUnit: String = "kg",
    isNewSet: Boolean = false,
    setPerformanceEnabled: Boolean = false,
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
    var seconds by remember { mutableStateOf(set.durataSecondi?.toString() ?: "45") }
    var note by remember { mutableStateOf(set.note ?: "") }
    var kind by remember { mutableStateOf(set.kind) }
    var rpe by remember { mutableStateOf(set.rpe) }
    // How the set really went
    var partsText by remember { mutableStateOf(set.parts.joinToString("+")) }
    // The pause (seconds) after each part but the last, kept as typed so a half-typed parts field loses nothing
    var pauseTexts by remember { mutableStateOf(set.pauses.map { it.toString() }) }
    var assisted by remember { mutableStateOf(set.assistedReps.takeIf { it > 0 }?.toString().orEmpty()) }
    var partial by remember { mutableStateOf(set.partialReps.takeIf { it > 0 }?.toString().orEmpty()) }
    var performed by remember { mutableStateOf(set.performed) }
    var showTechniqueDialog by remember { mutableStateOf(false) }
    val plannedTechniques = remember(set) { com.emanuel5014.trainable.domain.prescription.TechniqueCodec.decode(set.techniques) }
    val parsedParts = partsText.takeIf { it.isNotBlank() }?.let { SetPerformance.parseParts(it) }
    val partsInvalid = partsText.isNotBlank() && parsedParts == null
    // With the setting off a set carries only its note, but one saved with these details can still fix them
    val showExtras = remember(set) {
        setPerformanceEnabled || set.isInParts || set.assistedReps > 0 || set.partialReps > 0 || set.performedTechniques != null
    }
    if (showTechniqueDialog) {
        com.emanuel5014.trainable.ui.screens.workout.PerformedTechniqueDialog(
            planned = plannedTechniques,
            performed = performed,
            onDismiss = { showTechniqueDialog = false },
            onSave = { techniques, _ ->
                performed = techniques
                showTechniqueDialog = false
            },
            showApplyToFollowing = false
        )
    }

    // A Time & Reps set has no weight to fill in
    val isValid = !partsInvalid && (kind == SetLogKind.TIME_AND_REPS || weight.isNotBlank()) && when (kind) {
        SetLogKind.STRENGTH -> reps.isNotBlank()
        SetLogKind.TIME_AND_WEIGHT -> seconds.isNotBlank()
        SetLogKind.TIME_AND_REPS -> seconds.isNotBlank() && reps.isNotBlank()
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_exercise_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExerciseTypeSelector(
                    selectedType = when (kind) {
                        SetLogKind.STRENGTH -> "strength"
                        SetLogKind.TIME_AND_WEIGHT -> TIME_AND_WEIGHT
                        SetLogKind.TIME_AND_REPS -> TIME_AND_REPS
                    },
                    onTypeSelected = { type ->
                        kind = SetLogKind.ofExerciseType(type)
                        if (kind.isTimed && seconds.isBlank()) seconds = "45"
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (kind != SetLogKind.TIME_AND_REPS) GymInputField(
                    value = weight,
                    onValueChange = { newValue ->
                        weight = newValue.replace(",", ".")
                    },
                    label = stringResource(Res.string.weight_label_unit, weightUnit),
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
                if (kind.isTimed) {
                    GymInputField(
                        value = seconds,
                        onValueChange = { seconds = it.filter { char -> char.isDigit() } },
                        label = stringResource(Res.string.set_duration_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (kind == SetLogKind.TIME_AND_REPS) {
                    GymInputField(
                        value = reps,
                        onValueChange = { reps = it.filter { char -> char.isDigit() } },
                        label = stringResource(Res.string.reps),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else if (!kind.isTimed) {
                    GymInputField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = stringResource(Res.string.reps),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        // A set in parts has their sum as its reps
                        enabled = (parsedParts?.size ?: 0) < 2
                    )
                    if (showExtras) {
                        GymInputField(
                            value = partsText,
                            onValueChange = { text ->
                                partsText = text.filter { it.isDigit() || it == '+' || it == ' ' }
                                SetPerformance.parseParts(partsText)?.takeIf { it.size > 1 }?.let { reps = it.sum().toString() }
                            },
                            label = stringResource(Res.string.mini_set_parts_label),
                            supportingText = if (partsInvalid) stringResource(Res.string.parts_error) else null,
                            isError = partsInvalid
                        )
                        if (parsedParts != null && parsedParts.size > 1) {
                            PartPausesFields(
                                parts = parsedParts,
                                texts = pauseTexts,
                                onTextChange = { index, value ->
                                    val gaps = parsedParts.size - 1
                                    pauseTexts = List(gaps) { i -> if (i == index) value else pauseTexts.getOrElse(i) { "" } }
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            GymInputField(
                                value = assisted,
                                onValueChange = { assisted = it.filter { c -> c.isDigit() }.take(2) },
                                label = stringResource(Res.string.assisted_reps_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            GymInputField(
                                value = partial,
                                onValueChange = { partial = it.filter { c -> c.isDigit() }.take(2) },
                                label = stringResource(Res.string.partial_reps_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (plannedTechniques.isNotEmpty()) {
                            val techniqueText = LocalPrescriptionText.current
                            androidx.compose.material3.TextButton(onClick = { showTechniqueDialog = true }) {
                                Text(
                                    text = performed?.let { list ->
                                        if (list.isEmpty()) stringResource(Res.string.performed_technique_none)
                                        else stringResource(
                                            Res.string.performed_technique_badge,
                                            list.joinToString(" · ") { techniqueText.technique(it) }
                                        )
                                    } ?: stringResource(Res.string.performed_technique_title),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                com.emanuel5014.trainable.ui.components.editableRpeScale(hasValue = set.rpe != null)?.let { scale ->
                    com.emanuel5014.trainable.ui.components.RpeSelector(value = rpe, onValueChange = { rpe = it }, scale = scale)
                }
                GymInputField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(Res.string.routine_notes),
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
                        stringResource(Res.string.cancel).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
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
                            repsEffettive = if (kind == SetLogKind.TIME_AND_WEIGHT) 0 else (reps.toIntOrNull() ?: 0),
                            durataSecondi = if (kind.isTimed) (seconds.toIntOrNull() ?: 45) else null,
                            note = note.ifBlank { null },
                            rpe = rpe
                        ).asKind(kind).let { edited ->
                            if (kind.isTimed) edited.copy(repParts = null, intraRests = null, assistedReps = 0, partialReps = 0)
                            else {
                                val parts = parsedParts?.takeIf { it.size > 1 }
                                edited.copy(
                                    repParts = parts?.let { SetPerformance.encodeParts(it) },
                                    repsEffettive = parts?.sum() ?: edited.repsEffettive,
                                    intraRests = parts?.let { SetPerformance.encodePauses(SetPerformance.pausesFromInput(pauseTexts, it.size - 1)) },
                                    assistedReps = assisted.toIntOrNull() ?: 0,
                                    partialReps = partial.toIntOrNull() ?: 0,
                                    performedTechniques = encodePerformed(performed)
                                )
                            }
                        }
                        onConfirm(updatedSet)
                    },
                    enabled = isValid,
                    height = 48,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(Res.string.save).uppercase(),
                        fontWeight = FontWeight.Black,
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

/** One field per pause between the parts of a set done in mini-sets, in seconds (empty = not recorded). */
@Composable
private fun PartPausesFields(parts: List<Int>, texts: List<String>, onTextChange: (Int, String) -> Unit) {
    // One or two pauses get the width of the dialog; more of them go three to a row
    val gaps = parts.size - 1
    val columns = if (gaps <= 2) gaps else 3
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        (0 until gaps).chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                row.forEach { index ->
                    GymInputField(
                        value = texts.getOrElse(index) { "" },
                        onValueChange = { onTextChange(index, it.filter { c -> c.isDigit() }.take(4)) },
                        label = stringResource(Res.string.pause_field_label, index + 1),
                        supportingText = stringResource(Res.string.pause_field_hint, parts[index], parts[index + 1]),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                // A short last row keeps the fields as wide as the full ones
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The add button of the history menu in Liquid Glass: tinted glass that turns its plus into a cross when open. */
@Composable
private fun GlassMenuToggle(expanded: Boolean, onClick: () -> Unit, glass: GlassSource) {
    val tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val content = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "menuToggleRotation")
    Box(
        modifier = Modifier
            .size(56.dp)
            .actionGlass(glass, tint, cornerRadius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(if (expanded) Res.string.close else Res.string.add),
            tint = content,
            modifier = Modifier.rotate(rotation)
        )
    }
}

package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassElevation
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.active_routines_tab
import com.emanuel5014.trainable.sharedui.resources.archive
import com.emanuel5014.trainable.sharedui.resources.archive_routine
import com.emanuel5014.trainable.sharedui.resources.archive_routine_message
import com.emanuel5014.trainable.sharedui.resources.archived_appear_here
import com.emanuel5014.trainable.sharedui.resources.archived_routines
import com.emanuel5014.trainable.sharedui.resources.archived_routines_tab
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.confirm
import com.emanuel5014.trainable.sharedui.resources.create
import com.emanuel5014.trainable.sharedui.resources.create_routine
import com.emanuel5014.trainable.sharedui.resources.created_on
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.delete_routine
import com.emanuel5014.trainable.sharedui.resources.delete_routine_message
import com.emanuel5014.trainable.sharedui.resources.expiration_date
import com.emanuel5014.trainable.sharedui.resources.expires
import com.emanuel5014.trainable.sharedui.resources.new_routine
import com.emanuel5014.trainable.sharedui.resources.no_archived
import com.emanuel5014.trainable.sharedui.resources.no_routines
import com.emanuel5014.trainable.sharedui.resources.reset
import com.emanuel5014.trainable.sharedui.resources.routine_name
import com.emanuel5014.trainable.sharedui.resources.routine_notes
import com.emanuel5014.trainable.sharedui.resources.schedule_days
import com.emanuel5014.trainable.sharedui.resources.selected
import com.emanuel5014.trainable.sharedui.resources.share
import com.emanuel5014.trainable.sharedui.resources.share_option_maximums
import com.emanuel5014.trainable.sharedui.resources.share_option_maximums_desc
import com.emanuel5014.trainable.sharedui.resources.share_option_photos
import com.emanuel5014.trainable.sharedui.resources.share_option_photos_desc
import com.emanuel5014.trainable.sharedui.resources.share_options_title
import com.emanuel5014.trainable.sharedui.resources.start_date
import com.emanuel5014.trainable.sharedui.resources.tap_to_create
import com.emanuel5014.trainable.sharedui.resources.tap_to_set
import com.emanuel5014.trainable.sharedui.resources.training_plans
import com.emanuel5014.trainable.sharedui.resources.unarchive
import com.emanuel5014.trainable.sharedui.resources.unarchive_routine
import com.emanuel5014.trainable.sharedui.resources.unarchive_routine_message
import com.emanuel5014.trainable.sharedui.resources.your_routines
import com.emanuel5014.trainable.sharedui.resources.routine_group_action
import com.emanuel5014.trainable.sharedui.resources.routine_group_none
import com.emanuel5014.trainable.sharedui.resources.routine_group_routines
import com.emanuel5014.trainable.sharedui.resources.routine_group_since
import com.emanuel5014.trainable.ui.screens.history.MonthCalendar
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material.icons.rounded.CreateNewFolder
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.components.LocalPlanImage
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import com.emanuel5014.trainable.ui.util.narrowDayName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.components.NavBarAction
import com.emanuel5014.trainable.ui.components.NavBarActionEffect
import com.emanuel5014.trainable.ui.components.NavBarPage
import com.emanuel5014.trainable.ui.components.navBarBottomClearance
import com.emanuel5014.trainable.ui.components.floatingNavFabLift
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineListContent(
    state: RoutinesState,
    languageCode: String,
    sharePlans: SharePlans,
    onNavigateToDetail: (Int) -> Unit,
    onGenerateReport: (List<Int>) -> Unit = {},
    onSwipingItemChange: ((Boolean) -> Unit)? = null,
    isVisible: Boolean = true
) {
    val uiState by state.uiState.collectAsState()
    val weekCollapsed by state.weekCollapsed.collectAsState()
    val exportPrompt by state.exportPrompt.collectAsState()

    // La pagina resta composta quando si cambia tab (beyondViewportPageCount),
    // quindi la selezione sopravvivrebbe allo spostamento: la si resetta qui,
    // sia al cambio tab che all'uscita verso le schermate di dettaglio.
    LaunchedEffect(isVisible) {
        if (!isVisible) state.clearSelection()
    }
    DisposableEffect(Unit) {
        onDispose { state.clearSelection() }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)
    
    val swipeActionsEnabled by state.swipeActionsEnabled.collectAsState(initial = true)
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    // One scroll state per tab, so "Create routine" can shrink to its icon once the visible list leaves the top,
    // like "Start" in the routine detail
    val activeListState = rememberLazyListState()
    val archivedListState = rememberLazyListState()
    val createFabExpanded by remember {
        derivedStateOf {
            !(if (pagerState.currentPage == 0) activeListState else archivedListState).canScrollBackward
        }
    }
    val isCurrentlyArchived by remember { derivedStateOf { pagerState.currentPage == 1 } }
    val coroutineScope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkArchiveDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var planToArchive by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var showGroupSheet by remember { mutableStateOf(false) }
    // The group whose ⋮ was tapped, and whether on the Archived tab
    var groupMenu by remember { mutableStateOf<Pair<RoutineGroupEntity, Boolean>?>(null) }
    var groupToRename by remember { mutableStateOf<RoutineGroupEntity?>(null) }
    var groupToRecolor by remember { mutableStateOf<RoutineGroupEntity?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    fun openCreateSheet() {
        routineName = ""
        routineNote = ""
        startDate = Clock.System.now().toEpochMilliseconds()
        endDate = null
        selectedDays.clear()
        showSheet = true
    }

    // With the expressive navbar the main action lives next to it instead of floating here
    val showOwnFab = LocalNavBarStyle.current != NavBarStyle.Expressive
    val shareLabel = stringResource(Res.string.share)
    val createLabel = stringResource(Res.string.create_routine)
    NavBarActionEffect(
        page = NavBarPage.Routines,
        action = if (uiState.isSelectionMode) {
            NavBarAction.Click(Icons.Rounded.Share, shareLabel) { state.shareSelectedPlans(sharePlans) }
        } else {
            NavBarAction.Click(Icons.Default.Add, createLabel) { openCreateSheet() }
        }
    )

    val actionGlass = rememberActionGlassSource()
    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            val fabPadding = if (uiState.floatingNavBar) {
                if (ResponsiveSize.isCompact) 2.dp else 8.dp
            } else 0.dp
            
            if (showOwnFab && uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { state.shareSelectedPlans(sharePlans) },
                    containerColor = actionGlassContainer(actionGlass, Primary),
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    elevation = actionGlassElevation(actionGlass),
                    modifier = Modifier
                        .padding(end = fabPadding)
                        .padding(bottom = 80.dp + floatingNavFabLift())
                        .actionGlass(actionGlass, Primary)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(Res.string.share))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.share).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            } else if (showOwnFab) {
                ExtendedFloatingActionButton(
                    onClick = { openCreateSheet() },
                    expanded = createFabExpanded,
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.create)) },
                    text = { Text(stringResource(Res.string.create_routine).replace("CREATE ", ""), fontWeight = FontWeight.ExtraBold) },
                    containerColor = actionGlassContainer(actionGlass, Primary),
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    elevation = actionGlassElevation(actionGlass),
                    modifier = Modifier
                        .padding(end = fabPadding)
                        .padding(bottom = 80.dp + floatingNavFabLift())
                        .actionGlass(actionGlass, Primary)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .actionGlassSource(actionGlass)
                .padding(paddingValues)
        ) {
            // Header Section
            ScreenHeader(
                titleContent = {
                    AnimatedContent(
                        targetState = if (uiState.isSelectionMode) -1 else pagerState.currentPage,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { width -> direction * width / 2 } + fadeIn(animationSpec = tween(400, easing = EaseOutExpo)))
                                .togetherWith(slideOutHorizontally { width -> -direction * width / 2 } + fadeOut(animationSpec = tween(400, easing = EaseOutExpo)))
                                .using(SizeTransform(clip = true))
                        },
                        label = "title_anim"
                    ) { state ->
                        val routineStyle = if (uiState.isSelectionMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall
                        val responsiveFs = ResponsiveSize.responsiveFontSize(routineStyle.fontSize)
                        Text(
                            text = when (state) {
                                -1 -> "${uiState.selectedPlanIds.size} ${stringResource(Res.string.selected)}"
                                0 -> stringResource(Res.string.your_routines)
                                else -> stringResource(Res.string.archived_routines)
                            },
                            style = routineStyle.copy(fontSize = responsiveFs),
                            color = OnSurface,
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (uiState.isSelectionMode) 0.sp else (-1).sp,
                            lineHeight = if (uiState.isSelectionMode) responsiveFs * 1.2f else responsiveFs * 1.1f,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                },
                subtitle = if (uiState.isSelectionMode) null else stringResource(Res.string.training_plans),
                icon = if (uiState.isSelectionMode) null else Icons.AutoMirrored.Rounded.Notes,
                navigationIcon = null,
                actions = if (uiState.isSelectionMode) {
                    {
                        if (!swipeActionsEnabled) {
                            GymIconButton(
                                icon = if (pagerState.currentPage == 0) Icons.Rounded.Archive else Icons.Rounded.Unarchive,
                                onClick = { showBulkArchiveDialog = true },
                                containerColor = SurfaceContainerHigh,
                                contentColor = Primary
                            )
                            GymIconButton(
                                icon = Icons.Rounded.DeleteSweep,
                                onClick = { showBulkDeleteDialog = true },
                                containerColor = SurfaceContainerHigh,
                                contentColor = Error
                            )
                        }
                        GymIconButton(
                            icon = Icons.Rounded.CreateNewFolder,
                            onClick = { showGroupSheet = true },
                            containerColor = SurfaceContainerHigh,
                            contentColor = Primary,
                            description = stringResource(Res.string.routine_group_action)
                        )
                        GymIconButton(
                            icon = Icons.Rounded.Assessment,
                            onClick = { onGenerateReport(uiState.selectedPlanIds.toList()) },
                            containerColor = SurfaceContainerHigh,
                            contentColor = Primary
                        )
                        GymIconButton(
                            icon = Icons.Rounded.Close,
                            onClick = { state.clearSelection() },
                            containerColor = SurfaceContainerHigh
                        )
                    }
                } else null,
                titleInRow = uiState.isSelectionMode,
                modifier = if (uiState.isSelectionMode) Modifier.padding(top = 8.dp) else Modifier
            )

            // M3 Expressive segmented tabs — pill guidata dal pager per un glide sincronizzato
            if (LocalGlassSettings.current.isOn(GlassPart.Tabs) && LocalPlatformGlass.current.hasSegmentedTabs) {
                LocalPlatformGlass.current.SegmentedTabs(
                    titles = listOf(
                        stringResource(Res.string.active_routines_tab),
                        stringResource(Res.string.archived_routines_tab)
                    ),
                    selectedIndex = pagerState.currentPage,
                    progress = { (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f) },
                    onSelect = { index ->
                        if (pagerState.currentPage != index) {
                            coroutineScope.launch {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    labelSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.labelLarge.fontSize),
                    // Dragging the capsule pages the lists with it; letting go settles on the nearest tab
                    onScrub = { position ->
                        val pageSize = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
                        val now = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        pagerState.dispatchRawDelta((position - now) * pageSize)
                    },
                    onScrubEnd = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    onTick = { if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    modifier = Modifier
                        .padding(horizontal = ResponsiveSize.horizontalPadding)
                        .padding(bottom = 8.dp)
                )
            } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ResponsiveSize.horizontalPadding)
                    .padding(bottom = 8.dp)
                    .clip(Shapes.large)
                    .background(SurfaceContainerHigh)
                    .padding(4.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val indicatorWidth = maxWidth / 2
                    // Progresso continuo 0..1: segue animateScrollToPage (spring di default)
                    // così la pill resta sincronizzata con il parallax delle pagine sotto.
                    val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, 1f)
                    // 0 quando è ferma su un tab, 1 a metà strada: serve per lo squash & stretch
                    val transit = (kotlin.math.abs(progress - kotlin.math.round(progress)) * 2f)
                        .coerceIn(0f, 1f)
                    val pillScaleX = 1f + 0.10f * transit
                    val pillScaleY = 1f - 0.05f * transit

                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(44.dp)
                            .offset(x = indicatorWidth * progress)
                            .graphicsLayer {
                                scaleX = pillScaleX
                                scaleY = pillScaleY
                            }
                            .shadow(2.dp, Shapes.medium, clip = false)
                            .clip(Shapes.medium)
                            .background(Surface)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        stringResource(Res.string.active_routines_tab),
                        stringResource(Res.string.archived_routines_tab)
                    ).forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Primary else OnSurfaceVariant,
                            animationSpec = tween(durationMillis = 250),
                            label = "tab_content"
                        )
                        val textScale by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.94f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 500f
                            ),
                            label = "tab_scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(Shapes.medium)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.Tab,
                                    onClick = {
                                        if (pagerState.currentPage != index) {
                                            coroutineScope.launch {
                                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.labelLarge.fontSize)),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = contentColor,
                                letterSpacing = 1.sp,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = textScale
                                    scaleY = textScale
                                }
                            )
                        }
                    }
                }
            }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = false
                ) { page ->
                    val isArchivedPage = page == 1
                    val plans = if (isArchivedPage) uiState.archivedPlans else uiState.plans
                    val sections = remember(plans, uiState.groups) { routineSections(plans, uiState.groups) }
                    
                    // M3 Expressive: Page transformation based on scroll progress
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val progress = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f - progress)
                            .graphicsLayer {
                                // Dynamic parallax glide effect
                                translationX = pageOffset * (size.width * 0.3f)
                                
                                // Premium scale and alpha transitions
                                val scale = 1f - (progress * 0.08f)
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - progress
                            }
                    ) {
                        RoutineListPage(
                            plans = plans,
                            sections = sections,
                            lastSessionTimes = uiState.lastSessionTimes,
                            isArchived = isArchivedPage,
                            onNavigateToDetail = onNavigateToDetail,
                            onDelete = { planToDelete = it },
                            onArchiveToggle = { planToArchive = it },
                            onSwap = { planId, otherId -> state.swapPlans(planId, otherId) },
                            onGroupMenu = { groupMenu = it to isArchivedPage },
                            isLoading = uiState.isLoading,
                            isSelectionMode = uiState.isSelectionMode,
                            swipeActionsEnabled = swipeActionsEnabled,
                            selectedPlanIds = uiState.selectedPlanIds,
                            onToggleSelection = { state.togglePlanSelection(it) },
                            languageCode = languageCode,
                            listState = if (isArchivedPage) archivedListState else activeListState,
                            // "Your week": active routines only, when some have days. It stays in the list while
                            // selecting (only hidden), so the list does not lose its place when it comes back.
                            header = if (!isArchivedPage && hasAssignedDays(plans) && weekCollapsed != null) {
                                {
                                    RoutineWeekCard(
                                        plans = plans,
                                        sections = sections,
                                        collapsed = weekCollapsed == true,
                                        onToggle = { state.setWeekCollapsed(weekCollapsed != true) },
                                        onOpenPlan = onNavigateToDetail
                                    )
                                }
                            } else null,
                            headerVisible = !uiState.isSelectionMode
                        )
                    }
                }
                
                // Top gradient fade to smoothly hide items when scrolling up
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Surface, androidx.compose.ui.graphics.Color.Transparent)
                            )
                        )
                )
            }
        }
    }

    if (showGroupSheet) {
        val all = uiState.plans + uiState.archivedPlans
        val selected = all.filter { it.plan.id in uiState.selectedPlanIds }
        val choices = uiState.groups.mapNotNull { group ->
            val members = all.filter { it.plan.groupId == group.id }
            when {
                members.isEmpty() -> null
                // Already all in it: nothing to move
                selected.all { it.plan.groupId == group.id } -> null
                else -> GroupChoice(group, members.size, archived = members.none { it.plan.isActive })
            }
        }
        RoutineGroupSheet(
            selectedNames = selected.map { it.plan.nome },
            choices = choices,
            canUngroup = selected.any { it.plan.groupId != null },
            defaultColorRole = nextGroupColorRole(uiState.groups),
            onCreate = { name, role, custom ->
                state.groupSelectedPlans(name, role, custom)
                showGroupSheet = false
            },
            onMoveTo = { groupId ->
                state.moveSelectedPlansToGroup(groupId)
                showGroupSheet = false
            },
            onDismiss = { showGroupSheet = false }
        )
    }

    groupMenu?.let { (group, archivedTab) ->
        val all = uiState.plans + uiState.archivedPlans
        // Neighbours as this tab shows them, so "move up" never swaps with a group the tab does not show
        val shown = routineSections(if (archivedTab) uiState.archivedPlans else uiState.plans, uiState.groups)
            .mapNotNull { it.group }
        val index = shown.indexOfFirst { it.id == group.id }
        RoutineGroupMenuSheet(
            group = group,
            names = all.filter { it.plan.groupId == group.id }.joinToString(" · ") { it.plan.nome },
            archivedTab = archivedTab,
            canMoveUp = index > 0,
            canMoveDown = index >= 0 && index < shown.lastIndex,
            onRename = {
                groupMenu = null
                groupToRename = group
            },
            onChangeColor = {
                groupMenu = null
                groupToRecolor = group
            },
            onMove = { up ->
                shown.getOrNull(if (up) index - 1 else index + 1)?.let { state.swapGroups(group, it) }
                groupMenu = null
            },
            onArchive = {
                state.setGroupArchived(group, archived = !archivedTab)
                groupMenu = null
            },
            onDissolve = {
                state.dissolveGroup(group)
                groupMenu = null
            },
            onDismiss = { groupMenu = null }
        )
    }

    groupToRename?.let { group ->
        RoutineGroupRenameDialog(
            initialName = group.name,
            onDismiss = { groupToRename = null },
            onSave = { name ->
                state.renameGroup(group, name)
                groupToRename = null
            }
        )
    }

    groupToRecolor?.let { group ->
        RoutineGroupColorDialog(
            group = group,
            onDismiss = { groupToRecolor = null },
            onPick = { role, custom ->
                state.setGroupColor(group, role, custom)
                groupToRecolor = null
            }
        )
    }

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text(stringResource(Res.string.delete_routine)) },
            text = { Text(stringResource(Res.string.delete_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        state.deleteSelectedPlans()
                        showBulkDeleteDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkDeleteDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showBulkArchiveDialog) {
        val isArchiving = pagerState.currentPage == 0
        AlertDialog(
            onDismissRequest = { showBulkArchiveDialog = false },
            title = { Text(stringResource(if (isArchiving) Res.string.archive_routine else Res.string.unarchive_routine)) },
            text = { Text(stringResource(if (isArchiving) Res.string.archive_routine_message else Res.string.unarchive_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        if (isArchiving) state.archiveSelectedPlans()
                        else state.unarchiveSelectedPlans()
                        showBulkArchiveDialog = false
                    },
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(if (isArchiving) Res.string.archive else Res.string.unarchive).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkArchiveDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text(stringResource(Res.string.delete_routine)) },
            text = { Text(stringResource(Res.string.delete_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        planToDelete?.let { state.deletePlan(it) }
                        planToDelete = null
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { planToDelete = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (planToArchive != null) {
        val isArchiving = !isCurrentlyArchived
        AlertDialog(
            onDismissRequest = { planToArchive = null },
            title = { Text(stringResource(if (isArchiving) Res.string.archive_routine else Res.string.unarchive_routine)) },
            text = { Text(stringResource(if (isArchiving) Res.string.archive_routine_message else Res.string.unarchive_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        planToArchive?.let { state.toggleArchive(it) }
                        planToArchive = null
                    },
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(if (isArchiving) Res.string.archive else Res.string.unarchive).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { planToArchive = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Surface,
            tonalElevation = 0.dp,
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
            // This sheet has its own form and date pickers; the blur behind it was not worth the trouble
            LocalPlatformGlass.current.NoOverlayBlur()
            SheetFormLayout {
                SheetFormBody(horizontalPadding = ResponsiveSize.horizontalPadding, spacing = 24.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.create_routine),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(Res.string.new_routine),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                            color = OnSurface,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GymInputField(
                            value = routineName,
                            onValueChange = { routineName = it },
                            label = stringResource(Res.string.routine_name),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }) {
                                GymInputField(
                                    value = com.emanuel5014.trainable.ui.util.DateFormatter.format(startDate),
                                    onValueChange = {},
                                    label = stringResource(Res.string.start_date).replace(":", ""),
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }) {
                                GymInputField(
                                    value = endDate?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(Res.string.tap_to_set),
                                    onValueChange = {},
                                    label = stringResource(Res.string.expiration_date),
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (endDate != null) Primary else OnSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(Res.string.schedule_days),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                DayOfWeek.entries.forEachIndexed { index, day ->
                                    val isSelected = selectedDays.contains(day)
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (isSelected) selectedDays.remove(day)
                                            else selectedDays.add(day)
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(
                                            text = narrowDayName(day),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.bodyLarge.fontSize)),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        GymInputField(
                            value = routineNote,
                            onValueChange = { routineNote = it },
                            label = stringResource(Res.string.routine_notes),
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                SheetFormFooter(horizontalPadding = ResponsiveSize.horizontalPadding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymButton(
                            onClick = { showSheet = false },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurfaceVariant
                        ) {
                            Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    
                        GymButton(
                            onClick = {
                                val trimmedName = routineName.trim()
                                if (trimmedName.isNotEmpty()) {
                                    val note = routineNote.trim().takeIf { it.isNotBlank() }
                                    val daysString = if (selectedDays.isEmpty()) null 
                                                   else selectedDays.sortedBy { it.isoDayNumber }.joinToString(",") { it.isoDayNumber.toString() }
                                    state.createEmptyPlan(
                                        name = trimmedName,
                                        note = note,
                                        giorniSettimana = daysString,
                                        dataInizio = startDate,
                                        dataFine = endDate
                                    )
                                    showSheet = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(Res.string.create).uppercase(),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: Clock.System.now().toEpochMilliseconds())

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showStartDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        endDate = endDatePickerState.selectedDateMillis
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = {
                        endDate = null
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Error
                ) {
                    Text(stringResource(Res.string.reset).uppercase())
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    exportPrompt?.let { prompt ->
        ExportOptionsDialog(
            prompt = prompt,
            onConfirm = { includeImages, includeMaximums ->
                state.exportSelectedPlans(sharePlans, includeImages, includeMaximums)
            },
            onDismiss = { state.dismissExportPrompt() }
        )
    }
}

/** Asks what to put in the shared file besides the routine itself. */
@Composable
private fun ExportOptionsDialog(
    prompt: RoutinesState.ExportPrompt,
    onConfirm: (includeImages: Boolean, includeMaximums: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var includeImages by remember { mutableStateOf(prompt.hasImages) }
    var includeMaximums by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.share_options_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (prompt.hasImages) {
                    ExportOptionRow(
                        title = stringResource(Res.string.share_option_photos),
                        description = stringResource(Res.string.share_option_photos_desc),
                        checked = includeImages,
                        onCheckedChange = { includeImages = it }
                    )
                }
                if (prompt.hasMaximums) {
                    ExportOptionRow(
                        title = stringResource(Res.string.share_option_maximums),
                        description = stringResource(Res.string.share_option_maximums_desc),
                        checked = includeMaximums,
                        onCheckedChange = { includeMaximums = it }
                    )
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = { onConfirm(includeImages && prompt.hasImages, includeMaximums && prompt.hasMaximums) },
                modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
            ) {
                Text(stringResource(Res.string.share).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(Res.string.cancel).uppercase())
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

@Composable
private fun ExportOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.ExtraBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RoutineListPage(
    plans: List<PlanWithDetails>,
    /** [plans] split by group (see [routineSections]); one section without a group when nothing is grouped. */
    sections: List<RoutineSection>,
    lastSessionTimes: Map<Int, Long>,
    isArchived: Boolean,
    onNavigateToDetail: (Int) -> Unit,
    onDelete: (WorkoutPlanEntity) -> Unit,
    onArchiveToggle: (WorkoutPlanEntity) -> Unit,
    /** Swaps two routines in the order: a routine moving up or down next to its neighbour in its group. */
    onSwap: (Int, Int) -> Unit,
    onGroupMenu: (RoutineGroupEntity) -> Unit,
    isLoading: Boolean,
    isSelectionMode: Boolean = false,
    swipeActionsEnabled: Boolean = true,
    selectedPlanIds: Set<Int> = emptySet(),
    onToggleSelection: (Int) -> Unit = {},
    languageCode: String = "en",
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    /** Shown above the routines as the first list item (the week card on the Active tab). */
    header: (@Composable () -> Unit)? = null,
    /** False while [header] is hidden (selection mode): the first routine is then the top of the group. */
    headerVisible: Boolean = true
) {
    val bottomClearance = navBarBottomClearance()
    val grouped = sections.isGrouped()
    // Without groups the week card is the head of the one group of rows; with groups it stands alone
    val hasHead = header != null && headerVisible && !grouped
    // A header that appears while the list sits at the top would be placed above the screen (the list keeps the
    // first routine in place): scroll back up so it is seen.
    val hasHeader = header != null
    LaunchedEffect(hasHeader) {
        if (hasHeader && listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset == 0) {
            listState.requestScrollToItem(0)
        }
    }
    // Which groups are folded (key -1 = the routines in no group): open on Active, folded on Archived, until tapped
    val folded = remember(isArchived) { mutableStateMapOf<Int, Boolean>() }
    val monthNames = remember { MonthCalendar.shortMonthNames() }

    if (isLoading && plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GymLoadingIndicator()
        }
    } else if (plans.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.Notes,
            title = if (isArchived) stringResource(Res.string.no_archived) else stringResource(Res.string.no_routines),
            description = if (isArchived) stringResource(Res.string.archived_appear_here) else stringResource(Res.string.tap_to_create)
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(ResponsiveSize.horizontalPadding),
            // Rows joined into one group, Material 3 Expressive list style
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (header != null) item(key = "routines_week") {
                Column {
                    AnimatedVisibility(
                        visible = headerVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) { header() }
                }
            }
            sections.forEachIndexed { sectionIndex, section ->
                val group = section.group
                val foldKey = group?.id ?: -1
                val isFolded = grouped && (folded[foldKey] ?: isArchived)
                if (grouped) item(key = "group_$foldKey") {
                    val count = section.plans.size
                    val countText = pluralStringResource(Res.plurals.routine_group_routines, count, count)
                    val subtitle = when {
                        group == null -> countText
                        isArchived -> routineGroupPeriod(section.plans, lastSessionTimes)
                            ?.let { (start, end) -> formatMonthRange(start, end, monthNames) + " · " + countText }
                            ?: countText
                        else -> countText + " · " + stringResource(
                            Res.string.routine_group_since,
                            formatMonthYear(section.plans.minOf { it.plan.dataInizio }, monthNames)
                        )
                    }
                    val title = group?.name ?: stringResource(Res.string.routine_group_none)
                    val colors = if (group != null) routineGroupColors(group) else ungroupedCoverColors()
                    val onMenu: (() -> Unit)? = if (group != null && !isSelectionMode) {
                        { onGroupMenu(group) }
                    } else null
                    // Air above every group but a first one that opens the list
                    val top = if (sectionIndex == 0 && header == null) 0.dp else 14.dp
                    if (isFolded) {
                        RoutineGroupFoldedCard(
                            title = title,
                            subtitle = subtitle,
                            names = section.plans.joinToString(" · ") { it.plan.nome },
                            count = count,
                            colors = colors,
                            named = group != null,
                            onExpand = { folded[foldKey] = false },
                            modifier = Modifier.animateItem().padding(top = top)
                        )
                    } else {
                        RoutineGroupHeader(
                            title = title,
                            subtitle = subtitle,
                            swatch = if (group != null) colors.first else null,
                            onToggle = { folded[foldKey] = true },
                            onMenu = onMenu,
                            modifier = Modifier.animateItem().padding(top = top, bottom = 5.dp)
                        )
                    }
                }
                if (!isFolded) {
                    val sectionPlans = section.plans
                    itemsIndexed(sectionPlans, key = { _, planWithDetails -> planWithDetails.plan.id }) { index, planWithDetails ->
                        RoutineListItem(
                            planWithDetails = planWithDetails,
                            position = index,
                            count = sectionPlans.size,
                            hasHead = hasHead && sectionIndex == 0,
                            cover = when {
                                !grouped -> null
                                group != null -> routineGroupColors(group)
                                else -> ungroupedCoverColors()
                            },
                            isArchived = isArchived,
                            isSelectionMode = isSelectionMode,
                            isSelected = planWithDetails.plan.id in selectedPlanIds,
                            swipeActionsEnabled = swipeActionsEnabled,
                            languageCode = languageCode,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleSelection = onToggleSelection,
                            onDelete = onDelete,
                            onArchiveToggle = onArchiveToggle,
                            onMoveUp = sectionPlans.getOrNull(index - 1)?.let { previous ->
                                { onSwap(planWithDetails.plan.id, previous.plan.id) }
                            },
                            onMoveDown = sectionPlans.getOrNull(index + 1)?.let { next ->
                                { onSwap(planWithDetails.plan.id, next.plan.id) }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(bottomClearance)) }
        }
    }
}

/** One routine of the list with its swipe actions (archive one way, delete the other). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineListItem(
    planWithDetails: PlanWithDetails,
    position: Int,
    count: Int,
    hasHead: Boolean,
    cover: Pair<Color, Color>?,
    isArchived: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    swipeActionsEnabled: Boolean,
    languageCode: String,
    onNavigateToDetail: (Int) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onDelete: (WorkoutPlanEntity) -> Unit,
    onArchiveToggle: (WorkoutPlanEntity) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val plan = planWithDetails.plan
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)
    val dismissState = rememberSwipeToDismissBoxState()
    val rowShape = routineRowCorners(position, count, hasHead).toShape()

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            if (!isSelectionMode && swipeActionsEnabled) {
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(plan)
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onArchiveToggle(plan)
                    }
                    else -> {}
                }
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode && swipeActionsEnabled,
        enableDismissFromEndToStart = !isSelectionMode && swipeActionsEnabled,
        backgroundContent = {
            val progress = dismissState.progress

            val color by animateColorAsState(
                when {
                    progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.6f)
                    progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd -> Primary.copy(alpha = 0.6f)
                    else -> Color.Transparent
                }, label = "bg_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(rowShape)
                    .background(color)
                    .padding(horizontal = 28.dp)
            ) {
                if (progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        if (isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = "Archive",
                        tint = Primary,
                        modifier = Modifier.align(Alignment.CenterStart).size(28.dp)
                    )
                }
                if (progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = "Delete",
                        tint = Error,
                        modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        RoutineRow(
            planWithDetails = planWithDetails,
            position = position,
            count = count,
            hasHead = hasHead,
            languageCode = languageCode,
            cover = cover,
            onClick = {
                if (isSelectionMode) {
                    onToggleSelection(plan.id)
                } else {
                    onNavigateToDetail(plan.id)
                }
            },
            onLongClick = {
                if (!isSelectionMode) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSelection(plan.id)
                }
            },
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            isArchived = isArchived,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown
        )
    }
}

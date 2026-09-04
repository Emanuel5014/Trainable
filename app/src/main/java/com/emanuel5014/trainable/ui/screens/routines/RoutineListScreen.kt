package com.emanuel5014.trainable.ui.screens.routines

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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.UriMigrationHelper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onGenerateReport: (List<Int>) -> Unit = {},
    onSwipingItemChange: ((Boolean) -> Unit)? = null,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { preferences -> preferences[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    
    val swipeActionsEnabled by remember(context) {
        context.dataStore.data.map { preferences -> preferences[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true }
    }.collectAsState(initial = true)
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val isCurrentlyArchived by remember { derivedStateOf { pagerState.currentPage == 1 } }
    val coroutineScope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkArchiveDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var planToArchive by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    fun openCreateSheet() {
        routineName = ""
        routineNote = ""
        startDate = System.currentTimeMillis()
        endDate = null
        selectedDays.clear()
        showSheet = true
    }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            val fabPadding = if (uiState.floatingNavBar) {
                if (ResponsiveSize.isCompact) 2.dp else 8.dp
            } else 0.dp
            
            if (uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        if (viewModel.hasImagesInSelection()) {
                            showExportDialog = true
                        } else {
                            viewModel.exportSelectedPlans(context, includeImages = false)
                        }
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    modifier = Modifier
                        .padding(end = fabPadding)
                        .padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.share).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { openCreateSheet() },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    modifier = Modifier
                        .padding(end = fabPadding)
                        .padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_routine).replace("CREATE ", ""), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                                -1 -> "${uiState.selectedPlanIds.size} ${stringResource(R.string.selected)}"
                                0 -> stringResource(R.string.your_routines)
                                else -> stringResource(R.string.archived_routines)
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
                subtitle = if (uiState.isSelectionMode) null else stringResource(R.string.training_plans),
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
                            icon = Icons.Rounded.Assessment,
                            onClick = { onGenerateReport(uiState.selectedPlanIds.toList()) },
                            containerColor = SurfaceContainerHigh,
                            contentColor = Primary
                        )
                        GymIconButton(
                            icon = Icons.Rounded.Close,
                            onClick = { viewModel.clearSelection() },
                            containerColor = SurfaceContainerHigh
                        )
                    }
                } else null,
                titleInRow = uiState.isSelectionMode,
                modifier = if (uiState.isSelectionMode) Modifier.padding(top = 8.dp) else Modifier
            )

            // M3 Expressive segmented tabs — pill guidata dal pager per un glide sincronizzato
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
                        stringResource(R.string.active_routines_tab),
                        stringResource(R.string.archived_routines_tab)
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
                            isArchived = isArchivedPage,
                            onNavigateToDetail = onNavigateToDetail,
                            onDelete = { planToDelete = it },
                            onArchiveToggle = { planToArchive = it },
                            onReorder = { from, to -> viewModel.movePlan(from, to, isArchivedPage) },
                            isLoading = uiState.isLoading,
                            isSelectionMode = uiState.isSelectionMode,
                            swipeActionsEnabled = swipeActionsEnabled,
                            selectedPlanIds = uiState.selectedPlanIds,
                            onToggleSelection = { viewModel.togglePlanSelection(it) },
                            languageCode = languageCode
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

    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_routine)) },
            text = { Text(stringResource(R.string.delete_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        viewModel.deleteSelectedPlans()
                        showBulkDeleteDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkDeleteDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
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
            title = { Text(stringResource(if (isArchiving) R.string.archive_routine else R.string.unarchive_routine)) },
            text = { Text(stringResource(if (isArchiving) R.string.archive_routine_message else R.string.unarchive_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        if (isArchiving) viewModel.archiveSelectedPlans()
                        else viewModel.unarchiveSelectedPlans()
                        showBulkArchiveDialog = false
                    },
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(if (isArchiving) R.string.archive else R.string.unarchive).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkArchiveDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
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
            title = { Text(stringResource(R.string.delete_routine)) },
            text = { Text(stringResource(R.string.delete_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        planToDelete?.let { viewModel.deletePlan(it) }
                        planToDelete = null
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { planToDelete = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
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
            title = { Text(stringResource(if (isArchiving) R.string.archive_routine else R.string.unarchive_routine)) },
            text = { Text(stringResource(if (isArchiving) R.string.archive_routine_message else R.string.unarchive_routine_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        planToArchive?.let { viewModel.toggleArchive(it) }
                        planToArchive = null
                    },
                    containerColor = Primary.copy(alpha = 0.1f),
                    contentColor = Primary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(if (isArchiving) R.string.archive else R.string.unarchive).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { planToArchive = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ResponsiveSize.horizontalPadding)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.create_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.new_routine),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymInputField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = stringResource(R.string.routine_name),
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
                                label = stringResource(R.string.start_date).replace(":", ""),
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
                                value = endDate?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(R.string.tap_to_set),
                                onValueChange = {},
                                label = stringResource(R.string.expiration_date),
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
                            text = stringResource(R.string.schedule_days),
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
                                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
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
                        label = stringResource(R.string.routine_notes),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    
                    GymButton(
                        onClick = {
                            val trimmedName = routineName.trim()
                            if (trimmedName.isNotEmpty()) {
                                val note = routineNote.trim().takeIf { it.isNotBlank() }
                                val daysString = if (selectedDays.isEmpty()) null 
                                               else selectedDays.sortedBy { it.value }.joinToString(",") { it.value.toString() }
                                viewModel.createEmptyPlan(
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
                            text = stringResource(R.string.create).uppercase(),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())

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
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showStartDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
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
                    Text(stringResource(R.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
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
                    Text(stringResource(R.string.reset).uppercase())
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.share_include_images_title)) },
            text = { Text(stringResource(R.string.share_include_images_message)) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymButton(
                        onClick = {
                            viewModel.exportSelectedPlans(context, includeImages = true)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(stringResource(R.string.share_with_images).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    GymButton(
                        onClick = {
                            viewModel.exportSelectedPlans(context, includeImages = false)
                            showExportDialog = false
                        },
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(stringResource(R.string.share_without_images).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showExportDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }
}

@Composable
private fun RoutineListPage(
    plans: List<PlanWithDetails>,
    isArchived: Boolean,
    onNavigateToDetail: (Int) -> Unit,
    onDelete: (WorkoutPlanEntity) -> Unit,
    onArchiveToggle: (WorkoutPlanEntity) -> Unit,
    onReorder: (Int, Int) -> Unit,
    isLoading: Boolean,
    isSelectionMode: Boolean = false,
    swipeActionsEnabled: Boolean = true,
    selectedPlanIds: Set<Int> = emptySet(),
    onToggleSelection: (Int) -> Unit = {},
    isFirst: Boolean = false,
    isLast: Boolean = false,
    languageCode: String = "en"
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { preferences -> preferences[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    val listState = rememberLazyListState()
    
    // Reordering State local to page
    val localPlans = remember(plans) { mutableStateListOf<PlanWithDetails>().apply { addAll(plans) } }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    if (isLoading && plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GymLoadingIndicator()
        }
    } else if (plans.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.Notes,
            title = if (isArchived) stringResource(R.string.no_archived) else stringResource(R.string.no_routines),
            description = if (isArchived) stringResource(R.string.archived_appear_here) else stringResource(R.string.tap_to_create)
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(ResponsiveSize.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(localPlans, key = { _, planWithDetails -> planWithDetails.plan.id }) { index, planWithDetails ->
                val plan = planWithDetails.plan
                val isDragging = draggedItemIndex == index
                val zIndex = if (isDragging) 1f else 0f
                val isSelected = selectedPlanIds.contains(plan.id)

                val dismissState = rememberSwipeToDismissBoxState()

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
                                .clip(Shapes.extraLarge)
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
                    modifier = Modifier
                        .zIndex(zIndex)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                            shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                            shape = Shapes.extraLarge
                            clip = isDragging
                        }
                        // Reordering handled via buttons now to avoid gesture conflicts
                ) {
                    RoutineCard(
                        planWithDetails = planWithDetails,
                        languageCode = languageCode,
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
                        swipeActionsEnabled = swipeActionsEnabled,
                        isArchived = isArchived,
                        onMoveUp = if (index > 0) { { onReorder(index, index - 1) } } else null,
                        onMoveDown = if (index < localPlans.size - 1) { { onReorder(index, index + 1) } } else null
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineCard(
    planWithDetails: PlanWithDetails,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    swipeActionsEnabled: Boolean = true,
    isArchived: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    languageCode: String = "en"
) {
    val context = LocalContext.current
    val plan = planWithDetails.plan
    val firstImageUri = planWithDetails.images.firstOrNull()?.imageUri ?: plan.imageUri
    val fixedImageUri = remember(firstImageUri) {
        firstImageUri?.let { uri -> UriMigrationHelper.fixPath(uri, context) }
    }

    val trainedMuscleGroups = remember(planWithDetails.exercises) {
        planWithDetails.exercises.map { it.exercise.categoria }
            .distinct()
            .filter { it.isNotBlank() }
    }

    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.extraLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else SurfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Primary else if (isSelectionMode) Primary.copy(alpha = 0.2f) else Surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (fixedImageUri != null) {
                        AsyncImage(
                            model = fixedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null,
                            tint = if (isSelectionMode) Primary else OnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = plan.nome,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleLarge.fontSize)),
                        color = if (isSelected) Primary else OnSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    plan.dataFine?.let { expiry ->
                        Text(
                            text = stringResource(R.string.expires) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(expiry),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (expiry < System.currentTimeMillis()) Error else Primary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isArchived) {
                        Text(
                            text = stringResource(R.string.created_on) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(plan.dataInizio),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (!plan.giorniSettimana.isNullOrBlank()) {
                        val scheduledDays = remember(plan.giorniSettimana) {
                            plan.giorniSettimana.split(",").mapNotNull { 
                                it.toIntOrNull()?.let { value -> DayOfWeek.of(value) }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            DayOfWeek.entries.forEachIndexed { index, day ->
                                val isScheduled = scheduledDays.contains(day)
                                val dayShape = if (isScheduled) RoundedCornerShape(50)
                                else when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes().shape
                                    DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes().shape
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes().shape
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(dayShape)
                                        .background(if (isScheduled) Primary else SurfaceContainerHigh)
                                        .then(
                                            if (!isScheduled) Modifier.border(
                                                BorderStroke(1.dp, OnSurfaceVariant.copy(alpha = 0.2f)),
                                                dayShape
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isScheduled) OnPrimary else OnSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (trainedMuscleGroups.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            trainedMuscleGroups.forEach { category ->
                                val translatedCategory = ExerciseTranslations.translateCategory(category, languageCode)
                                androidx.compose.material3.Surface(
                                    color = Surface,
                                    contentColor = OnSurfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = translatedCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

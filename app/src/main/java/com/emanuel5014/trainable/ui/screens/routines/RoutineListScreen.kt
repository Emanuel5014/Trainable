package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
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
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.UriMigrationHelper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoutineListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onSwipingItemChange: ((Boolean) -> Unit)? = null,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    
    val swipeActionsEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true }
    }.collectAsState(initial = true)
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val isCurrentlyArchived by remember { derivedStateOf { pagerState.currentPage == 1 } }
    val coroutineScope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var planToArchive by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }

    fun openCreateSheet() {
        routineName = ""
        routineNote = ""
        showSheet = true
    }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
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
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.share).uppercase(), fontWeight = FontWeight.Bold)
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { openCreateSheet() },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_routine).replace("CREATE ", ""), fontWeight = FontWeight.Bold)
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
                                .using(SizeTransform(clip = false))
                        },
                        label = "title_anim"
                    ) { state ->
                        Text(
                            text = when (state) {
                                -1 -> "${uiState.selectedPlanIds.size} ${stringResource(R.string.selected)}"
                                0 -> stringResource(R.string.your_routines)
                                else -> stringResource(R.string.archived_routines)
                            },
                            style = if (uiState.isSelectionMode) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            lineHeight = if (uiState.isSelectionMode) 32.sp else 40.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                subtitle = if (uiState.isSelectionMode) null else stringResource(R.string.training_plans),
                icon = if (uiState.isSelectionMode) null else Icons.Rounded.FitnessCenter,
                navigationIcon = null,
                actions = if (uiState.isSelectionMode) {
                    {
                        if (!swipeActionsEnabled) {
                            GymIconButton(
                                icon = if (pagerState.currentPage == 0) Icons.Rounded.Archive else Icons.Rounded.Unarchive,
                                onClick = { 
                                    if (pagerState.currentPage == 0) viewModel.archiveSelectedPlans() 
                                    else viewModel.unarchiveSelectedPlans() 
                                },
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
                            icon = Icons.Rounded.Close,
                            onClick = { viewModel.clearSelection() },
                            containerColor = SurfaceContainerHigh
                        )
                    }
                } else null,
                titleInRow = uiState.isSelectionMode,
                modifier = if (uiState.isSelectionMode) Modifier.padding(top = 8.dp) else Modifier
            )

            // Modern Tab Row (click only, no swipe)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(Shapes.large)
                    .background(SurfaceContainerHigh)
                    .padding(4.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val indicatorWidth = maxWidth / 2
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (pagerState.currentPage == 0) 0.dp else indicatorWidth,
                        animationSpec = tween(500, easing = EaseOutExpo),
                        label = "indicator_offset"
                    )

                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(44.dp)
                            .offset(x = indicatorOffset)
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
                            animationSpec = tween(500, easing = EaseOutExpo),
                            label = "tab_content"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(Shapes.medium)
                                .clickable {
                                    coroutineScope.launch {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = contentColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

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
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Slight scale and alpha during transition
                            val scale = 1f - (kotlin.math.abs(pageOffset) * 0.05f)
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
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
                        onToggleSelection = { viewModel.togglePlanSelection(it) }
                    )
                }
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
                    containerColor = Error
                ) {
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkDeleteDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            },
            containerColor = Surface,
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
                    Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.Bold)
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
                    Text(stringResource(if (isArchiving) R.string.archive else R.string.unarchive).uppercase(), fontWeight = FontWeight.Bold)
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.create_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.new_routine),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymInputField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = stringResource(R.string.routine_name),
                        modifier = Modifier.fillMaxWidth()
                    )

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
                        Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    
                    GymButton(
                        onClick = {
                            val trimmedName = routineName.trim()
                            if (trimmedName.isNotEmpty()) {
                                val note = routineNote.trim().takeIf { it.isNotBlank() }
                                viewModel.createEmptyPlan(trimmedName, note)
                                showSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.create).uppercase(),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
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
                        Text(stringResource(R.string.share_with_images).uppercase(), fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.share_without_images).uppercase(), fontWeight = FontWeight.Bold)
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
    onToggleSelection: (Int) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
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
            icon = Icons.Rounded.FitnessCenter,
            title = if (isArchived) stringResource(R.string.no_archived) else stringResource(R.string.no_routines),
            description = if (isArchived) stringResource(R.string.archived_appear_here) else stringResource(R.string.tap_to_create)
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(localPlans, key = { _, planWithDetails -> planWithDetails.plan.id }) { index, planWithDetails ->
                val plan = planWithDetails.plan
                val isDragging = draggedItemIndex == index
                val zIndex = if (isDragging) 1f else 0f
                val isSelected = selectedPlanIds.contains(plan.id)

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (isSelectionMode) return@rememberSwipeToDismissBoxState false
                        when (value) {
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
                        false
                    }
                )

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
                        .pointerInput(isSelectionMode) {
                            if (isSelectionMode) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedItemIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val itemHeight = 100.dp.toPx()
                                    if (dragOffsetY > itemHeight / 2 && draggedItemIndex!! < localPlans.size - 1) {
                                        val targetIndex = draggedItemIndex!! + 1
                                        localPlans.add(targetIndex, localPlans.removeAt(draggedItemIndex!!))
                                        draggedItemIndex = targetIndex
                                        dragOffsetY -= itemHeight
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (dragOffsetY < -itemHeight / 2 && draggedItemIndex!! > 0) {
                                        val targetIndex = draggedItemIndex!! - 1
                                        localPlans.add(targetIndex, localPlans.removeAt(draggedItemIndex!!))
                                        draggedItemIndex = targetIndex
                                        dragOffsetY += itemHeight
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDragEnd = {
                                    onReorder(index, draggedItemIndex!!)
                                    draggedItemIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = { draggedItemIndex = null; dragOffsetY = 0f }
                            )
                        }
                ) {
                    RoutineCard(
                        planWithDetails = planWithDetails,
                        onClick = { 
                            if (isSelectionMode) {
                                onToggleSelection(plan.id)
                            } else {
                                onNavigateToDetail(plan.id) 
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                onToggleSelection(plan.id)
                            }
                        },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        swipeActionsEnabled = swipeActionsEnabled,
                        isArchived = isArchived
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
    isArchived: Boolean = false
) {
    val context = LocalContext.current
    val plan = planWithDetails.plan
    val firstImageUri = planWithDetails.images.firstOrNull()?.imageUri ?: plan.imageUri
    val fixedImageUri = remember(firstImageUri) {
        UriMigrationHelper.fixPath(firstImageUri, context)
    }

    GymCard(
        modifier = Modifier
            .fillMaxWidth()
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
                        .background(if (isSelected) Primary.copy(alpha = 0.2f) else Surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (fixedImageUri != null) {
                        AsyncImage(
                            model = fixedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Icon(
                            Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = if (isSelected) Primary else OnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = plan.nome,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected) Primary else OnSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Added: ${com.emanuel5014.trainable.ui.util.DateFormatter.format(plan.dataInizio)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            }
        }
    }
}

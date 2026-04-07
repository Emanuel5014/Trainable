package com.example.gymtracking.ui.screens.routines

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.gymtracking.R
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import com.example.gymtracking.data.repository.dataStore
import com.example.gymtracking.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.data.local.entity.WorkoutPlanEntity
import com.example.gymtracking.ui.components.EmptyState
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.components.GymInputField
import com.example.gymtracking.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var showSheet by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<WorkoutPlanEntity?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    var showingArchived by remember { mutableStateOf(false) }

    // Gesture state
    var swipeOffsetY by remember { mutableStateOf(0f) }
    val swipeThresholdPx = with(density) { 100.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    swipeOffsetY = (swipeOffsetY + available.y).coerceAtMost(swipeThresholdPx * 1.5f)
                    return Offset(0f, available.y)
                }
                if (available.y < 0 && swipeOffsetY > 0) {
                    val consumed = if (swipeOffsetY + available.y > 0) available.y else -swipeOffsetY
                    swipeOffsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (swipeOffsetY > swipeThresholdPx) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showingArchived = !showingArchived
                }
                swipeOffsetY = 0f
                return super.onPostFling(consumed, available)
            }
        }
    }

    // Reordering State
    val localPlans = remember { mutableStateListOf<WorkoutPlanEntity>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Sync local list
    LaunchedEffect(uiState.plans, uiState.archivedPlans, showingArchived) {
        if (draggedItemIndex == null) {
            localPlans.clear()
            localPlans.addAll(if (showingArchived) uiState.archivedPlans else uiState.plans)
        }
    }

    fun openCreateSheet() {
        editingPlan = null
        routineName = ""
        routineNote = ""
        showSheet = true
    }

    fun openEditSheet(plan: WorkoutPlanEntity) {
        editingPlan = plan
        routineName = plan.nome
        routineNote = plan.note.orEmpty()
        showSheet = true
    }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Drag Visual Indicator
            if (swipeOffsetY > 10f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = (swipeOffsetY / 4).dp)
                        .graphicsLayer { 
                            alpha = (swipeOffsetY / swipeThresholdPx).coerceIn(0f, 1f)
                            scaleX = (swipeOffsetY / swipeThresholdPx).coerceIn(0.5f, 1.2f)
                            scaleY = scaleX
                        }
                ) {
                    Icon(
                        if (showingArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            translationY = swipeOffsetY 
                            scaleX = 1f - (swipeOffsetY / (swipeThresholdPx * 10f))
                            scaleY = scaleX
                        },
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val headerAlpha by animateFloatAsState(
                            targetValue = if (swipeOffsetY > 20f) 0.3f else 1f,
                            label = "header_alpha"
                        )
                        Column(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .graphicsLayer { alpha = headerAlpha }
                        ) {
                            AnimatedContent(
                                targetState = showingArchived,
                                transitionSpec = {
                                    (fadeIn() + scaleIn(initialScale = 0.8f))
                                        .togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                                },
                                label = "title_anim"
                            ) { isArchived ->
                                Text(
                                    text = if (isArchived) stringResource(R.string.archived_routines) else stringResource(R.string.your_routines),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = OnSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showingArchived) stringResource(R.string.swipe_for_active) else stringResource(R.string.swipe_for_archived),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showingArchived) Primary.copy(alpha = 0.7f) else OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (localPlans.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.AutoMirrored.Rounded.ViewList,
                                title = if (showingArchived) stringResource(R.string.no_archived) else stringResource(R.string.no_routines),
                                description = if (showingArchived) stringResource(R.string.archived_appear_here) else stringResource(R.string.tap_to_create)
                            )
                        }
                    } else {
                        itemsIndexed(localPlans, key = { _, plan -> plan.id }) { index, plan ->
                            val isDragging = draggedItemIndex == index
                            val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        planToDelete = plan
                                        false
                                    } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (showingArchived) viewModel.unarchivePlan(plan) else viewModel.archivePlan(plan)
                                        false
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color by animateColorAsState(
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.8f)
                                            SwipeToDismissBoxValue.StartToEnd -> Primary.copy(alpha = 0.8f)
                                            else -> Color.Transparent
                                        }, label = "dismiss_bg"
                                    )
                                    val icon = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.DeleteSweep
                                        SwipeToDismissBoxValue.StartToEnd -> if (showingArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive
                                        else -> Icons.Rounded.DeleteSweep
                                    }
                                    Box(
                                        Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(color).padding(horizontal = 20.dp),
                                        contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Icon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) OnError else OnPrimary, modifier = Modifier.size(32.dp))
                                    }
                                },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        scaleX = if (isDragging) 1.02f else 1f
                                        scaleY = if (isDragging) 1.02f else 1f
                                        shadowElevation = elevation.toPx()
                                        shape = RoundedCornerShape(28.dp)
                                        clip = isDragging
                                    }
                                    .pointerInput(Unit) {
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
                                                viewModel.movePlan(index, draggedItemIndex!!, showingArchived)
                                                draggedItemIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = { draggedItemIndex = null; dragOffsetY = 0f }
                                        )
                                    }
                            ) {
                                RoutineCard(
                                    plan = plan,
                                    onClick = { onNavigateToDetail(plan.id) },
                                    onEdit = { openEditSheet(plan) }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
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
                        text = if (editingPlan == null) stringResource(R.string.create_routine) else stringResource(R.string.edit_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (editingPlan == null) stringResource(R.string.new_routine) else stringResource(R.string.update_plan),
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
                                val current = editingPlan
                                if (current == null) {
                                    viewModel.createEmptyPlan(trimmedName, note)
                                } else {
                                    viewModel.updatePlan(current, trimmedName, note)
                                }
                                showSheet = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (editingPlan == null) stringResource(R.string.create).uppercase() else stringResource(R.string.save).uppercase(),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    plan: WorkoutPlanEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = null,
                        tint = Primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = plan.nome,
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Added: ${com.example.gymtracking.ui.util.DateFormatter.format(plan.dataInizio)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit Plan",
                    tint = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

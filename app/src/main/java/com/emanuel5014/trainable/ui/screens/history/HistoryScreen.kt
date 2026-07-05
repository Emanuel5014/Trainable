package com.emanuel5014.trainable.ui.screens.history

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.AddCardioDialog
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.WorkoutShareCard
import com.emanuel5014.trainable.ui.components.captureViewToBitmap
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.navigation.CompareSessions
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.ShareUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class ExerciseWithSets(
    val exercise: com.emanuel5014.trainable.data.local.entity.ExerciseEntity,
    val sets: MutableList<com.emanuel5014.trainable.data.local.relation.SetWithExercise>
)

private data class HistoryBlock(
    val supersetId: String?,
    val exercises: List<ExerciseWithSets>
)

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

    LaunchedEffect(viewModel.compareNavigationEvent) {
        viewModel.compareNavigationEvent.collect { (sessionId1, sessionId2) ->
            navController?.navigate(CompareSessions(sessionId1, sessionId2))
        }
    }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var sessionToShare by remember { mutableStateOf<Triple<SessionWithDetails, String, List<PlanExerciseWithDetails>?>?>(null) }

    LaunchedEffect(Unit) {
        isNavigating = false
    }

    val fabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.canScrollForward
        }
    }

    var capturedBitmapToPreview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    DisposableEffect(capturedBitmapToPreview) {
        if (capturedBitmapToPreview != null) {
            BottomBarManager.isVisibleOverride = false
            BottomBarManager.swipeLocked = true
        }
        onDispose {
            BottomBarManager.isVisibleOverride = true
            BottomBarManager.swipeLocked = false
        }
    }

    BackHandler(capturedBitmapToPreview != null) { capturedBitmapToPreview = null }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = surfaceColor,
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    FloatingActionButtonMenu(
                        modifier = Modifier.padding(bottom = 60.dp).offset(x = 12.dp).zIndex(10f),
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
                                    onCheckedChange = { 
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        fabMenuExpanded = !fabMenuExpanded 
                                    }
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
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                fabMenuExpanded = false
                                showCardioDialog = true
                            },
                            icon = { Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null) },
                            text = { Text(text = stringResource(R.string.add_cardio)) }
                        )
                        FloatingActionButtonMenuItem(
                            onClick = { 
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                    "${uiState.selectedSessionIds.size} ${stringResource(R.string.selected)}"
                                } else {
                                    stringResource(R.string.history_title)
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
                    subtitle = if (uiState.isSelectionMode) null else stringResource(R.string.workout_logs),
                    icon = if (uiState.isSelectionMode) null else Icons.Rounded.History,
                    actions = if (uiState.isSelectionMode) {
                        {
                            if (uiState.selectedSessionIds.size == 2) {
                                GymIconButton(
                                    icon = Icons.AutoMirrored.Rounded.CompareArrows,
                                    onClick = { viewModel.compareSelectedSessions() },
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

                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading && uiState.sessions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GymLoadingIndicator()
                        }
                    } else if (uiState.sessions.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.no_history_yet),
                            description = stringResource(R.string.no_history_description_screen)
                        )
                    } else if (uiState.filteredSessions.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.FilterListOff,
                            title = stringResource(R.string.no_results_filters),
                            description = stringResource(R.string.try_adjust_filters),
                            action = {
                                GymButton(onClick = { viewModel.setFilters(null, null, null) }) {
                                    Text(stringResource(R.string.clear_filters).uppercase(), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = ResponsiveSize.horizontalPadding, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                        itemsIndexed(uiState.filteredSessions, key = { _, s -> s.session.id }) { index, sessionDetails ->
                            val session = sessionDetails.session
                            val planName = sessionDetails.session.noteSessione ?: sessionDetails.plan.nome
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
                                        scope.launch {
                                            val planExercises = viewModel.loadPlanExercises(details.session.planId)
                                            sessionToShare = Triple(details, planName, planExercises)
                                        }
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
                                        if (!uiState.isSelectionMode) {
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
                                            weightUnit = uiState.weightUnit,
                                            planExercises = sessionToShare!!.third
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                view.post {
                                    val bitmap = captureViewToBitmap(view)
                                    capturedBitmapToPreview = bitmap
                                    sessionToShare = null
                                }
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = capturedBitmapToPreview != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.90f, animationSpec = tween(200)),
            modifier = Modifier.zIndex(200f)
        ) {
            val imageSavedMessage = stringResource(R.string.image_saved_gallery)
            val failedSaveMessage = stringResource(R.string.failed_save_image)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.76f))
                    .clickable { capturedBitmapToPreview = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .padding(ResponsiveSize.cardPadding)
                        .fillMaxWidth(0.92f)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(ResponsiveSize.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.share_preview),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineSmall.fontSize)
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            IconButton(
                                onClick = { capturedBitmapToPreview = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(when {
                            ResponsiveSize.screenWidthDp < 360 -> 12.dp
                            ResponsiveSize.screenWidthDp < 400 -> 16.dp
                            else -> 18.dp
                        }))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                        ) {
                            if (capturedBitmapToPreview != null) {
                                Image(
                                    bitmap = capturedBitmapToPreview!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(when {
                            ResponsiveSize.screenWidthDp < 360 -> 14.dp
                            ResponsiveSize.screenWidthDp < 400 -> 18.dp
                            else -> 20.dp
                        }))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val success = ShareUtils.saveBitmapToGallery(context, capturedBitmapToPreview!!, "Workout")
                                    val message = if (success) imageSavedMessage else failedSaveMessage
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = stringResource(R.string.save),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    ShareUtils.shareBitmap(context, capturedBitmapToPreview!!)
                                    capturedBitmapToPreview = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.share),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
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
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
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
                            Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
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
                            Text(stringResource(R.string.edit).uppercase(), fontWeight = FontWeight.ExtraBold)
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
                    Text(stringResource(R.string.apply_filters).uppercase(), fontWeight = FontWeight.ExtraBold)
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
            plans.filter { it.note != "SYSTEM_PLAN" } 
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
                                fontWeight = FontWeight.ExtraBold,
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
                                fontWeight = FontWeight.ExtraBold,
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

@Composable
private fun HistoryExerciseGroup(
    exWithSets: ExerciseWithSets,
    isSuperset: Boolean,
    languageCode: String,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val exerciseName = ExerciseTranslations.translate(exWithSets.exercise.nome, languageCode)
    Column(modifier = modifier) {
        Text(
            text = exerciseName.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        exWithSets.sets.sortedBy { it.setLog.numeroSerie }.forEach { setWithEx ->
            val set = setWithEx.setLog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        fontWeight = FontWeight.Bold,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.extraLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = Shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ResponsiveSize.cardPadding,
                        end = ResponsiveSize.cardPadding,
                        top = ResponsiveSize.cardPadding,
                        bottom = if (isExpanded) 0.dp else ResponsiveSize.cardPadding
                    ),
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
                            fontWeight = FontWeight.ExtraBold,
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
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                if (isSelectionMode) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Primary else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveSize.cardPadding),
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
                        Text(
                            text = stringResource(R.string.no_exercises_in_session_detail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = ResponsiveSize.cardPadding)
                                .padding(bottom = ResponsiveSize.cardPadding)
                        )
                    } else {
                        val sortedSets = details.sets.sortedWith(
                            compareBy(
                                { it.setLog.ordineEsercizio },
                                { it.setLog.numeroSerie }
                            )
                        )
                        
                        // 1. Group sets by exercise while preserving order
                        val exercisesWithSets = mutableListOf<ExerciseWithSets>()
                        sortedSets.forEach { setWithEx ->
                            val existing = exercisesWithSets.find { it.exercise.id == setWithEx.exercise.id && it.sets.first().setLog.ordineEsercizio == setWithEx.setLog.ordineEsercizio }
                            if (existing != null) {
                                existing.sets.add(setWithEx)
                            } else {
                                exercisesWithSets.add(ExerciseWithSets(setWithEx.exercise, mutableListOf(setWithEx)))
                            }
                        }

                        // 2. Group exercises into blocks by supersetId
                        val blocks = mutableListOf<HistoryBlock>()
                        var currentSupersetId: String? = null
                        var currentBlock = mutableListOf<ExerciseWithSets>()

                        exercisesWithSets.forEach { item ->
                            val sid = item.sets.first().setLog.supersetId
                            if (sid != null) {
                                if (sid == currentSupersetId) {
                                    currentBlock.add(item)
                                } else {
                                    if (currentBlock.isNotEmpty()) {
                                        blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
                                        currentBlock = mutableListOf()
                                    }
                                    currentSupersetId = sid
                                    currentBlock.add(item)
                                }
                            } else {
                                if (currentBlock.isNotEmpty()) {
                                    blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
                                    currentBlock = mutableListOf()
                                }
                                currentSupersetId = null
                                blocks.add(HistoryBlock(null, listOf(item)))
                            }
                        }
                        if (currentBlock.isNotEmpty()) {
                            blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
                        }

                        blocks.forEach { block ->
                            val isSuperset = block.supersetId != null

                            if (isSuperset) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .background(Primary.copy(alpha = 0.04f), shape = Shapes.large)
                                        .border(1.dp, Primary.copy(alpha = 0.1f), shape = Shapes.large)
                                        .padding(horizontal = ResponsiveSize.cardPadding, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Link,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.superset),
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                            color = Primary,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    block.exercises.forEachIndexed { exIndex, exWithSets ->
                                        HistoryExerciseGroup(
                                            exWithSets = exWithSets,
                                            isSuperset = true,
                                            languageCode = languageCode,
                                            weightUnit = weightUnit,
                                            modifier = Modifier.padding(bottom = if (exIndex < block.exercises.lastIndex) 16.dp else 0.dp)
                                        )
                                    }
                                }
                            } else {
                                block.exercises.forEach { exWithSets ->
                                    HistoryExerciseGroup(
                                        exWithSets = exWithSets,
                                        isSuperset = false,
                                        languageCode = languageCode,
                                        weightUnit = weightUnit,
                                        modifier = Modifier
                                            .padding(horizontal = ResponsiveSize.cardPadding)
                                            .padding(bottom = 16.dp)
                                    )
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
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = ResponsiveSize.cardPadding)
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cardio.categoria.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.ExtraBold,
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
                                        fontWeight = FontWeight.Bold,
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
                        
                        Spacer(modifier = Modifier.height(ResponsiveSize.cardPadding - 16.dp))
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


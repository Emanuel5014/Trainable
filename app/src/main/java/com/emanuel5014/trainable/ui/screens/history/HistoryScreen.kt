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
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
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
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.ui.components.captureViewToBitmap
import com.emanuel5014.trainable.util.ShareUtils
import com.emanuel5014.trainable.ui.components.WorkoutShareCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController? = null,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val context = LocalContext.current
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

    val scope = rememberCoroutineScope()
    var sessionToShare by remember { mutableStateOf<Pair<SessionWithDetails, String>?>(null) }

    LaunchedEffect(Unit) {
        isNavigating = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface
        ) { paddingValues ->
            if (uiState.isLoading && uiState.sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    GymLoadingIndicator()
                }
            } else if (uiState.sessions.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.no_history_yet),
                    description = stringResource(R.string.no_history_description_screen),
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
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
                                color = OnSurface,
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
                                            containerColor = SurfaceContainerHigh,
                                            contentColor = Primary
                                        )
                                    }
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
                        titleInRow = uiState.isSelectionMode
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                    itemsIndexed(uiState.sessions, key = { _, s -> s.session.id }) { index, sessionDetails ->
                        val session = sessionDetails.session
                        val planName = sessionDetails.plan.nome
                        val isExpanded = expandedSessionId == session.id

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (uiState.isSelectionMode) return@rememberSwipeToDismissBoxState false
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sessionToDelete = session
                                    false
                                } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sessionToEdit = sessionDetails
                                    false
                                } else {
                                    false
                                }
                            }
                        )

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
                        containerColor = Error.copy(alpha = 0.1f),
                        contentColor = Error
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
                containerColor = SurfaceContainerHigh,
                titleContentColor = OnSurface,
                textContentColor = OnSurfaceVariant
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
                        containerColor = Error.copy(alpha = 0.1f),
                        contentColor = Error
                    ) {
                        Text(stringResource(R.string.delete).uppercase(), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    GymButton(
                        onClick = { sessionToDelete = null },
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariant
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
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
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
                        containerColor = Primary.copy(alpha = 0.15f),
                        contentColor = Primary
                    ) {
                        Text(stringResource(R.string.edit).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {},
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
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
                            text = planName,
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
                    } else if (details.sets.isEmpty()) {
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
                            note = if (note.isBlank()) null else note
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


package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkout: (planId: Int?, sessionId: Int?) -> Unit,
    onNavigateToQuickWorkout: (name: String?) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SessionWithPlanName?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showQuickWorkoutDialog by remember { mutableStateOf(false) }
    var quickWorkoutName by remember { mutableStateOf("") }

    if (showQuickWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showQuickWorkoutDialog = false },
            title = { Text(stringResource(R.string.quick_workout)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.emanuel5014.trainable.ui.components.GymInputField(
                        value = quickWorkoutName,
                        onValueChange = { quickWorkoutName = it },
                        label = stringResource(R.string.quick_workout_name_hint),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GymButton(
                    onClick = {
                        onNavigateToQuickWorkout(quickWorkoutName.takeIf { it.isNotBlank() })
                        showQuickWorkoutDialog = false
                        quickWorkoutName = ""
                    },
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(R.string.start).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showQuickWorkoutDialog = false },
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.gymMembershipExpiryDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.setGymMembershipExpiryDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        sessionToDelete?.let { viewModel.deleteSession(it.session.id) }
                        sessionToDelete = null
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
                    onClick = { sessionToDelete = null },
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else {
            Scaffold(
                containerColor = Surface,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showQuickWorkoutDialog = true },
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        shape = Shapes.large,
                        modifier = Modifier
                            .padding(
                                end = if (uiState.floatingNavBar) {
                                    if (ResponsiveSize.isCompact) 2.dp else 8.dp
                                } else 0.dp
                            )
                            .padding(bottom = 80.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = stringResource(R.string.quick_workout)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.quick_workout),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(ResponsiveSize.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(if (ResponsiveSize.isCompact) Spacing.large else Spacing.doubleLarge)
                ) {
                item {
                    DashboardSimpleHeader(
                        onSettingsClick = onNavigateToSettings,
                        dynamicColor = uiState.dynamicColor,
                        themePalette = uiState.themePalette,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedCount = uiState.selectedSessionIds.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelection = { showBulkDeleteDialog = true }
                    )
                }

                item {
                    GymMembershipCard(
                        expiryDateMillis = uiState.gymMembershipExpiryDate,
                        username = uiState.username,
                        onClick = { showDatePicker = true }
                    )
                }

                item {
                    WeeklyGoalCard(
                        workoutsThisWeek = uiState.workoutsThisWeek,
                        weeklyGoal = uiState.weeklyGoal
                    )
                }

                if (uiState.unfinishedSessions.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                            Text(
                                text = stringResource(R.string.resume_workout),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold
                            )
                            uiState.unfinishedSessions.forEach { session ->
                                val haptic = LocalHapticFeedback.current
                                val dismissState = rememberSwipeToDismissBoxState()

                                LaunchedEffect(dismissState.targetValue) {
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sessionToDelete = session
                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = uiState.swipeActionsEnabled,
                                    backgroundContent = {
                                        val progress = dismissState.progress
                                        val color by animateColorAsState(
                                            when {
                                                progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.6f)
                                                else -> Color.Transparent
                                            }, label = "swipe_bg"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(Shapes.extraLarge)
                                                .background(color)
                                                .padding(horizontal = 28.dp)
                                        ) {
                                            if (progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                                Icon(
                                                    Icons.Rounded.DeleteSweep,
                                                    contentDescription = "Delete",
                                                    tint = Error,
                                                    modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    UnfinishedSessionCard(
                                        session = session,
                                        isSelected = uiState.selectedSessionIds.contains(session.session.id),
                                        isSelectionMode = uiState.isSelectionMode,
                                        onClick = { 
                                            if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
                                                if (uiState.isSelectionMode) {
                                                    viewModel.toggleSelection(session.session.id)
                                                } else {
                                                    onNavigateToWorkout(session.session.planId, session.session.id)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.swipeActionsEnabled || uiState.isSelectionMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.toggleSelection(session.session.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.todayPlan != null) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.today_workout),
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = Spacing.medium)
                            )
                            GymCard(
                                modifier = Modifier.clickable { onNavigateToWorkout(uiState.todayPlan!!.id, null) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = uiState.todayPlan!!.nome,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = OnSurface,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = uiState.todayPlan!!.note ?: stringResource(R.string.select_routine),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.suggestedPlan != null) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.suggested_plan),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = Spacing.medium)
                            )
                            GymCard(
                                modifier = Modifier.clickable { onNavigateToWorkout(uiState.suggestedPlan!!.id, null) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = uiState.suggestedPlan!!.nome,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = OnSurface,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = uiState.suggestedPlan!!.note ?: stringResource(R.string.select_routine),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.todayPlan == null && uiState.suggestedPlan == null) {
                    item {
                        Text(
                            text = stringResource(R.string.no_active_routines_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun GymMembershipCard(
    expiryDateMillis: Long?,
    username: String,
    onClick: () -> Unit
) {
    val daysLeft = expiryDateMillis?.let {
        val expiryDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.DAYS.between(java.time.LocalDate.now(), expiryDate).toInt()
    }

    val isExpired = daysLeft != null && daysLeft < 0
    val isExpiringSoon = daysLeft != null && daysLeft in 0..7
    
    // Dynamic styling based on status
    val cardGradient = when {
        isExpired -> listOf(Error.copy(alpha = 0.8f), Error.copy(alpha = 0.5f))
        isExpiringSoon -> listOf(Tertiary.copy(alpha = 0.9f), Tertiary.copy(alpha = 0.6f))
        else -> listOf(Primary.copy(alpha = 0.9f), Primary.copy(alpha = 0.6f))
    }
    
    val textColor = Color(0xFF1A1A1A)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(color = OnPrimary)
            ),
        shape = Shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradient))
        ) {
            // Background expressive pattern
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = size.width / 2,
                    center = Offset(size.width, 0f)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.05f),
                    radius = size.width / 3,
                    center = Offset(0f, size.height)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = "Gym Membership",
                            tint = textColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gym_membership).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }
                    
                    if (isExpired) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Expired",
                            tint = textColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = username.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (expiryDateMillis != null) {
                            Text(
                                text = "${stringResource(R.string.valid_thru)} ${DateFormatter.format(expiryDateMillis)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.tap_to_set),
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    if (daysLeft != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = when {
                                    isExpired -> stringResource(R.string.expired)
                                    isExpiringSoon -> stringResource(R.string.expires_in_days, daysLeft)
                                    else -> stringResource(R.string.expires_in_days, daysLeft)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSimpleHeader(
    onSettingsClick: () -> Unit,
    dynamicColor: Boolean,
    themePalette: Int = 0,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onClearSelection: () -> Unit = {},
    onDeleteSelection: () -> Unit = {}
) {
    if (isSelectionMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount ${stringResource(R.string.selected)}",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymIconButton(
                    icon = Icons.Rounded.DeleteSweep,
                    onClick = onDeleteSelection,
                    containerColor = SurfaceContainerHigh,
                    contentColor = Error
                )
                GymIconButton(
                    icon = Icons.Rounded.Close,
                    onClick = onClearSelection,
                    containerColor = SurfaceContainerHigh
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val useCustomTint = dynamicColor || themePalette != 0
                Icon(
                    painter = painterResource(id = if (useCustomTint) R.drawable.ic_app_logo else R.drawable.ic_app_logo_static),
                    contentDescription = "Trainable Logo",
                    tint = if (useCustomTint) Primary else Color.Unspecified,
                    modifier = Modifier
                        .size(56.dp)
                )
            }
            GymIconButton(
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                description = stringResource(R.string.nav_settings)
            )
        }
    }
}

@Composable
private fun WeeklyGoalCard(workoutsThisWeek: Int, weeklyGoal: Int) {
    val progress = if (weeklyGoal > 0) (workoutsThisWeek.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f) else 0f
    
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.weekly_goal),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.workouts_this_week, workoutsThisWeek, weeklyGoal),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (workoutsThisWeek >= weeklyGoal) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.goal_met),
                        tint = Tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (workoutsThisWeek >= weeklyGoal) Tertiary else Primary,
                trackColor = Surface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnfinishedSessionCard(
    session: SessionWithPlanName,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else if (isSelectionMode) Primary.copy(alpha = 0.2f) else Primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = session.planNome,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.started, DateFormatter.format(session.session.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}
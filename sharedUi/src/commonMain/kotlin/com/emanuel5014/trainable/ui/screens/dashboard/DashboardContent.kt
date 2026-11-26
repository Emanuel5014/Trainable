package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.ui.screens.history.title
import com.emanuel5014.trainable.ui.screens.history.displayText
import com.emanuel5014.trainable.sharedui.resources.bulk_delete_sessions_message
import com.emanuel5014.trainable.sharedui.resources.bulk_delete_sessions_title
import org.jetbrains.compose.resources.pluralStringResource
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import androidx.compose.runtime.key
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.components.NavBarAction
import com.emanuel5014.trainable.ui.components.NavBarActionEffect
import com.emanuel5014.trainable.ui.components.NavBarPage
import com.emanuel5014.trainable.ui.components.navBarBottomClearance
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import com.emanuel5014.trainable.sharedui.resources.ic_app_logo
import com.emanuel5014.trainable.sharedui.resources.ic_app_logo_static
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.choose_custom_date
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.delete_session
import com.emanuel5014.trainable.sharedui.resources.delete_session_message
import com.emanuel5014.trainable.sharedui.resources.existing_workout_message
import com.emanuel5014.trainable.sharedui.resources.gym_membership
import com.emanuel5014.trainable.sharedui.resources.month_1
import com.emanuel5014.trainable.sharedui.resources.months_3
import com.emanuel5014.trainable.sharedui.resources.months_6
import com.emanuel5014.trainable.sharedui.resources.nav_settings
import com.emanuel5014.trainable.sharedui.resources.quick_renewal
import com.emanuel5014.trainable.sharedui.resources.quick_workout
import com.emanuel5014.trainable.sharedui.resources.quick_workout_name_hint
import com.emanuel5014.trainable.sharedui.resources.remove_membership
import com.emanuel5014.trainable.sharedui.resources.resume_workout
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.selected
import com.emanuel5014.trainable.sharedui.resources.start
import com.emanuel5014.trainable.sharedui.resources.tap_to_set
import com.emanuel5014.trainable.sharedui.resources.valid_thru
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.sharedui.resources.year_1
import com.emanuel5014.trainable.ui.util.uppercaseUi

/**
 * The Dashboard as both apps draw it. The platform owns the state and its changes ([uiState], [actions]); the
 * physical checks behind the triple tap on the logo are opened through [onNavigateToPhysicalChecks], which on Android
 * asks for the fingerprint first when the user wants it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    actions: DashboardActions,
    isActive: Boolean = true,
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkout: (planId: Int?, sessionId: Int?) -> Unit,
    onNavigateToQuickWorkout: (name: String?) -> Unit,
    onNavigateToPhysicalChecks: () -> Unit,
    /** "Serie fatta" in workout mode: open the workout, which ticks the set of [token] if it is still the one to do. */
    onLogSet: (planId: Int, sessionId: Int, token: String) -> Unit = { planId, sessionId, _ -> onNavigateToWorkout(planId, sessionId) }
) {
    val handleNavigateToPhysicalChecks = onNavigateToPhysicalChecks
    val advancedProgramming = com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming.current
    var showMembershipDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SessionWithPlanName?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showQuickWorkoutDialog by remember { mutableStateOf(false) }
    var quickWorkoutName by remember { mutableStateOf("") }
    var existingSessionForPlan by remember { mutableStateOf<SessionWithPlanName?>(null) }

    // With the expressive navbar the quick workout button lives next to it instead of under the actions here
    val showQuickButton = LocalNavBarStyle.current != NavBarStyle.Expressive
    val bottomClearance = navBarBottomClearance()
    val quickWorkoutLabel = stringResource(Res.string.quick_workout)
    NavBarActionEffect(
        page = NavBarPage.Dashboard,
        action = if (uiState.isLoading) null else NavBarAction.Click(Icons.Rounded.Bolt, quickWorkoutLabel) {
            showQuickWorkoutDialog = true
        }
    )

    // Read once: on iOS the dialog composes its content once more while closing, after the state is already null
    existingSessionForPlan?.let { session ->
        AlertDialog(
            onDismissRequest = { existingSessionForPlan = null },
            title = { Text(stringResource(Res.string.resume_workout)) },
            text = { Text(stringResource(Res.string.existing_workout_message, session.title.displayText())) },
            confirmButton = {
                GymButton(
                    onClick = {
                        existingSessionForPlan?.let { current ->
                            onNavigateToWorkout(current.session.planId, current.session.id)
                        }
                        existingSessionForPlan = null
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.resume_workout), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { existingSessionForPlan = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showQuickWorkoutDialog) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { showQuickWorkoutDialog = false },
            title = { Text(stringResource(Res.string.quick_workout)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.emanuel5014.trainable.ui.components.GymInputField(
                        value = quickWorkoutName,
                        onValueChange = { quickWorkoutName = it },
                        label = stringResource(Res.string.quick_workout_name_hint),
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
                    Text(stringResource(Res.string.start).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showQuickWorkoutDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showMembershipDialog) {
        AlertDialog(
            onDismissRequest = { showMembershipDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.gym_membership).uppercaseUi(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = OnSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentExpiry = uiState.gymMembershipExpiryDate
                    if (currentExpiry != null) {
                        Text(
                            text = "${stringResource(Res.string.valid_thru)} ${DateFormatter.format(currentExpiry)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.tap_to_set),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = stringResource(Res.string.quick_renewal).uppercaseUi(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    
                    val options = listOf(
                        Pair(Res.string.month_1, 1),
                        Pair(Res.string.months_3, 3),
                        Pair(Res.string.months_6, 6),
                        Pair(Res.string.year_1, 12)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until options.size step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (j in 0..1) {
                                    if (i + j < options.size) {
                                        val option = options[i + j]
                                            GymButton(
                                                onClick = {
                                                    val now = Clock.System.now().toEpochMilliseconds()
                                                    val baseTime = if (currentExpiry != null && currentExpiry > now) {
                                                        currentExpiry
                                                    } else {
                                                        now
                                                    }
                                                    val baseLocalDate = Instant.fromEpochMilliseconds(baseTime)
                                                        .toLocalDateTime(TimeZone.UTC)
                                                        .date
                                                    val newExpiry = baseLocalDate.plus(option.second, DateTimeUnit.MONTH)
                                                        .atStartOfDayIn(TimeZone.UTC)
                                                        .toEpochMilliseconds()
                                                    actions.setGymMembershipExpiryDate(newExpiry)
                                                    showMembershipDialog = false
                                                },
                                                containerColor = Primary,
                                                contentColor = OnPrimary,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                            ) {
                                            Text(
                                                text = "+ ${stringResource(option.first)}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                    
                    GymButton(
                        onClick = {
                            showMembershipDialog = false
                            showDatePicker = true
                        },
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.choose_custom_date).uppercaseUi(),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    if (currentExpiry != null) {
                        GymButton(
                            onClick = {
                                actions.setGymMembershipExpiryDate(null)
                                showMembershipDialog = false
                            },
                            containerColor = Error.copy(alpha = 0.1f),
                            contentColor = Error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.remove_membership).uppercaseUi(),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMembershipDialog = false }) {
                    Text(stringResource(Res.string.cancel).uppercaseUi(), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.gymMembershipExpiryDate ?: Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        actions.setGymMembershipExpiryDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(Res.string.delete_session)) },
            text = { Text(stringResource(Res.string.delete_session_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        sessionToDelete?.let { actions.deleteSession(it.session.id) }
                        sessionToDelete = null
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.delete).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { sessionToDelete = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi())
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
            title = { val n = uiState.selectedSessionIds.size; Text(pluralStringResource(Res.plurals.bulk_delete_sessions_title, n, n)) },
            text = { val n = uiState.selectedSessionIds.size; Text(pluralStringResource(Res.plurals.bulk_delete_sessions_message, n, n)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        actions.deleteSelectedSessions()
                        showBulkDeleteDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.delete).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showBulkDeleteDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercaseUi())
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
            val dashboardActionList = dashboardActions(uiState)
            val groupSpacing = if (ResponsiveSize.isCompact) Spacing.large else Spacing.doubleLarge
            val weekPillFor: @Composable (WorkoutPlanEntity) -> String? = { plan ->
                if (advancedProgramming && plan.weeksCount > 1) {
                    stringResource(Res.string.week_short, plan.currentWeek) + "/" + plan.weeksCount
                } else null
            }
            val startPlan: (WorkoutPlanEntity) -> Unit = { plan ->
                val existing = unfinishedSessionForPlan(uiState, plan.id)
                if (existing != null) existingSessionForPlan = existing else onNavigateToWorkout(plan.id, null)
            }
            // The workout the live card follows, read again every second while a timer counts
            val liveSession = uiState.liveSession
            val liveNow = rememberLiveNow(
                isActive = isActive && liveSession != null,
                counting = liveSession?.session?.let { s ->
                    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    (s.restTimerEndTime ?: 0L) > now || (s.warmupTimerEndTime ?: 0L) > now || s.setTimerRunning || s.cardioTimerRunning
                } == true
            )
            val liveUi = liveSession?.let { LiveWorkoutUi.from(it, uiState.liveSnapshot, liveNow) }
            val workoutMode = uiState.workoutMode && liveUi != null && !uiState.isSelectionMode
            Scaffold(containerColor = Surface) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Membership and weekly goal on top, the workouts right under them, quick workout last.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(ResponsiveSize.cardPadding)
                            .padding(bottom = bottomClearance)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(groupSpacing)) {
                            DashboardSimpleHeader(
                                onSettingsClick = onNavigateToSettings,
                                onLogoTripleClick = handleNavigateToPhysicalChecks,
                                dynamicColor = uiState.dynamicColor,
                                themePalette = uiState.themePalette,
                                isSelectionMode = uiState.isSelectionMode,
                                selectedCount = uiState.selectedSessionIds.size,
                                onClearSelection = { actions.clearSelection() },
                                onDeleteSelection = { showBulkDeleteDialog = true }
                            )
                            if (workoutMode && liveUi != null && liveSession != null) {
                                // Workout mode: the live panel and the exercise list first, the info cards under them
                                val open = { onNavigateToWorkout(liveSession.session.planId, liveSession.session.id) }
                                LiveWorkoutPanel(
                                    ui = liveUi,
                                    buttons = uiState.restButtons,
                                    isActive = isActive,
                                    onOpen = open,
                                    onRest = { actions.adjustRest(liveSession.session.id, it) },
                                    onSetDone = { token -> onLogSet(liveSession.session.planId, liveSession.session.id, token) },
                                    // The same confirmation as swiping a resume card away
                                    onDelete = { sessionToDelete = liveSession }
                                )
                                LiveExerciseList(liveUi, onOpen = open)
                            }
                            val onMembershipClick = {
                                if (uiState.gymMembershipExpiryDate == null) {
                                    showDatePicker = true
                                } else {
                                    showMembershipDialog = true
                                }
                            }
                            GymMembershipCard(
                                isActive = isActive,
                                expiryDateMillis = uiState.gymMembershipExpiryDate,
                                username = uiState.username,
                                onClick = onMembershipClick
                            )
                            WeeklyGoalCard(
                                isActive = isActive,
                                workoutsThisWeek = uiState.workoutsThisWeek,
                                weeklyGoal = uiState.weeklyGoal,
                                cardioWorkoutsThisWeek = uiState.cardioWorkoutsThisWeek
                            )
                        }

                        Column(
                            modifier = Modifier.padding(top = groupSpacing),
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            if (dashboardActionList.isEmpty()) NoRoutinesInvite()
                            // In workout mode the followed workout is the panel on top and nothing new is offered to start
                            val shownActions = if (workoutMode) dashboardActionList.filter {
                                it is DashboardAction.Resume && it.session.session.id != liveSession?.session?.id
                            } else dashboardActionList
                            shownActions.forEachIndexed { index, action ->
                                val card: @Composable (Boolean, Boolean, () -> Unit, (() -> Unit)?) -> Unit =
                                    { selected, selectionMode, onClick, onLongClick ->
                                        val pill = when (action) {
                                            is DashboardAction.Resume -> null
                                            is DashboardAction.Today -> weekPillFor(action.plan)
                                            is DashboardAction.Suggested -> weekPillFor(action.plan)
                                        }
                                        val live = liveUi?.takeIf {
                                            action is DashboardAction.Resume && action.session.session.id == it.sessionId && !selectionMode
                                        }
                                        if (index == 0 && live != null && !workoutMode) {
                                            LiveWorkoutCard(live, isActive, onClick, onLongClick)
                                        } else if (index == 0 && !workoutMode) {
                                            DashboardMainActionCard(action, pill, selected, selectionMode, onClick, onLongClick, isActive = isActive)
                                        } else {
                                            DashboardActionRow(action, pill, selected, selectionMode, onClick, onLongClick)
                                        }
                                    }
                                when (action) {
                                    is DashboardAction.Resume -> key(action.session.session.id) {
                                        val session = action.session
                                        val haptic = LocalHapticFeedback.current
                                        SessionSwipeToDelete(
                                            enabled = uiState.swipeActionsEnabled,
                                            onDeleteRequest = { sessionToDelete = session }
                                        ) {
                                            card(
                                                uiState.selectedSessionIds.contains(session.session.id),
                                                uiState.isSelectionMode,
                                                {
                                                    if (uiState.isSelectionMode) {
                                                        actions.toggleSelection(session.session.id)
                                                    } else {
                                                        onNavigateToWorkout(session.session.planId, session.session.id)
                                                    }
                                                },
                                                {
                                                    if (!uiState.swipeActionsEnabled || uiState.isSelectionMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        actions.toggleSelection(session.session.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    is DashboardAction.Today -> card(false, false, { startPlan(action.plan) }, null)
                                    is DashboardAction.Suggested -> card(false, false, { startPlan(action.plan) }, null)
                                }
                            }
                            if (showQuickButton && !workoutMode) {
                                QuickWorkoutButton(onClick = { showQuickWorkoutDialog = true })
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
    onLogoTripleClick: () -> Unit,
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
                text = "$selectedCount ${stringResource(Res.string.selected)}",
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
                var clickCount by remember { mutableStateOf(0) }
                var lastClickTime by remember { mutableStateOf(0L) }
                
                Icon(
                    painter = painterResource(if (useCustomTint) Res.drawable.ic_app_logo else Res.drawable.ic_app_logo_static),
                    contentDescription = "Trainable Logo",
                    tint = if (useCustomTint) Primary else Color.Unspecified,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val currentTime = Clock.System.now().toEpochMilliseconds()
                            if (currentTime - lastClickTime < 500L) {
                                clickCount++
                            } else {
                                clickCount = 1
                            }
                            lastClickTime = currentTime
                            if (clickCount >= 3) {
                                clickCount = 0
                                onLogoTripleClick()
                            }
                        }
                )
            }
            GymIconButton(
                icon = Icons.Default.Settings,
                onClick = onSettingsClick,
                description = stringResource(Res.string.nav_settings)
            )
        }
    }
}

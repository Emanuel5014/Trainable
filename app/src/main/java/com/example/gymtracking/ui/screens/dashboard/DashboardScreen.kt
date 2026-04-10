package com.example.gymtracking.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.R
import com.example.gymtracking.data.local.relation.SessionWithPlanName
import com.example.gymtracking.ui.components.EmptyState
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.components.GymIconButton
import com.example.gymtracking.ui.components.GymLoadingIndicator
import com.example.gymtracking.ui.components.StatCard
import com.example.gymtracking.ui.theme.*
import com.example.gymtracking.ui.util.DateFormatter

@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkout: (planId: Int?, sessionId: Int?) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            if (uiState.suggestedPlan != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        uiState.suggestedPlan?.let { plan ->
                            onNavigateToWorkout(plan.id, null)
                        }
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = Shapes.large,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.start))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Spacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.doubleLarge)
            ) {
                item {
                    DashboardSimpleHeader(
                        username = uiState.username,
                        onSettingsClick = onNavigateToSettings
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
                                fontWeight = FontWeight.Bold
                            )
                            uiState.unfinishedSessions.forEach { session ->
                                UnfinishedSessionCard(
                                    session = session,
                                    onClick = { onNavigateToWorkout(session.session.planId, session.session.id) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.suggested_plan),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )
                    uiState.suggestedPlan?.let { plan ->
                        GymCard(
                            modifier = Modifier.clickable { onNavigateToWorkout(plan.id, null) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = plan.nome,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = OnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = plan.note ?: stringResource(R.string.select_routine),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }
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
                                        tint = Primary
                                    )
                                }
                            }
                        }
                    } ?: run {
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
private fun DashboardSimpleHeader(username: String, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant
            )
            Text(
                text = username,
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }
        GymIconButton(
            icon = Icons.Default.Settings,
            onClick = onSettingsClick,
            description = stringResource(R.string.nav_settings)
        )
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
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.workouts_this_week, workoutsThisWeek, weeklyGoal),
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
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

@Composable
private fun UnfinishedSessionCard(session: SessionWithPlanName, onClick: () -> Unit) {
    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        containerColor = Primary.copy(alpha = 0.05f)
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
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = session.planNome,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
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

package com.example.gymtracking.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.gymtracking.R
import com.example.gymtracking.data.ExerciseTranslations
import com.example.gymtracking.data.repository.dataStore
import com.example.gymtracking.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracking.data.local.entity.WorkoutSessionEntity
import com.example.gymtracking.data.local.relation.SessionWithDetails
import com.example.gymtracking.ui.components.EmptyState
import com.example.gymtracking.ui.components.GymButton
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.components.GymIconButton
import com.example.gymtracking.ui.components.GymLoadingIndicator
import com.example.gymtracking.ui.components.SwipeableCard
import com.example.gymtracking.ui.components.SwipeAction
import com.example.gymtracking.ui.components.SwipeDirection
import com.example.gymtracking.ui.theme.*
import com.example.gymtracking.ui.util.DateFormatter

import com.example.gymtracking.ui.components.ScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val languageCode by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.USER_LANGUAGE] ?: "en" }
    }.collectAsState(initial = "en")
    var selectedSessionId by remember { mutableStateOf<Int?>(null) }
    var sessionToDelete by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

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
                    title = stringResource(R.string.history_title),
                    subtitle = "WORKOUT LOGS",
                    icon = Icons.Rounded.History
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                itemsIndexed(uiState.sessions, key = { _, s -> s.session.id }) { index, sessionDetails ->
                    val session = sessionDetails.session
                    val planName = sessionDetails.plan.nome

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                sessionToDelete = session
                            }
                            false
                        }
                    )

                    // Reset swipe state when dialog is dismissed
                    LaunchedEffect(sessionToDelete) {
                        if (sessionToDelete == null && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val progress = dismissState.progress
                            
                            val color by animateColorAsState(
                                if (progress > 0f) Error.copy(alpha = 0.6f) else Color.Transparent,
                                label = "bg_color"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(Shapes.extraLarge)
                                    .background(color)
                                    .padding(horizontal = 28.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (progress > 0f) {
                                    Icon(
                                        Icons.Rounded.DeleteSweep,
                                        contentDescription = "Delete",
                                        tint = Error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    ) {
                        SessionHistoryCard(
                            session = session,
                            planName = planName,
                            isExpanded = selectedSessionId == session.id,
                            onClick = {
                                selectedSessionId = if (selectedSessionId == session.id) null else session.id
                                if (selectedSessionId == session.id) {
                                    viewModel.loadSessionDetails(session.id)
                                }
                            },
                            details = if (selectedSessionId == session.id) uiState.selectedSession else null,
                            languageCode = languageCode
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                sessionToDelete = null
            },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        sessionToDelete?.let { viewModel.deleteSession(it.id) }
                        sessionToDelete = null
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
}

@Composable
fun SessionHistoryCard(
    session: WorkoutSessionEntity,
    planName: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    details: SessionWithDetails?,
    languageCode: String = "en"
) {
    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceVariant.copy(alpha = 0.5f)
                )
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
                    HorizontalDivider(color = SurfaceContainerHighest, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (details == null) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            GymLoadingIndicator(size = 24.dp)
                        }
                    } else if (details.sets.isEmpty()) {
                        Text(stringResource(R.string.no_exercises_in_session_detail), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    } else {
                        val groupedSets = details.sets.groupBy { it.exercise.id }
                        
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
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
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
                                                text = "${set.pesoSollevato}kg × ${set.repsEffettive}",
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

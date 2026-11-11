package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import com.emanuel5014.trainable.ui.util.plusBottom
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.ai_section_title
import com.emanuel5014.trainable.sharedui.resources.ai_settings_desc
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.edit_username
import com.emanuel5014.trainable.sharedui.resources.exercises_manage_desc
import com.emanuel5014.trainable.sharedui.resources.exercises_title
import com.emanuel5014.trainable.sharedui.resources.member_since
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_classic
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_classic_desc
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_expressive
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_expressive_desc
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_floating
import com.emanuel5014.trainable.sharedui.resources.nav_bar_style_floating_desc
import com.emanuel5014.trainable.sharedui.resources.notifications_settings_desc
import com.emanuel5014.trainable.sharedui.resources.notifications_title
import com.emanuel5014.trainable.sharedui.resources.personalization_desc
import com.emanuel5014.trainable.sharedui.resources.personalization_title
import com.emanuel5014.trainable.sharedui.resources.preferences
import com.emanuel5014.trainable.sharedui.resources.profile
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.settings_title
import com.emanuel5014.trainable.sharedui.resources.swipe_actions
import com.emanuel5014.trainable.sharedui.resources.swipe_actions_desc
import com.emanuel5014.trainable.sharedui.resources.tactile_feedback
import com.emanuel5014.trainable.sharedui.resources.tactile_feedback_desc
import com.emanuel5014.trainable.sharedui.resources.target_workouts
import com.emanuel5014.trainable.sharedui.resources.today
import com.emanuel5014.trainable.sharedui.resources.username
import com.emanuel5014.trainable.sharedui.resources.weekly_goal
import com.emanuel5014.trainable.sharedui.resources.weight_unit
import com.emanuel5014.trainable.sharedui.resources.weight_unit_desc
import com.emanuel5014.trainable.sharedui.resources.workout_settings
import com.emanuel5014.trainable.sharedui.resources.workout_settings_desc
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.emanuel5014.trainable.data.model.NavBarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutSettings: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToPersonalizationSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    languageUi: @Composable () -> Unit = {},
    platformSections: @Composable () -> Unit = {},
    aboutSection: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    aiDescription: org.jetbrains.compose.resources.StringResource = Res.string.ai_settings_desc
) {
    val currentUser by state.currentUser.collectAsState()
    val weeklyGoal by state.weeklyGoal.collectAsState()
    val hapticEnabled by state.hapticEnabled.collectAsState()
    val weightUnit by state.weightUnit.collectAsState()
    val navBarStyle by state.navBarStyle.collectAsState()
    val swipeActionsEnabled by state.swipeActionsEnabled.collectAsState()
    Scaffold(
        containerColor = Surface,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.settings_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = Color.Transparent,
                            contentColor = OnSurface,
                            description = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.bodyPadding())
                .verticalScroll(scrollState)
                .padding(20.dp)
                .padding(bottom = paddingValues.scrollEndInset()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = stringResource(Res.string.profile)) {
                var showEditUsernameDialog by remember { mutableStateOf(false) }

                if (showEditUsernameDialog) {
                    var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
                    AlertDialog(
                        modifier = Modifier.imePadding(),
                        onDismissRequest = { showEditUsernameDialog = false },
                        containerColor = SurfaceContainerHigh,
                        title = { Text(stringResource(Res.string.edit_username), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                        text = {
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text(stringResource(Res.string.username)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = OnSurfaceVariant
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newUsername.isNotBlank()) {
                                        state.updateUsername(newUsername)
                                        showEditUsernameDialog = false
                                    }
                                }
                            ) {
                                Text(stringResource(Res.string.save).uppercase(), color = Primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditUsernameDialog = false }) {
                                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                            }
                        }
                    )
                }

                GymCard(
                    containerColor = SurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.username ?: "Athlete",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(Res.string.member_since, currentUser?.dataIscrizione?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(Res.string.today)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditUsernameDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(Res.string.edit), tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(Res.string.preferences)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Scale, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.weight_unit), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.weight_unit_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHighest)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "kg",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (weightUnit == "kg") Primary else Color.Transparent)
                                        .clickable { state.setWeightUnit("kg") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (weightUnit == "kg") OnPrimary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "lb",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (weightUnit == "lb") Primary else Color.Transparent)
                                        .clickable { state.setWeightUnit("lb") }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (weightUnit == "lb") OnPrimary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.weekly_goal), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.target_workouts), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (weeklyGoal > 1) state.setWeeklyGoal(weeklyGoal - 1) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                }
                                Text(
                                    text = weeklyGoal.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (weeklyGoal < com.emanuel5014.trainable.domain.WeeklyGoal.MAX) state.setWeeklyGoal(weeklyGoal + 1) }) {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                                }
                            }
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Vibration, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.tactile_feedback), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.tactile_feedback_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = hapticEnabled,
                                onCheckedChange = { state.setHapticEnabled(it) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        languageUi()

                        var showNavBarStyleDialog by remember { mutableStateOf(false) }

                        if (showNavBarStyleDialog) {
                            AlertDialog(
                                onDismissRequest = { showNavBarStyleDialog = false },
                                containerColor = SurfaceContainerHigh,
                                title = { Text(stringResource(Res.string.nav_bar_style), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                                text = {
                                    Column {
                                        SettingsChoiceOption(
                                            title = stringResource(Res.string.nav_bar_style_expressive),
                                            description = stringResource(Res.string.nav_bar_style_expressive_desc),
                                            isSelected = navBarStyle == NavBarStyle.Expressive,
                                            onClick = {
                                                state.setNavBarStyle(NavBarStyle.Expressive)
                                                showNavBarStyleDialog = false
                                            }
                                        )
                                        SettingsChoiceOption(
                                            title = stringResource(Res.string.nav_bar_style_floating),
                                            description = stringResource(Res.string.nav_bar_style_floating_desc),
                                            isSelected = navBarStyle == NavBarStyle.Floating,
                                            onClick = {
                                                state.setNavBarStyle(NavBarStyle.Floating)
                                                showNavBarStyleDialog = false
                                            }
                                        )
                                        SettingsChoiceOption(
                                            title = stringResource(Res.string.nav_bar_style_classic),
                                            description = stringResource(Res.string.nav_bar_style_classic_desc),
                                            isSelected = navBarStyle == NavBarStyle.Classic,
                                            onClick = {
                                                state.setNavBarStyle(NavBarStyle.Classic)
                                                showNavBarStyleDialog = false
                                            }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showNavBarStyleDialog = false }) {
                                        Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNavBarStyleDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Dashboard, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.nav_bar_style), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        when (navBarStyle) {
                                            NavBarStyle.Classic -> stringResource(Res.string.nav_bar_style_classic)
                                            NavBarStyle.Floating -> stringResource(Res.string.nav_bar_style_floating)
                                            NavBarStyle.Expressive -> stringResource(Res.string.nav_bar_style_expressive)
                                        },
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.swipe_actions), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.swipe_actions_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = swipeActionsEnabled,
                                onCheckedChange = { state.setSwipeActionsEnabled(it) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToWorkoutSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.workout_settings), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.workout_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToExercises() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Collections,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.exercises_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.exercises_manage_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAiSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.DocumentScanner,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.ai_section_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(aiDescription), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPersonalizationSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.personalization_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.personalization_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToNotificationSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.notifications_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.notifications_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            platformSections()
            aboutSection()
        }
    }
}

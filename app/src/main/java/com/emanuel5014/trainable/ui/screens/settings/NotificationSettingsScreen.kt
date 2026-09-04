package com.emanuel5014.trainable.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val haptic = LocalHapticFeedback.current
    val timerNotificationsEnabled by viewModel.timerNotificationsEnabled.collectAsState()
    val timerFinishedLockscreenVibrationDuration by viewModel.timerFinishedLockscreenVibrationDuration.collectAsState()
    val gymMembershipExpiryNotificationsEnabled by viewModel.gymMembershipExpiryNotificationsEnabled.collectAsState()
    val gymMembershipExpiryNotificationDaysBefore by viewModel.gymMembershipExpiryNotificationDaysBefore.collectAsState()
    val warmupTimerEnabled by viewModel.warmupTimerEnabled.collectAsState()

    val context = LocalContext.current
    var pendingNotificationType by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingNotificationType) {
                "timer" -> viewModel.setTimerNotificationsEnabled(true)
                "membership" -> viewModel.setGymMembershipExpiryNotificationsEnabled(true)
            }
        } else {
            Toast.makeText(context, "Permission denied for notifications", Toast.LENGTH_SHORT).show()
        }
        pendingNotificationType = null
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted && timerNotificationsEnabled) {
                viewModel.setTimerNotificationsEnabled(false)
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.notifications_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(stringResource(R.string.timer_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.timer_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = timerNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    pendingNotificationType = "timer"
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setTimerNotificationsEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (timerNotificationsEnabled) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Vibration,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.lockscreen_vibration), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(stringResource(R.string.lockscreen_vibration_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = if (timerFinishedLockscreenVibrationDuration == 0) {
                                        stringResource(R.string.vibration_default)
                                    } else {
                                        stringResource(R.string.vibration_seconds, timerFinishedLockscreenVibrationDuration)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            var lastHapticSliderValue by remember { mutableIntStateOf(-1) }

                            Slider(
                                value = timerFinishedLockscreenVibrationDuration.toFloat(),
                                onValueChange = {
                                    val newValue = it.toInt()
                                    if (newValue != lastHapticSliderValue) {
                                        lastHapticSliderValue = newValue
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    viewModel.setTimerFinishedLockscreenVibrationDuration(newValue)
                                },
                                valueRange = 0f..30f,
                                steps = 29,
                                colors = SliderDefaults.colors(
                                    thumbColor = Primary,
                                    activeTrackColor = Primary,
                                    inactiveTrackColor = SurfaceContainerHighest,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.warmup_timer), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.warmup_timer_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = warmupTimerEnabled,
                            onCheckedChange = { viewModel.setWarmupTimerEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.gym_membership_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.gym_membership_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = gymMembershipExpiryNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    pendingNotificationType = "membership"
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setGymMembershipExpiryNotificationsEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (gymMembershipExpiryNotificationsEnabled) {
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
                                    Text(stringResource(R.string.notify_days_before), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.notify_days_before_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore > 1) viewModel.setGymMembershipExpiryNotificationDaysBefore(gymMembershipExpiryNotificationDaysBefore - 1) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                }
                                Text(
                                    text = gymMembershipExpiryNotificationDaysBefore.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore < 30) viewModel.setGymMembershipExpiryNotificationDaysBefore(gymMembershipExpiryNotificationDaysBefore + 1) }) {
                                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

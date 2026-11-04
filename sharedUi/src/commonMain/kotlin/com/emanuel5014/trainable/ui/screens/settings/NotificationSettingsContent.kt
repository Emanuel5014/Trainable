package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.gym_membership_notifications
import com.emanuel5014.trainable.sharedui.resources.gym_membership_notifications_desc
import com.emanuel5014.trainable.sharedui.resources.lockscreen_vibration
import com.emanuel5014.trainable.sharedui.resources.lockscreen_vibration_desc
import com.emanuel5014.trainable.sharedui.resources.notifications_title
import com.emanuel5014.trainable.sharedui.resources.notifications_unavailable
import com.emanuel5014.trainable.sharedui.resources.notify_days_before
import com.emanuel5014.trainable.sharedui.resources.notify_days_before_desc
import com.emanuel5014.trainable.sharedui.resources.timer_notifications
import com.emanuel5014.trainable.sharedui.resources.timer_notifications_desc
import com.emanuel5014.trainable.sharedui.resources.vibration_default
import com.emanuel5014.trainable.sharedui.resources.vibration_seconds
import com.emanuel5014.trainable.sharedui.resources.warmup_timer
import com.emanuel5014.trainable.sharedui.resources.warmup_timer_settings_desc
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
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(
    onNavigateBack: () -> Unit,
    state: SettingsState,
    onTimerNotificationsChange: (Boolean) -> Unit,
    onMembershipNotificationsChange: (Boolean) -> Unit,
    onMembershipDaysBeforeChange: (Int) -> Unit,
    deliveryAvailable: Boolean = true
) {
    val hapticEnabled by state.hapticEnabled.collectAsState()
    val timerNotificationsEnabled by state.timerNotificationsEnabled.collectAsState()
    val timerFinishedLockscreenVibrationDuration by state.timerFinishedLockscreenVibrationDuration.collectAsState()
    val gymMembershipExpiryNotificationsEnabled by state.gymMembershipExpiryNotificationsEnabled.collectAsState()
    val gymMembershipExpiryNotificationDaysBefore by state.gymMembershipExpiryNotificationDaysBefore.collectAsState()
    val warmupTimerEnabled by state.warmupTimerEnabled.collectAsState()
    val haptic = LocalHapticFeedback.current
    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.notifications_title),
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
            if (!deliveryAvailable) {
                Text(stringResource(Res.string.notifications_unavailable), color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
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
                                Text(stringResource(Res.string.timer_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(Res.string.timer_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = timerNotificationsEnabled && deliveryAvailable,
                            onCheckedChange = onTimerNotificationsChange,
                            enabled = deliveryAvailable
                        )
                    }

                    if (timerNotificationsEnabled && deliveryAvailable) {
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
                                        Text(stringResource(Res.string.lockscreen_vibration), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(stringResource(Res.string.lockscreen_vibration_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = if (timerFinishedLockscreenVibrationDuration == 0) {
                                        stringResource(Res.string.vibration_default)
                                    } else {
                                        stringResource(Res.string.vibration_seconds, timerFinishedLockscreenVibrationDuration)
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
                                    state.setTimerFinishedLockscreenVibrationDuration(newValue)
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
                                Text(stringResource(Res.string.warmup_timer), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(Res.string.warmup_timer_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = warmupTimerEnabled,
                            onCheckedChange = { state.setWarmupTimerEnabled(it) }
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
                                Text(stringResource(Res.string.gym_membership_notifications), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(Res.string.gym_membership_notifications_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = gymMembershipExpiryNotificationsEnabled && deliveryAvailable,
                            onCheckedChange = onMembershipNotificationsChange,
                            enabled = deliveryAvailable
                        )
                    }

                    if (gymMembershipExpiryNotificationsEnabled && deliveryAvailable) {
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
                                    Text(stringResource(Res.string.notify_days_before), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.notify_days_before_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore > 1) onMembershipDaysBeforeChange(gymMembershipExpiryNotificationDaysBefore - 1) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                }
                                Text(
                                    text = gymMembershipExpiryNotificationDaysBefore.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore < 30) onMembershipDaysBeforeChange(gymMembershipExpiryNotificationDaysBefore + 1) }) {
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

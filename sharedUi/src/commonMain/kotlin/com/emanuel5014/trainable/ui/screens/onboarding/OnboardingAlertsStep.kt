package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.Slider
import org.jetbrains.compose.resources.stringResource

/** "Resta aggiornato": timer notifications (+ lock-screen vibration on Android) and the membership reminder. */
@Composable
fun AlertsStep(choices: OnboardingChoices, customVibrationSupported: Boolean, hero: @Composable () -> Unit) {
    var timer by choices.timerNotificationsEnabled
    var vibration by choices.timerFinishedLockscreenVibrationDuration
    var membership by choices.gymMembershipExpiryNotificationsEnabled
    var daysBefore by choices.gymMembershipExpiryNotificationDaysBefore
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by choices.hapticEnabled

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_notifications_title))

        JoinedColumn(
            listOf(
                { position ->
                    SwitchRow(
                        position = position,
                        title = stringResource(Res.string.timer_notifications),
                        description = stringResource(Res.string.timer_notifications_desc),
                        checked = timer,
                        onCheckedChange = { timer = it },
                        options = if (customVibrationSupported) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(Res.string.lockscreen_vibration),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (vibration == 0) stringResource(Res.string.vibration_default)
                                        else stringResource(Res.string.vibration_seconds, vibration),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                var last by remember { mutableIntStateOf(-1) }
                                Slider(
                                    value = vibration.toFloat(),
                                    onValueChange = {
                                        val value = it.toInt()
                                        if (value != last) {
                                            last = value
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        vibration = value
                                    },
                                    valueRange = 0f..30f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else null
                    )
                },
                { position ->
                    SwitchRow(
                        position = position,
                        title = stringResource(Res.string.gym_membership_notifications),
                        description = stringResource(Res.string.gym_membership_notifications_desc),
                        checked = membership,
                        onCheckedChange = { membership = it },
                        options = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.notify_days_before),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FilledTonalIconButton(onClick = { if (daysBefore > 1) daysBefore-- }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "−")
                                }
                                Text(daysBefore.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                FilledTonalIconButton(onClick = { if (daysBefore < 30) daysBefore++ }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "+")
                                }
                            }
                        }
                    )
                }
            )
        )
    }
}

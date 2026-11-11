package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.util.DateFormatter
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

/** "Iniziamo da te": name, unit, workouts per week, weight and the optional membership expiry. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileStep(choices: OnboardingChoices, hero: @Composable () -> Unit) {
    var username by choices.username
    var weightInput by choices.weightInput
    var goal by choices.weeklyGoalInput
    val unit by choices.weightUnit
    val expiry by choices.membershipExpiryDate
    var showDatePicker by remember { mutableStateOf(false) }

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_you_title))

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            GymInputField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(Res.string.onboarding_setup_username_label),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.onboarding_weight_unit))
                OptionGroup(listOf("kg", "lb"), unit, { it }, { choices.changeUnit(it) })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.onboarding_setup_goal_label))
                OptionGroup(
                    options = (1..7).map { it.toString() },
                    selected = goal.toIntOrNull()?.coerceIn(1, 7)?.toString() ?: "3",
                    label = { it },
                    onSelect = { goal = it }
                )
            }

            GymInputField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = stringResource(Res.string.onboarding_setup_weight_label),
                trailingIcon = { Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
            )

            JoinedRowSurface(JoinedPosition.Single) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.onboarding_membership_expiry),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            expiry?.let { DateFormatter.format(it) } ?: stringResource(Res.string.onboarding_membership_pick),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expiry != null) {
                        IconButton(onClick = { choices.pickMembershipDate(null) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.onboarding_membership_clear))
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expiry ?: Clock.System.now().toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { choices.pickMembershipDate(it) }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}

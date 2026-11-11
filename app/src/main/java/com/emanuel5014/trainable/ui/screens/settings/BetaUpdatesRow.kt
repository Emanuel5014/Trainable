package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

/**
 * Android only: the beta-updates switch under "Check for updates". Turning it on first lists the risks and asks;
 * turning it off is immediate. A note under the switch says what to expect in either state.
 */
@Composable
fun BetaUpdatesRow(
    betaOn: Boolean,
    installedIsBeta: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var askToEnable by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Science, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.beta_updates_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    stringResource(R.string.beta_updates_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            SettingsSwitch(
                checked = betaOn,
                onCheckedChange = { on -> if (on) askToEnable = true else onChange(false) }
            )
        }

        val note = when {
            betaOn -> stringResource(R.string.beta_updates_on_note)
            installedIsBeta -> stringResource(R.string.beta_updates_off_note)
            else -> null
        }
        if (note != null) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 40.dp)) {
                if (betaOn) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(note, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }

    if (askToEnable) {
        AlertDialog(
            onDismissRequest = { askToEnable = false },
            icon = { Icon(Icons.Rounded.Science, contentDescription = null, tint = Primary) },
            title = {
                Text(stringResource(R.string.beta_updates_dialog_title), fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    stringResource(R.string.beta_updates_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                Button(onClick = {
                    askToEnable = false
                    onChange(true)
                }) { Text(stringResource(R.string.beta_updates_enable)) }
            },
            dismissButton = {
                TextButton(onClick = { askToEnable = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import java.text.DateFormat
import java.util.Locale

/** What the advanced programming features are, with the switch that turns them on. */
@Composable
internal fun AdvancedSlide(
    advancedProgramming: Boolean,
    onAdvancedProgrammingChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_advanced_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        CustomizeToggleItem(
            icon = Icons.Rounded.Percent,
            title = stringResource(R.string.advanced_programming),
            desc = stringResource(R.string.advanced_programming_desc),
            checked = advancedProgramming,
            onCheckedChange = onAdvancedProgrammingChange
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.EmojiEvents,
                title = stringResource(R.string.onboarding_advanced_percent_title),
                desc = stringResource(R.string.onboarding_advanced_percent_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.onboarding_advanced_techniques_title),
                desc = stringResource(R.string.onboarding_advanced_techniques_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.CalendarMonth,
                title = stringResource(R.string.onboarding_advanced_weeks_title),
                desc = stringResource(R.string.onboarding_advanced_weeks_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.onboarding_advanced_scan_title),
                desc = stringResource(R.string.onboarding_advanced_scan_desc)
            )
        }

        Text(
            text = stringResource(R.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(140.dp))
    }
}

/** Connect an own Nextcloud, choose how often to back up to it, or restore a backup from it (new phone). */
@Composable
internal fun NextcloudSlide(
    connectedAs: String?,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    remoteFolder: String,
    onRemoteFolderChange: (String) -> Unit,
    isConnecting: Boolean,
    connectState: NextcloudConnectionResult?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    autoBackupEnabled: Boolean,
    onAutoBackupChange: (Boolean) -> Unit,
    frequency: Int,
    onFrequencyChange: (Int) -> Unit,
    maxCount: Int,
    onMaxCountChange: (Int) -> Unit,
    includeImages: Boolean,
    onIncludeImagesChange: (Boolean) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    onRestore: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_nextcloud_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )
        Text(
            text = stringResource(R.string.onboarding_nextcloud_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            lineHeight = 22.sp
        )

        if (connectedAs == null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GymInputField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    label = stringResource(R.string.nextcloud_server_url),
                    placeholder = stringResource(R.string.nextcloud_server_url_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                GymInputField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.nextcloud_username)
                )
                GymInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.nextcloud_password_token),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = OnSurfaceVariant
                            )
                        }
                    }
                )
                GymInputField(
                    value = remoteFolder,
                    onValueChange = onRemoteFolderChange,
                    label = stringResource(R.string.nextcloud_remote_folder),
                    placeholder = stringResource(R.string.nextcloud_remote_folder_hint)
                )

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.nextcloud_app_password_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                GymButton(
                    onClick = onConnect,
                    enabled = !isConnecting && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.nextcloud_testing_connection), fontWeight = FontWeight.ExtraBold)
                    } else {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.nextcloud_connect).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }

                (connectState as? NextcloudConnectionResult.Error)?.let { error ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error)
                        Text(
                            text = stringResource(R.string.nextcloud_connection_failed, error.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                    Text(
                        text = connectedAs,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                TextButton(onClick = onDisconnect) {
                    Text(stringResource(R.string.nextcloud_disconnect).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            CustomizeToggleItem(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(R.string.nextcloud_auto_backup),
                desc = stringResource(R.string.nextcloud_auto_backup_desc),
                checked = autoBackupEnabled,
                onCheckedChange = onAutoBackupChange
            )

            if (autoBackupEnabled) {
                StepperRow(
                    label = stringResource(R.string.frequency),
                    value = stringResource(R.string.day_s, frequency),
                    onDecrease = { if (frequency > 1) onFrequencyChange(frequency - 1) },
                    onIncrease = { if (frequency < 7) onFrequencyChange(frequency + 1) }
                )
                StepperRow(
                    label = stringResource(R.string.keep_last),
                    value = stringResource(R.string.backup_s, maxCount),
                    onDecrease = { if (maxCount > 1) onMaxCountChange(maxCount - 1) },
                    onIncrease = { if (maxCount < 10) onMaxCountChange(maxCount + 1) }
                )
                SwitchRow(
                    title = stringResource(R.string.include_images),
                    description = null,
                    checked = includeImages,
                    onCheckedChange = onIncludeImagesChange
                )
                SwitchRow(
                    title = stringResource(R.string.nextcloud_wifi_only),
                    description = stringResource(R.string.nextcloud_wifi_only_desc),
                    checked = wifiOnly,
                    onCheckedChange = onWifiOnlyChange
                )
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            GymButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = OnSurface
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.nextcloud_restore), fontWeight = FontWeight.ExtraBold)
            }
        }

        Text(
            text = stringResource(R.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
private fun StepperRow(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) {
                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = OnSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
            IconButton(onClick = onIncrease) {
                Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Lists the backups on the server, newest first; picking one asks for confirmation before it replaces the data. */
@Composable
internal fun NextcloudRestoreDialog(
    backups: List<NextcloudBackupFile>,
    isLoading: Boolean,
    isRestoring: Boolean,
    selected: NextcloudBackupFile?,
    onPick: (NextcloudBackupFile) -> Unit,
    onConfirm: (NextcloudBackupFile) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) onBack() },
            containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(R.string.nextcloud_restore_confirm_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = {
                if (isRestoring) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        Text(stringResource(R.string.nextcloud_restoring), color = OnSurfaceVariant)
                    }
                } else {
                    Text(stringResource(R.string.nextcloud_restore_confirm_desc), color = OnSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(enabled = !isRestoring, onClick = { onConfirm(selected) }) {
                    Text(stringResource(R.string.confirm).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isRestoring, onClick = onBack) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(R.string.nextcloud_backups_list_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.nextcloud_restore_picker_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                when {
                    isLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        Text(stringResource(R.string.nextcloud_loading_backups), color = OnSurfaceVariant)
                    }
                    backups.isEmpty() -> Text(stringResource(R.string.nextcloud_no_backups_found), color = OnSurfaceVariant)
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backups.sortedByDescending { it.lastModified.time }, key = { it.remotePath }) { backup ->
                            BackupRow(backup, onClick = { onPick(backup) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun BackupRow(backup: NextcloudBackupFile, onClick: () -> Unit) {
    val date = remember(backup.lastModified) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(backup.lastModified)
    }
    val size = String.format(Locale.getDefault(), "%.1f MB", backup.sizeBytes / 1_048_576f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(SurfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(date, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.ExtraBold)
        Text("$size · ${backup.name}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 1)
    }
}

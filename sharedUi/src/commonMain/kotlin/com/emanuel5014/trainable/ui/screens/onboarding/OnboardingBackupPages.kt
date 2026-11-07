@file:Suppress("UnusedImport")

package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.ui.components.*
import com.emanuel5014.trainable.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.auto_backup
import com.emanuel5014.trainable.sharedui.resources.backup_disabled
import com.emanuel5014.trainable.sharedui.resources.backup_enabled
import com.emanuel5014.trainable.sharedui.resources.backup_s
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.choose_folder
import com.emanuel5014.trainable.sharedui.resources.confirm
import com.emanuel5014.trainable.sharedui.resources.day_s
import com.emanuel5014.trainable.sharedui.resources.folder_required_desc
import com.emanuel5014.trainable.sharedui.resources.frequency
import com.emanuel5014.trainable.sharedui.resources.import_database
import com.emanuel5014.trainable.sharedui.resources.include_images
import com.emanuel5014.trainable.sharedui.resources.keep_last
import com.emanuel5014.trainable.sharedui.resources.nextcloud_app_password_notice
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_backups_list_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_connect
import com.emanuel5014.trainable.sharedui.resources.nextcloud_connection_failed
import com.emanuel5014.trainable.sharedui.resources.nextcloud_disconnect
import com.emanuel5014.trainable.sharedui.resources.nextcloud_loading_backups
import com.emanuel5014.trainable.sharedui.resources.nextcloud_no_backups_found
import com.emanuel5014.trainable.sharedui.resources.nextcloud_password_token
import com.emanuel5014.trainable.sharedui.resources.nextcloud_remote_folder
import com.emanuel5014.trainable.sharedui.resources.nextcloud_remote_folder_hint
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_confirm_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_confirm_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_picker_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restoring
import com.emanuel5014.trainable.sharedui.resources.nextcloud_server_url
import com.emanuel5014.trainable.sharedui.resources.nextcloud_server_url_hint
import com.emanuel5014.trainable.sharedui.resources.nextcloud_testing_connection
import com.emanuel5014.trainable.sharedui.resources.nextcloud_username
import com.emanuel5014.trainable.sharedui.resources.nextcloud_wifi_only
import com.emanuel5014.trainable.sharedui.resources.nextcloud_wifi_only_desc
import com.emanuel5014.trainable.sharedui.resources.onboarding_backup_title
import com.emanuel5014.trainable.sharedui.resources.onboarding_nextcloud_desc
import com.emanuel5014.trainable.sharedui.resources.onboarding_nextcloud_title
import com.emanuel5014.trainable.sharedui.resources.onboarding_setup_footer
import com.emanuel5014.trainable.sharedui.resources.storage_location

// The onboarding's automatic-backup and Nextcloud pages (moved unchanged from Android, B2).

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupSlide(
    autoBackupEnabled: Boolean,
    onAutoBackupChange: (Boolean) -> Unit,
    autoBackupFrequency: Int,
    onAutoBackupFrequencyChange: (Int) -> Unit,
    autoBackupFolderUri: String?,
    autoBackupMaxCount: Int,
    onAutoBackupMaxCountChange: (Int) -> Unit,
    autoBackupIncludeImages: Boolean,
    onAutoBackupIncludeImagesChange: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onImport: () -> Unit
) {

    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_backup_title))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Auto Backup Toggle
            CustomizeToggleItem(
                icon = Icons.Rounded.Backup,
                title = stringResource(Res.string.auto_backup),
                desc = if (autoBackupEnabled) stringResource(Res.string.backup_enabled, autoBackupFrequency) else stringResource(Res.string.backup_disabled),
                checked = autoBackupEnabled,
                onCheckedChange = onAutoBackupChange
            )

            if (autoBackupEnabled) {
                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.frequency), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (autoBackupFrequency > 1) onAutoBackupFrequencyChange(autoBackupFrequency - 1) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                        }
                        Text(stringResource(Res.string.day_s, autoBackupFrequency), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                        IconButton(onClick = { if (autoBackupFrequency < 7) onAutoBackupFrequencyChange(autoBackupFrequency + 1) }) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Max Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.keep_last), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (autoBackupMaxCount > 1) onAutoBackupMaxCountChange(autoBackupMaxCount - 1) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                        }
                        Text(stringResource(Res.string.backup_s, autoBackupMaxCount), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                        IconButton(onClick = { if (autoBackupMaxCount < 10) onAutoBackupMaxCountChange(autoBackupMaxCount + 1) }) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Include Images
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.include_images), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    }
                    Switch(
                        checked = autoBackupIncludeImages,
                        onCheckedChange = onAutoBackupIncludeImagesChange,
                        thumbContent = if (autoBackupIncludeImages) {
                            {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize), tint = Primary)
                            }
                        } else null
                    )
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Folder Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.storage_location), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    GymButton(
                        onClick = onPickFolder,
                        containerColor = if (autoBackupFolderUri == null) Primary.copy(alpha = 0.1f) else SurfaceContainerHighest,
                        contentColor = if (autoBackupFolderUri == null) Primary else OnSurface
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (autoBackupFolderUri != null) "Folder selected"
                            else stringResource(Res.string.choose_folder)
                        )
                    }
                    if (autoBackupFolderUri == null) {
                        Text(
                            stringResource(Res.string.folder_required_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error
                        )
                    }
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Import Button
            GymButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = OnSurface
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.import_database), fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

    }
}


/** Connect an own Nextcloud, choose how often to back up to it, or restore a backup from it (new phone). */
@Composable
fun NextcloudSlide(
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

    OnboardingPage(verticalArrangement = Arrangement.spacedBy(OnboardingMetrics.sectionSpacing * 0.75f)) {
        OnboardingTitle(stringResource(Res.string.onboarding_nextcloud_title))
        Text(
            text = stringResource(Res.string.onboarding_nextcloud_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            lineHeight = 22.sp
        )

        if (connectedAs == null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GymInputField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    label = stringResource(Res.string.nextcloud_server_url),
                    placeholder = stringResource(Res.string.nextcloud_server_url_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                GymInputField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = stringResource(Res.string.nextcloud_username)
                )
                GymInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(Res.string.nextcloud_password_token),
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
                    label = stringResource(Res.string.nextcloud_remote_folder),
                    placeholder = stringResource(Res.string.nextcloud_remote_folder_hint)
                )

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(Res.string.nextcloud_app_password_notice),
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
                        Text(stringResource(Res.string.nextcloud_testing_connection), fontWeight = FontWeight.ExtraBold)
                    } else {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.nextcloud_connect).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }

                (connectState as? NextcloudConnectionResult.Error)?.let { error ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error)
                        Text(
                            text = stringResource(Res.string.nextcloud_connection_failed, error.message),
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
                    Text(stringResource(Res.string.nextcloud_disconnect).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            CustomizeToggleItem(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(Res.string.nextcloud_auto_backup),
                desc = stringResource(Res.string.nextcloud_auto_backup_desc),
                checked = autoBackupEnabled,
                onCheckedChange = onAutoBackupChange
            )

            if (autoBackupEnabled) {
                StepperRow(
                    label = stringResource(Res.string.frequency),
                    value = stringResource(Res.string.day_s, frequency),
                    onDecrease = { if (frequency > 1) onFrequencyChange(frequency - 1) },
                    onIncrease = { if (frequency < 7) onFrequencyChange(frequency + 1) }
                )
                StepperRow(
                    label = stringResource(Res.string.keep_last),
                    value = stringResource(Res.string.backup_s, maxCount),
                    onDecrease = { if (maxCount > 1) onMaxCountChange(maxCount - 1) },
                    onIncrease = { if (maxCount < 10) onMaxCountChange(maxCount + 1) }
                )
                SwitchRow(
                    title = stringResource(Res.string.include_images),
                    description = null,
                    checked = includeImages,
                    onCheckedChange = onIncludeImagesChange
                )
                SwitchRow(
                    title = stringResource(Res.string.nextcloud_wifi_only),
                    description = stringResource(Res.string.nextcloud_wifi_only_desc),
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
                Text(stringResource(Res.string.nextcloud_restore), fontWeight = FontWeight.ExtraBold)
            }
        }

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

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
fun NextcloudRestoreDialog(
    backups: List<NextcloudBackupFile>,
    isLoading: Boolean,
    isRestoring: Boolean,
    selected: NextcloudBackupFile?,
    onPick: (NextcloudBackupFile) -> Unit,
    onConfirm: (NextcloudBackupFile) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    /** Android: DateFormat MEDIUM/SHORT; iOS: its own medium/short date. */
    dateText: (NextcloudBackupFile) -> String,
    /** Android: "%.1f MB" with the device locale. */
    sizeText: (Long) -> String
) {
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) onBack() },
            containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(Res.string.nextcloud_restore_confirm_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = {
                if (isRestoring) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        Text(stringResource(Res.string.nextcloud_restoring), color = OnSurfaceVariant)
                    }
                } else {
                    Text(stringResource(Res.string.nextcloud_restore_confirm_desc), color = OnSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(enabled = !isRestoring, onClick = { onConfirm(selected) }) {
                    Text(stringResource(Res.string.confirm).uppercase(), color = Primary, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isRestoring, onClick = onBack) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.nextcloud_backups_list_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.nextcloud_restore_picker_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                when {
                    isLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        Text(stringResource(Res.string.nextcloud_loading_backups), color = OnSurfaceVariant)
                    }
                    backups.isEmpty() -> Text(stringResource(Res.string.nextcloud_no_backups_found), color = OnSurfaceVariant)
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backups.sortedByDescending { it.lastModifiedMillis }, key = { it.remotePath }) { backup ->
                            BackupRow(backup, dateText, sizeText, onClick = { onPick(backup) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun BackupRow(backup: NextcloudBackupFile, dateText: (NextcloudBackupFile) -> String, sizeText: (Long) -> String, onClick: () -> Unit) {
    val date = remember(backup.lastModifiedMillis) { dateText(backup) }
    val size = sizeText(backup.sizeBytes)
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

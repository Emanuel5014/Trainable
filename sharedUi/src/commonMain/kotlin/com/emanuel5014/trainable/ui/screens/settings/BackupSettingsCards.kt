@file:Suppress("UnusedImport")

package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ic_nextcloud
import com.emanuel5014.trainable.sharedui.resources.auto_backup
import com.emanuel5014.trainable.sharedui.resources.auto_backup_setup
import com.emanuel5014.trainable.sharedui.resources.backup_disabled
import com.emanuel5014.trainable.sharedui.resources.backup_enabled
import com.emanuel5014.trainable.sharedui.resources.backup_s
import com.emanuel5014.trainable.sharedui.resources.backup_type_auto
import com.emanuel5014.trainable.sharedui.resources.backup_type_manual
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.choose_folder
import com.emanuel5014.trainable.sharedui.resources.configure_backup
import com.emanuel5014.trainable.sharedui.resources.day_s
import com.emanuel5014.trainable.sharedui.resources.edit_backup
import com.emanuel5014.trainable.sharedui.resources.folder_required_desc
import com.emanuel5014.trainable.sharedui.resources.frequency
import com.emanuel5014.trainable.sharedui.resources.include_images
import com.emanuel5014.trainable.sharedui.resources.include_images_description
import com.emanuel5014.trainable.sharedui.resources.keep_last
import com.emanuel5014.trainable.sharedui.resources.nextcloud_app_password_notice
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup_setup
import com.emanuel5014.trainable.sharedui.resources.nextcloud_auto_backup_summary
import com.emanuel5014.trainable.sharedui.resources.nextcloud_backup_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_backup_now
import com.emanuel5014.trainable.sharedui.resources.nextcloud_backups_list_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_configured_as
import com.emanuel5014.trainable.sharedui.resources.nextcloud_connection_failed
import com.emanuel5014.trainable.sharedui.resources.nextcloud_connection_successful
import com.emanuel5014.trainable.sharedui.resources.nextcloud_disconnect
import com.emanuel5014.trainable.sharedui.resources.nextcloud_disconnect_confirm
import com.emanuel5014.trainable.sharedui.resources.nextcloud_loading_backups
import com.emanuel5014.trainable.sharedui.resources.nextcloud_no_backups_found
import com.emanuel5014.trainable.sharedui.resources.nextcloud_not_configured
import com.emanuel5014.trainable.sharedui.resources.nextcloud_password_hint
import com.emanuel5014.trainable.sharedui.resources.nextcloud_password_token
import com.emanuel5014.trainable.sharedui.resources.nextcloud_remote_folder
import com.emanuel5014.trainable.sharedui.resources.nextcloud_remote_folder_hint
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_confirm_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_confirm_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_restore_picker_desc
import com.emanuel5014.trainable.sharedui.resources.nextcloud_server_url
import com.emanuel5014.trainable.sharedui.resources.nextcloud_server_url_hint
import com.emanuel5014.trainable.sharedui.resources.nextcloud_setup_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_test_connection
import com.emanuel5014.trainable.sharedui.resources.nextcloud_testing_connection
import com.emanuel5014.trainable.sharedui.resources.nextcloud_title
import com.emanuel5014.trainable.sharedui.resources.nextcloud_username
import com.emanuel5014.trainable.sharedui.resources.nextcloud_wifi_only
import com.emanuel5014.trainable.sharedui.resources.nextcloud_wifi_only_desc
import com.emanuel5014.trainable.sharedui.resources.no
import com.emanuel5014.trainable.sharedui.resources.restore_action
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.storage_location
import com.emanuel5014.trainable.sharedui.resources.with_images
import com.emanuel5014.trainable.sharedui.resources.without_images
import com.emanuel5014.trainable.sharedui.resources.yes

/** What the backup rows and dialogs show (Settings › Data): the same values Android's Settings screen collected. */
data class BackupSettingsUiState(
    val autoBackupEnabled: Boolean,
    val autoBackupFrequency: Int,
    val autoBackupFolderUri: String?,
    val autoBackupFolderLabel: String?,
    val autoBackupMaxCount: Int,
    val autoBackupIncludeImages: Boolean,
    val nextcloudBackupEnabled: Boolean,
    val nextcloudAutoBackupEnabled: Boolean,
    val nextcloudServerUrl: String?,
    val nextcloudUsername: String?,
    /** The server's host name for "Connected as … on …" (Android: Uri.parse(url).host, the URL itself when none). */
    val nextcloudHost: String?,
    val nextcloudRemoteFolder: String,
    val nextcloudWifiOnly: Boolean,
    val nextcloudAutoBackupFrequency: Int,
    val nextcloudAutoBackupMaxCount: Int,
    val nextcloudAutoBackupIncludeImages: Boolean,
    val nextcloudTestState: NextcloudConnectionResult?,
    val isTestingConnection: Boolean,
    val isNextcloudBackingUp: Boolean,
    val isNextcloudRestoring: Boolean,
    val nextcloudBackups: List<NextcloudBackupFile>,
    val isLoadingNextcloudBackups: Boolean,
)

/** What the backup rows and dialogs do; Android passes its SettingsViewModel functions of the same names. */
interface BackupSettingsActions {
    fun setAutoBackupEnabled(enabled: Boolean)
    fun setAutoBackupFrequency(frequency: Int)
    fun setAutoBackupMaxCount(count: Int)
    fun setAutoBackupIncludeImages(include: Boolean)
    fun setNextcloudBackupEnabled(enabled: Boolean)
    fun setNextcloudAutoBackupEnabled(enabled: Boolean)
    fun setNextcloudAutoBackupSettings(frequency: Int, maxCount: Int, includeImages: Boolean, wifiOnly: Boolean)
    fun clearNextcloudTestState()
    fun testNextcloudConnection(serverUrl: String, username: String, passwordOrToken: String)
    fun saveNextcloudConfig(serverUrl: String, username: String, passwordOrToken: String, remoteFolder: String)
    fun disconnectNextcloud()
    fun backupToNextcloud(includeImages: Boolean = false)
    fun loadNextcloudBackups()
    fun restoreFromNextcloud(backupFile: NextcloudBackupFile, onSuccess: () -> Unit)
}

/**
 * The automatic-backup and Nextcloud cards of Settings › Data and all their dialogs (moved unchanged from
 * Android's SettingsScreen, B2). [dateText]/[sizeText] format a backup's date and size; [onRestored] runs after
 * a successful Nextcloud restore (Android restarts the app, iOS asks to reopen it).
 */
@Composable
fun BackupSettingsCards(
    state: BackupSettingsUiState,
    actions: BackupSettingsActions,
    onPickFolder: () -> Unit,
    onRestored: () -> Unit,
    dateText: (NextcloudBackupFile) -> String,
    sizeText: (Long) -> String,
    /** Android passes its own vector (drawn by the platform, pixel-identical to before); iOS the shared copy. */
    nextcloudIcon: androidx.compose.ui.graphics.painter.Painter = painterResource(Res.drawable.ic_nextcloud),
    /** False hides the automatic local backup card (iOS until its Files folder support exists). */
    localSupported: Boolean = true
) {
    val autoBackupEnabled = state.autoBackupEnabled
    val autoBackupFrequency = state.autoBackupFrequency
    val autoBackupFolderUri = state.autoBackupFolderUri
    val autoBackupFolderLabel = state.autoBackupFolderLabel
    val autoBackupMaxCount = state.autoBackupMaxCount
    val autoBackupIncludeImages = state.autoBackupIncludeImages
    val nextcloudBackupEnabled = state.nextcloudBackupEnabled
    val nextcloudAutoBackupEnabled = state.nextcloudAutoBackupEnabled
    val nextcloudServerUrl = state.nextcloudServerUrl
    val nextcloudUsername = state.nextcloudUsername
    val nextcloudRemoteFolder = state.nextcloudRemoteFolder
    val nextcloudWifiOnly = state.nextcloudWifiOnly
    val nextcloudAutoBackupFrequency = state.nextcloudAutoBackupFrequency
    val nextcloudAutoBackupMaxCount = state.nextcloudAutoBackupMaxCount
    val nextcloudAutoBackupIncludeImages = state.nextcloudAutoBackupIncludeImages
    val nextcloudTestState = state.nextcloudTestState
    val isTestingConnection = state.isTestingConnection
    val isNextcloudBackingUp = state.isNextcloudBackingUp
    val isNextcloudRestoring = state.isNextcloudRestoring
    val nextcloudBackups = state.nextcloudBackups
    val isLoadingNextcloudBackups = state.isLoadingNextcloudBackups
    var showBackupSetupDialog by remember { mutableStateOf(false) }
    var showNextcloudSetupDialog by remember { mutableStateOf(false) }
    var showNextcloudBackupSetupDialog by remember { mutableStateOf(false) }
    var showNextcloudRestoreDialog by remember { mutableStateOf(false) }
    var showNextcloudDisconnectDialog by remember { mutableStateOf(false) }
    var showNextcloudIncludeImagesDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }

    if (showBackupSetupDialog) {
        var tempFrequency by remember { mutableIntStateOf(autoBackupFrequency) }
        var tempMaxCount by remember { mutableIntStateOf(autoBackupMaxCount) }
        var tempIncludeImages by remember { mutableStateOf(autoBackupIncludeImages) }

        AlertDialog(
            onDismissRequest = { showBackupSetupDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(Res.string.auto_backup_setup), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(Res.string.configure_backup), color = OnSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.frequency), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempFrequency > 1) tempFrequency-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(Res.string.day_s, tempFrequency), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempFrequency < 7) tempFrequency++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.keep_last), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempMaxCount > 1) tempMaxCount-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(Res.string.backup_s, tempMaxCount), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempMaxCount < 10) tempMaxCount++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.include_images), color = OnSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = tempIncludeImages,
                            onCheckedChange = { tempIncludeImages = it }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.storage_location), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        GymButton(
                            onClick = { onPickFolder() },
                            containerColor = if (autoBackupFolderUri == null) Primary.copy(alpha = 0.1f) else SurfaceContainerHighest,
                            contentColor = if (autoBackupFolderUri == null) Primary else OnSurface
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (autoBackupFolderUri != null)
                                    (autoBackupFolderLabel ?: "")
                                else
                                    stringResource(Res.string.choose_folder)
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
            },
            confirmButton = {
                TextButton(
                    enabled = autoBackupFolderUri != null,
                    onClick = {
                        actions.setAutoBackupFrequency(tempFrequency)
                        actions.setAutoBackupMaxCount(tempMaxCount)
                        actions.setAutoBackupIncludeImages(tempIncludeImages)
                        actions.setAutoBackupEnabled(true)
                        showBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save).uppercase(), color = if (autoBackupFolderUri != null) Primary else OnSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupSetupDialog = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showNextcloudSetupDialog) {
        var tempServerUrl by remember { mutableStateOf(nextcloudServerUrl ?: "") }
        var tempUsername by remember { mutableStateOf(nextcloudUsername ?: "") }
        var tempPassword by remember { mutableStateOf("") }
        var tempRemoteFolder by remember { mutableStateOf(nextcloudRemoteFolder) }
        var showPassword by remember { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = {
                showNextcloudSetupDialog = false
                actions.clearNextcloudTestState()
            },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(Res.string.nextcloud_setup_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        stringResource(Res.string.nextcloud_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text(stringResource(Res.string.nextcloud_server_url)) },
                        placeholder = { Text(stringResource(Res.string.nextcloud_server_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OnSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text(stringResource(Res.string.nextcloud_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OnSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text(stringResource(Res.string.nextcloud_password_token)) },
                        placeholder = { Text(stringResource(Res.string.nextcloud_password_hint)) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OnSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = tempRemoteFolder,
                        onValueChange = { tempRemoteFolder = it },
                        label = { Text(stringResource(Res.string.nextcloud_remote_folder)) },
                        placeholder = { Text(stringResource(Res.string.nextcloud_remote_folder_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OnSurfaceVariant
                        )
                    )

                    GymCard(containerColor = SurfaceContainerHighest) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(Res.string.nextcloud_app_password_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    GymButton(
                        onClick = {
                            actions.testNextcloudConnection(tempServerUrl, tempUsername, tempPassword)
                        },
                        enabled = !isTestingConnection && tempServerUrl.isNotBlank() && tempUsername.isNotBlank() && tempPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHighest,
                        contentColor = Primary
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.nextcloud_testing_connection))
                        } else {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.nextcloud_test_connection), fontWeight = FontWeight.Bold)
                        }
                    }

                    when (val result = nextcloudTestState) {
                        is NextcloudConnectionResult.Success -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary)
                                Text(
                                    stringResource(Res.string.nextcloud_connection_successful),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is NextcloudConnectionResult.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error)
                                Text(
                                    stringResource(Res.string.nextcloud_connection_failed, result.message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Error
                                )
                            }
                        }
                        null -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = tempServerUrl.isNotBlank() && tempUsername.isNotBlank() && tempPassword.isNotBlank(),
                    onClick = {
                        actions.saveNextcloudConfig(
                            serverUrl = tempServerUrl,
                            username = tempUsername,
                            passwordOrToken = tempPassword,
                            remoteFolder = tempRemoteFolder
                        )
                        actions.clearNextcloudTestState()
                        showNextcloudSetupDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save).uppercase(), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        actions.clearNextcloudTestState()
                        showNextcloudSetupDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showNextcloudBackupSetupDialog) {
        var tempFrequency by remember { mutableIntStateOf(nextcloudAutoBackupFrequency) }
        var tempMaxCount by remember { mutableIntStateOf(nextcloudAutoBackupMaxCount) }
        var tempIncludeImages by remember { mutableStateOf(nextcloudAutoBackupIncludeImages) }
        var tempWifiOnly by remember { mutableStateOf(nextcloudWifiOnly) }

        AlertDialog(
            onDismissRequest = { showNextcloudBackupSetupDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(Res.string.nextcloud_auto_backup_setup),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        stringResource(Res.string.nextcloud_auto_backup_desc),
                        color = OnSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(Res.string.frequency),
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { if (tempFrequency > 1) tempFrequency-- }) {
                                Icon(
                                    Icons.Rounded.RemoveCircleOutline,
                                    contentDescription = "Decrease",
                                    tint = OnSurfaceVariant
                                )
                            }
                            Text(
                                stringResource(Res.string.day_s, tempFrequency),
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { if (tempFrequency < 7) tempFrequency++ }) {
                                Icon(
                                    Icons.Rounded.AddCircleOutline,
                                    contentDescription = "Increase",
                                    tint = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(Res.string.keep_last),
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { if (tempMaxCount > 1) tempMaxCount-- }) {
                                Icon(
                                    Icons.Rounded.RemoveCircleOutline,
                                    contentDescription = "Decrease",
                                    tint = OnSurfaceVariant
                                )
                            }
                            Text(
                                stringResource(Res.string.backup_s, tempMaxCount),
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { if (tempMaxCount < 10) tempMaxCount++ }) {
                                Icon(
                                    Icons.Rounded.AddCircleOutline,
                                    contentDescription = "Increase",
                                    tint = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.include_images), color = OnSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = tempIncludeImages,
                            onCheckedChange = { tempIncludeImages = it }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.nextcloud_wifi_only), color = OnSurface, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(Res.string.nextcloud_wifi_only_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = tempWifiOnly,
                            onCheckedChange = { tempWifiOnly = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.setNextcloudAutoBackupSettings(
                            frequency = tempFrequency,
                            maxCount = tempMaxCount,
                            includeImages = tempIncludeImages,
                            wifiOnly = tempWifiOnly
                        )
                        showNextcloudBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.save).uppercase(), color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNextcloudBackupSetupDialog = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showNextcloudRestoreDialog) {
        Dialog(
            onDismissRequest = { showNextcloudRestoreDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(Res.string.nextcloud_backups_list_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = stringResource(Res.string.nextcloud_restore_picker_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { actions.loadNextcloudBackups() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerHighest)
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Body
                        if (isLoadingNextcloudBackups) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = Primary)
                                    Text(
                                        text = stringResource(Res.string.nextcloud_loading_backups),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        } else if (nextcloudBackups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.nextcloud_no_backups_found),
                                        color = OnSurface,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(nextcloudBackups) { backup ->
                                    val isAuto = backup.name.startsWith("Trainable_AutoBackup_")
                                    val formattedDate = remember(backup.name, backup.lastModifiedMillis) {
                                        dateText(backup)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedBackupForRestore = backup
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = SurfaceContainerHighest
                                        ),
                                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Primary.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            if (isAuto) Icons.Rounded.CloudSync else Icons.Rounded.CloudUpload,
                                                            contentDescription = null,
                                                            tint = Primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = formattedDate,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = OnSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isAuto) Primary.copy(alpha = 0.15f) else OnSurfaceVariant.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isAuto) stringResource(Res.string.backup_type_auto) else stringResource(Res.string.backup_type_manual),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isAuto) Primary else OnSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.15f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Storage,
                                                        contentDescription = null,
                                                        tint = OnSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = sizeText(backup.sizeBytes),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = OnSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Primary.copy(alpha = 0.12f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(Res.string.restore_action),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Primary,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Icon(
                                                        Icons.Rounded.ChevronRight,
                                                        contentDescription = null,
                                                        tint = Primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showNextcloudRestoreDialog = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.cancel).uppercase(),
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedBackupForRestore != null) {
        val backup = selectedBackupForRestore!!
        val isAuto = backup.name.startsWith("Trainable_AutoBackup_")
        val formattedDate = dateText(backup)

        AlertDialog(
            onDismissRequest = { selectedBackupForRestore = null },
            containerColor = SurfaceContainerHigh,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    stringResource(Res.string.nextcloud_restore_confirm_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        stringResource(Res.string.nextcloud_restore_confirm_desc),
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceContainerHighest
                        ),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sizeText(backup.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = if (isAuto) stringResource(Res.string.backup_type_auto) else stringResource(Res.string.backup_type_manual),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                GymButton(
                    onClick = {
                        val file = selectedBackupForRestore!!
                        selectedBackupForRestore = null
                        showNextcloudRestoreDialog = false
                        actions.restoreFromNextcloud(file) {
                            onRestored()
                        }
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary
                ) {
                    Text(stringResource(Res.string.restore_action).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupForRestore = null }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showNextcloudDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showNextcloudDisconnectDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(Res.string.nextcloud_disconnect),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(Res.string.nextcloud_disconnect_confirm),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.disconnectNextcloud()
                        showNextcloudDisconnectDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.nextcloud_disconnect).uppercase(), color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNextcloudDisconnectDialog = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showNextcloudIncludeImagesDialog) {
        AlertDialog(
            onDismissRequest = { showNextcloudIncludeImagesDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(Res.string.include_images),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(Res.string.include_images_description),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNextcloudIncludeImagesDialog = false
                        actions.backupToNextcloud(includeImages = true)
                    }
                ) {
                    Text(stringResource(Res.string.yes), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNextcloudIncludeImagesDialog = false
                        actions.backupToNextcloud(includeImages = false)
                    }
                ) {
                    Text(stringResource(Res.string.no), color = OnSurfaceVariant)
                }
            }
        )
    }


                if (localSupported) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Backup, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.auto_backup), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        if (autoBackupEnabled) stringResource(Res.string.backup_enabled, autoBackupFrequency) else stringResource(Res.string.backup_disabled),
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            SettingsSwitch(
                                checked = autoBackupEnabled,
                                onCheckedChange = {
                                    if (it) {
                                        showBackupSetupDialog = true
                                    } else {
                                        actions.setAutoBackupEnabled(false)
                                    }
                                }
                            )
                        }

                        if (autoBackupEnabled) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(Res.string.storage_location), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(
                                            (autoBackupFolderLabel ?: ""),
                                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { showBackupSetupDialog = true }) {
                                    Text(stringResource(Res.string.edit_backup).uppercase(), color = Primary)
                                }
                            }
                        }
                    }
                }
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
                                    painter = nextcloudIcon,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(Res.string.nextcloud_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        if (nextcloudBackupEnabled && nextcloudServerUrl != null && nextcloudUsername != null) {
                                            val host = state.nextcloudHost ?: nextcloudServerUrl
                                            stringResource(Res.string.nextcloud_configured_as, nextcloudUsername!!, host!!)
                                        } else {
                                            stringResource(Res.string.nextcloud_not_configured)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            SettingsSwitch(
                                checked = nextcloudBackupEnabled,
                                onCheckedChange = {
                                    if (it) {
                                        if (nextcloudServerUrl != null && nextcloudUsername != null) {
                                            actions.setNextcloudBackupEnabled(true)
                                        } else {
                                            showNextcloudSetupDialog = true
                                        }
                                    } else {
                                        actions.setNextcloudBackupEnabled(false)
                                    }
                                }
                            )
                        }

                        if (nextcloudBackupEnabled) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(Res.string.nextcloud_auto_backup),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        if (nextcloudAutoBackupEnabled)
                                            stringResource(
                                                Res.string.nextcloud_auto_backup_summary,
                                                nextcloudAutoBackupFrequency,
                                                nextcloudAutoBackupMaxCount,
                                                if (nextcloudAutoBackupIncludeImages) stringResource(Res.string.with_images) else stringResource(Res.string.without_images)
                                            )
                                        else
                                            stringResource(Res.string.backup_disabled),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                                SettingsSwitch(
                                    checked = nextcloudAutoBackupEnabled,
                                    onCheckedChange = {
                                        if (it) {
                                            showNextcloudBackupSetupDialog = true
                                        } else {
                                            actions.setNextcloudAutoBackupEnabled(false)
                                        }
                                    }
                                )
                            }

                            if (nextcloudAutoBackupEnabled) {
                                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            if (nextcloudWifiOnly) Icons.Rounded.Wifi else Icons.Rounded.CloudSync,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                stringResource(Res.string.nextcloud_auto_backup),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = OnSurface,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                if (nextcloudWifiOnly) stringResource(Res.string.nextcloud_wifi_only) else stringResource(Res.string.nextcloud_remote_folder) + ": $nextcloudRemoteFolder",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showNextcloudBackupSetupDialog = true }) {
                                        Text(stringResource(Res.string.edit_backup).uppercase(), color = Primary)
                                    }
                                }
                            }

                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GymButton(
                                    onClick = { showNextcloudIncludeImagesDialog = true },
                                    enabled = !isNextcloudBackingUp,
                                    modifier = Modifier.weight(1f),
                                    containerColor = SurfaceContainerHighest,
                                    contentColor = OnSurface
                                ) {
                                    if (isNextcloudBackingUp) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                                    } else {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = Primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(Res.string.nextcloud_backup_now), style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                GymButton(
                                    onClick = {
                                        actions.loadNextcloudBackups()
                                        showNextcloudRestoreDialog = true
                                    },
                                    enabled = !isNextcloudRestoring,
                                    modifier = Modifier.weight(1f),
                                    containerColor = SurfaceContainerHighest,
                                    contentColor = OnSurface
                                ) {
                                    if (isNextcloudRestoring) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                                    } else {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(Res.string.nextcloud_restore), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showNextcloudSetupDialog = true }) {
                                    Text(stringResource(Res.string.edit_backup).uppercase(), color = Primary)
                                }
                                TextButton(onClick = { showNextcloudDisconnectDialog = true }) {
                                    Text(stringResource(Res.string.nextcloud_disconnect).uppercase(), color = Error)
                                }
                            }
                        }
                    }
                }
}

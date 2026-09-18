package com.emanuel5014.trainable.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import java.text.SimpleDateFormat
import java.util.Locale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.BuildConfig
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.UpdateDialog
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutSettings: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToPersonalizationSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToDonors: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val weeklyGoal by viewModel.weeklyGoal.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()
    val autoBackupFolderUri by viewModel.autoBackupFolderUri.collectAsState()
    val autoBackupMaxCount by viewModel.autoBackupMaxCount.collectAsState()
    val autoBackupIncludeImages by viewModel.autoBackupIncludeImages.collectAsState()
    val nextcloudBackupEnabled by viewModel.nextcloudBackupEnabled.collectAsState()
    val nextcloudAutoBackupEnabled by viewModel.nextcloudAutoBackupEnabled.collectAsState()
    val nextcloudServerUrl by viewModel.nextcloudServerUrl.collectAsState()
    val nextcloudUsername by viewModel.nextcloudUsername.collectAsState()
    val nextcloudRemoteFolder by viewModel.nextcloudRemoteFolder.collectAsState()
    val nextcloudWifiOnly by viewModel.nextcloudWifiOnly.collectAsState()
    val nextcloudAutoBackupFrequency by viewModel.nextcloudAutoBackupFrequency.collectAsState()
    val nextcloudAutoBackupMaxCount by viewModel.nextcloudAutoBackupMaxCount.collectAsState()
    val nextcloudAutoBackupIncludeImages by viewModel.nextcloudAutoBackupIncludeImages.collectAsState()
    val nextcloudTestState by viewModel.nextcloudTestState.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val isNextcloudBackingUp by viewModel.isNextcloudBackingUp.collectAsState()
    val isNextcloudRestoring by viewModel.isNextcloudRestoring.collectAsState()
    val nextcloudBackups by viewModel.nextcloudBackups.collectAsState()
    val isLoadingNextcloudBackups by viewModel.isLoadingNextcloudBackups.collectAsState()

    val backupStatus by viewModel.backupStatus.collectAsState()
    val floatingNavBar by viewModel.floatingNavBar.collectAsState()
    val swipeActionsEnabled by viewModel.swipeActionsEnabled.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val webServerState by viewModel.webServerState.collectAsState()

    val latestRelease by viewModel.latestRelease.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var easterEggClicks by remember { mutableIntStateOf(0) }
    var showBackupSetupDialog by remember { mutableStateOf(false) }
    var showIncludeImagesDialog by remember { mutableStateOf(false) }
    var includeImagesChoice by remember { mutableStateOf(false) }

    var showNextcloudSetupDialog by remember { mutableStateOf(false) }
    var showNextcloudBackupSetupDialog by remember { mutableStateOf(false) }
    var showNextcloudRestoreDialog by remember { mutableStateOf(false) }
    var showNextcloudDisconnectDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }
    var showNextcloudIncludeImagesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    if (latestRelease != null) {
        UpdateDialog(
            release = latestRelease!!,
            onDismiss = { viewModel.clearUpdate() },
            onConfirm = { viewModel.downloadAndInstall(latestRelease!!) },
            isDownloading = isDownloading,
            downloadProgress = downloadProgress
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it, includeImagesChoice) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { outputUri ->
            viewModel.getCsvContent()?.let { csv ->
                try {
                    context.contentResolver.openOutputStream(outputUri)?.use { output ->
                        output.write(csv.toByteArray())
                    }
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error writing file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importDatabase(it) {
                Toast.makeText(context, "Database imported. Restarting app...", Toast.LENGTH_LONG).show()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                exitProcess(0)
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setAutoBackupFolder(it) }
    }

    if (showBackupSetupDialog) {
        var tempFrequency by remember { mutableIntStateOf(autoBackupFrequency) }
        var tempMaxCount by remember { mutableIntStateOf(autoBackupMaxCount) }
        var tempIncludeImages by remember { mutableStateOf(autoBackupIncludeImages) }

        AlertDialog(
            onDismissRequest = { showBackupSetupDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(R.string.auto_backup_setup), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(R.string.configure_backup), color = OnSurfaceVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.frequency), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempFrequency > 1) tempFrequency-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(R.string.day_s, tempFrequency), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                            IconButton(onClick = { if (tempFrequency < 7) tempFrequency++ }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.keep_last), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { if (tempMaxCount > 1) tempMaxCount-- }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(stringResource(R.string.backup_s, tempMaxCount), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
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
                        Text(stringResource(R.string.include_images), color = OnSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = tempIncludeImages,
                            onCheckedChange = { tempIncludeImages = it }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.storage_location), fontWeight = FontWeight.ExtraBold, color = OnSurface)
                        GymButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            containerColor = if (autoBackupFolderUri == null) Primary.copy(alpha = 0.1f) else SurfaceContainerHighest,
                            contentColor = if (autoBackupFolderUri == null) Primary else OnSurface
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (autoBackupFolderUri != null)
                                    viewModel.getFolderDisplayPath(autoBackupFolderUri)
                                else
                                    stringResource(R.string.choose_folder)
                            )
                        }
                        if (autoBackupFolderUri == null) {
                            Text(
                                stringResource(R.string.folder_required_desc),
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
                        viewModel.setAutoBackupFrequency(tempFrequency)
                        viewModel.setAutoBackupMaxCount(tempMaxCount)
                        viewModel.setAutoBackupIncludeImages(tempIncludeImages)
                        viewModel.setAutoBackupEnabled(true)
                        showBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = if (autoBackupFolderUri != null) Primary else OnSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupSetupDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showIncludeImagesDialog) {
        AlertDialog(
            onDismissRequest = { showIncludeImagesDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.include_images),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.include_images_description),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        includeImagesChoice = true
                        showIncludeImagesDialog = false
                        exportLauncher.launch("Trainable_Backup_Full.zip")
                    }
                ) {
                    Text(stringResource(R.string.yes), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        includeImagesChoice = false
                        showIncludeImagesDialog = false
                        exportLauncher.launch("Trainable_Backup.zip")
                    }
                ) {
                    Text(stringResource(R.string.no), color = OnSurfaceVariant)
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
            onDismissRequest = {
                showNextcloudSetupDialog = false
                viewModel.clearNextcloudTestState()
            },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.nextcloud_setup_title),
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
                        stringResource(R.string.nextcloud_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text(stringResource(R.string.nextcloud_server_url)) },
                        placeholder = { Text(stringResource(R.string.nextcloud_server_url_hint)) },
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
                        label = { Text(stringResource(R.string.nextcloud_username)) },
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
                        label = { Text(stringResource(R.string.nextcloud_password_token)) },
                        placeholder = { Text(stringResource(R.string.nextcloud_password_hint)) },
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
                        label = { Text(stringResource(R.string.nextcloud_remote_folder)) },
                        placeholder = { Text(stringResource(R.string.nextcloud_remote_folder_hint)) },
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
                                stringResource(R.string.nextcloud_app_password_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    GymButton(
                        onClick = {
                            viewModel.testNextcloudConnection(tempServerUrl, tempUsername, tempPassword)
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
                            Text(stringResource(R.string.nextcloud_testing_connection))
                        } else {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.nextcloud_test_connection), fontWeight = FontWeight.Bold)
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
                                    stringResource(R.string.nextcloud_connection_successful),
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
                                    stringResource(R.string.nextcloud_connection_failed, result.message),
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
                        viewModel.saveNextcloudConfig(
                            serverUrl = tempServerUrl,
                            username = tempUsername,
                            passwordOrToken = tempPassword,
                            remoteFolder = tempRemoteFolder
                        )
                        viewModel.clearNextcloudTestState()
                        showNextcloudSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearNextcloudTestState()
                        showNextcloudSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                    stringResource(R.string.nextcloud_auto_backup_setup),
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
                        stringResource(R.string.nextcloud_auto_backup_desc),
                        color = OnSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.frequency),
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
                                stringResource(R.string.day_s, tempFrequency),
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
                            stringResource(R.string.keep_last),
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
                                stringResource(R.string.backup_s, tempMaxCount),
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
                        Text(stringResource(R.string.include_images), color = OnSurface)
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
                            Text(stringResource(R.string.nextcloud_wifi_only), color = OnSurface, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.nextcloud_wifi_only_desc),
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
                        viewModel.setNextcloudAutoBackupSettings(
                            frequency = tempFrequency,
                            maxCount = tempMaxCount,
                            includeImages = tempIncludeImages,
                            wifiOnly = tempWifiOnly
                        )
                        showNextcloudBackupSetupDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save).uppercase(), color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNextcloudBackupSetupDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                                        text = stringResource(R.string.nextcloud_backups_list_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.nextcloud_restore_picker_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.loadNextcloudBackups() },
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
                                        text = stringResource(R.string.nextcloud_loading_backups),
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
                                        text = stringResource(R.string.nextcloud_no_backups_found),
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
                                    val formattedDate = remember(backup.name, backup.lastModified) {
                                        formatNextcloudBackupDate(backup)
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
                                                        text = if (isAuto) stringResource(R.string.backup_type_auto) else stringResource(R.string.backup_type_manual),
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
                                                        text = Formatter.formatFileSize(context, backup.sizeBytes),
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
                                                        text = stringResource(R.string.restore_action),
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
                                    stringResource(R.string.cancel).uppercase(),
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
        val formattedDate = formatNextcloudBackupDate(backup)

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
                    stringResource(R.string.nextcloud_restore_confirm_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        stringResource(R.string.nextcloud_restore_confirm_desc),
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
                                    text = Formatter.formatFileSize(context, backup.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = if (isAuto) stringResource(R.string.backup_type_auto) else stringResource(R.string.backup_type_manual),
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
                        viewModel.restoreFromNextcloud(file) {
                            Toast.makeText(context, context.getString(R.string.database_imported), Toast.LENGTH_LONG).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        }
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary
                ) {
                    Text(stringResource(R.string.restore_action).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupForRestore = null }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                    stringResource(R.string.nextcloud_disconnect),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.nextcloud_disconnect_confirm),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.disconnectNextcloud()
                        showNextcloudDisconnectDialog = false
                    }
                ) {
                    Text(stringResource(R.string.nextcloud_disconnect).uppercase(), color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNextcloudDisconnectDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                    stringResource(R.string.include_images),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    stringResource(R.string.include_images_description),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNextcloudIncludeImagesDialog = false
                        viewModel.backupToNextcloud(includeImages = true)
                    }
                ) {
                    Text(stringResource(R.string.yes), color = Primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNextcloudIncludeImagesDialog = false
                        viewModel.backupToNextcloud(includeImages = false)
                    }
                ) {
                    Text(stringResource(R.string.no), color = OnSurfaceVariant)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                    Text(
                        stringResource(R.string.reset_app_dialog),
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
            },
            text = {
                Text(
                    stringResource(R.string.reset_app_message),
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                val restartingText = stringResource(R.string.restarting_app)
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetApp {
                            Toast.makeText(context, restartingText, Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        }
                    }
                ) {
                    Text(stringResource(R.string.reset_app_button).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
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
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = stringResource(R.string.profile)) {
                var showEditUsernameDialog by remember { mutableStateOf(false) }

                if (showEditUsernameDialog) {
                    var newUsername by remember { mutableStateOf(currentUser?.username ?: "") }
                    AlertDialog(
                        onDismissRequest = { showEditUsernameDialog = false },
                        containerColor = SurfaceContainerHigh,
                        title = { Text(stringResource(R.string.edit_username), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                        text = {
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text(stringResource(R.string.username)) },
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
                                        viewModel.updateUsername(newUsername)
                                        showEditUsernameDialog = false
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.save).uppercase(), color = Primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditUsernameDialog = false }) {
                                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                                text = stringResource(R.string.member_since, currentUser?.dataIscrizione?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(R.string.today)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditUsernameDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit), tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.preferences)) {
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
                                    Text(stringResource(R.string.weight_unit), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.weight_unit_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                        .clickable { viewModel.setWeightUnit("kg") }
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
                                        .clickable { viewModel.setWeightUnit("lb") }
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
                                    Text(stringResource(R.string.weekly_goal), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.target_workouts), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (weeklyGoal > 1) viewModel.setWeeklyGoal(weeklyGoal - 1) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                                }
                                Text(
                                    text = weeklyGoal.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Primary,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (weeklyGoal < 7) viewModel.setWeeklyGoal(weeklyGoal + 1) }) {
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
                                    Text(stringResource(R.string.tactile_feedback), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.tactile_feedback_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.setHapticEnabled(it) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        var showLanguageDialog by remember { mutableStateOf(false) }
                        val currentLanguage by viewModel.userLanguage.collectAsState()
                        val context = LocalContext.current

                        if (showLanguageDialog) {
                            AlertDialog(
                                onDismissRequest = { showLanguageDialog = false },
                                containerColor = SurfaceContainerHigh,
                                title = { Text(stringResource(R.string.language), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                                text = {
                                    Column {
                                        LanguageOption(
                                            title = stringResource(R.string.language_system_default),
                                            isSelected = currentLanguage == "system",
                                            onClick = {
                                                viewModel.setLanguage("system") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_english),
                                            isSelected = currentLanguage == "en",
                                            onClick = {
                                                viewModel.setLanguage("en") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_italian),
                                            isSelected = currentLanguage == "it",
                                            onClick = {
                                                viewModel.setLanguage("it") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_spanish),
                                            isSelected = currentLanguage == "es",
                                            onClick = {
                                                viewModel.setLanguage("es") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_french),
                                            isSelected = currentLanguage == "fr",
                                            onClick = {
                                                viewModel.setLanguage("fr") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_german),
                                            isSelected = currentLanguage == "de",
                                            onClick = {
                                                viewModel.setLanguage("de") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                        LanguageOption(
                                            title = stringResource(R.string.language_portuguese),
                                            isSelected = currentLanguage == "pt",
                                            onClick = {
                                                viewModel.setLanguage("pt") {
                                                    showLanguageDialog = false
                                                    val intent = Intent(context, MainActivity::class.java)
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    context.startActivity(intent)
                                                    exitProcess(0)
                                                }
                                            }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showLanguageDialog = false }) {
                                        Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        when (currentLanguage) {
                                            "en" -> stringResource(R.string.language_english)
                                            "it" -> stringResource(R.string.language_italian)
                                            "es" -> stringResource(R.string.language_spanish)
                                            "fr" -> stringResource(R.string.language_french)
                                            "de" -> stringResource(R.string.language_german)
                                            "pt" -> stringResource(R.string.language_portuguese)
                                            else -> stringResource(R.string.language_system_default)
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
                                Icon(Icons.Rounded.Dashboard, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.floating_nav_bar), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.floating_nav_bar_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = !floatingNavBar,
                                onCheckedChange = { viewModel.setFloatingNavBar(!it) }
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
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.swipe_actions), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.swipe_actions_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            SettingsSwitch(
                                checked = swipeActionsEnabled,
                                onCheckedChange = { viewModel.setSwipeActionsEnabled(it) }
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
                                    Text(stringResource(R.string.workout_settings), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.workout_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                    Text(stringResource(R.string.ai_section_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.ai_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                    Text(stringResource(R.string.personalization_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.personalization_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                    Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.notifications_settings_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.data_backup)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                        Text(stringResource(R.string.auto_backup), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                        Text(
                                            if (autoBackupEnabled) stringResource(R.string.backup_enabled, autoBackupFrequency) else stringResource(R.string.backup_disabled),
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
                                            viewModel.setAutoBackupEnabled(false)
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
                                            Text(stringResource(R.string.storage_location), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                            Text(
                                                viewModel.getFolderDisplayPath(autoBackupFolderUri),
                                                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showBackupSetupDialog = true }) {
                                        Text(stringResource(R.string.edit_backup).uppercase(), color = Primary)
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
                                        painter = painterResource(R.drawable.ic_nextcloud),
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            stringResource(R.string.nextcloud_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = OnSurface,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            if (nextcloudBackupEnabled && nextcloudServerUrl != null && nextcloudUsername != null) {
                                                val host = try { Uri.parse(nextcloudServerUrl).host ?: nextcloudServerUrl } catch (e: Exception) { nextcloudServerUrl }
                                                stringResource(R.string.nextcloud_configured_as, nextcloudUsername!!, host!!)
                                            } else {
                                                stringResource(R.string.nextcloud_not_configured)
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
                                                viewModel.setNextcloudBackupEnabled(true)
                                            } else {
                                                showNextcloudSetupDialog = true
                                            }
                                        } else {
                                            viewModel.setNextcloudBackupEnabled(false)
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
                                            stringResource(R.string.nextcloud_auto_backup),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = OnSurface,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            if (nextcloudAutoBackupEnabled)
                                                stringResource(
                                                    R.string.nextcloud_auto_backup_summary,
                                                    nextcloudAutoBackupFrequency,
                                                    nextcloudAutoBackupMaxCount,
                                                    if (nextcloudAutoBackupIncludeImages) stringResource(R.string.with_images) else stringResource(R.string.without_images)
                                                )
                                            else
                                                stringResource(R.string.backup_disabled),
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
                                                viewModel.setNextcloudAutoBackupEnabled(false)
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
                                                    stringResource(R.string.nextcloud_auto_backup),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = OnSurface,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    if (nextcloudWifiOnly) stringResource(R.string.nextcloud_wifi_only) else stringResource(R.string.nextcloud_remote_folder) + ": $nextcloudRemoteFolder",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnSurfaceVariant
                                                )
                                            }
                                        }
                                        TextButton(onClick = { showNextcloudBackupSetupDialog = true }) {
                                            Text(stringResource(R.string.edit_backup).uppercase(), color = Primary)
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
                                            Text(stringResource(R.string.nextcloud_backup_now), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    GymButton(
                                        onClick = {
                                            viewModel.loadNextcloudBackups()
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
                                            Text(stringResource(R.string.nextcloud_restore), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showNextcloudSetupDialog = true }) {
                                        Text(stringResource(R.string.edit_backup).uppercase(), color = Primary)
                                    }
                                    TextButton(onClick = { showNextcloudDisconnectDialog = true }) {
                                        Text(stringResource(R.string.nextcloud_disconnect).uppercase(), color = Error)
                                    }
                                }
                            }
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymButton(
                            onClick = { showIncludeImagesDialog = true },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_database), fontWeight = FontWeight.ExtraBold)
                        }

                        GymButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.import_database), fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    GymButton(
                        onClick = {
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            csvExportLauncher.launch("Workouts_$timestamp.csv")
                            viewModel.exportWorkoutsToCsv()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.export_csv), fontWeight = FontWeight.ExtraBold)
                    }

                    GymButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.reset_app_button), fontWeight = FontWeight.ExtraBold, color = Error)
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.web_server_section_title)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.web_dashboard), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        if (webServerState.isRunning) stringResource(R.string.web_server_active) else stringResource(R.string.web_server_inactive),
                                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                    )
                                }
                            }
                            SettingsSwitch(
                                checked = webServerState.isRunning,
                                onCheckedChange = { viewModel.toggleWebServer() }
                            )
                        }

                        if (webServerState.isRunning) {
                            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.web_server_open_browser),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    webServerState.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.web_server_instructions),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.about)) {
                GymCard(containerColor = SurfaceContainerHigh) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Trainable v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Made with ❤️ by Emanuel5014",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        easterEggClicks++
                                        if (easterEggClicks >= 3) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=5jc_R9_im9w"))
                                            context.startActivity(intent)
                                            easterEggClicks = 0
                                        }
                                    }
                                )
                            }
                            GymButton(
                                onClick = { viewModel.checkForUpdates() },
                                containerColor = Primary.copy(alpha = 0.1f),
                                contentColor = Primary,
                                height = 48,
                                contentPadding = PaddingValues(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.check_for_updates),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Surface.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Emanuel5014/Trainable"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = Surface.copy(alpha = 0.5f),
                                contentColor = OnSurface
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_github),
                                    contentDescription = null,
                                    tint = OnSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.star_on_github), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            }

                            GymButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/emanuel5014"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = Surface.copy(alpha = 0.5f),
                                contentColor = OnSurface
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_kofi),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.buy_me_a_coffee), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = Surface.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToDonors() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.VolunteerActivism,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.donors_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        stringResource(R.string.donors_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatNextcloudBackupDate(backup: NextcloudBackupFile): String {
    try {
        val regex = Regex("""Trainable_(?:Auto)?Backup_(\d{4}-\d{2}-\d{2})_(\d{2})(\d{2})(\d{2})\.zip""")
        val match = regex.find(backup.name)
        if (match != null) {
            val (datePart, hour, min, sec) = match.destructured
            val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val parsed = sdf.parse("${datePart}_${hour}${min}${sec}")
            if (parsed != null) {
                val outFormat = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
                return outFormat.format(parsed)
            }
        }
    } catch (e: Exception) {
        // fallback
    }
    return try {
        val outFormat = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
        outFormat.format(backup.lastModified)
    } catch (e: Exception) {
        backup.name
    }
}

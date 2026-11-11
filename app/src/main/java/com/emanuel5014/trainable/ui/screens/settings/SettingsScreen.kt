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
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.emanuel5014.trainable.util.AppVersion
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
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutSettings: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToPersonalizationSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToDonors: () -> Unit,
    onNavigateToImportHistory: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
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
    val webServerState by viewModel.webServerState.collectAsState()

    val latestRelease by viewModel.latestRelease.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val betaUpdates by viewModel.betaUpdates.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showIncludeImagesDialog by remember { mutableStateOf(false) }
    var includeImagesChoice by remember { mutableStateOf(false) }

    val backupState = BackupSettingsUiState(
        autoBackupEnabled = autoBackupEnabled,
        autoBackupFrequency = autoBackupFrequency,
        autoBackupFolderUri = autoBackupFolderUri,
        autoBackupFolderLabel = autoBackupFolderUri?.let(viewModel::getFolderDisplayPath),
        autoBackupMaxCount = autoBackupMaxCount,
        autoBackupIncludeImages = autoBackupIncludeImages,
        nextcloudBackupEnabled = nextcloudBackupEnabled,
        nextcloudAutoBackupEnabled = nextcloudAutoBackupEnabled,
        nextcloudServerUrl = nextcloudServerUrl,
        nextcloudUsername = nextcloudUsername,
        nextcloudHost = nextcloudServerUrl?.let { url -> try { Uri.parse(url).host ?: url } catch (e: Exception) { url } },
        nextcloudRemoteFolder = nextcloudRemoteFolder,
        nextcloudWifiOnly = nextcloudWifiOnly,
        nextcloudAutoBackupFrequency = nextcloudAutoBackupFrequency,
        nextcloudAutoBackupMaxCount = nextcloudAutoBackupMaxCount,
        nextcloudAutoBackupIncludeImages = nextcloudAutoBackupIncludeImages,
        nextcloudTestState = nextcloudTestState,
        isTestingConnection = isTestingConnection,
        isNextcloudBackingUp = isNextcloudBackingUp,
        isNextcloudRestoring = isNextcloudRestoring,
        nextcloudBackups = nextcloudBackups,
        isLoadingNextcloudBackups = isLoadingNextcloudBackups
    )
    val backupActions = remember(viewModel) { viewModel.backupSettingsActions() }

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

    SettingsContent(
        state = viewModel.state,
        onNavigateBack = onNavigateBack,
        onNavigateToWorkoutSettings = onNavigateToWorkoutSettings,
        onNavigateToExercises = onNavigateToExercises,
        onNavigateToAiSettings = onNavigateToAiSettings,
        onNavigateToPersonalizationSettings = onNavigateToPersonalizationSettings,
        onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        scrollState = scrollState,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        languageUi = {
                        var showLanguageDialog by remember { mutableStateOf(false) }
                        val currentLanguage by viewModel.userLanguage.collectAsState()
                        val context = LocalContext.current

                        if (showLanguageDialog) {
                            LanguageDialog(
                                currentLanguage = currentLanguage,
                                onPick = { code ->
                                    viewModel.setLanguage(code) {
                                        showLanguageDialog = false
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        context.startActivity(intent)
                                        exitProcess(0)
                                    }
                                },
                                onDismiss = { showLanguageDialog = false }
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

        },
        platformSections = {
            SettingsSection(title = stringResource(R.string.data_backup)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BackupSettingsCards(
                        state = backupState,
                        actions = backupActions,
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        onRestored = {
                            Toast.makeText(context, context.getString(R.string.database_imported), Toast.LENGTH_LONG).show()
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            exitProcess(0)
                        },
                        dateText = ::formatNextcloudBackupDate,
                        sizeText = { Formatter.formatFileSize(context, it) },
                        nextcloudIcon = painterResource(R.drawable.ic_nextcloud)
                    )


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
                        onClick = onNavigateToImportHistory,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.import_csv), fontWeight = FontWeight.ExtraBold)
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

        },
        aboutSection = {
            AboutSettingsSection(
                versionName = BuildConfig.VERSION_NAME,
                onOpenSource = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Emanuel5014/Trainable"))
                    context.startActivity(intent)
                },
                onSupport = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/emanuel5014"))
                    context.startActivity(intent)
                },
                onNavigateToDonors = onNavigateToDonors,
                onAuthorEasterEgg = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=5jc_R9_im9w"))
                    context.startActivity(intent)
                },
                githubIcon = painterResource(R.drawable.ic_github),
                kofiIcon = painterResource(R.drawable.ic_kofi),
                updateAction = {
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
                    BetaUpdatesRow(
                        betaOn = betaUpdates,
                        installedIsBeta = AppVersion.isPreRelease(BuildConfig.VERSION_NAME),
                        onChange = viewModel::setBetaUpdates
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    )
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
        outFormat.format(java.util.Date(backup.lastModifiedMillis))
    } catch (e: Exception) {
        backup.name
    }
}

/** The shared backup cards call the view model functions of the same names. */
private fun SettingsViewModel.backupSettingsActions() = object : BackupSettingsActions {
    override fun setAutoBackupEnabled(enabled: Boolean) = this@backupSettingsActions.setAutoBackupEnabled(enabled)
    override fun setAutoBackupFrequency(frequency: Int) = this@backupSettingsActions.setAutoBackupFrequency(frequency)
    override fun setAutoBackupMaxCount(count: Int) { this@backupSettingsActions.setAutoBackupMaxCount(count) }
    override fun setAutoBackupIncludeImages(include: Boolean) { this@backupSettingsActions.setAutoBackupIncludeImages(include) }
    override fun setNextcloudBackupEnabled(enabled: Boolean) = this@backupSettingsActions.setNextcloudBackupEnabled(enabled)
    override fun setNextcloudAutoBackupEnabled(enabled: Boolean) = this@backupSettingsActions.setNextcloudAutoBackupEnabled(enabled)
    override fun setNextcloudAutoBackupSettings(frequency: Int, maxCount: Int, includeImages: Boolean, wifiOnly: Boolean) =
        this@backupSettingsActions.setNextcloudAutoBackupSettings(frequency, maxCount, includeImages, wifiOnly)
    override fun clearNextcloudTestState() = this@backupSettingsActions.clearNextcloudTestState()
    override fun testNextcloudConnection(serverUrl: String, username: String, passwordOrToken: String) =
        this@backupSettingsActions.testNextcloudConnection(serverUrl, username, passwordOrToken)
    override fun saveNextcloudConfig(serverUrl: String, username: String, passwordOrToken: String, remoteFolder: String) =
        this@backupSettingsActions.saveNextcloudConfig(serverUrl, username, passwordOrToken, remoteFolder)
    override fun disconnectNextcloud() = this@backupSettingsActions.disconnectNextcloud()
    override fun backupToNextcloud(includeImages: Boolean) = this@backupSettingsActions.backupToNextcloud(includeImages)
    override fun loadNextcloudBackups() = this@backupSettingsActions.loadNextcloudBackups()
    override fun restoreFromNextcloud(backupFile: NextcloudBackupFile, onSuccess: () -> Unit) =
        this@backupSettingsActions.restoreFromNextcloud(backupFile, onSuccess)
}

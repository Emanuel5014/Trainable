package com.emanuel5014.trainable.ui.screens.onboarding

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.ModelImportError
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.shared.ModelImportOption
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val choices = remember { OnboardingChoices() }
    val context = LocalContext.current

    var autoBackupEnabled by choices.autoBackupEnabled
    var autoBackupFrequency by choices.autoBackupFrequency
    var autoBackupFolderUri by choices.autoBackupFolderUri
    var autoBackupMaxCount by choices.autoBackupMaxCount
    var autoBackupIncludeImages by choices.autoBackupIncludeImages
    var aiScanEnabled by choices.aiScanEnabled
    var nextcloudServerUrl by remember { mutableStateOf("") }
    var nextcloudUsername by remember { mutableStateOf("") }
    var nextcloudPassword by remember { mutableStateOf("") }
    var nextcloudFolder by remember { mutableStateOf("Trainable/Backups") }
    var nextcloudAutoBackupEnabled by choices.nextcloudAutoBackupEnabled
    var nextcloudAutoBackupFrequency by choices.nextcloudAutoBackupFrequency
    var nextcloudAutoBackupMaxCount by choices.nextcloudAutoBackupMaxCount
    var nextcloudAutoBackupIncludeImages by choices.nextcloudAutoBackupIncludeImages
    var nextcloudWifiOnly by choices.nextcloudWifiOnly
    var showNextcloudRestore by remember { mutableStateOf(false) }
    var nextcloudBackupToRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }
    val selectedAiModelVariant by viewModel.aiModelVariant.collectAsState()
    val aiDeviceSupported = viewModel.aiDeviceSupported
    val aiModelStatus by viewModel.aiModelStatus.collectAsState()
    val aiModelImportError by viewModel.aiModelImportError.collectAsState()

    val backupStatus by viewModel.backupStatus.collectAsState()
    val connectedNextcloudServer by viewModel.nextcloudServerUrl.collectAsState()
    val connectedNextcloudUser by viewModel.nextcloudUsername.collectAsState()
    val isConnectingNextcloud by viewModel.isConnectingNextcloud.collectAsState()
    val nextcloudConnectState by viewModel.nextcloudConnectState.collectAsState()
    val nextcloudBackups by viewModel.nextcloudBackups.collectAsState()
    val isLoadingNextcloudBackups by viewModel.isLoadingNextcloudBackups.collectAsState()
    val isNextcloudRestoring by viewModel.isNextcloudRestoring.collectAsState()

    // The password is only needed to connect; don't keep it around afterwards
    LaunchedEffect(nextcloudConnectState) {
        if (nextcloudConnectState is NextcloudConnectionResult.Success) nextcloudPassword = ""
    }

    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    val restartApp = {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        exitProcess(0)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importDatabase(it) {
                Toast.makeText(context, "Import successful. Restarting app...", Toast.LENGTH_LONG).show()
                restartApp()
            }
        }
    }

    // Android 13+ needs the notification permission; a refusal turns the switches off, as Settings does
    var askedForTimer by remember { mutableStateOf(false) }
    var askedForMembership by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            if (askedForTimer) choices.timerNotificationsEnabled.value = false
            if (askedForMembership) choices.gymMembershipExpiryNotificationsEnabled.value = false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.persistFolderUri(it)
            autoBackupFolderUri = it.toString()
        }
    }

    OnboardingContent(
        model = viewModel.model,
        onFinished = onFinished,
        choices = choices,
        pages = (0..10).toList(),
        selectedAiModelVariant = selectedAiModelVariant,
        logo = { modifier, tint -> Icon(painterResource(R.drawable.ic_app_logo), null, modifier, tint = tint) },
        onNotificationsPageLeft = { timer, membership ->
            if ((timer || membership) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                askedForTimer = timer
                askedForMembership = membership
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        platformPage = { page, _ ->
            when (page) {
                            7 -> LocalAiSlide(
                                aiDeviceSupported = aiDeviceSupported,
                                aiScanEnabled = aiScanEnabled,
                                onAiScanEnabledChange = { aiScanEnabled = it },
                                selectedVariant = AiModelVariant.fromId(selectedAiModelVariant),
                                onVariantSelected = { viewModel.setAiModelVariant(it.id) },
                                aiModelStatus = aiModelStatus,
                                onDownloadModel = { viewModel.downloadAiModel() },
                                onCancelDownload = { viewModel.cancelAiModelDownload() },
                                importError = aiModelImportError,
                                onImportModel = { viewModel.importAiModel(it) },
                                onClearImportError = { viewModel.clearAiModelImportError() }
                            )
                            8 -> BackupSlide(
                                autoBackupEnabled = autoBackupEnabled,
                                onAutoBackupChange = { autoBackupEnabled = it },
                                autoBackupFrequency = autoBackupFrequency,
                                onAutoBackupFrequencyChange = { autoBackupFrequency = it },
                                autoBackupFolderUri = autoBackupFolderUri,
                                autoBackupMaxCount = autoBackupMaxCount,
                                onAutoBackupMaxCountChange = { autoBackupMaxCount = it },
                                autoBackupIncludeImages = autoBackupIncludeImages,
                                onAutoBackupIncludeImagesChange = { autoBackupIncludeImages = it },
                                onPickFolder = { folderPickerLauncher.launch(null) },
                                onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                            )
                            9 -> NextcloudSlide(
                                connectedAs = connectedNextcloudUser?.let { user ->
                                    stringResource(R.string.nextcloud_configured_as, user, connectedNextcloudServer.orEmpty())
                                },
                                serverUrl = nextcloudServerUrl,
                                onServerUrlChange = { nextcloudServerUrl = it },
                                username = nextcloudUsername,
                                onUsernameChange = { nextcloudUsername = it },
                                password = nextcloudPassword,
                                onPasswordChange = { nextcloudPassword = it },
                                remoteFolder = nextcloudFolder,
                                onRemoteFolderChange = { nextcloudFolder = it },
                                isConnecting = isConnectingNextcloud,
                                connectState = nextcloudConnectState,
                                onConnect = {
                                    viewModel.connectNextcloud(nextcloudServerUrl, nextcloudUsername, nextcloudPassword, nextcloudFolder)
                                },
                                onDisconnect = { viewModel.disconnectNextcloud() },
                                autoBackupEnabled = nextcloudAutoBackupEnabled,
                                onAutoBackupChange = { nextcloudAutoBackupEnabled = it },
                                frequency = nextcloudAutoBackupFrequency,
                                onFrequencyChange = { nextcloudAutoBackupFrequency = it },
                                maxCount = nextcloudAutoBackupMaxCount,
                                onMaxCountChange = { nextcloudAutoBackupMaxCount = it },
                                includeImages = nextcloudAutoBackupIncludeImages,
                                onIncludeImagesChange = { nextcloudAutoBackupIncludeImages = it },
                                wifiOnly = nextcloudWifiOnly,
                                onWifiOnlyChange = { nextcloudWifiOnly = it },
                                onRestore = {
                                    nextcloudBackupToRestore = null
                                    showNextcloudRestore = true
                                    viewModel.loadNextcloudBackups()
                                }
                            )
            }
        }
    )

    if (showNextcloudRestore) {
        NextcloudRestoreDialog(
            backups = nextcloudBackups,
            isLoading = isLoadingNextcloudBackups,
            isRestoring = isNextcloudRestoring,
            selected = nextcloudBackupToRestore,
            onPick = { nextcloudBackupToRestore = it },
            onConfirm = { backup ->
                viewModel.restoreFromNextcloud(backup) {
                    Toast.makeText(context, "Restore complete. Restarting app...", Toast.LENGTH_LONG).show()
                    restartApp()
                }
            },
            onBack = { nextcloudBackupToRestore = null },
            onDismiss = {
                showNextcloudRestore = false
                nextcloudBackupToRestore = null
            },
            dateText = { backup ->
                java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                    .format(java.util.Date(backup.lastModifiedMillis))
            },
            sizeText = { bytes -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1_048_576f) }
        )
    }
}


@Composable
private fun LocalAiSlide(
    aiDeviceSupported: Boolean,
    aiScanEnabled: Boolean,
    onAiScanEnabledChange: (Boolean) -> Unit,
    selectedVariant: AiModelVariant,
    onVariantSelected: (AiModelVariant) -> Unit,
    aiModelStatus: AiModelStatus,
    onDownloadModel: () -> Unit,
    onCancelDownload: () -> Unit,
    importError: ModelImportError?,
    onImportModel: (Uri) -> Unit,
    onClearImportError: () -> Unit
) {
    // An error left by an earlier visit is no news now
    LaunchedEffect(Unit) { onClearImportError() }

    OnboardingPage {
        OnboardingTitle(stringResource(R.string.onboarding_ai_title))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (!aiDeviceSupported) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerHigh)
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_device_unsupported),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.onboarding_ai_unsupported_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                CustomizeToggleItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.onboarding_ai_toggle_title),
                    desc = stringResource(R.string.onboarding_ai_toggle_desc),
                    checked = aiScanEnabled,
                    onCheckedChange = onAiScanEnabledChange
                )

                if (aiScanEnabled) {
                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Text(
                        text = stringResource(R.string.ai_model_variant),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AiModelVariant.entries.forEach { variant ->
                            val isSelected = variant == selectedVariant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Primary else OutlineVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onVariantSelected(variant) }
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = variant.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) Primary else OnSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${variant.sizeLabel} • ≥${variant.requiredRamGb}GB RAM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    when (aiModelStatus) {
                        is AiModelStatus.Downloading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(
                                            if (aiModelStatus.importing) R.string.ai_model_importing else R.string.ai_model_downloading,
                                            (aiModelStatus.progress * 100).toInt()
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    TextButton(onClick = onCancelDownload) {
                                        Text(
                                            stringResource(R.string.ai_download_cancel).uppercase(),
                                            color = Error,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                LinearWavyProgressIndicator(
                                    progress = { aiModelStatus.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        is AiModelStatus.Ready -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Primary.copy(alpha = 0.1f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    stringResource(R.string.ai_model_ready),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GymButton(
                                    onClick = onDownloadModel,
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = OnSurface
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.ai_model_download, selectedVariant.displayName),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.onboarding_ai_download_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                ModelImportOption(
                                    error = importError,
                                    onPicked = onImportModel,
                                    onClearError = onClearImportError,
                                    centered = true
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    // BETA Disclaimer Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceContainerHigh.copy(alpha = 0.6f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "BETA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            }
                            Text(
                                text = stringResource(R.string.ai_disclaimer_beta),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

    }
}


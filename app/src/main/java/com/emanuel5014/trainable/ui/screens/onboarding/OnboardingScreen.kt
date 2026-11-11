package com.emanuel5014.trainable.ui.screens.onboarding

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.ui.components.shared.ModelImportOption
import com.emanuel5014.trainable.ui.screens.settings.LanguageDialog
import com.emanuel5014.trainable.ui.screens.settings.languageName
import kotlin.system.exitProcess

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val choices = remember { OnboardingChoices() }
    val nextcloudForm = remember { NextcloudForm() }
    val context = LocalContext.current

    var autoBackupEnabled by choices.autoBackupEnabled
    var autoBackupFrequency by choices.autoBackupFrequency
    var autoBackupFolderUri by choices.autoBackupFolderUri
    var autoBackupMaxCount by choices.autoBackupMaxCount
    var autoBackupIncludeImages by choices.autoBackupIncludeImages
    var nextcloudAutoBackupEnabled by choices.nextcloudAutoBackupEnabled
    var nextcloudAutoBackupFrequency by choices.nextcloudAutoBackupFrequency
    var nextcloudAutoBackupMaxCount by choices.nextcloudAutoBackupMaxCount
    var nextcloudAutoBackupIncludeImages by choices.nextcloudAutoBackupIncludeImages
    var nextcloudWifiOnly by choices.nextcloudWifiOnly
    var showNextcloudRestore by remember { mutableStateOf(false) }
    var nextcloudBackupToRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val selectedAiModelVariant by viewModel.aiModelVariant.collectAsState()
    val aiModelStatus by viewModel.aiModelStatus.collectAsState()
    val aiModelImportError by viewModel.aiModelImportError.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val backupStatus by viewModel.backupStatus.collectAsState()
    val connectedNextcloudServer by viewModel.nextcloudServerUrl.collectAsState()
    val connectedNextcloudUser by viewModel.nextcloudUsername.collectAsState()
    val isConnectingNextcloud by viewModel.isConnectingNextcloud.collectAsState()
    val nextcloudConnectState by viewModel.nextcloudConnectState.collectAsState()
    val nextcloudBackups by viewModel.nextcloudBackups.collectAsState()
    val isLoadingNextcloudBackups by viewModel.isLoadingNextcloudBackups.collectAsState()
    val isNextcloudRestoring by viewModel.isNextcloudRestoring.collectAsState()

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

    val connection = NextcloudConnection(
        connectedAs = connectedNextcloudUser?.let { user ->
            stringResource(R.string.nextcloud_configured_as, user, connectedNextcloudServer.orEmpty())
        },
        isConnecting = isConnectingNextcloud,
        state = nextcloudConnectState,
        connect = {
            viewModel.connectNextcloud(
                nextcloudForm.serverUrl.value, nextcloudForm.username.value,
                nextcloudForm.password.value, nextcloudForm.remoteFolder.value
            )
        },
        disconnect = { viewModel.disconnectNextcloud() }
    )

    val ai = if (viewModel.aiDeviceSupported) {
        OnboardingAi(
            variants = AiModelVariant.entries,
            selected = AiModelVariant.fromId(selectedAiModelVariant),
            onSelect = { viewModel.setAiModelVariant(it.id) },
            status = aiModelStatus,
            onDownload = { viewModel.downloadAiModel() },
            onCancel = { viewModel.cancelAiModelDownload() },
            importOption = {
                // An error left by an earlier visit is no news now
                LaunchedEffect(Unit) { viewModel.clearAiModelImportError() }
                ModelImportOption(
                    error = aiModelImportError,
                    onPicked = { viewModel.importAiModel(it) },
                    onClearError = { viewModel.clearAiModelImportError() },
                    centered = true
                )
            }
        )
    } else null

    OnboardingContent(
        model = viewModel.model,
        onFinished = onFinished,
        choices = choices,
        ai = ai,
        selectedAiModelVariant = selectedAiModelVariant,
        languageLabel = languageName(currentLanguage),
        onPickLanguage = { showLanguageDialog = true },
        restore = OnboardingRestore(
            fromFile = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            form = nextcloudForm,
            connection = connection,
            openNextcloudBackups = {
                nextcloudBackupToRestore = null
                showNextcloudRestore = true
                viewModel.loadNextcloudBackups()
            }
        ),
        nextcloudConnected = connectedNextcloudUser != null,
        dataLocal = {
            LocalBackupBody(
                autoBackupEnabled = autoBackupEnabled,
                onAutoBackupChange = { autoBackupEnabled = it },
                autoBackupFrequency = autoBackupFrequency,
                onAutoBackupFrequencyChange = { autoBackupFrequency = it },
                autoBackupFolderUri = autoBackupFolderUri,
                autoBackupMaxCount = autoBackupMaxCount,
                onAutoBackupMaxCountChange = { autoBackupMaxCount = it },
                autoBackupIncludeImages = autoBackupIncludeImages,
                onAutoBackupIncludeImagesChange = { autoBackupIncludeImages = it },
                onPickFolder = { folderPickerLauncher.launch(null) }
            )
        },
        dataNextcloud = {
            NextcloudBody(
                form = nextcloudForm,
                connection = connection,
                autoBackupEnabled = nextcloudAutoBackupEnabled,
                onAutoBackupChange = { nextcloudAutoBackupEnabled = it },
                frequency = nextcloudAutoBackupFrequency,
                onFrequencyChange = { nextcloudAutoBackupFrequency = it },
                maxCount = nextcloudAutoBackupMaxCount,
                onMaxCountChange = { nextcloudAutoBackupMaxCount = it },
                includeImages = nextcloudAutoBackupIncludeImages,
                onIncludeImagesChange = { nextcloudAutoBackupIncludeImages = it },
                wifiOnly = nextcloudWifiOnly,
                onWifiOnlyChange = { nextcloudWifiOnly = it }
            )
        },
        logo = { modifier, tint -> Icon(painterResource(R.drawable.ic_app_logo), null, modifier, tint = tint) },
        onNotificationsPageLeft = { timer, membership ->
            if ((timer || membership) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                askedForTimer = timer
                askedForMembership = membership
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            onPick = { code ->
                viewModel.setLanguage(code) {
                    showLanguageDialog = false
                    restartApp()
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

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

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.onboarding.BackupSlide
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudRestoreDialog
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudSlide
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingChoices
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingContent
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingModel
import org.jetbrains.compose.resources.stringResource

/** Same setup as Android, with only capabilities available on iOS. */
@Composable
internal fun IosOnboarding() {
    val scope = rememberCoroutineScope()
    val show = LocalShowMessage.current
    val choices = remember { OnboardingChoices() }
    val model = remember {
        OnboardingModel(IosAppGraph.users, IosAppGraph.preferences, scope, onCompleted = { IosAutoBackup.reschedule() })
    }
    // A backup restored from here also ends the setup when it is applied on the next launch
    val backups = remember {
        IosBackupSettingsModel(scope, show, service = IosNextcloud.service(IosBackupArchiver(completeOnboarding = true), wifiOnly = false))
    }
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
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remoteFolder by remember { mutableStateOf("Trainable/Backups") }
    var showRestore by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }
    val connectedServer by IosAppGraph.preferences.nextcloudServerUrl.collectAsState(null)
    val connectedUser by IosAppGraph.preferences.nextcloudUsername.collectAsState(null)
    val pickFolder = rememberIosBackupFolderPicker { autoBackupFolderUri = it }
    val restoreFromFiles = rememberIosBackupRestore(completeOnboarding = true)

    // The password is only needed to connect; don't keep it around afterwards
    LaunchedEffect(backups.testState) {
        if (backups.testState is NextcloudConnectionResult.Success) password = ""
    }

    OnboardingContent(
        model = model,
        onFinished = {}, // The persisted flag is the sole owner of the first-launch gate.
        choices = choices,
        pages = IosOnboardingPages,
        wallpaperSupported = false,
        customVibrationSupported = false,
        prepareCompletion = { selected ->
            val timers = selected.timerNotificationsEnabled.value
            val membership = selected.gymMembershipExpiryNotificationsEnabled.value
            if (timers || membership) {
                val allowed = IosNotifications.authorizeForOnboarding()
                selected.timerNotificationsEnabled.value = timers && allowed
                selected.gymMembershipExpiryNotificationsEnabled.value = membership && allowed
            }
        },
        platformPage = { page, _ ->
            when (page) {
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
                    onPickFolder = pickFolder,
                    onImport = restoreFromFiles.open
                )
                9 -> NextcloudSlide(
                    connectedAs = connectedUser?.let { user ->
                        stringResource(Res.string.nextcloud_configured_as, user, connectedServer.orEmpty())
                    },
                    serverUrl = serverUrl,
                    onServerUrlChange = { serverUrl = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    remoteFolder = remoteFolder,
                    onRemoteFolderChange = { remoteFolder = it },
                    isConnecting = backups.isTesting,
                    connectState = backups.testState,
                    onConnect = { backups.connectNextcloud(serverUrl, username, password, remoteFolder) },
                    onDisconnect = { backups.disconnectNextcloudSilently() },
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
                        backupToRestore = null
                        showRestore = true
                        backups.loadNextcloudBackups()
                    }
                )
                else -> error("No iOS page $page")
            }
        }
    )

    if (showRestore) {
        NextcloudRestoreDialog(
            backups = backups.backups,
            isLoading = backups.isLoadingBackups,
            isRestoring = backups.isRestoring,
            selected = backupToRestore,
            onPick = { backupToRestore = it },
            onConfirm = { backup ->
                backups.restoreFromNextcloud(backup) {
                    showRestore = false
                    backupToRestore = null
                    IosBackups.restartRequired = true
                }
            },
            onBack = { backupToRestore = null },
            onDismiss = {
                showRestore = false
                backupToRestore = null
            },
            dateText = ::iosNextcloudBackupDate,
            sizeText = ::iosFileSize
        )
    }
}

internal val IosOnboardingPages = listOf(0, 1, 2, 3, 4, 5, 6, 8, 9, 10)

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
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.data.ai.IosLocalAiDevice
import com.emanuel5014.trainable.ui.screens.onboarding.LocalBackupBody
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudBody
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudConnection
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudForm
import com.emanuel5014.trainable.ui.screens.onboarding.NextcloudRestoreDialog
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingAi
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingLocalAi
import com.emanuel5014.trainable.ui.screens.settings.AiProvidersModel
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingChoices
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingContent
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingModel
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingRestore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Same setup as Android, with only capabilities available on iOS (E2B only for the on-device model). */
@Composable
internal fun IosOnboarding() {
    val scope = rememberCoroutineScope()
    val show = LocalShowMessage.current
    val choices = remember { OnboardingChoices() }
    val form = remember { NextcloudForm() }
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
    var showRestore by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<NextcloudBackupFile?>(null) }
    var showLanguage by remember { mutableStateOf(false) }
    val connectedServer by IosAppGraph.preferences.nextcloudServerUrl.collectAsState(null)
    val connectedUser by IosAppGraph.preferences.nextcloudUsername.collectAsState(null)
    val language by IosAppGraph.preferences.userLanguage.collectAsState(initial = null)
    val pickFolder = rememberIosBackupFolderPicker { autoBackupFolderUri = it }
    val restoreFromFiles = rememberIosBackupRestore(completeOnboarding = true)

    val connection = NextcloudConnection(
        connectedAs = connectedUser?.let { user -> stringResource(Res.string.nextcloud_configured_as, user, connectedServer.orEmpty()) },
        isConnecting = backups.isTesting,
        state = backups.testState,
        connect = { backups.connectNextcloud(form.serverUrl.value, form.username.value, form.password.value, form.remoteFolder.value) },
        disconnect = { backups.disconnectNextcloudSilently() }
    )

    val providers = remember { AiProvidersModel(IosAppGraph.aiProfiles, IosAppGraph.aiHttp, scope) }
    val local = if (IosLocalAiDevice.isSupported) {
        val store = IosAppGraph.localModels
        val status by store.status.collectAsState()
        val importError by store.importError.collectAsState()
        OnboardingLocalAi(
            variants = listOf(store.variant),
            selected = store.variant,
            onSelect = {},
            status = status,
            onDownload = { store.startDownload() },
            onCancel = { store.cancelDownload() },
            // An iPhone needs 8 GB for it (IosLocalAiDevice), not Android's 6
            requiredRamGb = { 8 },
            importOption = {
                LaunchedEffect(Unit) { store.clearImportError() }
                ImportOption(importError, store)
            }
        )
    } else null
    // An iPhone needs 8 GB for it (IosLocalAiDevice), not Android's 6
    val ai = OnboardingAi(providers = providers, local = local, localRequiredRamGb = 8)

    // Liquid Glass starts on for a new iPhone setup (the user can turn it off on Look); once per launch, so a
    // language change (which rebuilds the tree) doesn't turn it back on
    LaunchedEffect(Unit) {
        if (!iosGlassDefaultApplied) {
            iosGlassDefaultApplied = true
            IosAppGraph.preferences.setLiquidGlassEnabled(true)
        }
    }

    val languageOptions = iosLanguageOptions()
    val selectedLanguage = language ?: "system"

    OnboardingContent(
        model = model,
        onFinished = {}, // The persisted flag is the sole owner of the first-launch gate.
        choices = choices,
        ai = ai,
        selectedAiModelVariant = IosAppGraph.localModels.variant.id,
        languageLabel = languageOptions.firstOrNull { it.first == selectedLanguage }?.second ?: languageOptions.first().second,
        onPickLanguage = { showLanguage = true },
        restore = OnboardingRestore(
            fromFile = restoreFromFiles.open,
            form = form,
            connection = connection,
            openNextcloudBackups = {
                backupToRestore = null
                showRestore = true
                backups.loadNextcloudBackups()
            }
        ),
        nextcloudConnected = connectedUser != null,
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
                onPickFolder = pickFolder
            )
        },
        dataNextcloud = {
            NextcloudBody(
                form = form,
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
        }
    )

    if (showLanguage) {
        IosLanguageDialog(selectedLanguage, onPick = { code ->
            showLanguage = false
            scope.launch { IosAppGraph.preferences.setUserLanguage(code) }
        }, onDismiss = { showLanguage = false })
    }

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

/** Liquid Glass was switched on for this setup already (a process-wide flag: it survives the language rebuild). */
private var iosGlassDefaultApplied = false

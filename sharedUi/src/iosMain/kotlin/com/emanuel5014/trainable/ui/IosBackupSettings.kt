@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.emanuel5014.trainable.data.backup.BackupArchiver
import com.emanuel5014.trainable.data.backup.IosLocalBackupFolder
import com.emanuel5014.trainable.data.backup.IosNextcloudVault
import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.data.remote.nextcloud.IosWebDavHttp
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import com.emanuel5014.trainable.platform.IosAppLocale
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.settings.BackupSettingsActions
import com.emanuel5014.trainable.ui.screens.settings.BackupSettingsCards
import com.emanuel5014.trainable.ui.screens.settings.BackupSettingsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.choose_folder

/** iOS backup ZIP for the shared backup code: the G1 export, and a restore staged for the next launch. */
internal class IosBackupArchiver(private val completeOnboarding: Boolean = false) : BackupArchiver {
    override suspend fun createZip(fileName: String, includeImages: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            IosBackups.manager.exportBackup(includeImages, IosAppGraph.settingsStore.data.first())
        } catch (error: CancellationException) { throw error } catch (_: Exception) { null }
    }

    override suspend fun applyZip(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            IosBackups.manager.stageRestore(path, completeOnboarding)
            true
        } catch (error: CancellationException) { throw error } catch (_: Exception) { false }
    }

    override fun discard(path: String) {
        if (path.startsWith(NSTemporaryDirectory())) NSFileManager.defaultManager.removeItemAtPath(path, null)
        else IosBackups.manager.removeExport(path)
    }

    override fun tempPath(name: String): String = NSTemporaryDirectory() + "nextcloud-" + NSUUID().UUIDString + ".zip"
}

/** Nextcloud on iOS: the shared service on NSURLSession and a Keychain-held password. */
internal object IosNextcloud {
    private fun now() = Clock.System.now().toEpochMilliseconds()
    val webDav by lazy { NextcloudWebDav(IosWebDavHttp()) }
    val service by lazy { NextcloudBackupService(IosAppGraph.preferences, webDav, IosNextcloudVault(), IosBackupArchiver()) { now() } }
    fun service(archiver: BackupArchiver, wifiOnly: Boolean) =
        NextcloudBackupService(IosAppGraph.preferences, NextcloudWebDav(IosWebDavHttp(wifiOnly)), IosNextcloudVault(), archiver) { now() }
}

/**
 * The SettingsViewModel functions of Android, same names and same result messages, for the shared backup cards.
 * iOS shows a message as an alert to dismiss, so the "Uploading…"/"Restoring…" progress messages are left out:
 * the cards already show the progress.
 */
internal class IosBackupSettingsModel(
    private val scope: CoroutineScope,
    private val show: (String) -> Unit,
    private val service: NextcloudBackupService = IosNextcloud.service,
    private val webDav: NextcloudWebDav = IosNextcloud.webDav,
    private val reschedule: suspend () -> Unit = {}
) : BackupSettingsActions {
    private val prefs = IosAppGraph.preferences
    var testState by mutableStateOf<NextcloudConnectionResult?>(null)
    var isTesting by mutableStateOf(false)
    var isBackingUp by mutableStateOf(false)
    var isRestoring by mutableStateOf(false)
    var backups by mutableStateOf<List<NextcloudBackupFile>>(emptyList())
    var isLoadingBackups by mutableStateOf(false)

    private fun change(block: suspend () -> Unit) { scope.launch { block(); reschedule() } }

    override fun setAutoBackupEnabled(enabled: Boolean) = change { prefs.setAutoBackupEnabled(enabled) }
    override fun setAutoBackupFrequency(frequency: Int) = change { prefs.setAutoBackupFrequency(frequency) }
    override fun setAutoBackupMaxCount(count: Int) = change { prefs.setAutoBackupMaxCount(count) }
    override fun setAutoBackupIncludeImages(include: Boolean) = change { prefs.setAutoBackupIncludeImages(include) }
    override fun setNextcloudBackupEnabled(enabled: Boolean) = change { prefs.setNextcloudBackupEnabled(enabled) }
    override fun setNextcloudAutoBackupEnabled(enabled: Boolean) = change { prefs.setNextcloudAutoBackupEnabled(enabled) }
    override fun setNextcloudAutoBackupSettings(frequency: Int, maxCount: Int, includeImages: Boolean, wifiOnly: Boolean) = change {
        prefs.setNextcloudAutoBackupFrequency(frequency)
        prefs.setNextcloudAutoBackupMaxCount(maxCount)
        prefs.setNextcloudAutoBackupIncludeImages(includeImages)
        prefs.setNextcloudWifiOnly(wifiOnly)
        prefs.setNextcloudAutoBackupEnabled(true)
    }

    override fun clearNextcloudTestState() { testState = null }

    override fun testNextcloudConnection(serverUrl: String, username: String, passwordOrToken: String) {
        scope.launch {
            isTesting = true
            testState = null
            testState = webDav.testConnection(NextcloudConfig(serverUrl.trim(), username.trim(), passwordOrToken.trim()))
            isTesting = false
        }
    }

    override fun saveNextcloudConfig(serverUrl: String, username: String, passwordOrToken: String, remoteFolder: String) = change {
        service.saveConfig(NextcloudConfig(serverUrl, username, passwordOrToken, remoteFolder))
        show("Nextcloud configured successfully")
    }

    override fun disconnectNextcloud() = change {
        prefs.clearNextcloudConfig()
        backups = emptyList()
        show("Nextcloud disconnected")
    }

    override fun backupToNextcloud(includeImages: Boolean) {
        scope.launch {
            isBackingUp = true
            val result = service.backupNow(includeImages)
            isBackingUp = false
            if (result.isSuccess) {
                show("Backup uploaded to Nextcloud successfully!")
                loadNextcloudBackups()
            } else show("Nextcloud backup failed: ${result.exceptionOrNull()?.message}")
        }
    }

    override fun loadNextcloudBackups() {
        scope.launch {
            isLoadingBackups = true
            val result = service.list()
            isLoadingBackups = false
            result.onSuccess { backups = it }.onFailure { show("Failed to load backups: ${it.message}") }
        }
    }

    override fun restoreFromNextcloud(backupFile: NextcloudBackupFile, onSuccess: () -> Unit) {
        scope.launch {
            isRestoring = true
            val result = service.restore(backupFile)
            isRestoring = false
            if (result.isSuccess) onSuccess() else show("Restore failed: ${result.exceptionOrNull()?.message}")
        }
    }
}

/** Android's formatNextcloudBackupDate: the time in the file name, else the server date, as "d MMM yyyy • HH:mm". */
internal fun iosNextcloudBackupDate(backup: NextcloudBackupFile): String {
    val out = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale(); dateFormat = "d MMM yyyy • HH:mm" }
    Regex("""Trainable_(?:Auto)?Backup_(\d{4}-\d{2}-\d{2})_(\d{2})(\d{2})(\d{2})\.zip""").find(backup.name)?.let { match ->
        val (day, hour, minute, second) = match.destructured
        val parse = NSDateFormatter().apply { locale = IosAppLocale.formattingLocale(); dateFormat = "yyyy-MM-dd_HHmmss" }
        parse.dateFromString("${day}_$hour$minute$second")?.let { return out.stringFromDate(it) }
    }
    return out.stringFromDate(NSDate.dateWithTimeIntervalSince1970(backup.lastModifiedMillis / 1000.0))
}

/** Android's Formatter.formatFileSize counterpart. */
internal fun iosFileSize(bytes: Long): String = NSByteCountFormatter.stringFromByteCount(bytes, NSByteCountFormatterCountStyleFile)

/** Settings › Data: the shared automatic-backup and Nextcloud cards over [IosBackupSettingsModel]. */
@Composable
internal fun IosBackupSettingsCards() {
    val scope = rememberCoroutineScope()
    val show = LocalShowMessage.current
    val model = remember { IosBackupSettingsModel(scope, show, reschedule = { IosAutoBackup.reschedule() }) }
    val p = IosAppGraph.preferences
    val autoBackupEnabled by p.autoBackupEnabled.collectAsState(false)
    val autoBackupFrequency by p.autoBackupFrequency.collectAsState(1)
    val autoBackupFolderUri by p.autoBackupFolderUri.collectAsState(null)
    // The bookmarked folder's name; "" while it is read, null when it was removed or cannot be opened any more
    val folderName by produceState<String?>("", autoBackupFolderUri) {
        value = autoBackupFolderUri?.let { IosLocalBackupFolder(p, IosBackupArchiver()).folderLabel() }
    }
    val folderMissing = autoBackupFolderUri != null && folderName == null
    val pickFolder = rememberIosBackupFolderPicker()
    val autoBackupMaxCount by p.autoBackupMaxCount.collectAsState(5)
    val autoBackupIncludeImages by p.autoBackupIncludeImages.collectAsState(false)
    val nextcloudBackupEnabled by p.nextcloudBackupEnabled.collectAsState(false)
    val nextcloudAutoBackupEnabled by p.nextcloudAutoBackupEnabled.collectAsState(false)
    val nextcloudServerUrl by p.nextcloudServerUrl.collectAsState(null)
    val nextcloudUsername by p.nextcloudUsername.collectAsState(null)
    val nextcloudRemoteFolder by p.nextcloudRemoteFolder.collectAsState("Trainable/Backups")
    val nextcloudWifiOnly by p.nextcloudWifiOnly.collectAsState(false)
    val nextcloudAutoBackupFrequency by p.nextcloudAutoBackupFrequency.collectAsState(1)
    val nextcloudAutoBackupMaxCount by p.nextcloudAutoBackupMaxCount.collectAsState(5)
    val nextcloudAutoBackupIncludeImages by p.nextcloudAutoBackupIncludeImages.collectAsState(false)
    BackupSettingsCards(
        state = BackupSettingsUiState(
            autoBackupEnabled = autoBackupEnabled,
            autoBackupFrequency = autoBackupFrequency,
            // A folder iOS cannot open any more reads as "no folder": the card offers "Choose folder" again
            autoBackupFolderUri = autoBackupFolderUri.takeUnless { folderMissing },
            autoBackupFolderLabel = if (folderMissing) stringResource(Res.string.choose_folder) else folderName,
            autoBackupMaxCount = autoBackupMaxCount,
            autoBackupIncludeImages = autoBackupIncludeImages,
            nextcloudBackupEnabled = nextcloudBackupEnabled,
            nextcloudAutoBackupEnabled = nextcloudAutoBackupEnabled,
            nextcloudServerUrl = nextcloudServerUrl,
            nextcloudUsername = nextcloudUsername,
            nextcloudHost = nextcloudServerUrl?.let { url -> NSURL.URLWithString(url)?.host ?: url },
            nextcloudRemoteFolder = nextcloudRemoteFolder,
            nextcloudWifiOnly = nextcloudWifiOnly,
            nextcloudAutoBackupFrequency = nextcloudAutoBackupFrequency,
            nextcloudAutoBackupMaxCount = nextcloudAutoBackupMaxCount,
            nextcloudAutoBackupIncludeImages = nextcloudAutoBackupIncludeImages,
            nextcloudTestState = model.testState,
            isTestingConnection = model.isTesting,
            isNextcloudBackingUp = model.isBackingUp,
            isNextcloudRestoring = model.isRestoring,
            nextcloudBackups = model.backups,
            isLoadingNextcloudBackups = model.isLoadingBackups
        ),
        actions = model,
        onPickFolder = pickFolder,
        onRestored = { IosBackups.restartRequired = true },
        dateText = ::iosNextcloudBackupDate,
        sizeText = ::iosFileSize
    )
}

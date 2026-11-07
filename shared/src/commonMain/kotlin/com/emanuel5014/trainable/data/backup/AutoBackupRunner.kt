package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

enum class AutoBackupOutcome { Nothing, Success, Retry }

/**
 * One automatic backup run (formerly the body of Android's AutoBackupWorker): the local folder first, then
 * Nextcloud; retry only when something failed and nothing succeeded. A success is remembered for the iOS catch-up.
 */
class AutoBackupRunner(
    private val prefs: UserPreferencesRepository,
    private val archiver: BackupArchiver,
    private val folder: LocalBackupFolder?,
    private val nextcloud: NextcloudBackupService,
    private val clock: () -> Long
) {
    suspend fun run(): AutoBackupOutcome = try {
        val localEnabled = prefs.autoBackupEnabled.first()
        val nextcloudEnabled = prefs.nextcloudAutoBackupEnabled.first()
        var anySuccess = false
        var anyFailure = false
        if (localEnabled && folder != null && folder.isAvailable()) {
            if (folder.exportTo(BackupNames.automatic(clock()), prefs.autoBackupIncludeImages.first())) {
                cleanUp(folder, prefs.autoBackupMaxCount.first())
                anySuccess = true
            } else anyFailure = true
        }
        if (nextcloudEnabled) {
            val result = nextcloud.performAutoBackup(prefs.nextcloudAutoBackupMaxCount.first(),
                prefs.nextcloudAutoBackupIncludeImages.first())
            if (result.isSuccess) anySuccess = true else anyFailure = true
        }
        if (anySuccess) prefs.setAutoBackupLastRun(clock())
        when {
            !localEnabled && !nextcloudEnabled -> AutoBackupOutcome.Nothing
            anyFailure && !anySuccess -> AutoBackupOutcome.Retry
            else -> AutoBackupOutcome.Success
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        AutoBackupOutcome.Retry
    }

    private suspend fun cleanUp(folder: LocalBackupFolder, max: Int) {
        try {
            BackupRetention.toDelete(folder.listAutoBackups(), { it.first }, { it.second }, max).forEach { folder.delete(it.first) }
        } catch (e: CancellationException) { throw e } catch (_: Exception) {}
    }
}

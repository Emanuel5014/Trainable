package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

enum class AutoBackupOutcome { Nothing, Success, Retry }

/**
 * One automatic backup run (formerly the body of Android's AutoBackupWorker): the local folder first, then
 * Nextcloud; retry only when something failed and nothing succeeded. Each destination's success is remembered on its
 * own, for iOS to decide what is due; Android never reads these. [skipNextcloud] leaves Nextcloud out (iOS only).
 */
class AutoBackupRunner(
    private val prefs: UserPreferencesRepository,
    private val archiver: BackupArchiver,
    private val folder: LocalBackupFolder?,
    private val nextcloud: NextcloudBackupService,
    private val clock: () -> Long
) {
    suspend fun run(skipNextcloud: Boolean = false): AutoBackupOutcome = try {
        val localEnabled = prefs.autoBackupEnabled.first()
        val nextcloudEnabled = prefs.nextcloudAutoBackupEnabled.first()
        var anySuccess = false
        var anyFailure = false
        if (localEnabled && folder != null && folder.isAvailable()) {
            if (folder.exportTo(BackupNames.automatic(clock()), prefs.autoBackupIncludeImages.first())) {
                cleanUp(folder, prefs.autoBackupMaxCount.first())
                prefs.setAutoBackupLastRun(clock())
                anySuccess = true
            } else anyFailure = true
        }
        if (nextcloudEnabled && !skipNextcloud) {
            val result = nextcloud.performAutoBackup(prefs.nextcloudAutoBackupMaxCount.first(),
                prefs.nextcloudAutoBackupIncludeImages.first())
            if (result.isSuccess) {
                prefs.setNextcloudAutoBackupLastRun(clock())
                anySuccess = true
            } else anyFailure = true
        }
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

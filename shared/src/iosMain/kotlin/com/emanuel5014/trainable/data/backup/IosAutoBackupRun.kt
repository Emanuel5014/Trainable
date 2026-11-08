package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** iOS background work, installed by Swift at launch (BGTaskScheduler is reached from Swift, like LiteRT-LM). */
interface IosBackgroundTasks {
    /** Asks iOS for one processing task not before [earliestMillis] (epoch millis), replacing any earlier request. */
    fun submit(earliestMillis: Long, needsNetwork: Boolean)
    fun cancel()
}

object IosBackgroundTasksBridge {
    var tasks: IosBackgroundTasks? = null
}

private const val DAY_MILLIS = 86_400_000L

/** One BGProcessingTask request at the next due time from the preferences (Android's WorkManager periodic job). */
class IosBackupScheduler(
    private val prefs: UserPreferencesRepository,
    private val tasks: () -> IosBackgroundTasks? = { IosBackgroundTasksBridge.tasks },
    private val clock: () -> Long
) : BackupScheduler {
    override suspend fun reschedule() {
        val target = tasks() ?: return
        val plan = BackupSchedule.plan(prefs) ?: return target.cancel()
        val now = clock()
        val due = (prefs.autoBackupLastRun.first() ?: now) + plan.frequencyDays * DAY_MILLIS
        target.submit(maxOf(now, due), plan.needsNetwork)
    }
}

/**
 * The two ways an automatic backup starts on iOS: the system's background task ([runScheduled]) and the catch-up
 * when the app leaves the screen and a backup is overdue ([catchUp]). Nothing runs while a restore or a recovery
 * waits for the next launch. With "Wi-Fi only" on mobile data Nextcloud is left for the next run, and the local copy
 * is written only when the folder has none from the current period, so leaving the app again does not add copies.
 */
class IosAutoBackupRun(
    private val prefs: UserPreferencesRepository,
    private val archiver: BackupArchiver,
    private val folder: LocalBackupFolder?,
    private val nextcloud: NextcloudBackupService,
    private val scheduler: BackupScheduler,
    private val restorePending: () -> Boolean,
    private val expensiveNetwork: suspend () -> Boolean,
    private val clock: () -> Long
) {
    /** True when the run finished without anything left to retry. */
    suspend fun runScheduled(): Boolean {
        if (restorePending()) return false
        val plan = BackupSchedule.plan(prefs)
        if (plan == null) {
            scheduler.reschedule()
            return true
        }
        val wifiBlocked = plan.nextcloudEnabled && plan.wifiOnly && expensiveNetwork()
        val localFolder = if (wifiBlocked && hasRecentLocalCopy(plan)) null else folder
        val outcome = AutoBackupRunner(prefs, archiver, localFolder, nextcloud, clock).run(skipNextcloud = wifiBlocked)
        scheduler.reschedule()
        return outcome != AutoBackupOutcome.Retry || (wifiBlocked && plan.localEnabled)
    }

    /** Runs only when a backup is due; true when it ran. */
    suspend fun catchUp(): Boolean {
        if (restorePending()) return false
        val plan = BackupSchedule.plan(prefs) ?: return false
        if (!BackupSchedule.isDue(plan, prefs.autoBackupLastRun.first(), clock())) return false
        runScheduled()
        return true
    }

    private suspend fun hasRecentLocalCopy(plan: BackupPlan): Boolean {
        val local = folder ?: return false
        val newest = try { local.listAutoBackups().maxOfOrNull { it.second } } catch (e: CancellationException) { throw e } catch (_: Exception) { null } ?: return false
        return clock() - newest < plan.frequencyDays * DAY_MILLIS
    }
}

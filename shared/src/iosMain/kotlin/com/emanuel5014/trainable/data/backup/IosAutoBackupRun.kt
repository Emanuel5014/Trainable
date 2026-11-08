package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import com.emanuel5014.trainable.data.remote.nextcloud.WebDavRequest
import kotlinx.coroutines.flow.first

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

/** One BGProcessingTask request at the first due time from the preferences (Android's WorkManager periodic job). */
class IosBackupScheduler(
    private val prefs: UserPreferencesRepository,
    private val tasks: () -> IosBackgroundTasks? = { IosBackgroundTasksBridge.tasks },
    private val clock: () -> Long
) : BackupScheduler {
    override suspend fun reschedule() {
        val target = tasks() ?: return
        val plan = BackupSchedule.plan(prefs) ?: return target.cancel()
        val now = clock()
        val period = plan.frequencyDays * DAY_MILLIS
        val due = listOfNotNull(
            if (plan.localEnabled) (prefs.autoBackupLastRun.first() ?: now) + period else null,
            if (plan.nextcloudEnabled) (prefs.nextcloudAutoBackupLastRun.first() ?: now) + period else null
        ).min()
        target.submit(maxOf(now, due), plan.needsNetwork)
    }
}

/** Uploads iOS carries out by itself, with the app suspended or closed. */
interface BackgroundUploadSession {
    /** The files of the uploads still in progress or waiting. */
    suspend fun pendingPaths(): List<String>
    /** Moves the ZIP to where it stays until the upload ends; its new path, or null. */
    fun keep(zipPath: String, name: String): String?
    /** Hands the upload over; false when it could not start. */
    fun start(request: WebDavRequest, wifiOnly: Boolean): Boolean
    fun remove(path: String)
}

/**
 * The catch-up's Nextcloud upload: nothing waits for it. [start] hands it to iOS, which may run it much later; [finish]
 * ends it when iOS reports back, even in a later process: remembers the run, applies the copy limit, removes the file.
 */
class IosNextcloudAutoUpload(
    private val prefs: UserPreferencesRepository,
    private val webDav: NextcloudWebDav,
    private val nextcloud: NextcloudBackupService,
    private val archiver: BackupArchiver,
    private val session: BackgroundUploadSession,
    private val scheduler: BackupScheduler,
    private val clock: () -> Long
) {
    suspend fun isPending(): Boolean = session.pendingPaths().isNotEmpty()

    /** False when it could not start (offline, not configured): it stays due. No ZIP is made before the server answers. */
    suspend fun start(): Boolean {
        val config = nextcloud.activeConfig() ?: return false
        if (webDav.ensureFolder(config).isFailure) return false
        val name = BackupNames.automatic(clock())
        val zip = archiver.createZip(name, prefs.nextcloudAutoBackupIncludeImages.first()) ?: return false
        try {
            val kept = session.keep(zip, name) ?: return false
            val started = session.start(webDav.uploadRequest(config, kept, name), prefs.nextcloudWifiOnly.first())
            if (!started) session.remove(kept)
            return started
        } finally {
            archiver.discard(zip)
        }
    }

    suspend fun finish(path: String, code: Int?) {
        try {
            if (code != null && code in 200..299) {
                // Disconnected in the meantime: nothing to remember and nothing to touch on the server
                nextcloud.activeConfig()?.let { config ->
                    prefs.setNextcloudAutoBackupLastRun(clock())
                    nextcloud.trimAutoBackups(config, prefs.nextcloudAutoBackupMaxCount.first())
                }
            }
        } finally {
            session.remove(path)
        }
        scheduler.reschedule()
    }
}

/**
 * The two ways an automatic backup starts on iOS: the system's background task ([runScheduled]) and the catch-up when
 * the app leaves the screen ([catchUp]). Each destination runs when its own last success is older than the frequency,
 * so a failure in one is retried without another copy in the other. Nextcloud also waits while an upload is in
 * progress and, with "Wi-Fi only", while on mobile data. Nothing runs while a restore or a recovery waits for the next
 * launch.
 */
class IosAutoBackupRun(
    private val prefs: UserPreferencesRepository,
    private val archiver: BackupArchiver,
    private val folder: LocalBackupFolder?,
    private val nextcloud: NextcloudBackupService,
    private val upload: IosNextcloudAutoUpload,
    private val scheduler: BackupScheduler,
    private val restorePending: () -> Boolean,
    private val expensiveNetwork: suspend () -> Boolean,
    private val clock: () -> Long
) {
    /** True unless it failed with nothing done. There are minutes here, so Nextcloud is waited for. */
    suspend fun runScheduled(): Boolean {
        if (restorePending()) return false
        val plan = BackupSchedule.plan(prefs)
        var outcome = AutoBackupOutcome.Nothing
        if (plan != null) {
            val local = localDue(plan)
            val cloud = cloudDue(plan)
            if (local || cloud) outcome = runner(local).run(skipNextcloud = !cloud)
        }
        scheduler.reschedule()
        return outcome != AutoBackupOutcome.Retry
    }

    /** Runs what is due in the few seconds iOS grants, handing the upload over; true when something was due. */
    suspend fun catchUp(): Boolean {
        if (restorePending()) return false
        val plan = BackupSchedule.plan(prefs)
        val local = plan != null && localDue(plan)
        val cloud = plan != null && cloudDue(plan)
        if (local) runner(true).run(skipNextcloud = true)
        if (cloud) upload.start()
        scheduler.reschedule()
        return local || cloud
    }

    private fun runner(local: Boolean) = AutoBackupRunner(prefs, archiver, folder.takeIf { local }, nextcloud, clock)

    private suspend fun localDue(plan: BackupPlan) =
        plan.localEnabled && BackupSchedule.isDue(plan, prefs.autoBackupLastRun.first(), clock())

    private suspend fun cloudDue(plan: BackupPlan) =
        plan.nextcloudEnabled && BackupSchedule.isDue(plan, prefs.nextcloudAutoBackupLastRun.first(), clock()) &&
            !upload.isPending() && !(plan.wifiOnly && expensiveNetwork())
}

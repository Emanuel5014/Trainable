package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Backup file names, as Android writes them (`yyyy-MM-dd_HHmmss`, device time zone). */
object BackupNames {
    const val AUTO_PREFIX = "Trainable_AutoBackup_"
    private const val MANUAL_PREFIX = "Trainable_Backup_"

    fun automatic(nowMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()) = AUTO_PREFIX + stamp(nowMillis, zone) + ".zip"
    fun manual(nowMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()) = MANUAL_PREFIX + stamp(nowMillis, zone) + ".zip"

    private fun stamp(millis: Long, zone: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
        fun two(v: Int) = v.toString().padStart(2, '0')
        return "${t.year}-${two(t.month.ordinal + 1)}-${two(t.day)}_${two(t.hour)}${two(t.minute)}${two(t.second)}"
    }
}

/** Which automatic backups to delete: only `Trainable_AutoBackup_*`, the newest [max] kept. */
object BackupRetention {
    fun <T> toDelete(files: List<T>, name: (T) -> String, modified: (T) -> Long, max: Int): List<T> =
        files.filter { name(it).startsWith(BackupNames.AUTO_PREFIX) }.sortedByDescending(modified).drop(max)
}

/** What the automatic backup has to do, read from the preferences. */
data class BackupPlan(
    val localEnabled: Boolean,
    val nextcloudEnabled: Boolean,
    val frequencyDays: Int,
    val needsNetwork: Boolean,
    val wifiOnly: Boolean
)

object BackupSchedule {
    private const val DAY_MILLIS = 86_400_000L

    /** Null when neither destination is on. One job runs at the more frequent of the two (as Android). */
    suspend fun plan(prefs: UserPreferencesRepository): BackupPlan? {
        val local = prefs.autoBackupEnabled.first()
        val cloud = prefs.nextcloudAutoBackupEnabled.first()
        if (!local && !cloud) return null
        val localDays = prefs.autoBackupFrequency.first()
        val cloudDays = prefs.nextcloudAutoBackupFrequency.first()
        val days = when {
            local && cloud -> minOf(localDays, cloudDays)
            local -> localDays
            else -> cloudDays
        }
        return BackupPlan(local, cloud, days, needsNetwork = cloud, wifiOnly = prefs.nextcloudWifiOnly.first())
    }

    fun isDue(plan: BackupPlan, lastRunMillis: Long?, nowMillis: Long): Boolean =
        lastRunMillis == null || nowMillis - lastRunMillis >= plan.frequencyDays * DAY_MILLIS
}

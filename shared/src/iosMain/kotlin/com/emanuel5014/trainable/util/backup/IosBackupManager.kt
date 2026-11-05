@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.util.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository as P
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okio.*
import okio.Path.Companion.toPath
import platform.Foundation.*

/**
 * iOS backup IO. A restore is validated and staged while the app runs, then installed at the next
 * process launch, before Room or DataStore are opened. A durable journal can undo an interrupted install.
 */
class IosBackupManager(
    private val documents: Path = iosBackupDocuments(),
    private val workspace: Path = iosBackupWorkspace(),
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val domain: String = requireNotNull(NSBundle.mainBundle.bundleIdentifier),
    private val fs: FileSystem = FileSystem.SYSTEM
) {
    private val pending get() = workspace / "pending"
    private val journal get() = workspace / "journal"
    private val ready get() = workspace / "ready"

    fun exportBackup(includeImages: Boolean, preferences: Preferences): String {
        fs.createDirectories(workspace)
        val staging = workspace / ("export-" + NSUUID().UUIDString)
        fs.createDirectories(staging)
        try {
            val database = staging / BackupFormat.DATABASE
            snapshot(documents / BackupFormat.DATABASE, database)
            val entries = mutableListOf(IosBackupZip.File(BackupFormat.DATABASE, database))
            fun json(name: String, text: String?) {
                if (text == null) return
                val path = staging / name
                fs.write(path) { writeUtf8(text) }
                entries += IosBackupZip.File(name, path)
            }
            json(BackupFormat.SETTINGS, preferences[P.GYM_MEMBERSHIP_EXPIRY_DATE]?.let(BackupSettingsCodec::membership))
            val analytics = BackupAnalyticsSettings(
                defaults.stringArrayForKey("analytics_prefs.selected_exercise_ids")?.map { it as String }?.toSet(),
                defaults.stringForKey("analytics_prefs.widget_order"),
                if (defaults.objectForKey("analytics_prefs.show_progress_cards") != null)
                    defaults.boolForKey("analytics_prefs.show_progress_cards") else null,
                defaults.stringForKey("analytics_prefs.category_volume_time_range")
            )
            json(BackupFormat.ANALYTICS_SETTINGS, BackupSettingsCodec.analytics(analytics))
            json(BackupFormat.APP_PREFERENCES, BackupSettingsCodec.appPreferences(preferences))
            if (includeImages) {
                val referenced = referencedImages(database)
                val photos = listOf(documents, documents / "routine_images").flatMap { fs.listOrNull(it).orEmpty() }
                    .filter { fs.metadata(it).isRegularFile && it.name in referenced }
                    .associateBy { it.name }
                photos.values.forEach { entries += IosBackupZip.File(BackupFormat.IMAGES + it.name, it) }
                for ((folder, prefix) in listOf("exercise_media" to BackupFormat.EXERCISE_MEDIA,
                    "physical_checks" to BackupFormat.PHYSICAL_CHECKS)) {
                    fs.listOrNull(documents / folder).orEmpty().filter { fs.metadata(it).isRegularFile }
                        .forEach { entries += IosBackupZip.File(prefix + it.name, it) }
                }
            }
            val output = staging / if (includeImages) "Trainable_Backup_Full.zip" else "Trainable_Backup.zip"
            IosBackupZip.write(output, entries, fs)
            // Only the archive must survive until the export picker has finished.
            fs.list(staging).filter { it != output }.forEach { fs.delete(it) }
            return output.toString()
        } catch (error: Throwable) {
            fs.deleteRecursively(staging)
            throw error
        }
    }

    fun removeExport(path: String) {
        val file = path.toPath()
        if (file.parent?.parent == workspace && file.parent?.name?.startsWith("export-") == true)
            fs.deleteRecursively(requireNotNull(file.parent))
    }

    /** No changes to live app data. Call only after the user confirms replacing the current data. */
    suspend fun stageRestore(archive: String) {
        fs.createDirectories(workspace)
        val staging = workspace / ("import-" + NSUUID().UUIDString)
        fs.createDirectories(staging)
        try {
            IosBackupZip.extract(archive.toPath(), staging, fs)
            // SHM is a process-specific SQLite index; SQLite reconstructs it from the database and WAL.
            fs.delete(staging / (BackupFormat.DATABASE + "-shm"))
            validateSettings(staging)
            val database = staging / BackupFormat.DATABASE
            checkDatabase(database)
            val room = Room.databaseBuilder<GymDatabase>(name = database.toString())
                .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(*GymDatabase.ALL_MIGRATIONS).build()
            try { room.exerciseDao().getAllExercisesIncludingDeleted().first() } finally { room.close() }
            val compact = staging / "validated.db"
            snapshot(database, compact)
            fs.atomicMove(compact, database)
            listOf("-wal", "-shm", "-journal").forEach { fs.delete(staging / (BackupFormat.DATABASE + it)) }
            writeSynced(staging / "validated", "1")
            // Replacing a previous pending restore is explicit: stageRestore is only called from confirmation.
            fs.deleteRecursively(pending)
            fs.atomicMove(staging, pending)
        } finally { fs.deleteRecursively(staging) }
    }

    fun hasPendingRestore(): Boolean = fs.exists(pending / "validated")
    fun cancelPendingRestore() { fs.deleteRecursively(pending) }

    /** Must run once at startup, before constructing the application's database/settings graph. */
    suspend fun finishPendingRestore(): RestoreResult {
        fs.createDirectories(workspace)
        fs.list(workspace).filter { it.name.startsWith("export-") || it.name.startsWith("import-") || it.name == "journal-preparing" }
            .forEach { fs.deleteRecursively(it) }
        if (fs.exists(journal)) {
            val committed = fs.exists(journal / "committed")
            recoverJournal()
            return if (committed) RestoreResult.Restored else RestoreResult.Failed
        }
        if (!hasPendingRestore()) return RestoreResult.None
        try {
            prepareReplacement()
            installReplacement()
            return RestoreResult.Restored
        } catch (error: CancellationException) {
            if (fs.exists(journal)) recoverJournal()
            throw error
        } catch (_: Exception) {
            if (fs.exists(journal)) recoverJournal()
            fs.deleteRecursively(pending)
            fs.deleteRecursively(ready)
            return RestoreResult.Failed
        }
    }

    private suspend fun prepareReplacement() {
        fs.deleteRecursively(ready)
        fs.createDirectories(ready)
        fs.copy(pending / BackupFormat.DATABASE, ready / BackupFormat.DATABASE)
        val preferences = ready / "settings.preferences_pb"
        if (fs.exists(documents / preferences.name)) fs.copy(documents / preferences.name, preferences)
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.createWithPath(scope = CoroutineScope(job + Dispatchers.IO),
            produceFile = { preferences })
        try {
            store.edit { values ->
                readText(pending, BackupFormat.APP_PREFERENCES)?.let {
                    BackupSettingsCodec.restoreAppPreferences(KotlinxBackupJsonObject(it), values)
                }
                readText(pending, BackupFormat.SETTINGS)?.let { text ->
                    BackupSettingsCodec.membershipExpiry(text)?.let { values[P.GYM_MEMBERSHIP_EXPIRY_DATE] = it }
                }
                // Keys wrapped on this device belong to its previous vault, never to the imported one.
                values.remove(P.PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE)
                values.remove(P.PHYSICAL_CHECK_WRAPPED_KEY_IV)
            }
        } finally { job.cancelAndJoin() }
        for ((source, target) in imageFolders) {
            if (!fs.exists(pending / source)) continue
            copyTree(documents / target, ready / target)
            copyTree(pending / source, ready / target)
        }
        val newDomain = currentDomain().toMutableMap()
        newDomain.remove("retired_presets_version")
        newDomain.keys.filter { it.startsWith("workout_mini_set.") }.forEach(newDomain::remove)
        readText(pending, BackupFormat.ANALYTICS_SETTINGS)?.let { text ->
            val analytics = BackupSettingsCodec.readAnalytics(KotlinxBackupJsonObject(text))
            analytics.selectedExerciseIds?.let { newDomain["analytics_prefs.selected_exercise_ids"] = it.toList() }
            analytics.widgetOrder?.let { newDomain["analytics_prefs.widget_order"] = it }
            analytics.showProgressCards?.let { newDomain["analytics_prefs.show_progress_cards"] = it }
            analytics.categoryVolumeTimeRange?.let { newDomain["analytics_prefs.category_volume_time_range"] = it }
        }
        writeDomain(ready / "defaults.plist", newDomain)
    }

    private fun installReplacement() {
        val targets = listOf(BackupFormat.DATABASE, BackupFormat.DATABASE + "-wal",
            BackupFormat.DATABASE + "-shm", BackupFormat.DATABASE + "-journal", "settings.preferences_pb") +
            imageFolders.map { it.second }.filter { fs.exists(ready / it) }
        val preparing = workspace / "journal-preparing"
        fs.deleteRecursively(preparing)
        fs.createDirectories(preparing / "old")
        writeDomain(preparing / "defaults.plist", currentDomain())
        writeSynced(preparing / "manifest", targets.joinToString("") { "$it\t${fs.exists(documents / it)}\n" })
        fs.atomicMove(preparing, journal)
        for (name in targets) {
            val target = documents / name
            if (fs.exists(target)) fs.atomicMove(target, journal / "old" / name)
            if (fs.exists(ready / name)) fs.atomicMove(ready / name, target)
        }
        defaults.setPersistentDomain(readDomain(ready / "defaults.plist").mapKeys { it.key as Any? }, domain)
        check(defaults.synchronize()) { "Could not save restored settings" }
        writeSynced(journal / "committed", "1")
        recoverJournal() // committed: cleanup only
    }

    private fun recoverJournal() {
        if (!fs.exists(journal / "committed")) {
            val manifest = fs.read(journal / "manifest") { readUtf8() }
            for (line in manifest.lineSequence().filter { it.isNotBlank() }) {
                val (name, existed) = line.split('\t')
                require(name in allowedTargets)
                val old = journal / "old" / name
                val target = documents / name
                if (fs.exists(old)) {
                    fs.deleteRecursively(target)
                    fs.atomicMove(old, target)
                } else if (existed == "false") fs.deleteRecursively(target)
            }
            defaults.setPersistentDomain(readDomain(journal / "defaults.plist").mapKeys { it.key as Any? }, domain)
            check(defaults.synchronize()) { "Could not restore previous settings" }
        }
        fs.deleteRecursively(pending)
        fs.deleteRecursively(ready)
        fs.deleteRecursively(journal)
    }

    private fun validateSettings(folder: Path) {
        readText(folder, BackupFormat.APP_PREFERENCES)?.let {
            BackupSettingsCodec.restoreAppPreferences(KotlinxBackupJsonObject(it), mutablePreferencesOf())
        }
        readText(folder, BackupFormat.ANALYTICS_SETTINGS)?.let {
            BackupSettingsCodec.readAnalytics(KotlinxBackupJsonObject(it))
        }
        readText(folder, BackupFormat.SETTINGS)?.let { KotlinxBackupJsonObject(it) }
    }

    private fun snapshot(source: Path, output: Path) {
        require(fs.exists(source)) { "Database is missing" }
        BundledSQLiteDriver().open(source.toString()).use { connection ->
            connection.prepare("PRAGMA busy_timeout=5000").use { it.step() }
            connection.prepare("VACUUM INTO ?").use { statement ->
                statement.bindText(1, output.toString()); statement.step()
            }
        }
        checkDatabase(output)
    }

    private fun checkDatabase(path: Path) {
        BundledSQLiteDriver().open(path.toString()).use { connection ->
            connection.prepare("PRAGMA quick_check").use { statement ->
                require(statement.step() && statement.getText(0) == "ok") { "Invalid backup database" }
            }
        }
    }

    private fun referencedImages(database: Path): Set<String> = buildSet {
        BundledSQLiteDriver().open(database.toString()).use { connection ->
            for (table in listOf("workout_plans", "workout_plan_images")) {
                connection.prepare("SELECT image_uri FROM $table WHERE image_uri IS NOT NULL").use { statement ->
                    while (statement.step()) add(statement.getText(0).substringAfterLast('/'))
                }
            }
        }
    }

    private fun writeSynced(path: Path, text: String) {
        fs.write(path) { writeUtf8(text) }
        fs.openReadWrite(path, mustExist = true).use { it.flush() }
    }

    private fun readText(folder: Path, name: String): String? = (folder / name).takeIf(fs::exists)?.let {
        require((fs.metadata(it).size ?: Long.MAX_VALUE) <= 2L * 1024 * 1024)
        fs.read(it) { readUtf8() }
    }
    private fun copyTree(source: Path, target: Path) {
        fs.createDirectories(target)
        for (file in fs.listOrNull(source).orEmpty()) {
            require(fs.metadata(file).isRegularFile) { "Unexpected backup folder item" }
            fs.copy(file, target / file.name)
        }
    }
    @Suppress("UNCHECKED_CAST")
    private fun currentDomain(): Map<String, Any> = defaults.persistentDomainForName(domain) as? Map<String, Any> ?: emptyMap()
    private fun writeDomain(path: Path, values: Map<String, Any>) {
        val data = requireNotNull(NSPropertyListSerialization.dataWithPropertyList(values,
            NSPropertyListXMLFormat_v1_0, options = 0u, error = null))
        check(data.writeToFile(path.toString(), atomically = true))
    }
    @Suppress("UNCHECKED_CAST")
    private fun readDomain(path: Path): Map<String, Any> {
        val data = requireNotNull(NSData.dataWithContentsOfFile(path.toString()))
        return NSPropertyListSerialization.propertyListWithData(data, options = 0u, format = null, error = null) as Map<String, Any>
    }

    enum class RestoreResult { None, Restored, Failed }
    companion object {
        private val imageFolders = listOf("images" to "routine_images", "exercise_media" to "exercise_media", "physical_checks" to "physical_checks")
        private val allowedTargets = setOf(BackupFormat.DATABASE, BackupFormat.DATABASE + "-wal", BackupFormat.DATABASE + "-shm",
            BackupFormat.DATABASE + "-journal", "settings.preferences_pb", "routine_images", "exercise_media", "physical_checks")
    }
}

private fun iosBackupDocuments(): Path = requireNotNull(NSFileManager.defaultManager.URLForDirectory(
    NSDocumentDirectory, NSUserDomainMask, null, true, null)?.path).toPath()
private fun iosBackupWorkspace(): Path = requireNotNull(NSFileManager.defaultManager.URLForDirectory(
    NSApplicationSupportDirectory, NSUserDomainMask, null, true, null)?.path).toPath() / "TrainableBackup"

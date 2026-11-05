package com.emanuel5014.trainable.util.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import com.emanuel5014.trainable.data.local.GymDatabaseInstance
import com.emanuel5014.trainable.data.local.RetiredPresetsMarker
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.util.ExerciseMediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutDao: WorkoutDao
) {

    private val dbName = BackupFormat.DATABASE

    /**
     * Common logic to write the backup ZIP to any OutputStream
     */
    private suspend fun writeBackupToStream(os: OutputStream, includeImages: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(os).use { zos ->
                // 1. Export Database Files
                val dbFile = context.getDatabasePath(dbName)
                val walFile = context.getDatabasePath("$dbName-wal")
                val shmFile = context.getDatabasePath("$dbName-shm")

                val dbFiles = listOfNotNull(
                    dbFile.takeIf { it.exists() },
                    walFile.takeIf { it.exists() },
                    shmFile.takeIf { it.exists() }
                )

                for (file in dbFiles) {
                    FileInputStream(file).use { fis ->
                        zos.putNextEntry(ZipEntry(file.name))
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }

                // 2. Export Membership Expiry Date
                val membershipExpiry = getMembershipExpiryDate()
                if (membershipExpiry != null) {
                    val settingsJson = BackupSettingsCodec.membership(membershipExpiry)
                    zos.putNextEntry(ZipEntry(BackupFormat.SETTINGS))
                    zos.write(settingsJson.toByteArray())
                    zos.closeEntry()
                }

                // 2b. Export Analytics Preferences (Widgets)
                val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
                val selectedExerciseIds = prefs.getStringSet("selected_exercise_ids", null)
                val widgetOrder = prefs.getString("widget_order", null)
                val showProgressCards = if (prefs.contains("show_progress_cards")) prefs.getBoolean("show_progress_cards", false) else null
                val categoryVolumeTimeRange = prefs.getString("category_volume_time_range", null)
                
                if (selectedExerciseIds != null || widgetOrder != null || showProgressCards != null || categoryVolumeTimeRange != null) {
                    val analyticsJson = BackupSettingsCodec.analytics(
                        BackupAnalyticsSettings(selectedExerciseIds, widgetOrder, showProgressCards, categoryVolumeTimeRange),
                        AndroidBackupJsonObject()
                    )!!
                    zos.putNextEntry(ZipEntry(BackupFormat.ANALYTICS_SETTINGS))
                    zos.write(analyticsJson.toByteArray())
                    zos.closeEntry()
                }

                // 2c. Export App Preferences (Theme, Weight Unit, Language, Swipe Actions, etc.)
                val appPrefs = buildAppPreferencesJson()
                zos.putNextEntry(ZipEntry(BackupFormat.APP_PREFERENCES))
                zos.write(appPrefs.toByteArray())
                zos.closeEntry()

                // 3. Export Images if requested - Only if referenced in database
                if (includeImages) {
                    val planImages = workoutDao.getAllPlanImages()
                    val multiplePlanImages = workoutDao.getAllMultiplePlanImages()
                    val referencedFilenames = (planImages + multiplePlanImages)
                        .mapNotNull { path ->
                            path.takeIf { it.isNotBlank() }?.let { File(it).name }
                        }
                        .toSet()

                    val filesDir = context.filesDir
                    val routineImagesDir = File(context.filesDir, "routine_images")
                    
                    val allLocalFiles = (filesDir.listFiles()?.toList() ?: emptyList()) + 
                                        (routineImagesDir.listFiles()?.toList() ?: emptyList())

                    allLocalFiles.forEach { file ->
                        if (file.isFile && referencedFilenames.contains(file.name)) {
                            FileInputStream(file).use { fis ->
                                zos.putNextEntry(ZipEntry("${BackupFormat.IMAGES}${file.name}"))
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }


                    // Export the user's own exercise images / GIFs
                    ExerciseMediaStorage.dir(context).listFiles()?.forEach { file ->
                        if (file.isFile) {
                            FileInputStream(file).use { fis ->
                                zos.putNextEntry(ZipEntry("${ExerciseMediaStorage.DIR_NAME}/${file.name}"))
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }

                    // Export physical check images
                    val physicalChecksDir = File(context.filesDir, "physical_checks")
                    if (physicalChecksDir.exists()) {
                        physicalChecksDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                FileInputStream(file).use { fis ->
                                    zos.putNextEntry(ZipEntry("${BackupFormat.PHYSICAL_CHECKS}${file.name}"))
                                    fis.copyTo(zos)
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportDatabaseZip(outputUri: Uri, includeImages: Boolean = false): Boolean {
        return try {
            context.contentResolver.openOutputStream(outputUri)?.use { os ->
                writeBackupToStream(os, includeImages)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportDatabaseToFile(outputFile: File, includeImages: Boolean = false): Boolean {
        return try {
            FileOutputStream(outputFile).use { os ->
                writeBackupToStream(os, includeImages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Creates a new backup file inside a SAF folder (tree URI)
     */
    suspend fun exportDatabaseToFolder(folderUri: Uri, fileName: String, includeImages: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val directoryUri = DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )
            
            val docUri = DocumentsContract.createDocument(
                context.contentResolver,
                directoryUri,
                "application/zip",
                fileName
            ) ?: return@withContext false

            context.contentResolver.openOutputStream(docUri)?.use { os ->
                writeBackupToStream(os, includeImages)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getMembershipExpiryDate(): Long? {
        return try {
            runBlocking {
                context.dataStore.data.first()[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_DATE]
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildAppPreferencesJson(): String {
        return try {
            runBlocking {
                val prefs = context.dataStore.data.first()
                BackupSettingsCodec.appPreferences(prefs, AndroidBackupJsonObject())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }

    private suspend fun importDatabaseFromStream(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            GymDatabaseInstance.closeDatabase()
            // The backup may predate the retired presets: the clean-up has to run again on the database restored
            RetiredPresetsMarker.forget(context)
            val dbDir = context.getDatabasePath(dbName).parentFile ?: return@withContext false
            if (!dbDir.exists()) {
                dbDir.mkdirs()
            } else {
                dbDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(dbName)) {
                        file.delete()
                    }
                }
            }
            val filesDir = context.filesDir
            val routineImagesDir = File(context.filesDir, "routine_images")
            if (!routineImagesDir.exists()) routineImagesDir.mkdirs()

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == BackupFormat.SETTINGS -> {
                                val jsonContent = zis.bufferedReader().readText()
                                val expiryDate = parseMembershipExpiryFromJson(jsonContent)
                                if (expiryDate != null) {
                                    runBlocking {
                                        context.dataStore.edit { prefs ->
                                            prefs[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_DATE] = expiryDate
                                        }
                                    }
                                }
                            }
                            entry.name == BackupFormat.ANALYTICS_SETTINGS -> {
                                try {
                                    val jsonContent = zis.bufferedReader().readText()
                                    val values = BackupSettingsCodec.readAnalytics(AndroidBackupJsonObject(jsonContent))
                                    val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
                                    val editor = prefs.edit()
                                    values.selectedExerciseIds?.let { editor.putStringSet("selected_exercise_ids", it) }
                                    values.widgetOrder?.let { editor.putString("widget_order", it) }
                                    values.showProgressCards?.let { editor.putBoolean("show_progress_cards", it) }
                                    values.categoryVolumeTimeRange?.let { editor.putString("category_volume_time_range", it) }

                                    editor.commit()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            entry.name == BackupFormat.APP_PREFERENCES -> {
                                try {
                                    val jsonContent = zis.bufferedReader().readText()
                                    val jsonObject = AndroidBackupJsonObject(jsonContent)
                                    runBlocking {
                                        context.dataStore.edit { prefs ->
                                            BackupSettingsCodec.restoreAppPreferences(jsonObject, prefs)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            entry.name.startsWith(BackupFormat.PHYSICAL_CHECKS) -> {
                                val imageName = entry.name.removePrefix(BackupFormat.PHYSICAL_CHECKS)
                                if (imageName.isNotEmpty()) {
                                    val physicalChecksDir = File(context.filesDir, "physical_checks")
                                    if (!physicalChecksDir.exists()) physicalChecksDir.mkdirs()
                                    val outFile = File(physicalChecksDir, imageName)
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry.name.startsWith("${ExerciseMediaStorage.DIR_NAME}/") -> {
                                val mediaName = File(entry.name).name
                                if (mediaName.isNotEmpty()) {
                                    FileOutputStream(File(ExerciseMediaStorage.dir(context), mediaName)).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry.name.startsWith(BackupFormat.IMAGES) -> {
                                val imageName = entry.name.removePrefix(BackupFormat.IMAGES)
                                if (imageName.isNotEmpty()) {
                                    val outFile = File(routineImagesDir, imageName)
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            entry.name.startsWith(dbName) -> {
                                val outFile = File(dbDir, entry.name)
                                FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun importDatabaseZip(inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(inputUri)?.use { fis ->
                importDatabaseFromStream(fis)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDatabaseFromFile(inputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(inputFile).use { fis ->
                importDatabaseFromStream(fis)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportDatabaseToTempFile(fileName: String, includeImages: Boolean = false): File? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, fileName)
            if (tempFile.exists()) tempFile.delete()
            val success = exportDatabaseToFile(tempFile, includeImages)
            if (success) tempFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseMembershipExpiryFromJson(json: String): Long? {
        return try {
            BackupSettingsCodec.membershipExpiry(json)
        } catch (e: Exception) {
            null
        }
    }


    @Deprecated("Use exportDatabaseToFolder or exportDatabaseZip", ReplaceWith("exportDatabaseZip(folderUri, includeImages)"))
    suspend fun exportDatabaseZipToUri(folderUri: Uri, includeImages: Boolean = false): Boolean {
        return exportDatabaseZip(folderUri, includeImages)
    }
}

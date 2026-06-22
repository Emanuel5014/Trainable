package com.emanuel5014.trainable.util.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.edit
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutDao: WorkoutDao
) {

    private val dbName = "gym_tracking_database"

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
                    val settingsJson = """{"gym_membership_expiry_date":$membershipExpiry}"""
                    zos.putNextEntry(ZipEntry("settings.json"))
                    zos.write(settingsJson.toByteArray())
                    zos.closeEntry()
                }

                // 2b. Export Analytics Preferences (Widgets)
                val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
                val selectedExerciseIds = prefs.getStringSet("selected_exercise_ids", null)
                val widgetOrder = prefs.getString("widget_order", null)
                
                if (selectedExerciseIds != null || widgetOrder != null) {
                    val jsonObject = org.json.JSONObject()
                    selectedExerciseIds?.let { ids ->
                        val jsonArray = org.json.JSONArray()
                        ids.forEach { jsonArray.put(it) }
                        jsonObject.put("selected_exercise_ids", jsonArray)
                    }
                    widgetOrder?.let { jsonObject.put("widget_order", it) }
                    
                    zos.putNextEntry(ZipEntry("analytics_settings.json"))
                    zos.write(jsonObject.toString().toByteArray())
                    zos.closeEntry()
                }

                // 2c. Export App Preferences (Theme, Weight Unit, Language, Swipe Actions, etc.)
                val appPrefs = buildAppPreferencesJson()
                zos.putNextEntry(ZipEntry("app_preferences.json"))
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
                                zos.putNextEntry(ZipEntry("images/${file.name}"))
                                fis.copyTo(zos)
                                zos.closeEntry()
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
                val json = org.json.JSONObject()

                json.put("weight_unit", prefs[UserPreferencesRepository.WEIGHT_UNIT] ?: "kg")
                prefs[UserPreferencesRepository.USER_LANGUAGE]?.let { json.put("user_language", it) }
                json.put("swipe_actions_enabled", prefs[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true)
                json.put("dynamic_color", prefs[UserPreferencesRepository.DYNAMIC_COLOR] ?: true)
                prefs[UserPreferencesRepository.DYNAMIC_COLOR_SEED]?.let { json.put("dynamic_color_seed", it) }
                json.put("theme_palette", prefs[UserPreferencesRepository.THEME_PALETTE] ?: 0)
                json.put("theme_style", prefs[UserPreferencesRepository.THEME_STYLE] ?: 0)
                json.put("haptic_enabled", prefs[UserPreferencesRepository.HAPTIC_ENABLED] ?: true)
                json.put("weekly_goal", prefs[UserPreferencesRepository.WEEKLY_GOAL] ?: 3)
                json.put("floating_nav_bar", prefs[UserPreferencesRepository.FLOATING_NAV_BAR] ?: true)

                json.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }

    suspend fun importDatabaseZip(inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbDir = context.getDatabasePath(dbName).parentFile ?: return@withContext false
            if (!dbDir.exists()) dbDir.mkdirs()
            val filesDir = context.filesDir
            val routineImagesDir = File(context.filesDir, "routine_images")
            if (!routineImagesDir.exists()) routineImagesDir.mkdirs()

            context.contentResolver.openInputStream(inputUri)?.use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "settings.json" -> {
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
                            entry.name == "analytics_settings.json" -> {
                                try {
                                    val jsonContent = zis.bufferedReader().readText()
                                    val jsonObject = org.json.JSONObject(jsonContent)
                                    val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
                                    val editor = prefs.edit()
                                    
                                    if (jsonObject.has("selected_exercise_ids")) {
                                        val jsonArray = jsonObject.getJSONArray("selected_exercise_ids")
                                        val ids = mutableSetOf<String>()
                                        for (i in 0 until jsonArray.length()) {
                                            ids.add(jsonArray.getString(i))
                                        }
                                        editor.putStringSet("selected_exercise_ids", ids)
                                    }
                                    
                                    if (jsonObject.has("widget_order")) {
                                        editor.putString("widget_order", jsonObject.getString("widget_order"))
                                    }
                                    
                                    editor.apply()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            entry.name == "app_preferences.json" -> {
                                try {
                                    val jsonContent = zis.bufferedReader().readText()
                                    val jsonObject = org.json.JSONObject(jsonContent)
                                    runBlocking {
                                        context.dataStore.edit { prefs ->
                                            if (jsonObject.has("weight_unit"))
                                                prefs[UserPreferencesRepository.WEIGHT_UNIT] = jsonObject.getString("weight_unit")
                                            if (jsonObject.has("user_language") && !jsonObject.isNull("user_language"))
                                                prefs[UserPreferencesRepository.USER_LANGUAGE] = jsonObject.getString("user_language")
                                            if (jsonObject.has("swipe_actions_enabled"))
                                                prefs[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] = jsonObject.getBoolean("swipe_actions_enabled")
                                            if (jsonObject.has("dynamic_color"))
                                                prefs[UserPreferencesRepository.DYNAMIC_COLOR] = jsonObject.getBoolean("dynamic_color")
                                            if (jsonObject.has("dynamic_color_seed") && !jsonObject.isNull("dynamic_color_seed"))
                                                prefs[UserPreferencesRepository.DYNAMIC_COLOR_SEED] = jsonObject.getInt("dynamic_color_seed")
                                            if (jsonObject.has("theme_palette"))
                                                prefs[UserPreferencesRepository.THEME_PALETTE] = jsonObject.getInt("theme_palette")
                                            if (jsonObject.has("theme_style"))
                                                prefs[UserPreferencesRepository.THEME_STYLE] = jsonObject.getInt("theme_style")
                                            if (jsonObject.has("haptic_enabled"))
                                                prefs[UserPreferencesRepository.HAPTIC_ENABLED] = jsonObject.getBoolean("haptic_enabled")
                                            if (jsonObject.has("weekly_goal"))
                                                prefs[UserPreferencesRepository.WEEKLY_GOAL] = jsonObject.getInt("weekly_goal")
                                            if (jsonObject.has("floating_nav_bar"))
                                                prefs[UserPreferencesRepository.FLOATING_NAV_BAR] = jsonObject.getBoolean("floating_nav_bar")
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            entry.name.startsWith("images/") -> {
                                val imageName = entry.name.removePrefix("images/")
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
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseMembershipExpiryFromJson(json: String): Long? {
        return try {
            val regex = """"gym_membership_expiry_date"\s*:\s*(\d+)""".toRegex()
            regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }


    @Deprecated("Use exportDatabaseToFolder or exportDatabaseZip", ReplaceWith("exportDatabaseZip(folderUri, includeImages)"))
    suspend fun exportDatabaseZipToUri(folderUri: Uri, includeImages: Boolean = false): Boolean {
        return exportDatabaseZip(folderUri, includeImages)
    }
}

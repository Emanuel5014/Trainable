package com.example.gymtracking.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class BackupManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val dbName = "gym_tracking_database"

    suspend fun exportDatabaseZip(outputUri: Uri, includeImages: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val walFile = context.getDatabasePath("$dbName-wal")
            val shmFile = context.getDatabasePath("$dbName-shm")

            context.contentResolver.openOutputStream(outputUri)?.use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 1. Export Database Files
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

                    // 2. Export Images if requested
                    if (includeImages) {
                        val filesDir = context.filesDir
                        filesDir.listFiles()?.forEach { file ->
                            if (file.isFile && !file.name.endsWith(".db") && !file.name.contains(dbName)) {
                                FileInputStream(file).use { fis ->
                                    zos.putNextEntry(ZipEntry("images/${file.name}"))
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

    suspend fun importDatabaseZip(inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbDir = context.getDatabasePath(dbName).parentFile ?: return@withContext false
            if (!dbDir.exists()) dbDir.mkdirs()
            val filesDir = context.filesDir

            context.contentResolver.openInputStream(inputUri)?.use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name.startsWith("images/") -> {
                                val imageName = entry.name.removePrefix("images/")
                                if (imageName.isNotEmpty()) {
                                    val outFile = File(filesDir, imageName)
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

    suspend fun exportDatabaseToFile(outputFile: File, includeImages: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val walFile = context.getDatabasePath("$dbName-wal")
            val shmFile = context.getDatabasePath("$dbName-shm")

            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 1. Export Database Files
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

                    // 2. Export Images if requested
                    if (includeImages) {
                        val filesDir = context.filesDir
                        filesDir.listFiles()?.forEach { file ->
                            if (file.isFile && !file.name.endsWith(".db") && !file.name.contains(dbName)) {
                                FileInputStream(file).use { fis ->
                                    zos.putNextEntry(ZipEntry("images/${file.name}"))
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

    fun getAutoBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "auto_backups")
        return if (backupDir.exists()) {
            backupDir.listFiles { file -> file.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun exportDatabaseZipToUri(folderUri: Uri, includeImages: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(folderUri)?.use { fos ->
                ZipOutputStream(fos).use { zos ->
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

                    // 2. Export Images if requested
                    if (includeImages) {
                        val filesDir = context.filesDir
                        filesDir.listFiles()?.forEach { file ->
                            if (file.isFile && !file.name.endsWith(".db") && !file.name.contains(dbName)) {
                                FileInputStream(file).use { fis ->
                                    zos.putNextEntry(ZipEntry("images/${file.name}"))
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
}

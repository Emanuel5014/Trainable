package com.emanuel5014.trainable.util.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.emanuel5014.trainable.data.backup.BackupArchiver
import com.emanuel5014.trainable.data.backup.BackupNames
import com.emanuel5014.trainable.data.backup.BackupScheduler
import com.emanuel5014.trainable.data.backup.LocalBackupFolder
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Android's backup ZIP: BackupManager's temp export and import, the restore download kept in the cache folder. */
@Singleton
class AndroidBackupArchiver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : BackupArchiver {
    override suspend fun createZip(fileName: String, includeImages: Boolean): String? =
        backupManager.exportDatabaseToTempFile(fileName, includeImages)?.path

    override suspend fun applyZip(path: String): Boolean = backupManager.importDatabaseFromFile(File(path))

    override fun discard(path: String) {
        File(path).takeIf { it.exists() }?.delete()
    }

    override fun tempPath(name: String): String = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip").path
}

/** The SAF folder chosen in Settings: export straight into it, list and delete `Trainable_AutoBackup_*.zip`. */
@Singleton
class SafBackupFolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val prefs: UserPreferencesRepository
) : LocalBackupFolder {

    private suspend fun folder(): Uri? = prefs.autoBackupFolderUri.first()?.takeIf { it.isNotEmpty() }?.let(Uri::parse)

    override suspend fun isAvailable(): Boolean = folder() != null

    override suspend fun exportTo(name: String, includeImages: Boolean): Boolean {
        val uri = folder() ?: return false
        return backupManager.exportDatabaseToFolder(uri, name, includeImages)
    }

    override suspend fun listAutoBackups(): List<Pair<String, Long>> = documents().map { it.name to it.lastModified }

    override suspend fun delete(name: String) {
        val uri = folder() ?: return
        documents().filter { it.name == name }.forEach {
            DocumentsContract.deleteDocument(context.contentResolver, DocumentsContract.buildDocumentUriUsingTree(uri, it.id))
        }
    }

    private data class Document(val id: String, val name: String, val lastModified: Long)

    /** The same query the old AutoBackupWorker.cleanupOldBackupsSaf made. */
    private suspend fun documents(): List<Document> {
        val folderUri = folder() ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val result = mutableListOf<Document>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                if (name.startsWith(BackupNames.AUTO_PREFIX) && name.endsWith(".zip")) {
                    result += Document(cursor.getString(idColumn), name, cursor.getLong(modifiedColumn))
                }
            }
        }
        return result
    }
}

/** WorkManager scheduling exactly as before (one periodic job from the preferences). */
@Singleton
class WorkManagerBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository
) : BackupScheduler {
    override suspend fun reschedule() = AutoBackupWorker.scheduleFromPreferences(context, prefs)
}

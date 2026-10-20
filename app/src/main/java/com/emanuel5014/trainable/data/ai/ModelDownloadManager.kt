package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelFileManager: ModelFileManager,
    private val userPrefsRepository: UserPreferencesRepository
) {
    private val _activeDownloads = MutableStateFlow<Map<String, AiModelStatus>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, AiModelStatus>> = _activeDownloads.asStateFlow()

    /** Imports outlive the screen that started them, so they run here rather than in a view model. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val importLock = Any()
    private val importJobs = HashMap<String, Job>()

    private val _importError = MutableStateFlow<ModelImportError?>(null)

    /** Why the last import was refused or failed; cleared when a new one starts or by [clearImportError]. */
    val importError: StateFlow<ModelImportError?> = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    fun updateStatus(variant: AiModelVariant, status: AiModelStatus) {
        val current = _activeDownloads.value.toMutableMap()
        when (status) {
            is AiModelStatus.Downloading -> {
                current[variant.id] = status
            }
            else -> {
                current.remove(variant.id)
            }
        }
        _activeDownloads.value = current
    }

    fun startDownload(variant: AiModelVariant) {
        val current = _activeDownloads.value.toMutableMap()
        current[variant.id] = AiModelStatus.Downloading(0f)
        _activeDownloads.value = current

        val intent = Intent(context, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_START_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_VARIANT_ID, variant.id)
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            current.remove(variant.id)
            _activeDownloads.value = current
        }
    }

    fun cancelDownload(variant: AiModelVariant) {
        val importJob = synchronized(importLock) { importJobs.remove(variant.id) }
        if (importJob != null) {
            importJob.cancel()
            updateStatus(variant, AiModelStatus.NotDownloaded)
            return
        }

        val intent = Intent(context, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_CANCEL_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_VARIANT_ID, variant.id)
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
        }

        val current = _activeDownloads.value.toMutableMap()
        current.remove(variant.id)
        _activeDownloads.value = current
    }

    /**
     * Imports a model the user already has (picked with the system file picker): checks that it is one of the
     * supported models, selects that variant and copies it in, showing up in [activeDownloads] like a download.
     * Anything that stops it ends up in [importError].
     */
    fun importModel(uri: Uri) {
        _importError.value = null
        scope.launch {
            val source = try {
                readSource(uri)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _importError.value = ModelImportError.CannotRead
                return@launch
            }
            when (val check = ModelFileValidator.check(source.header, source.sizeBytes)) {
                ModelImportCheck.NotLiteRtLm -> _importError.value = ModelImportError.NotLiteRtLm
                ModelImportCheck.UnsupportedModel -> _importError.value = ModelImportError.UnsupportedModel
                is ModelImportCheck.Supported -> copyIn(uri, check.variant, source.sizeBytes)
            }
        }
    }

    private class ImportSource(val header: ByteArray, val sizeBytes: Long)

    private fun readSource(uri: Uri): ImportSource {
        val resolver = context.contentResolver
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        } ?: resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.takeIf { it >= 0 }
            ?: throw IOException("Unknown size")
        val header = resolver.openInputStream(uri)?.use { ModelFileValidator.readHeader(it) }
            ?: throw IOException("Cannot open")
        return ImportSource(header, size)
    }

    private suspend fun copyIn(uri: Uri, variant: AiModelVariant, sizeBytes: Long) {
        // The model is already here: just switch to it. A damaged copy is replaced by deleting it first.
        if (modelFileManager.isDownloaded(variant)) {
            userPrefsRepository.setAiModelVariant(variant.id)
            return
        }

        if (!modelFileManager.hasRoomFor(sizeBytes)) {
            _importError.value = ModelImportError.NotEnoughSpace(modelFileManager.spaceNeededFor(sizeBytes))
            return
        }

        // Claimed together with the job, so that a cancel never finds the import half registered
        val job = currentCoroutineContext()[Job]
        val claimed = synchronized(importLock) {
            if (variant.id in _activeDownloads.value) {
                false
            } else {
                if (job != null) importJobs[variant.id] = job
                updateStatus(variant, AiModelStatus.Downloading(0f, importing = true))
                true
            }
        }
        if (!claimed) {
            _importError.value = ModelImportError.Busy
            return
        }

        val part = modelFileManager.getPartFile(variant)
        try {
            userPrefsRepository.setAiModelVariant(variant.id)
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open")
            input.use {
                copyModel(it, part, sizeBytes) { progress ->
                    updateStatus(variant, AiModelStatus.Downloading(progress, importing = true))
                }
            }
            val finalFile = modelFileManager.getModelFile(variant)
            if (finalFile.exists()) finalFile.delete()
            if (!part.renameTo(finalFile)) throw IOException("Could not finalize model file")
            updateStatus(variant, AiModelStatus.Ready)
            modelFileManager.notifyFilesChanged()
        } catch (e: CancellationException) {
            part.delete()
            updateStatus(variant, AiModelStatus.NotDownloaded)
            modelFileManager.notifyFilesChanged()
            throw e
        } catch (e: Exception) {
            // Looked at before the partial file goes, since that frees what the copy was using
            val outOfSpace = e !is IncompleteModelCopy && !modelFileManager.hasRoomFor(0L)
            part.delete()
            updateStatus(variant, AiModelStatus.NotDownloaded)
            modelFileManager.notifyFilesChanged()
            _importError.value = if (outOfSpace) {
                ModelImportError.NotEnoughSpace(modelFileManager.spaceNeededFor(sizeBytes))
            } else {
                ModelImportError.CannotRead
            }
        } finally {
            synchronized(importLock) {
                if (importJobs[variant.id] === job) importJobs.remove(variant.id)
            }
        }
    }
}

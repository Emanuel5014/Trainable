package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activeDownloads = MutableStateFlow<Map<String, AiModelStatus>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, AiModelStatus>> = _activeDownloads.asStateFlow()

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
}

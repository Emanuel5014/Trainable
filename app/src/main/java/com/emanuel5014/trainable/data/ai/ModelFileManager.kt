package com.emanuel5014.trainable.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelsDir: File
        get() = File(context.filesDir, "ai_models").apply { mkdirs() }

    private val _filesUpdatedTrigger = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val filesUpdatedTrigger: kotlinx.coroutines.flow.StateFlow<Long> = _filesUpdatedTrigger

    fun notifyFilesChanged() {
        _filesUpdatedTrigger.value = System.currentTimeMillis()
    }

    fun getModelFile(variant: AiModelVariant): File = File(modelsDir, variant.fileName)

    fun isDownloaded(variant: AiModelVariant): Boolean {
        val file = getModelFile(variant)
        return file.exists() && file.length() > 0
    }

    fun delete(variant: AiModelVariant): Boolean {
        val file = getModelFile(variant)
        val deleted = if (file.exists()) file.delete() else true
        val partFile = File(modelsDir, variant.fileName + ".part")
        if (partFile.exists()) partFile.delete()
        notifyFilesChanged()
        return deleted
    }

    suspend fun download(variant: AiModelVariant): Flow<AiModelStatus> = flow {
        val target = File(modelsDir, variant.fileName + ".part")
        try {
            if (isDownloaded(variant)) {
                emit(AiModelStatus.Ready)
                return@flow
            }

            val request = Request.Builder()
                .url(variant.downloadUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(AiModelStatus.Error("HTTP ${response.code}"))
                    return@flow
                }
                val body = response.body ?: run {
                    emit(AiModelStatus.Error("Empty response"))
                    return@flow
                }
                val contentLength = body.contentLength()
                var bytesWritten = 0L

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(1 shl 16)
                        while (true) {
                            if (target.parentFile?.let { it.usableSpace } ?: Long.MAX_VALUE < MIN_FREE_SPACE) {
                                throw IllegalStateException("Insufficient storage")
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            if (contentLength > 0) {
                                emit(AiModelStatus.Downloading((bytesWritten.toFloat() / contentLength)))
                            }
                        }
                    }
                }
            }

            val finalFile = getModelFile(variant)
            if (finalFile.exists()) finalFile.delete()
            if (!target.renameTo(finalFile)) {
                throw IllegalStateException("Could not finalize model file")
            }
            emit(AiModelStatus.Ready)
        } catch (e: Exception) {
            target.delete()
            emit(AiModelStatus.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val MIN_FREE_SPACE = 512L * 1024 * 1024
    }
}

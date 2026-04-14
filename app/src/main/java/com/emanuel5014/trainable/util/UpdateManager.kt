package com.emanuel5014.trainable.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.emanuel5014.trainable.BuildConfig
import com.emanuel5014.trainable.data.remote.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    // Replace with actual repository info
    private val GITHUB_OWNER = "Emanuel5014"
    private val GITHUB_REPO = "Trainable"
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val body = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString<GitHubRelease>(body)
                
                // Compare versions
                // GitHub tags usually have "v1.0.0" or "1.0.0"
                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = BuildConfig.VERSION_NAME
                
                if (isNewerVersion(currentVersion, latestVersion)) {
                    release
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val curr = currentParts.getOrElse(i) { 0 }
            val late = latestParts.getOrElse(i) { 0 }
            if (late > curr) return true
            if (late < curr) return false
        }
        return false
    }

    suspend fun downloadAndInstall(release: GitHubRelease, onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        val asset = release.assets.find { it.name.endsWith(".apk") } 
            ?: return@withContext Result.failure(Exception("No APK found in release assets"))

        try {
            val request = Request.Builder().url(asset.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Failed to download file")
                
                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()
                val apkFile = File(context.cacheDir, "app_update.apk")
                
                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                withContext(Dispatchers.Main) {
                                    onProgress(totalRead.toFloat() / totalBytes)
                                }
                            }
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }
}

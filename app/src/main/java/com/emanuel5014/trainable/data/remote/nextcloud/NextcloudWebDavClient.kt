package com.emanuel5014.trainable.data.remote.nextcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextcloudWebDavClient @Inject constructor(
    baseOkHttpClient: OkHttpClient
) {
    private val client: OkHttpClient = baseOkHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val xmlMediaType = "application/xml; charset=utf-8".toMediaType()
    private val zipMediaType = "application/zip".toMediaType()

    private fun encodeSegment(s: String): String {
        return try {
            URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        } catch (e: Exception) {
            s
        }
    }

    private fun decodeString(s: String): String {
        return try {
            URLDecoder.decode(s, "UTF-8")
        } catch (e: Exception) {
            s
        }
    }

    /**
     * Builds the base WebDAV URL for the given server and username.
     * Output format: https://domain/remote.php/dav/files/username
     */
    fun buildUserDavBaseUrl(serverUrl: String, username: String): String {
        var cleanUrl = serverUrl.trim().trimEnd('/')
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return if (cleanUrl.contains("/remote.php/dav/files/")) {
            cleanUrl
        } else if (cleanUrl.contains("/remote.php/webdav")) {
            cleanUrl
        } else {
            "$cleanUrl/remote.php/dav/files/${encodeSegment(username)}"
        }
    }

    /**
     * Builds the full remote folder WebDAV URL.
     */
    fun buildRemoteFolderUrl(config: NextcloudConfig): String {
        val base = buildUserDavBaseUrl(config.serverUrl, config.username).trimEnd('/')
        val folder = config.remoteFolder.trim().trim('/')
        return if (folder.isEmpty()) {
            base
        } else {
            val encodedSegments = folder.split('/').filter { it.isNotEmpty() }.joinToString("/") { encodeSegment(it) }
            "$base/$encodedSegments"
        }
    }

    private fun getAuthHeader(config: NextcloudConfig): String {
        val credentials = "${config.username}:${config.passwordOrToken}"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        return "Basic $encoded"
    }

    suspend fun testConnection(config: NextcloudConfig): NextcloudConnectionResult = withContext(Dispatchers.IO) {
        try {
            val userBaseUrl = buildUserDavBaseUrl(config.serverUrl, config.username) + "/"
            val propfindXml = """
                <?xml version="1.0" encoding="utf-8" ?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:resourcetype/>
                  </d:prop>
                </d:propfind>
            """.trimIndent()

            val request = Request.Builder()
                .url(userBaseUrl)
                .method("PROPFIND", propfindXml.toRequestBody(xmlMediaType))
                .header("Authorization", getAuthHeader(config))
                .header("Depth", "0")
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 207 -> {
                        NextcloudConnectionResult.Success
                    }
                    response.code == 401 || response.code == 403 -> {
                        NextcloudConnectionResult.Error("Authentication failed: check username or password/token", response.code)
                    }
                    response.code == 404 -> {
                        NextcloudConnectionResult.Error("Nextcloud endpoint not found. Verify server URL", response.code)
                    }
                    else -> {
                        NextcloudConnectionResult.Error("Server returned error HTTP ${response.code}: ${response.message}", response.code)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NextcloudConnectionResult.Error(e.localizedMessage ?: "Connection error")
        }
    }

    /**
     * Ensures all parent directories and the target remote folder exist by issuing MKCOL where needed.
     */
    suspend fun ensureDirectoryExists(config: NextcloudConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val base = buildUserDavBaseUrl(config.serverUrl, config.username).trimEnd('/')
            val folder = config.remoteFolder.trim().trim('/')
            if (folder.isEmpty()) return@withContext Result.success(Unit)

            val segments = folder.split('/').filter { it.isNotEmpty() }
            var currentPath = base

            for (segment in segments) {
                currentPath = "$currentPath/${encodeSegment(segment)}"
                val folderUrlWithSlash = "$currentPath/"

                // Check if directory exists with PROPFIND Depth: 0
                val checkRequest = Request.Builder()
                    .url(folderUrlWithSlash)
                    .method("PROPFIND", ByteArray(0).toRequestBody(null))
                    .header("Authorization", getAuthHeader(config))
                    .header("Depth", "0")
                    .build()

                val exists = client.newCall(checkRequest).execute().use { response ->
                    response.isSuccessful || response.code == 207
                }

                if (!exists) {
                    // Create directory with MKCOL
                    val mkcolRequest = Request.Builder()
                        .url(folderUrlWithSlash)
                        .method("MKCOL", ByteArray(0).toRequestBody(null))
                        .header("Authorization", getAuthHeader(config))
                        .build()

                    client.newCall(mkcolRequest).execute().use { response ->
                        if (!response.isSuccessful && response.code != 405) { // 405 means already exists
                            return@withContext Result.failure(
                                Exception("Failed to create folder '$segment': HTTP ${response.code}")
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        config: NextcloudConfig,
        file: File,
        remoteFileName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ensureResult = ensureDirectoryExists(config)
            if (ensureResult.isFailure) return@withContext ensureResult

            val folderUrl = buildRemoteFolderUrl(config).trimEnd('/')
            val uploadUrl = "$folderUrl/${encodeSegment(remoteFileName)}"

            val request = Request.Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(zipMediaType))
                .header("Authorization", getAuthHeader(config))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..204) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Upload failed with HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun listBackups(config: NextcloudConfig): Result<List<NextcloudBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val folderUrl = buildRemoteFolderUrl(config).trimEnd('/') + "/"
            val propfindXml = """
                <?xml version="1.0" encoding="utf-8" ?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:displayname/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:resourcetype/>
                  </d:prop>
                </d:propfind>
            """.trimIndent()

            val request = Request.Builder()
                .url(folderUrl)
                .method("PROPFIND", propfindXml.toRequestBody(xmlMediaType))
                .header("Authorization", getAuthHeader(config))
                .header("Depth", "1")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 207) {
                    if (response.code == 404) {
                        return@withContext Result.success(emptyList())
                    }
                    return@withContext Result.failure(Exception("Failed to list backups: HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val backups = parseWebDavListXml(body)
                Result.success(backups.sortedByDescending { it.lastModified.time })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        config: NextcloudConfig,
        remoteFileName: String,
        destinationFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderUrl = buildRemoteFolderUrl(config).trimEnd('/')
            val downloadUrl = "$folderUrl/${encodeSegment(remoteFileName)}"

            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .header("Authorization", getAuthHeader(config))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
                }

                val inputStream = response.body?.byteStream()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                FileOutputStream(destinationFile).use { output ->
                    inputStream.copyTo(output)
                }

                Result.success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteFile(config: NextcloudConfig, remoteFileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folderUrl = buildRemoteFolderUrl(config).trimEnd('/')
            val deleteUrl = "$folderUrl/${encodeSegment(remoteFileName)}"

            val request = Request.Builder()
                .url(deleteUrl)
                .delete()
                .header("Authorization", getAuthHeader(config))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..204 || response.code == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Delete failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Parses the 207 Multi-Status XML response from PROPFIND.
     */
    fun parseWebDavListXml(xml: String): List<NextcloudBackupFile> {
        val result = mutableListOf<NextcloudBackupFile>()
        try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xml.byteInputStream(Charsets.UTF_8))
            val responses = doc.getElementsByTagNameNS("*", "response")

            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }

            for (i in 0 until responses.length) {
                val response = responses.item(i) as? org.w3c.dom.Element ?: continue
                val href = response.getElementsByTagNameNS("*", "href").item(0)?.textContent?.trim() ?: continue
                val collectionNodes = response.getElementsByTagNameNS("*", "collection")
                val isCollection = collectionNodes.length > 0
                if (isCollection) continue

                val decodedHref = decodeString(href)
                val fileName = decodedHref.trimEnd('/').substringAfterLast('/')

                if (fileName.endsWith(".zip", ignoreCase = true) && fileName.startsWith("Trainable_")) {
                    val lengthStr = response.getElementsByTagNameNS("*", "getcontentlength").item(0)?.textContent?.trim()
                    val sizeBytes = lengthStr?.toLongOrNull() ?: 0L
                    val dateStr = response.getElementsByTagNameNS("*", "getlastmodified").item(0)?.textContent?.trim()
                    val lastModified = try {
                        if (dateStr != null) dateFormat.parse(dateStr) else null
                    } catch (e: Exception) {
                        null
                    } ?: Date()

                    result.add(
                        NextcloudBackupFile(
                            name = fileName,
                            remotePath = href,
                            sizeBytes = sizeBytes,
                            lastModified = lastModified
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}

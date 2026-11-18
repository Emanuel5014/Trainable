package com.emanuel5014.trainable.data.remote.nextcloud

import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64

/**
 * The Nextcloud WebDAV calls Android always made (formerly NextcloudWebDavClient): same requests, same bodies,
 * same results and error texts, over a platform [WebDavHttp].
 */
class NextcloudWebDav(private val http: WebDavHttp) {

    suspend fun testConnection(config: NextcloudConfig): NextcloudConnectionResult = try {
        val response = http.send(propfind(NextcloudUrls.userDavBase(config.serverUrl, config.username) + "/", config, "0", TEST_BODY))
        when {
            response.isSuccessful || response.code == 207 -> NextcloudConnectionResult.Success
            response.code == 401 || response.code == 403 ->
                NextcloudConnectionResult.Error("Authentication failed: check username or password/token", response.code)
            response.code == 404 -> NextcloudConnectionResult.Error("Nextcloud endpoint not found. Verify server URL", response.code)
            else -> NextcloudConnectionResult.Error("Server returned error HTTP ${response.code}: ${response.message}", response.code)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NextcloudConnectionResult.Error(e.message ?: "Connection error")
    }

    /** PROPFIND each folder segment, MKCOL the missing ones (405 = already there). */
    suspend fun ensureFolder(config: NextcloudConfig): Result<Unit> = attempt {
        val folder = config.remoteFolder.trim().trim('/')
        if (folder.isEmpty()) return@attempt Result.success(Unit)
        var current = NextcloudUrls.userDavBase(config.serverUrl, config.username).trimEnd('/')
        for (segment in folder.split('/').filter { it.isNotEmpty() }) {
            current = "$current/${NextcloudUrls.encodeSegment(segment)}"
            val exists = http.send(WebDavRequest("PROPFIND", "$current/", auth(config) + ("Depth" to "0"))).let {
                it.isSuccessful || it.code == 207
            }
            if (!exists) {
                val created = http.send(WebDavRequest("MKCOL", "$current/", auth(config)))
                if (!created.isSuccessful && created.code != 405)
                    return@attempt Result.failure(Exception("Failed to create folder '$segment': HTTP ${created.code}"))
            }
        }
        Result.success(Unit)
    }

    suspend fun upload(config: NextcloudConfig, localPath: String, name: String): Result<Unit> = attempt {
        ensureFolder(config).onFailure { return@attempt Result.failure(it) }
        val response = http.send(uploadRequest(config, localPath, name))
        if (response.isSuccessful || response.code in 200..204) Result.success(Unit)
        else Result.failure(Exception("Upload failed with HTTP ${response.code}: ${response.message}"))
    }

    /** The PUT of [upload], for a caller that sends it another way (iOS background session). */
    fun uploadRequest(config: NextcloudConfig, localPath: String, name: String) =
        WebDavRequest("PUT", NextcloudUrls.file(config, name), auth(config), WebDavBody.FilePath(localPath, "application/zip"))

    /** The backups in the folder, newest first; a missing folder is an empty list. */
    suspend fun list(config: NextcloudConfig): Result<List<NextcloudBackupFile>> = attempt {
        val response = http.send(propfind(NextcloudUrls.remoteFolder(config).trimEnd('/') + "/", config, "1", LIST_BODY))
        if (!response.isSuccessful && response.code != 207) {
            return@attempt if (response.code == 404) Result.success(emptyList())
            else Result.failure(Exception("Failed to list backups: HTTP ${response.code}"))
        }
        val body = response.body ?: return@attempt Result.success(emptyList())
        Result.success(PropfindParser.parse(body).sortedByDescending { it.lastModifiedMillis })
    }

    suspend fun download(config: NextcloudConfig, name: String, toPath: String): Result<Unit> = attempt {
        val response = http.send(WebDavRequest("GET", NextcloudUrls.file(config, name), auth(config), downloadTo = toPath))
        if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Download failed: HTTP ${response.code}"))
    }

    suspend fun delete(config: NextcloudConfig, name: String): Result<Unit> = attempt {
        val response = http.send(WebDavRequest("DELETE", NextcloudUrls.file(config, name), auth(config)))
        if (response.isSuccessful || response.code in 200..204 || response.code == 404) Result.success(Unit)
        else Result.failure(Exception("Delete failed: HTTP ${response.code}"))
    }

    private inline fun <T> attempt(block: () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun auth(config: NextcloudConfig) =
        listOf("Authorization" to "Basic " + Base64.encode("${config.username}:${config.passwordOrToken}".encodeToByteArray()))

    private fun propfind(url: String, config: NextcloudConfig, depth: String, body: String) =
        WebDavRequest("PROPFIND", url, auth(config) + ("Depth" to depth), WebDavBody.Text(body, XML))

    private companion object {
        const val XML = "application/xml; charset=utf-8"
        const val TEST_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<d:propfind xmlns:d=\"DAV:\">\n  <d:prop>\n" +
            "    <d:resourcetype/>\n  </d:prop>\n</d:propfind>"
        const val LIST_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<d:propfind xmlns:d=\"DAV:\">\n  <d:prop>\n" +
            "    <d:displayname/>\n    <d:getcontentlength/>\n    <d:getlastmodified/>\n    <d:resourcetype/>\n  </d:prop>\n</d:propfind>"
    }
}

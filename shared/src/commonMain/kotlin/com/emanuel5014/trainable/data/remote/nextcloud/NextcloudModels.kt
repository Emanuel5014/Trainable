package com.emanuel5014.trainable.data.remote.nextcloud

data class NextcloudConfig(
    val serverUrl: String,
    val username: String,
    val passwordOrToken: String,
    val remoteFolder: String = "Trainable/Backups"
)

data class NextcloudBackupFile(
    val name: String,
    val remotePath: String,
    val sizeBytes: Long,
    /** Last change on the server, epoch milliseconds (WebDAV getlastmodified). */
    val lastModifiedMillis: Long
)

sealed interface NextcloudConnectionResult {
    data object Success : NextcloudConnectionResult
    data class Error(val message: String, val statusCode: Int? = null) : NextcloudConnectionResult
}

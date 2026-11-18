package com.emanuel5014.trainable.data.remote.nextcloud

/**
 * Reads the 207 Multi-Status answer of a PROPFIND: only files (not folders) named `Trainable_*.zip`
 * (`.zip` in any case), the name decoded from the last path segment, size 0 when missing, the date as
 * `EEE, dd MMM yyyy HH:mm:ss zzz` in GMT or "now" when unreadable. A document that cannot be read gives an
 * empty list. Android keeps its DOM parser, iOS uses NSXMLParser; both give the same files.
 */
expect object PropfindParser {
    fun parse(xml: String): List<NextcloudBackupFile>
}

internal fun isTrainableBackup(fileName: String) = fileName.endsWith(".zip", ignoreCase = true) && fileName.startsWith("Trainable_")

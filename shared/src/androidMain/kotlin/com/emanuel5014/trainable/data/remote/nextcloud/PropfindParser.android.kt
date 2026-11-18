package com.emanuel5014.trainable.data.remote.nextcloud

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The parser Android always used (formerly NextcloudWebDavClient.parseWebDavListXml), unchanged. */
actual object PropfindParser {
    actual fun parse(xml: String): List<NextcloudBackupFile> {
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

                val decodedHref = NextcloudUrls.decode(href)
                val fileName = decodedHref.trimEnd('/').substringAfterLast('/')

                if (isTrainableBackup(fileName)) {
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
                            lastModifiedMillis = lastModified.time
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

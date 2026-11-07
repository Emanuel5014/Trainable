@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.remote.nextcloud

import com.emanuel5014.trainable.data.repository.toNSData
import kotlinx.cinterop.convert
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT
import platform.darwin.NSObject
import kotlin.time.Clock

/** Same reading as Android's DOM parser, on NSXMLParser with namespaces resolved (any prefix of DAV:). */
actual object PropfindParser {
    actual fun parse(xml: String): List<NextcloudBackupFile> {
        if (xml.isEmpty()) return emptyList()
        val data = xml.encodeToByteArray().toNSData()
        val reader = Reader()
        val parser = NSXMLParser(data)
        parser.shouldProcessNamespaces = true
        parser.delegate = reader
        return if (parser.parse()) reader.files else emptyList()
    }

    private val dates = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0.convert())
        dateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"
    }

    private fun millis(text: String?): Long =
        text?.let { dates.dateFromString(it) }?.let { (it.timeIntervalSince1970 * 1000).toLong() }
            ?: Clock.System.now().toEpochMilliseconds()

    private class Reader : NSObject(), NSXMLParserDelegateProtocol {
        val files = mutableListOf<NextcloudBackupFile>()
        private var inResponse = false
        private var collection = false
        private val values = mutableMapOf<String, String>()
        private var text = StringBuilder()

        override fun parser(parser: NSXMLParser, didStartElement: String, namespaceURI: String?,
                            qualifiedName: String?, attributes: Map<Any?, *>) {
            text = StringBuilder()
            when (didStartElement) {
                "response" -> { inResponse = true; collection = false; values.clear() }
                "collection" -> if (inResponse) collection = true
            }
        }

        override fun parser(parser: NSXMLParser, foundCharacters: String) { text.append(foundCharacters) }

        override fun parser(parser: NSXMLParser, didEndElement: String, namespaceURI: String?, qualifiedName: String?) {
            if (!inResponse) return
            when (didEndElement) {
                // The first one wins, like getElementsByTagNameNS(...).item(0).
                "href", "getcontentlength", "getlastmodified" -> values.getOrPut(didEndElement) { text.toString().trim() }
                "response" -> {
                    inResponse = false
                    val href = values["href"] ?: return
                    if (collection) return
                    val name = NextcloudUrls.decode(href).trimEnd('/').substringAfterLast('/')
                    if (isTrainableBackup(name)) files += NextcloudBackupFile(name, href,
                        values["getcontentlength"]?.toLongOrNull() ?: 0L, millis(values["getlastmodified"]))
                }
            }
        }
    }
}

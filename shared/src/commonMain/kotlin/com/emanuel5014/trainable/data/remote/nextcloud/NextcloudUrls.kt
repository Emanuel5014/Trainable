package com.emanuel5014.trainable.data.remote.nextcloud

/**
 * WebDAV addresses of a Nextcloud account, built exactly as Android always did (java.net.URLEncoder rules:
 * letters, digits and `.-*_` kept, space as `%20`, everything else UTF-8 percent-encoded).
 */
object NextcloudUrls {

    /** `https://domain/remote.php/dav/files/<user>`, or the URL itself when it already names a WebDAV root. */
    fun userDavBase(serverUrl: String, username: String): String {
        var clean = serverUrl.trim().trimEnd('/')
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) clean = "https://$clean"
        return if (clean.contains("/remote.php/dav/files/") || clean.contains("/remote.php/webdav")) clean
        else "$clean/remote.php/dav/files/${encodeSegment(username)}"
    }

    /** The backup folder: every path segment encoded on its own. */
    fun remoteFolder(config: NextcloudConfig): String {
        val base = userDavBase(config.serverUrl, config.username).trimEnd('/')
        val folder = config.remoteFolder.trim().trim('/')
        if (folder.isEmpty()) return base
        return base + "/" + folder.split('/').filter { it.isNotEmpty() }.joinToString("/") { encodeSegment(it) }
    }

    fun file(config: NextcloudConfig, name: String): String = remoteFolder(config).trimEnd('/') + "/" + encodeSegment(name)

    fun encodeSegment(s: String): String = buildString {
        for (byte in s.encodeToByteArray()) {
            val c = (byte.toInt() and 0xFF)
            val ch = c.toChar()
            when {
                ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch in ".-*_" -> append(ch)
                ch == ' ' -> append("%20")
                else -> append('%').append(HEX[c shr 4]).append(HEX[c and 0x0F])
            }
        }
    }

    /** java.net.URLDecoder: `+` is a space, `%XX` a byte; a malformed text comes back unchanged. */
    fun decode(s: String): String {
        val out = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch == '+' -> { out += ' '.code.toByte(); i++ }
                ch == '%' -> {
                    val hi = s.getOrNull(i + 1)?.digitToIntOrNull(16) ?: return s
                    val lo = s.getOrNull(i + 2)?.digitToIntOrNull(16) ?: return s
                    out += ((hi shl 4) + lo).toByte(); i += 3
                }
                else -> { ch.toString().encodeToByteArray().forEach { out += it }; i++ }
            }
        }
        return out.toByteArray().decodeToString()
    }

    private const val HEX = "0123456789ABCDEF"
}

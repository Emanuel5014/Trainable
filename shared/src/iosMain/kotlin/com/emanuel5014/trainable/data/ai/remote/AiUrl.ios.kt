package com.emanuel5014.trainable.data.ai.remote

import platform.Foundation.NSURLComponents

/**
 * Like OkHttp's parser on Android for the addresses people type: only http/https with a host; the host lower-cased
 * and without the brackets of an IPv6 literal. A text with spaces or other characters a URL can't hold is refused.
 */
internal actual fun parseHttpUrl(text: String): AiUrl? {
    val components = NSURLComponents.componentsWithString(text) ?: return null
    val scheme = components.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = components.host?.removePrefix("[")?.removeSuffix("]")?.lowercase()
    if (host.isNullOrEmpty()) return null
    return AiUrl(scheme, host, components.user.orEmpty(), components.password.orEmpty())
}

/** Same rules as Java's InetAddress checks on Android: loopback, fe80::/10, fc00::/7, IPv4-mapped by [isPrivateV4]. */
internal actual fun isPrivateIpv6(text: String, isPrivateV4: (List<Int>) -> Boolean): Boolean {
    // A zone id (fe80::1%en0) is accepted on Android and says nothing about the address itself
    val bytes = ipv6Bytes(text.substringBefore('%')) ?: return false
    val mapped = bytes.subList(0, 10).all { it == 0 } && bytes[10] == 0xff && bytes[11] == 0xff
    if (mapped) return isPrivateV4(bytes.subList(12, 16))
    val loopback = bytes.subList(0, 15).all { it == 0 } && bytes[15] == 1
    val linkLocal = bytes[0] == 0xfe && (bytes[1] and 0xc0) == 0x80
    return loopback || linkLocal || (bytes[0] and 0xfe) == 0xfc
}

/** The 16 bytes of an IPv6 literal (RFC 4291 text forms: "::" once, an IPv4 dotted tail), or null. */
internal fun ipv6Bytes(text: String): List<Int>? {
    if (text.isEmpty() || text.count { it == ':' } < 2) return null
    val halves = text.split("::")
    if (halves.size > 2) return null
    fun groups(part: String): List<Int>? {
        if (part.isEmpty()) return emptyList()
        val out = mutableListOf<Int>()
        val pieces = part.split(':')
        pieces.forEachIndexed { index, piece ->
            if (index == pieces.lastIndex && '.' in piece) {
                val octets = piece.split('.')
                if (octets.size != 4) return null
                val values = octets.map { o -> o.takeIf { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) }?.toInt() ?: return null }
                if (values.any { it > 255 }) return null
                out += (values[0] shl 8) or values[1]
                out += (values[2] shl 8) or values[3]
            } else {
                if (piece.isEmpty() || piece.length > 4) return null
                out += piece.toIntOrNull(16) ?: return null
            }
        }
        return out
    }
    val head = groups(halves[0]) ?: return null
    val tail = if (halves.size == 2) groups(halves[1]) ?: return null else emptyList()
    val words = if (halves.size == 2) {
        val missing = 8 - head.size - tail.size
        if (missing < 1) return null
        head + List(missing) { 0 } + tail
    } else {
        head.takeIf { it.size == 8 } ?: return null
    }
    return words.flatMap { listOf(it shr 8, it and 0xff) }
}

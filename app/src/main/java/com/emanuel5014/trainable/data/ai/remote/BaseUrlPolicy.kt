package com.emanuel5014.trainable.data.ai.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet4Address
import java.net.InetAddress

enum class HostKind { PRIVATE, PUBLIC }

enum class UrlRejection { INVALID, UNSUPPORTED_SCHEME, CLEARTEXT_TO_PUBLIC_HOST }

sealed interface UrlCheck {
    data class Ok(val url: HttpUrl, val kind: HostKind) : UrlCheck
    data class Rejected(val reason: UrlRejection) : UrlCheck
}

/**
 * Which base URLs the remote AI may talk to. `https` is always fine; plain `http` only to a host on
 * the user's own network, so an API key never travels unencrypted over the internet.
 */
object BaseUrlPolicy {

    private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://")
    private val IPV4 = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
    private val PRIVATE_SUFFIXES = listOf(".local", ".lan", ".home.arpa")

    fun normalize(raw: String): String = raw.trim().trimEnd('/')

    fun check(raw: String): UrlCheck {
        val text = normalize(raw)
        val scheme = SCHEME.find(text)?.groupValues?.get(1)?.lowercase()
            ?: return UrlCheck.Rejected(UrlRejection.INVALID)
        if (scheme != "http" && scheme != "https") return UrlCheck.Rejected(UrlRejection.UNSUPPORTED_SCHEME)

        val url = text.toHttpUrlOrNull() ?: return UrlCheck.Rejected(UrlRejection.INVALID)
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return UrlCheck.Rejected(UrlRejection.INVALID)

        val kind = hostKind(url.host)
        if (scheme == "http" && kind == HostKind.PUBLIC) {
            return UrlCheck.Rejected(UrlRejection.CLEARTEXT_TO_PUBLIC_HOST)
        }
        return UrlCheck.Ok(url, kind)
    }

    fun hostKind(host: String): HostKind {
        val name = host.trim().removePrefix("[").removeSuffix("]").lowercase()
        ipv4Octets(name)?.let { return if (isPrivateV4(it)) HostKind.PRIVATE else HostKind.PUBLIC }
        if (':' in name) return if (isPrivateV6(name)) HostKind.PRIVATE else HostKind.PUBLIC
        val local = name == "localhost" || '.' !in name || PRIVATE_SUFFIXES.any { name.endsWith(it) }
        return if (local) HostKind.PRIVATE else HostKind.PUBLIC
    }

    private fun ipv4Octets(text: String): List<Int>? {
        val octets = IPV4.matchEntire(text)?.groupValues?.drop(1)?.map { it.toInt() } ?: return null
        return octets.takeIf { list -> list.all { it in 0..255 } }
    }

    private fun isPrivateV4(o: List<Int>): Boolean = when {
        o[0] == 127 || o[0] == 10 -> true
        o[0] == 172 -> o[1] in 16..31
        o[0] == 192 -> o[1] == 168
        o[0] == 169 -> o[1] == 254
        o[0] == 100 -> o[1] in 64..127
        else -> false
    }

    /** [text] holds a ':' so it is an IPv6 literal: parsing it does no DNS lookup. */
    private fun isPrivateV6(text: String): Boolean {
        val address = try {
            InetAddress.getByName(text)
        } catch (_: Exception) {
            return false
        }
        if (address is Inet4Address) return isPrivateV4(address.address.map { it.toInt() and 0xff })
        return address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            (address.address[0].toInt() and 0xfe) == 0xfc
    }
}

package com.emanuel5014.trainable.data.ai.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet4Address
import java.net.InetAddress

// The exact code BaseUrlPolicy used before it moved to the shared module

internal actual fun parseHttpUrl(text: String): AiUrl? =
    text.toHttpUrlOrNull()?.let { AiUrl(it.scheme, it.host, it.username, it.password) }

internal actual fun isPrivateIpv6(text: String, isPrivateV4: (List<Int>) -> Boolean): Boolean {
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

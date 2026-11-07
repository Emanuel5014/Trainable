package com.emanuel5014.trainable.data.ai.remote

/** The parts of an http(s) address the remote AI rules look at; [host] has no brackets around an IPv6 literal. */
data class AiUrl(val scheme: String, val host: String, val username: String, val password: String) {
    companion object {
        /** Null unless [text] is a well-formed http or https address with a host (Android: OkHttp's own parser). */
        fun parse(text: String): AiUrl? = parseHttpUrl(text)
    }
}

internal expect fun parseHttpUrl(text: String): AiUrl?

/**
 * Whether [text], an IPv6 literal (it holds a ':'), is loopback, link-local or unique-local; an IPv4-mapped address
 * is judged by [isPrivateV4]. Parsing a literal does no DNS lookup. False when it can't be parsed.
 */
internal expect fun isPrivateIpv6(text: String, isPrivateV4: (List<Int>) -> Boolean): Boolean

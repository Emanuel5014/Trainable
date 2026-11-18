package com.emanuel5014.trainable.data.remote.nextcloud

/** What a WebDAV request carries. [Empty] on PROPFIND/MKCOL is a zero-length body without a content type. */
sealed interface WebDavBody {
    data class Text(val text: String, val contentType: String) : WebDavBody
    data class FilePath(val path: String, val contentType: String) : WebDavBody
    data object Empty : WebDavBody
}

/** One request; when [downloadTo] is set the body is written to that file instead of being returned. */
data class WebDavRequest(
    val method: String,
    val url: String,
    val headers: List<Pair<String, String>>,
    val body: WebDavBody = WebDavBody.Empty,
    val downloadTo: String? = null
) {
    fun header(name: String): String? = headers.lastOrNull { it.first.equals(name, ignoreCase = true) }?.second
}

/** Status, reason phrase and (unless downloaded to a file) the body text. */
data class WebDavResponse(val code: Int, val message: String, val body: String?) {
    val isSuccessful: Boolean get() = code in 200..299
}

/**
 * Sends one WebDAV request. Implementations allow https only, never follow a redirect between http and https,
 * and throw on network errors with the platform's own message (shown to the user as Android always did).
 */
interface WebDavHttp {
    suspend fun send(request: WebDavRequest): WebDavResponse
}

package com.emanuel5014.trainable.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String = "",
    @SerialName("body") val body: String = "", // Changelog (null on GitHub when empty, see the Json setup)
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
    /** Marked "Pre-release" on GitHub: only the beta channel is offered it. */
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    /** The release page on GitHub, where the APK can be downloaded by hand. */
    @SerialName("html_url") val htmlUrl: String = "",
    /** ISO date-time, such as "2026-11-11T22:14:38Z"; empty when unknown. */
    @SerialName("published_at") val publishedAt: String = ""
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String = "",
    /** Bytes; 0 when unknown. */
    @SerialName("size") val size: Long = 0,
    /** "sha256:<hex>" for files GitHub hashed on upload (since mid-2025); null on older releases. */
    @SerialName("digest") val digest: String? = null
)

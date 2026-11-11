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
    @SerialName("draft") val draft: Boolean = false
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String
)

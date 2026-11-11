package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.GitHubRelease

/**
 * Which GitHub releases the Android updater offers.
 *
 * Both read the same release list. Stable offers the highest final release (never one marked "Pre-release"); beta
 * offers the highest Android version, pre-release or final, so a beta tester also gets the final release when it
 * comes out. The list is used instead of GitHub's "Latest" release, which an iOS release could take by mistake. Neither channel ever offers an older version: Android refuses to install it over a newer one, and a
 * downgrade would meet a database newer than its code.
 */
object UpdateChannel {

    /** The channel in use: the user's choice, or beta for a beta install until they choose. */
    fun isBeta(choice: Boolean?, installedVersion: String): Boolean =
        choice ?: AppVersion.isPreRelease(installedVersion)

    /**
     * The release to offer from [releases] (newest first or any order), or null. Only real Android releases count:
     * published (not a draft), with an APK, and a tag that reads as a version, which leaves out the iOS
     * pre-releases (tags "ios-…", .ipa only) published in the same repository.
     */
    fun pick(releases: List<GitHubRelease>, installedVersion: String, beta: Boolean): GitHubRelease? {
        val newest = releases
            .filter { isAndroidRelease(it) && (beta || !it.prerelease) }
            .maxWithOrNull { a, b -> AppVersion.compare(a.tagName, b.tagName) }
            ?: return null
        return newest.takeIf { AppVersion.isNewer(installedVersion, it.tagName) }
    }

    fun isAndroidRelease(release: GitHubRelease): Boolean =
        !release.draft &&
            release.assets.any { it.name.endsWith(".apk", ignoreCase = true) } &&
            AppVersion.isValid(release.tagName)
}

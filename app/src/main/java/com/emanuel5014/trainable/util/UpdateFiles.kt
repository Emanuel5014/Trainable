package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.GitHubAsset
import com.emanuel5014.trainable.data.remote.GitHubRelease
import java.io.File
import java.security.MessageDigest

/**
 * The files the updater keeps in its own folder: "<tag>.apk.part" while downloading (kept between attempts so a
 * download resumes where it stopped), "<tag>.apk" once complete. Only the release being offered keeps its files.
 */
object UpdateFiles {

    /** The APK of a release: the first .apk file, as GitHub lists it. */
    fun apkAsset(release: GitHubRelease): GitHubAsset? =
        release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

    fun apkFile(dir: File, tag: String): File = File(dir, safeName(tag) + ".apk")

    fun partFile(dir: File, tag: String): File = File(dir, safeName(tag) + ".apk.part")

    /** Tag as a file name: "v3.0.0-beta2" stays as is, anything outside letters, digits, dot, dash becomes "_". */
    fun safeName(tag: String): String = tag.replace(Regex("[^A-Za-z0-9.\\-]"), "_").ifEmpty { "update" }

    /** The tag a file in the folder belongs to, or null for a file the updater did not write. */
    fun tagOf(file: File): String? = when {
        file.name.endsWith(".apk.part") -> file.name.removeSuffix(".apk.part")
        file.name.endsWith(".apk") -> file.name.removeSuffix(".apk")
        else -> null
    }

    /**
     * Files to delete: anything not belonging to [keepTag] (an older offer, or a version now installed). With a null
     * [keepTag] the whole folder goes.
     */
    fun stale(files: List<File>, keepTag: String?): List<File> {
        val keep = keepTag?.let(::safeName)
        return files.filter { tagOf(it) == null || tagOf(it) != keep }
    }

    /** The lower-case hex SHA-256 in a GitHub digest ("sha256:ab12…"), or null when there is none to check. */
    fun sha256Of(digest: String?): String? {
        val value = digest?.trim() ?: return null
        if (!value.startsWith("sha256:", ignoreCase = true)) return null
        val hex = value.substringAfter(':').lowercase()
        return hex.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** True when [file] has the size and SHA-256 GitHub lists for [asset] (each checked only when GitHub gives it). */
    fun matches(file: File, asset: GitHubAsset): Boolean {
        if (!file.isFile) return false
        if (asset.size > 0 && file.length() != asset.size) return false
        val expected = sha256Of(asset.digest) ?: return true
        return sha256(file) == expected
    }

    /**
     * How to go on with a download that already has [partBytes] on disk, given the HTTP status of the request that
     * asked for the rest. 206 appends; 200 means the server sent the whole file, so it starts over; anything else
     * is not a body to write.
     */
    fun resumeMode(httpCode: Int, partBytes: Long): ResumeMode = when {
        httpCode == 206 && partBytes > 0 -> ResumeMode.APPEND
        httpCode == 200 -> ResumeMode.FROM_START
        httpCode == 416 -> ResumeMode.RESTART
        else -> ResumeMode.ERROR
    }

    enum class ResumeMode {
        APPEND,
        FROM_START,
        /** The range asked is past the end (the part file is stale): delete it and ask again for the whole file. */
        RESTART,
        ERROR
    }

    /** The error to show for a failed HTTP answer from GitHub. */
    fun httpError(code: Int, rateLimitRemaining: String?): UpdateError = when {
        code == 429 -> UpdateError.RATE_LIMITED
        code == 403 && rateLimitRemaining?.trim() == "0" -> UpdateError.RATE_LIMITED
        else -> UpdateError.SERVER
    }
}

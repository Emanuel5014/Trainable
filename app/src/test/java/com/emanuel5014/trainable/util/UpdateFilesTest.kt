package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.GitHubAsset
import com.emanuel5014.trainable.data.remote.GitHubRelease
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UpdateFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // SHA-256 of the bytes "hello"
    private val helloSha = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

    private fun asset(size: Long, digest: String?) = GitHubAsset("Trainable.apk", "https://example.invalid/a.apk", "", size, digest)

    @Test
    fun `digest is read only when it is a well-formed sha256`() {
        assertEquals(helloSha, UpdateFiles.sha256Of("sha256:$helloSha"))
        assertEquals(helloSha, UpdateFiles.sha256Of("SHA256:${helloSha.uppercase()}"))
        assertNull(UpdateFiles.sha256Of(null))
        assertNull(UpdateFiles.sha256Of("md5:abc"))
        assertNull(UpdateFiles.sha256Of("sha256:1234"))
        assertNull(UpdateFiles.sha256Of("sha256:" + "z".repeat(64)))
    }

    @Test
    fun `file matches only with the right size and hash`() {
        val file = tmp.newFile("x.apk").apply { writeText("hello") }
        assertTrue(UpdateFiles.matches(file, asset(5, "sha256:$helloSha")))
        assertTrue(UpdateFiles.matches(file, asset(5, null))) // old releases: size only
        assertTrue(UpdateFiles.matches(file, asset(0, null))) // nothing to check against
        assertFalse(UpdateFiles.matches(file, asset(6, "sha256:$helloSha")))
        assertFalse(UpdateFiles.matches(file, asset(5, "sha256:" + "0".repeat(64))))
        assertFalse(UpdateFiles.matches(File(tmp.root, "missing.apk"), asset(0, null)))
    }

    @Test
    fun `resume mode follows the http status`() {
        assertEquals(UpdateFiles.ResumeMode.APPEND, UpdateFiles.resumeMode(206, 100))
        assertEquals(UpdateFiles.ResumeMode.FROM_START, UpdateFiles.resumeMode(200, 100)) // server ignored the range
        assertEquals(UpdateFiles.ResumeMode.FROM_START, UpdateFiles.resumeMode(200, 0))
        assertEquals(UpdateFiles.ResumeMode.RESTART, UpdateFiles.resumeMode(416, 100))
        assertEquals(UpdateFiles.ResumeMode.ERROR, UpdateFiles.resumeMode(206, 0))
        assertEquals(UpdateFiles.ResumeMode.ERROR, UpdateFiles.resumeMode(404, 0))
    }

    @Test
    fun `rate limit is told apart from other refusals`() {
        assertEquals(UpdateError.RATE_LIMITED, UpdateFiles.httpError(403, "0"))
        assertEquals(UpdateError.RATE_LIMITED, UpdateFiles.httpError(429, null))
        assertEquals(UpdateError.SERVER, UpdateFiles.httpError(403, "12"))
        assertEquals(UpdateError.SERVER, UpdateFiles.httpError(404, null))
        assertEquals(UpdateError.SERVER, UpdateFiles.httpError(500, null))
    }

    @Test
    fun `only the offered release keeps its files`() {
        val dir = tmp.root
        val keepApk = UpdateFiles.apkFile(dir, "v3.0.0-beta3")
        val keepPart = UpdateFiles.partFile(dir, "v3.0.0-beta3")
        val old = UpdateFiles.apkFile(dir, "v3.0.0-beta2")
        val stray = File(dir, "notes.txt")
        val files = listOf(keepApk, keepPart, old, stray)
        assertEquals(listOf(old, stray), UpdateFiles.stale(files, "v3.0.0-beta3"))
        assertEquals(files, UpdateFiles.stale(files, null))
        assertEquals("v3.0.0-beta3", UpdateFiles.tagOf(keepPart))
        assertEquals("v3.0.0-beta3", UpdateFiles.tagOf(keepApk))
        assertNull(UpdateFiles.tagOf(stray))
    }

    @Test
    fun `odd tags become safe file names`() {
        assertEquals("v3.0.0-beta2", UpdateFiles.safeName("v3.0.0-beta2"))
        assertEquals("v3_0_.._x", UpdateFiles.safeName("v3/0 ../x"))
        assertEquals("update", UpdateFiles.safeName(""))
    }

    @Test
    fun `size and digest are read from the GitHub json`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val release = json.decodeFromString<GitHubRelease>(
            """{"tag_name":"v3.0.0-beta2","name":null,"body":null,"prerelease":true,"draft":false,
               "assets":[{"name":"Trainable.apk","browser_download_url":"https://x/Trainable.apk",
               "content_type":"application/vnd.android.package-archive","size":110870093,
               "digest":"sha256:acb1c4941dc96eaaaf694a3d2eb24db754d20b0c430f8ecc888de425bc12f0f5"}]}"""
        )
        val asset = UpdateFiles.apkAsset(release)!!
        assertEquals(110870093L, asset.size)
        assertEquals("acb1c4941dc96eaaaf694a3d2eb24db754d20b0c430f8ecc888de425bc12f0f5", UpdateFiles.sha256Of(asset.digest))
        // Older assets have no digest
        val old = json.decodeFromString<GitHubAsset>("""{"name":"a.apk","browser_download_url":"u","content_type":"x","size":1,"digest":null}""")
        assertNull(old.digest)
    }
}

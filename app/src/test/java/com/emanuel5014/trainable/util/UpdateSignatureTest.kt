package com.emanuel5014.trainable.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSignatureTest {

    private val release = "e7410df96be8deec8243dce02f58da247ca086c31a8a5254ea8b668fbc8be479"
    private val debug = "a".repeat(64)
    private val rotated = "b".repeat(64)

    @Test
    fun `release key check`() {
        assertEquals(true, UpdateSignature.isReleaseSigned(setOf(release), release))
        assertEquals(true, UpdateSignature.isReleaseSigned(setOf(release), " ${release.uppercase()} "))
        assertEquals(true, UpdateSignature.isReleaseSigned(setOf(rotated, release), release)) // key in the history
        assertEquals(false, UpdateSignature.isReleaseSigned(setOf(debug), release))
        assertNull(UpdateSignature.isReleaseSigned(setOf(debug), "")) // debug builds: not checked
        assertNull(UpdateSignature.isReleaseSigned(emptySet(), release)) // unreadable: let Android decide
    }

    @Test
    fun `apk is compatible when it shares a certificate`() {
        assertTrue(UpdateSignature.compatible(setOf(release), setOf(release))!!)
        assertTrue(UpdateSignature.compatible(setOf(release), setOf(rotated, release))!!) // new key, old one in history
        assertFalse(UpdateSignature.compatible(setOf(release), setOf(debug))!!)
        assertNull(UpdateSignature.compatible(emptySet(), setOf(debug)))
        assertNull(UpdateSignature.compatible(setOf(release), emptySet()))
    }

    @Test
    fun `only unfixable errors replace retry`() {
        assertTrue(UpdateError.OTHER_SOURCE.isFinal)
        assertTrue(UpdateError.WRONG_APP.isFinal)
        assertTrue(UpdateError.INSTALL_CONFLICT.isFinal)
        assertTrue(UpdateError.NO_APK.isFinal)
        assertFalse(UpdateError.NETWORK.isFinal)
        assertFalse(UpdateError.CORRUPT.isFinal)
        assertFalse(UpdateError.INSTALL_FAILED.isFinal)
    }
}

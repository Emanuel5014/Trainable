package com.emanuel5014.trainable.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Signing checks that let the updater explain a refusal Android would otherwise give as a bare "App not installed":
 * before downloading (is this copy signed with the key of the GitHub releases?) and before installing (is the
 * downloaded APK this app, signed with a key Android accepts over this install?). Certificates are compared as
 * lower-case SHA-256 hex, the form `apksigner verify --print-certs` shows.
 */
object UpdateSignature {

    private const val TAG = "UpdateSignature"

    class Archive(val packageName: String, val certs: Set<String>)

    /** The certificates of the installed app (current signers plus the rotation history); empty when unreadable. */
    fun installed(context: Context): Set<String> = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        certs(info.signingInfo)
    } catch (e: Exception) {
        Log.w(TAG, "Installed certificates unreadable", e)
        emptySet()
    }

    /** Package name and certificates of an APK file, or null when Android cannot read it. */
    fun archive(context: Context, apk: File): Archive? = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?.let { Archive(it.packageName, certs(it.signingInfo)) }
    } catch (e: Exception) {
        Log.w(TAG, "APK certificates unreadable", e)
        null
    }

    /**
     * Whether this install is signed with [expected], the release key; null when there is nothing to compare (no key
     * set for this build, or the installed certificates could not be read).
     */
    fun isReleaseSigned(installed: Set<String>, expected: String): Boolean? {
        val key = expected.trim().lowercase()
        if (key.isEmpty() || installed.isEmpty()) return null
        return key in installed
    }

    /**
     * Whether Android should accept an APK with [apk] certificates over an install with [installed] ones: they share
     * a certificate (equal, or one in the other's rotation history). Null when either side is unknown.
     */
    fun compatible(installed: Set<String>, apk: Set<String>): Boolean? {
        if (installed.isEmpty() || apk.isEmpty()) return null
        return installed.any { it in apk }
    }

    private fun certs(info: SigningInfo?): Set<String> {
        if (info == null) return emptySet()
        val signers = info.apkContentsSigners.orEmpty().toList() +
            if (info.hasMultipleSigners()) emptyList() else info.signingCertificateHistory.orEmpty().toList()
        return signers.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

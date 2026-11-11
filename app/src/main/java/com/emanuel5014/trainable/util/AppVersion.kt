package com.emanuel5014.trainable.util

/**
 * Compares app versions such as "2.9.2", "v3.0.0", "3.0.0-beta2" or "3.0.0-rc.1".
 *
 * A pre-release ranks below the same version without a suffix (3.0.0-beta2 < 3.0.0), so a beta install is offered
 * the final release. Pre-release labels compare piece by piece: numbers as numbers (beta9 < beta10), words
 * alphabetically (alpha < beta < rc). The "-debug" build-type suffix is not a pre-release and is ignored.
 */
object AppVersion {

    private const val BUILD_TYPE_SUFFIX = "-debug"

    /** True when [latest] is a newer version than [current]. A version that cannot be read is never newer. */
    fun isNewer(current: String, latest: String): Boolean {
        val currentVersion = parse(current) ?: return false
        val latestVersion = parse(latest) ?: return false
        return compare(latestVersion, currentVersion) > 0
    }

    /** Orders two versions (negative when [a] is older). Versions that cannot be read sort below every real one. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        return when {
            pa == null && pb == null -> 0
            pa == null -> -1
            pb == null -> 1
            else -> compare(pa, pb)
        }
    }

    /** True for a version that reads as one, such as "v3.0.0" or "3.0.0-beta1"; false for "ios-2.9.1" or "nightly". */
    fun isValid(raw: String): Boolean = parse(raw) != null

    /** True for a pre-release such as "3.0.0-beta1" (also with the "-debug" suffix); false for "3.0.0" or junk. */
    fun isPreRelease(raw: String): Boolean = parse(raw)?.preRelease != null

    private class Parsed(val core: List<Int>, val preRelease: List<String>?)

    private fun parse(raw: String): Parsed? {
        var text = raw.trim().removePrefix("v").removePrefix("V").substringBefore('+')
        if (text.endsWith(BUILD_TYPE_SUFFIX, ignoreCase = true)) text = text.dropLast(BUILD_TYPE_SUFFIX.length)
        val core = text.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
        val preRelease = if ('-' in text) tokens(text.substringAfter('-')) else null
        return Parsed(core, preRelease)
    }

    /** "beta.10" and "beta10" both become [beta, 10]. */
    private fun tokens(label: String): List<String> =
        Regex("[0-9]+|[^0-9.\\-]+").findAll(label.lowercase()).map { it.value }.toList()

    private fun compare(a: Parsed, b: Parsed): Int {
        for (i in 0 until maxOf(a.core.size, b.core.size)) {
            val diff = a.core.getOrElse(i) { 0 }.compareTo(b.core.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        val aPre = a.preRelease
        val bPre = b.preRelease
        return when {
            aPre == null && bPre == null -> 0
            aPre == null -> 1
            bPre == null -> -1
            else -> comparePreRelease(aPre, bPre)
        }
    }

    private fun comparePreRelease(a: List<String>, b: List<String>): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val aNum = a[i].toBigIntegerOrNull()
            val bNum = b[i].toBigIntegerOrNull()
            val diff = when {
                aNum != null && bNum != null -> aNum.compareTo(bNum)
                aNum != null -> -1
                bNum != null -> 1
                else -> a[i].compareTo(b[i])
            }
            if (diff != 0) return diff
        }
        return a.size.compareTo(b.size)
    }
}

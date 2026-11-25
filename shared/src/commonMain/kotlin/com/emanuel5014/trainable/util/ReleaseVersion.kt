package com.emanuel5014.trainable.util

/** A release tag or notes version as people read it: "v3.0.0-beta2" → "3.0.0 beta 2", "v3.0.0-rc1" → "3.0.0 RC 1", "v3.0.0" → "3.0.0". */
fun releaseDisplayVersion(tag: String): String {
    val text = tag.trim().removePrefix("v").removePrefix("V")
    val match = Regex("""^(\d+(?:\.\d+)*)-(alpha|beta|rc)\.?(\d+)$""", RegexOption.IGNORE_CASE).find(text) ?: return text
    val (core, label, number) = match.destructured
    val name = if (label.equals("rc", ignoreCase = true)) "RC" else label.lowercase()
    return "$core $name $number"
}

/** "v3.1.0-beta1" → "3.1.0": the version without its pre-release label, as iOS's CFBundleShortVersionString. */
fun releaseCoreVersion(tag: String): String =
    tag.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore(' ')

/** True for "3.1.0-beta1", "3.1.0-rc2" and the like. */
fun isPreReleaseVersion(tag: String): Boolean =
    Regex("""-(alpha|beta|rc)""", RegexOption.IGNORE_CASE).containsMatchIn(tag)

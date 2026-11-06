package com.emanuel5014.trainable.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

/**
 * The app language on iOS, chosen in Settings like on Android (same `userLanguage` preference: a code, "system"
 * or unset). A chosen language is written to the app's own `AppleLanguages`, which Compose resources and
 * `Locale.current` read; "system" removes it so the phone's languages apply again.
 */
object IosAppLocale {
    val SUPPORTED = listOf("en", "it", "es", "fr", "de", "pt")
    private const val APPLE_LANGUAGES = "AppleLanguages"
    private const val CHOICE_KEY = "app_language.choice"

    private val defaults get() = NSUserDefaults.standardUserDefaults

    /** The explicit choice applied last ("it", "en"...), or null for the phone's language. */
    val choice: String? get() = defaults.stringForKey(CHOICE_KEY)

    /** Applies the stored preference; returns true when the language really changed. */
    fun apply(preference: String?): Boolean {
        val wanted = preference?.takeIf { it in SUPPORTED }
        if (wanted == choice) return false
        if (wanted == null) {
            defaults.removeObjectForKey(APPLE_LANGUAGES)
            defaults.removeObjectForKey(CHOICE_KEY)
        } else {
            defaults.setObject(listOf(wanted), APPLE_LANGUAGES)
            defaults.setObject(wanted, CHOICE_KEY)
        }
        return true
    }

    /** The language the app's texts are in: the choice, else the phone's when supported, else English (as Android). */
    fun language(): String {
        choice?.let { return it }
        val phone = (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-')?.lowercase()
        return phone?.takeIf { it in SUPPORTED } ?: "en"
    }

    /** Dates and numbers: the chosen language when there is one, otherwise the phone's own locale as before. */
    fun formattingLocale(): NSLocale = choice?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
}

package com.emanuel5014.trainable.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocaleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf("en", "it")
        const val LANGUAGE_SYSTEM = "system"
        
        fun getLanguageDisplayName(code: String): String {
            return when (code) {
                "en" -> "English"
                "it" -> "Italiano"
                else -> code
            }
        }
    }

    val currentLanguage: Flow<String> = userPreferencesRepository.userLanguage.map { userLang ->
        userLang ?: LANGUAGE_SYSTEM
    }

    val userSelectedLanguage: Flow<String?> = userPreferencesRepository.userLanguage

    suspend fun getResolvedLanguage(): String {
        val userLang = userPreferencesRepository.userLanguage.first()
        
        if (userLang != null && userLang != LANGUAGE_SYSTEM) {
            return userLang
        }
        
        val systemLang = getSystemLanguage()
        return if (systemLang in SUPPORTED_LANGUAGES) {
            systemLang
        } else {
            "en"
        }
    }

    private fun getSystemLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val locales = localeManager?.applicationLocales ?: LocaleList.getEmptyLocaleList()
            if (locales.isEmpty) {
                Locale.getDefault().language
            } else {
                locales.get(0)?.language ?: Locale.getDefault().language
            }
        } else {
            val locales = context.resources.configuration.locales
            if (locales.isEmpty) {
                Locale.getDefault().language
            } else {
                locales.get(0)?.language ?: Locale.getDefault().language
            }
        }
    }

    fun getSystemLanguageCompat(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Locale.getDefault().language
        } else {
            Locale.getDefault().language
        }
    }

    fun resolveLanguageForCompose(userLang: String?): String {
        if (userLang != null && userLang != LANGUAGE_SYSTEM) {
            return userLang
        }
        val systemLang = getSystemLanguageCompat()
        return if (systemLang in SUPPORTED_LANGUAGES) {
            systemLang
        } else {
            "en"
        }
    }

    suspend fun setUserLanguage(languageCode: String?) {
        userPreferencesRepository.setUserLanguage(languageCode)
        
        val resolvedLang = if (languageCode == null || languageCode == LANGUAGE_SYSTEM) {
            getSystemLanguage().takeIf { it in SUPPORTED_LANGUAGES } ?: "en"
        } else {
            languageCode
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applyLanguage(resolvedLang)
        } else {
            applyLanguageLegacy(context, resolvedLang)
        }
    }

    private fun applyLanguage(languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLanguageLegacy(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        
        // Also update application context
        val appContext = context.applicationContext
        if (appContext != null && appContext !== context) {
            val appConfig = appContext.resources.configuration
            appConfig.setLocale(locale)
            appContext.resources.updateConfiguration(appConfig, appContext.resources.displayMetrics)
        }
    }

    suspend fun applyStoredLanguage() {
        val lang = getResolvedLanguage()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applyLanguage(lang)
        } else {
            applyLanguageLegacy(context, lang)
        }
    }

    fun wrapContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        
        // This is a synchronous call but it's only called during context attachment
        // and it's fast because it's local DataStore
        val userLang = runBlocking { userPreferencesRepository.userLanguage.first() }
        if (userLang == null || userLang == LANGUAGE_SYSTEM) {
            return context
        }

        val locale = Locale(userLang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun getString(id: Int): String {
        return context.getString(id)
    }
}
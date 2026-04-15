package com.emanuel5014.trainable.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    suspend fun setUserLanguage(languageCode: String?) {
        userPreferencesRepository.setUserLanguage(languageCode)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val resolvedLang = if (languageCode == null || languageCode == LANGUAGE_SYSTEM) {
                getSystemLanguage().takeIf { it in SUPPORTED_LANGUAGES } ?: "en"
            } else {
                languageCode
            }
            applyLanguage(resolvedLang)
        }
    }

    private fun applyLanguage(languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        }
    }

    suspend fun applyStoredLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lang = getResolvedLanguage()
            applyLanguage(lang)
        }
    }
}
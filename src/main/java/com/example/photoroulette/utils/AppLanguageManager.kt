package com.example.photoroulette.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.photoroulette.data.datastore.SettingsRepository

object AppLanguageManager {

    private val supportedTags = setOf(
        "ar",
        "en",
        "es",
        "fr",
        "ru",
        "zh",
    )

    fun normalizeLanguageTag(tag: String): String {
        if (tag == SettingsRepository.SYSTEM_LANGUAGE_TAG) {
            return tag
        }

        val normalized = tag.substringBefore('-').lowercase()
        return if (normalized in supportedTags) {
            normalized
        } else {
            SettingsRepository.SYSTEM_LANGUAGE_TAG
        }
    }

    fun applyLanguage(tag: String) {
        val normalizedTag = normalizeLanguageTag(tag)
        val locales = if (normalizedTag == SettingsRepository.SYSTEM_LANGUAGE_TAG) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(normalizedTag)
        }

        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}

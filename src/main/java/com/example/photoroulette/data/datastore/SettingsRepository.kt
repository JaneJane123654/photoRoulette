package com.example.photoroulette.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext

    private object Keys {
        val EnableSwipeDelete = booleanPreferencesKey("enable_swipe_delete")
        val AppLanguageTag = stringPreferencesKey("app_language_tag")
    }

    val isSwipeDeleteEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableSwipeDelete] ?: false
        }

    val appLanguageTag: Flow<String> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.AppLanguageTag] ?: SYSTEM_LANGUAGE_TAG
        }

    suspend fun setSwipeDeleteEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableSwipeDelete] = enabled
        }
    }

    suspend fun setAppLanguageTag(tag: String) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.AppLanguageTag] = tag
        }
    }

    companion object {
        const val SYSTEM_LANGUAGE_TAG = "system"
    }
}

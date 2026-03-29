package com.example.photoroulette.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.photoroulette.model.SwipeAction
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
        val EnableSilentDelete = booleanPreferencesKey("enable_silent_delete")
        val ShowFullImage = booleanPreferencesKey("show_full_image")
        val ShowFloatingDeleteButton = booleanPreferencesKey("show_floating_delete_button")
        val EnableGestureBall = booleanPreferencesKey("enable_gesture_ball")
        val GestureBallSizeScale = floatPreferencesKey("gesture_ball_size_scale")
        val SilentDeleteTreeUris = stringSetPreferencesKey("silent_delete_tree_uris")
        val SilentDeleteTreeUri = stringPreferencesKey("silent_delete_tree_uri")
        val AppLanguageTag = stringPreferencesKey("app_language_tag")
        val SwipeLeftAction = stringPreferencesKey("swipe_left_action")
        val SwipeRightAction = stringPreferencesKey("swipe_right_action")
        val SwipeUpAction = stringPreferencesKey("swipe_up_action")
        val SwipeDownAction = stringPreferencesKey("swipe_down_action")
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
            preferences[Keys.EnableSwipeDelete] ?: true
        }

    val showFullImage: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.ShowFullImage] ?: false
        }

    val showFloatingDeleteButton: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.ShowFloatingDeleteButton] ?: false
        }

    val isGestureBallEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableGestureBall] ?: false
        }

    val gestureBallSizeScale: Flow<Float> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.GestureBallSizeScale]
                ?.coerceIn(MIN_GESTURE_BALL_SIZE_SCALE, MAX_GESTURE_BALL_SIZE_SCALE)
                ?: DEFAULT_GESTURE_BALL_SIZE_SCALE
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

    val isSilentDeleteEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableSilentDelete] ?: false
        }

    val silentDeleteTreeUris: Flow<List<String>> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val persistedUris = preferences[Keys.SilentDeleteTreeUris]
                ?.mapNotNull { uri -> uri.takeIf { it.isNotBlank() } }
                ?.toSet()
                .orEmpty()

            if (persistedUris.isNotEmpty()) {
                return@map persistedUris.sorted()
            }

            val legacyUri = preferences[Keys.SilentDeleteTreeUri]
                ?.takeIf { it.isNotBlank() }
                ?: return@map emptyList()

            listOf(legacyUri)
        }

    val swipeLeftAction: Flow<SwipeAction> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SwipeAction.fromStorageValue(preferences[Keys.SwipeLeftAction]) ?: DEFAULT_LEFT_ACTION
        }

    val swipeRightAction: Flow<SwipeAction> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SwipeAction.fromStorageValue(preferences[Keys.SwipeRightAction]) ?: DEFAULT_RIGHT_ACTION
        }

    val swipeUpAction: Flow<SwipeAction> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SwipeAction.fromStorageValue(preferences[Keys.SwipeUpAction]) ?: DEFAULT_UP_ACTION
        }

    val swipeDownAction: Flow<SwipeAction> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SwipeAction.fromStorageValue(preferences[Keys.SwipeDownAction]) ?: DEFAULT_DOWN_ACTION
        }

    suspend fun setSwipeDeleteEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableSwipeDelete] = enabled
        }
    }

    suspend fun setShowFullImage(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.ShowFullImage] = enabled
        }
    }

    suspend fun setShowFloatingDeleteButton(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.ShowFloatingDeleteButton] = enabled
        }
    }

    suspend fun setGestureBallEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableGestureBall] = enabled
        }
    }

    suspend fun setGestureBallSizeScale(scale: Float) {
        val normalizedScale = scale.coerceIn(MIN_GESTURE_BALL_SIZE_SCALE, MAX_GESTURE_BALL_SIZE_SCALE)
        appContext.dataStore.edit { preferences ->
            preferences[Keys.GestureBallSizeScale] = normalizedScale
        }
    }

    suspend fun setSilentDeleteEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableSilentDelete] = enabled
        }
    }

    suspend fun setSilentDeleteTreeUris(uris: Collection<String>) {
        val normalizedUris = uris
            .mapNotNull { uri -> uri.takeIf { it.isNotBlank() } }
            .toSet()

        appContext.dataStore.edit { preferences ->
            if (normalizedUris.isEmpty()) {
                preferences.remove(Keys.SilentDeleteTreeUris)
                preferences.remove(Keys.SilentDeleteTreeUri)
            } else {
                preferences[Keys.SilentDeleteTreeUris] = normalizedUris

                if (normalizedUris.size == 1) {
                    preferences[Keys.SilentDeleteTreeUri] = normalizedUris.first()
                } else {
                    preferences.remove(Keys.SilentDeleteTreeUri)
                }
            }
        }
    }

    suspend fun setSilentDeleteTreeUri(uri: String?) {
        setSilentDeleteTreeUris(uri?.let(::listOf).orEmpty())
    }

    suspend fun setAppLanguageTag(tag: String) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.AppLanguageTag] = tag
        }
    }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.SwipeLeftAction] = action.storageValue
        }
    }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.SwipeRightAction] = action.storageValue
        }
    }

    suspend fun setSwipeUpAction(action: SwipeAction) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.SwipeUpAction] = action.storageValue
        }
    }

    suspend fun setSwipeDownAction(action: SwipeAction) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.SwipeDownAction] = action.storageValue
        }
    }

    companion object {
        const val SYSTEM_LANGUAGE_TAG = "system"

        const val MIN_GESTURE_BALL_SIZE_SCALE = 0.78f
        const val MAX_GESTURE_BALL_SIZE_SCALE = 1.38f
        const val DEFAULT_GESTURE_BALL_SIZE_SCALE = 1f

        val DEFAULT_LEFT_ACTION = SwipeAction.Delete
        val DEFAULT_RIGHT_ACTION = SwipeAction.Next
        val DEFAULT_UP_ACTION = SwipeAction.Next
        val DEFAULT_DOWN_ACTION = SwipeAction.Previous
    }
}

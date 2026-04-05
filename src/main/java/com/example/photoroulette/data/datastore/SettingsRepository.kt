package com.example.photoroulette.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.photoroulette.model.DefaultBehaviorNoticeMode
import com.example.photoroulette.model.SwipeAction
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext

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

    val isDeleteReminderEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableDeleteReminder] ?: DEFAULT_DELETE_REMINDER_ENABLED
        }

    val swipeGestureSensitivity: Flow<Float> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.SwipeGestureSensitivity]
                ?.coerceIn(MIN_SWIPE_GESTURE_SENSITIVITY, MAX_SWIPE_GESTURE_SENSITIVITY)
                ?: DEFAULT_SWIPE_GESTURE_SENSITIVITY
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

    val showCardActionsButton: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.ShowCardActionsButton] ?: DEFAULT_CARD_ACTIONS_BUTTON_VISIBLE
        }

    val isTapImageToggleEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableTapImageToggle] ?: DEFAULT_TAP_IMAGE_TOGGLE_ENABLED
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

    val isGestureBallFeedbackEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.EnableGestureBallFeedback] ?: DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED
        }

    val showGestureBallActionHint: Flow<Boolean> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.ShowGestureBallActionHint] ?: DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED
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

    val defaultBehaviorNoticeMode: Flow<DefaultBehaviorNoticeMode> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            DefaultBehaviorNoticeMode.fromStorageValue(preferences[Keys.DefaultBehaviorNoticeMode])
                ?: DefaultBehaviorNoticeMode.Visible
        }

    val skippedUpdateVersion: Flow<String?> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.DeferredUpdateVersion]?.takeIf { version -> version.isNotBlank() }
        }

    suspend fun setSwipeDeleteEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableSwipeDelete] = enabled
        }
    }

    suspend fun setDeleteReminderEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableDeleteReminder] = enabled
        }
    }

    suspend fun setSwipeGestureSensitivity(sensitivity: Float) {
        val normalizedSensitivity = sensitivity.coerceIn(
            MIN_SWIPE_GESTURE_SENSITIVITY,
            MAX_SWIPE_GESTURE_SENSITIVITY,
        )
        appContext.dataStore.edit { preferences ->
            preferences[Keys.SwipeGestureSensitivity] = normalizedSensitivity
        }
    }

    suspend fun setShowFullImage(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.ShowFullImage] = enabled
        }
    }

    suspend fun setShowCardActionsButton(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.ShowCardActionsButton] = enabled
        }
    }

    suspend fun setTapImageToggleEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableTapImageToggle] = enabled
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

    suspend fun setGestureBallFeedbackEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.EnableGestureBallFeedback] = enabled
        }
    }

    suspend fun setShowGestureBallActionHint(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[Keys.ShowGestureBallActionHint] = enabled
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

    suspend fun setDefaultBehaviorNoticeEnabled(enabled: Boolean) {
        val mode = if (enabled) {
            DefaultBehaviorNoticeMode.Visible
        } else {
            DefaultBehaviorNoticeMode.UserHidden
        }

        appContext.dataStore.edit { preferences ->
            preferences[Keys.DefaultBehaviorNoticeMode] = mode.storageValue
            if (enabled) {
                preferences[Keys.DefaultBehaviorNoticeShownCount] = 0
            }
        }
    }

    suspend fun prepareDefaultBehaviorNoticeForSession(
        currentMonthKey: String,
        monthlyDisplayLimit: Int,
    ): Boolean {
        require(monthlyDisplayLimit > 0) {
            "monthlyDisplayLimit must be greater than zero."
        }

        var shouldShowNotice = false

        appContext.dataStore.edit { preferences ->
            var mode = DefaultBehaviorNoticeMode.fromStorageValue(
                preferences[Keys.DefaultBehaviorNoticeMode],
            ) ?: DefaultBehaviorNoticeMode.Visible

            var shownMonth = preferences[Keys.DefaultBehaviorNoticeShownMonth] ?: currentMonthKey
            var shownCount = (preferences[Keys.DefaultBehaviorNoticeShownCount] ?: 0).coerceAtLeast(0)

            if (shownMonth != currentMonthKey) {
                shownMonth = currentMonthKey
                shownCount = 0

                if (mode == DefaultBehaviorNoticeMode.AutoHidden) {
                    mode = DefaultBehaviorNoticeMode.Visible
                }
            }

            if (mode == DefaultBehaviorNoticeMode.Visible) {
                if (shownCount >= monthlyDisplayLimit) {
                    mode = DefaultBehaviorNoticeMode.AutoHidden
                    shouldShowNotice = false
                } else {
                    shownCount += 1
                    shouldShowNotice = true
                }
            }

            preferences[Keys.DefaultBehaviorNoticeMode] = mode.storageValue
            preferences[Keys.DefaultBehaviorNoticeShownMonth] = shownMonth
            preferences[Keys.DefaultBehaviorNoticeShownCount] = shownCount
        }

        return shouldShowNotice
    }

    suspend fun setSkippedUpdateVersion(versionName: String?) {
        appContext.dataStore.edit { preferences ->
            val normalizedVersion = versionName?.trim().orEmpty()
            if (normalizedVersion.isBlank()) {
                preferences.remove(Keys.DeferredUpdateVersion)
            } else {
                preferences[Keys.DeferredUpdateVersion] = normalizedVersion
            }
        }
    }

    companion object {
        const val SYSTEM_LANGUAGE_TAG = "system"

        const val MIN_GESTURE_BALL_SIZE_SCALE = 0.78f
        const val MAX_GESTURE_BALL_SIZE_SCALE = 1.38f
        const val DEFAULT_GESTURE_BALL_SIZE_SCALE = 1f
        const val DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED = true
        const val DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED = true
        const val MIN_SWIPE_GESTURE_SENSITIVITY = 0.8f
        const val MAX_SWIPE_GESTURE_SENSITIVITY = 1.35f
        const val DEFAULT_SWIPE_GESTURE_SENSITIVITY = 1f
        const val DEFAULT_DELETE_REMINDER_ENABLED = true
        const val DEFAULT_TAP_IMAGE_TOGGLE_ENABLED = true
        const val DEFAULT_CARD_ACTIONS_BUTTON_VISIBLE = true

        val DEFAULT_LEFT_ACTION = SwipeAction.Delete
        val DEFAULT_RIGHT_ACTION = SwipeAction.Next
        val DEFAULT_UP_ACTION = SwipeAction.Next
        val DEFAULT_DOWN_ACTION = SwipeAction.Previous
    }
}

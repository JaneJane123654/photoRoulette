package com.example.photoroulette.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal object Keys {
    val EnableSwipeDelete = booleanPreferencesKey("enable_swipe_delete")
    val EnableDeleteReminder = booleanPreferencesKey("enable_delete_reminder")
    val EnableSilentDelete = booleanPreferencesKey("enable_silent_delete")
    val EnableTapImageToggle = booleanPreferencesKey("enable_tap_image_toggle")
    val ShowFullImage = booleanPreferencesKey("show_full_image")
    val ShowFloatingDeleteButton = booleanPreferencesKey("show_floating_delete_button")
    val EnableGestureBall = booleanPreferencesKey("enable_gesture_ball")
    val EnableGestureBallFeedback = booleanPreferencesKey("enable_gesture_ball_feedback")
    val ShowGestureBallActionHint = booleanPreferencesKey("show_gesture_ball_action_hint")
    val SwipeGestureSensitivity = floatPreferencesKey("swipe_gesture_sensitivity")
    val GestureBallSizeScale = floatPreferencesKey("gesture_ball_size_scale")
    val SilentDeleteTreeUris = stringSetPreferencesKey("silent_delete_tree_uris")
    val SilentDeleteTreeUri = stringPreferencesKey("silent_delete_tree_uri")
    val AppLanguageTag = stringPreferencesKey("app_language_tag")
    val SwipeLeftAction = stringPreferencesKey("swipe_left_action")
    val SwipeRightAction = stringPreferencesKey("swipe_right_action")
    val SwipeUpAction = stringPreferencesKey("swipe_up_action")
    val SwipeDownAction = stringPreferencesKey("swipe_down_action")
    val DefaultBehaviorNoticeMode = stringPreferencesKey("default_behavior_notice_mode")
    val DefaultBehaviorNoticeShownMonth = stringPreferencesKey("default_behavior_notice_shown_month")
    val DefaultBehaviorNoticeShownCount = intPreferencesKey("default_behavior_notice_shown_count")
    val DeferredUpdateVersion = stringPreferencesKey("deferred_update_version")
}

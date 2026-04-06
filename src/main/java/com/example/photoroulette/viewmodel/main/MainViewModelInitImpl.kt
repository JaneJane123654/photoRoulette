package com.example.photoroulette.viewmodel

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.photoroulette.BuildConfig
import com.example.photoroulette.data.datastore.SettingsRepository
import com.example.photoroulette.data.media.MediaRepository
import com.example.photoroulette.model.MediaCard
import com.example.photoroulette.model.MediaKind
import com.example.photoroulette.data.update.AppUpdateRepository
import com.example.photoroulette.model.AppReleaseInfo
import com.example.photoroulette.model.DefaultBehaviorNoticeMode
import com.example.photoroulette.model.SilentDeleteScope
import com.example.photoroulette.model.SwipeAction
import com.example.photoroulette.model.UpdateCheckFeedback
import com.example.photoroulette.utils.IntentHelper
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.utils.VersionNameUtils
import com.example.photoroulette.viewmodel.states.HomeUiState
import java.io.File
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.photoroulette.viewmodel.MainViewModel.DeleteReminderEvent
import com.example.photoroulette.viewmodel.MainViewModel.PendingDeleteEntry
import com.example.photoroulette.viewmodel.MainViewModel.SystemDeleteRequest

internal fun MainViewModel.initializeStateCollectorsImpl() {
        scope.launch(ioDispatcher) {
            settingsRepository.ensureCardActionsButtonDefaultInitialized()
        }

        scope.launch {
            settingsRepository.isSwipeDeleteEnabled.collect { enabled ->
                _isSwipeDeleteEnabled.value = enabled
                savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.isDeleteReminderEnabled.collect { enabled ->
                _isDeleteReminderEnabled.value = enabled
                savedStateHandle[KEY_IS_DELETE_REMINDER_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.swipeGestureSensitivity.collect { sensitivity ->
                _swipeGestureSensitivity.value = sensitivity
                savedStateHandle[KEY_SWIPE_GESTURE_SENSITIVITY] = sensitivity
            }
        }

        scope.launch {
            settingsRepository.showFullImage.collect { enabled ->
                _showFullImage.value = enabled
                savedStateHandle[KEY_SHOW_FULL_IMAGE] = enabled
            }
        }

        scope.launch {
            settingsRepository.showCardActionsButton.collect { enabled ->
                _showCardActionsButton.value = enabled
                savedStateHandle[KEY_SHOW_CARD_ACTIONS_BUTTON] = enabled
            }
        }

        scope.launch {
            settingsRepository.isTapImageToggleEnabled.collect { enabled ->
                _isTapImageToggleEnabled.value = enabled
                savedStateHandle[KEY_IS_TAP_IMAGE_TOGGLE_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.showFloatingDeleteButton.collect { enabled ->
                _showFloatingDeleteButton.value = enabled
                savedStateHandle[KEY_SHOW_FLOATING_DELETE_BUTTON] = enabled
            }
        }

        scope.launch {
            settingsRepository.isGestureBallEnabled.collect { enabled ->
                _isGestureBallEnabled.value = enabled
                savedStateHandle[KEY_IS_GESTURE_BALL_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.gestureBallSizeScale.collect { scale ->
                _gestureBallSizeScale.value = scale
                savedStateHandle[KEY_GESTURE_BALL_SIZE_SCALE] = scale
            }
        }

        scope.launch {
            settingsRepository.isGestureBallFeedbackEnabled.collect { enabled ->
                _isGestureBallFeedbackEnabled.value = enabled
                savedStateHandle[KEY_IS_GESTURE_BALL_FEEDBACK_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.showGestureBallActionHint.collect { enabled ->
                _showGestureBallActionHint.value = enabled
                savedStateHandle[KEY_SHOW_GESTURE_BALL_ACTION_HINT] = enabled
            }
        }

        scope.launch {
            settingsRepository.isSilentDeleteEnabled.collect { enabled ->
                _isSilentDeleteEnabled.value = enabled
                savedStateHandle[KEY_IS_SILENT_DELETE_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.silentDeleteTreeUris.collect { treeUris ->
                _silentDeleteTreeUris.value = treeUris
                saveSilentDeleteTreeUris(treeUris)
            }
        }

        scope.launch {
            settingsRepository.appLanguageTag.collect { tag ->
                _appLanguageTag.value = tag
                savedStateHandle[KEY_APP_LANGUAGE_TAG] = tag
            }
        }

        scope.launch {
            settingsRepository.swipeLeftAction.collect { action ->
                _swipeLeftAction.value = action
                savedStateHandle[KEY_SWIPE_LEFT_ACTION] = action.storageValue
            }
        }

        scope.launch {
            settingsRepository.swipeRightAction.collect { action ->
                _swipeRightAction.value = action
                savedStateHandle[KEY_SWIPE_RIGHT_ACTION] = action.storageValue
            }
        }

        scope.launch {
            settingsRepository.swipeUpAction.collect { action ->
                _swipeUpAction.value = action
                savedStateHandle[KEY_SWIPE_UP_ACTION] = action.storageValue
            }
        }

        scope.launch {
            settingsRepository.swipeDownAction.collect { action ->
                _swipeDownAction.value = action
                savedStateHandle[KEY_SWIPE_DOWN_ACTION] = action.storageValue
            }
        }

        scope.launch {
            settingsRepository.defaultBehaviorNoticeMode.collect { mode ->
                _defaultBehaviorNoticeMode.value = mode
                savedStateHandle[KEY_DEFAULT_BEHAVIOR_NOTICE_MODE] = mode.storageValue
            }
        }

        scope.launch {
            settingsRepository.skippedUpdateVersion.collect { skippedVersion ->
                _skippedUpdateVersion.value = skippedVersion
                savedStateHandle[KEY_SKIPPED_UPDATE_VERSION] = skippedVersion
            }
        }

        scope.launch(ioDispatcher) {
            appUpdateRepository.cleanupDownloadedApkPackages()
        }

        if (
            _permissionMode.value != PermissionHelper.PermissionMode.DENIED &&
            queueIds.isNotEmpty() &&
            hasCachedCardsForVisibleWindow()
        ) {
            emitQueueState()
            preloadUpcomingImages()
        }
}

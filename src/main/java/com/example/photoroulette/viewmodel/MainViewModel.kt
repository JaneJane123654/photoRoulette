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
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    internal val savedStateHandle: SavedStateHandle,
    internal val settingsRepository: SettingsRepository,
    internal val mediaRepository: MediaRepository,
    internal val appUpdateRepository: AppUpdateRepository,
    internal val imageLoader: ImageLoader,
    internal val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    data class DeleteReminderEvent(
        val deletedImageId: Long,
    )

    internal data class SystemDeleteRequest(
        val imageId: Long,
        val request: IntentSenderRequest,
    )

    internal data class PendingDeleteEntry(
        val originalIndex: Int,
    )

    constructor(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ) : this(
        application = application,
        savedStateHandle = savedStateHandle,
        settingsRepository = SettingsRepository(application),
        mediaRepository = MediaRepository(application),
        appUpdateRepository = AppUpdateRepository(application),
        imageLoader = ImageLoader.Builder(application)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build(),
    )

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal var loadJob: Job? = null
    internal var updateCheckJob: Job? = null
    internal var queueIds: MutableList<Long> = restoreQueueIds()
    internal val mediaCardCache: MutableMap<Long, MediaCard> = mutableMapOf()

    internal var currentIndex: Int = (savedStateHandle[KEY_CURRENT_INDEX] ?: 0).coerceAtLeast(0)
        set(value) {
            field = value.coerceAtLeast(0)
            savedStateHandle[KEY_CURRENT_INDEX] = field
        }

    internal val pendingDeleteEntries: MutableMap<Long, PendingDeleteEntry> = mutableMapOf()
    internal val pendingSystemDeleteRequests: ArrayDeque<SystemDeleteRequest> = ArrayDeque()
    internal var activeSystemDeleteRequest: SystemDeleteRequest? = null

    internal val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    internal val _permissionMode = MutableStateFlow(
        savedStateHandle[KEY_PERMISSION_MODE] ?: defaultRestoredPermissionMode(),
    )
    val permissionMode: StateFlow<PermissionHelper.PermissionMode> = _permissionMode.asStateFlow()

    internal val _isSwipeDeleteEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] ?: true,
    )
    val isSwipeDeleteEnabled: StateFlow<Boolean> = _isSwipeDeleteEnabled.asStateFlow()

    internal val _isDeleteReminderEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_DELETE_REMINDER_ENABLED]
            ?: SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
    )
    val isDeleteReminderEnabled: StateFlow<Boolean> = _isDeleteReminderEnabled.asStateFlow()

    internal val _swipeGestureSensitivity = MutableStateFlow(
        (savedStateHandle[KEY_SWIPE_GESTURE_SENSITIVITY]
            ?: SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY)
            .coerceIn(
                SettingsRepository.MIN_SWIPE_GESTURE_SENSITIVITY,
                SettingsRepository.MAX_SWIPE_GESTURE_SENSITIVITY,
            ),
    )
    val swipeGestureSensitivity: StateFlow<Float> = _swipeGestureSensitivity.asStateFlow()

    internal val _showFullImage = MutableStateFlow(
        savedStateHandle[KEY_SHOW_FULL_IMAGE] ?: false,
    )
    val showFullImage: StateFlow<Boolean> = _showFullImage.asStateFlow()

    internal val _isTapImageToggleEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_TAP_IMAGE_TOGGLE_ENABLED]
            ?: SettingsRepository.DEFAULT_TAP_IMAGE_TOGGLE_ENABLED,
    )
    val isTapImageToggleEnabled: StateFlow<Boolean> = _isTapImageToggleEnabled.asStateFlow()

    internal val _showFloatingDeleteButton = MutableStateFlow(
        savedStateHandle[KEY_SHOW_FLOATING_DELETE_BUTTON] ?: false,
    )
    val showFloatingDeleteButton: StateFlow<Boolean> = _showFloatingDeleteButton.asStateFlow()

    internal val _isGestureBallEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_GESTURE_BALL_ENABLED] ?: false,
    )
    val isGestureBallEnabled: StateFlow<Boolean> = _isGestureBallEnabled.asStateFlow()

    internal val _gestureBallSizeScale = MutableStateFlow(
        (savedStateHandle[KEY_GESTURE_BALL_SIZE_SCALE] ?: SettingsRepository.DEFAULT_GESTURE_BALL_SIZE_SCALE)
            .coerceIn(
                SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE,
                SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
            ),
    )
    val gestureBallSizeScale: StateFlow<Float> = _gestureBallSizeScale.asStateFlow()

    internal val _isGestureBallFeedbackEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_GESTURE_BALL_FEEDBACK_ENABLED]
            ?: SettingsRepository.DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED,
    )
    val isGestureBallFeedbackEnabled: StateFlow<Boolean> = _isGestureBallFeedbackEnabled.asStateFlow()

    internal val _showGestureBallActionHint = MutableStateFlow(
        savedStateHandle[KEY_SHOW_GESTURE_BALL_ACTION_HINT]
            ?: SettingsRepository.DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED,
    )
    val showGestureBallActionHint: StateFlow<Boolean> = _showGestureBallActionHint.asStateFlow()

    internal val _isSilentDeleteEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_SILENT_DELETE_ENABLED] ?: false,
    )
    val isSilentDeleteEnabled: StateFlow<Boolean> = _isSilentDeleteEnabled.asStateFlow()

    internal val _silentDeleteTreeUris = MutableStateFlow(restoreSilentDeleteTreeUris())
    val silentDeleteTreeUris: StateFlow<List<String>> = _silentDeleteTreeUris.asStateFlow()

    internal var pendingSilentDeleteScope: SilentDeleteScope? = null

    internal val _silentDeleteDirectoryRequests = MutableSharedFlow<SilentDeleteScope>(extraBufferCapacity = 1)
    val silentDeleteDirectoryRequests: SharedFlow<SilentDeleteScope> = _silentDeleteDirectoryRequests.asSharedFlow()

    internal val _appLanguageTag = MutableStateFlow(
        savedStateHandle[KEY_APP_LANGUAGE_TAG] ?: SettingsRepository.SYSTEM_LANGUAGE_TAG,
    )
    val appLanguageTag: StateFlow<String> = _appLanguageTag.asStateFlow()

    internal val _swipeLeftAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_LEFT_ACTION) ?: SettingsRepository.DEFAULT_LEFT_ACTION,
    )
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    internal val _swipeRightAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_RIGHT_ACTION) ?: SettingsRepository.DEFAULT_RIGHT_ACTION,
    )
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    internal val _swipeUpAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_UP_ACTION) ?: SettingsRepository.DEFAULT_UP_ACTION,
    )
    val swipeUpAction: StateFlow<SwipeAction> = _swipeUpAction.asStateFlow()

    internal val _swipeDownAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_DOWN_ACTION) ?: SettingsRepository.DEFAULT_DOWN_ACTION,
    )
    val swipeDownAction: StateFlow<SwipeAction> = _swipeDownAction.asStateFlow()

    internal val _defaultBehaviorNoticeMode = MutableStateFlow(
        restoredDefaultBehaviorNoticeMode(),
    )
    val defaultBehaviorNoticeMode: StateFlow<DefaultBehaviorNoticeMode> =
        _defaultBehaviorNoticeMode.asStateFlow()

    internal val _shouldShowDefaultBehaviorNotice = MutableStateFlow(
        savedStateHandle[KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE] ?: false,
    )
    val shouldShowDefaultBehaviorNotice: StateFlow<Boolean> =
        _shouldShowDefaultBehaviorNotice.asStateFlow()

    internal var hasPreparedDefaultBehaviorNoticeForSession =
        savedStateHandle[KEY_HAS_PREPARED_DEFAULT_BEHAVIOR_NOTICE] ?: false

    internal var pendingUpdatePackagePath: String? = null

    internal val _skippedUpdateVersion = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SKIPPED_UPDATE_VERSION),
    )
    val skippedUpdateVersion: StateFlow<String?> = _skippedUpdateVersion.asStateFlow()

    internal val _availableUpdateRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val availableUpdateRelease: StateFlow<AppReleaseInfo?> = _availableUpdateRelease.asStateFlow()

    internal val _updateCheckFeedback = MutableStateFlow<UpdateCheckFeedback>(UpdateCheckFeedback.Idle)
    val updateCheckFeedback: StateFlow<UpdateCheckFeedback> = _updateCheckFeedback.asStateFlow()

    internal val _isUpdateInstallInProgress = MutableStateFlow(false)
    val isUpdateInstallInProgress: StateFlow<Boolean> = _isUpdateInstallInProgress.asStateFlow()

    internal val _updateInstallRequests = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val updateInstallRequests: SharedFlow<Uri> = _updateInstallRequests.asSharedFlow()

    internal var hasCheckedUpdatesOnLaunch = false

    internal val _deleteRequests = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val deleteRequests: SharedFlow<IntentSenderRequest> = _deleteRequests.asSharedFlow()

    internal val _deleteReminderEvents = MutableSharedFlow<DeleteReminderEvent>(extraBufferCapacity = 1)
    val deleteReminderEvents: SharedFlow<DeleteReminderEvent> = _deleteReminderEvents.asSharedFlow()


    init {
        initializeStateCollectorsImpl()
    }

    fun onPermissionModeChanged(mode: PermissionHelper.PermissionMode) = onPermissionModeChangedImpl(mode)
    fun onPermissionResult(grants: Map<String, Boolean>, sdkInt: Int = Build.VERSION.SDK_INT) = onPermissionResultImpl(grants, sdkInt)
    fun refreshMedia() = refreshMediaImpl()
    fun swipeSkip(imageId: Long): Boolean = swipeSkipImpl(imageId)
    fun swipePrevious(imageId: Long): Boolean = swipePreviousImpl(imageId)
    fun swipeNext(imageId: Long): Boolean = swipeNextImpl(imageId)
    fun swipeDelete(imageId: Long): Boolean = swipeDeleteImpl(imageId)
    fun performSwipeAction(action: SwipeAction, imageId: Long): Boolean = performSwipeActionImpl(action, imageId)
    fun onSystemDeleteCancelled() = onSystemDeleteCancelledImpl()
    fun onSystemDeleteConfirmed() = onSystemDeleteConfirmedImpl()
    internal fun dismissCardForDelete(imageId: Long): Boolean = dismissCardForDeleteImpl(imageId)
    internal fun confirmDismissedDelete(imageId: Long) = confirmDismissedDeleteImpl(imageId)
    internal fun restoreDismissedDelete(imageId: Long) = restoreDismissedDeleteImpl(imageId)
    internal fun enqueueSystemDeleteRequest(imageId: Long, request: IntentSenderRequest) = enqueueSystemDeleteRequestImpl(imageId, request)
    internal fun dispatchNextSystemDeleteRequest() = dispatchNextSystemDeleteRequestImpl()
    internal fun reconcilePendingDeletesWithAvailableIds(availableIds: Set<Long>) = reconcilePendingDeletesWithAvailableIdsImpl(availableIds)
    fun setSwipeDeleteEnabled(enabled: Boolean) = setSwipeDeleteEnabledImpl(enabled)
    fun setDeleteReminderEnabled(enabled: Boolean) = setDeleteReminderEnabledImpl(enabled)
    fun setSwipeGestureSensitivity(sensitivity: Float) = setSwipeGestureSensitivityImpl(sensitivity)
    fun setShowFullImage(enabled: Boolean) = setShowFullImageImpl(enabled)
    fun setTapImageToggleEnabled(enabled: Boolean) = setTapImageToggleEnabledImpl(enabled)
    fun setShowFloatingDeleteButton(enabled: Boolean) = setShowFloatingDeleteButtonImpl(enabled)
    fun setGestureBallEnabled(enabled: Boolean) = setGestureBallEnabledImpl(enabled)
    fun setGestureBallSizeScale(scale: Float) = setGestureBallSizeScaleImpl(scale)
    fun setGestureBallFeedbackEnabled(enabled: Boolean) = setGestureBallFeedbackEnabledImpl(enabled)
    fun setShowGestureBallActionHint(enabled: Boolean) = setShowGestureBallActionHintImpl(enabled)
    fun setSilentDeleteEnabled(enabled: Boolean) = setSilentDeleteEnabledImpl(enabled)
    fun requestSilentDeleteDirectorySelection(scope: SilentDeleteScope? = null) = requestSilentDeleteDirectorySelectionImpl(scope)
    fun onSilentDeleteDirectoryGranted(scope: SilentDeleteScope, treeUri: Uri) = onSilentDeleteDirectoryGrantedImpl(scope, treeUri)
    fun onSilentDeleteDirectoryRequestCancelled(scope: SilentDeleteScope?) = onSilentDeleteDirectoryRequestCancelledImpl(scope)
    fun getSilentDeleteDirectoryLabel(scope: SilentDeleteScope): String? = getSilentDeleteDirectoryLabelImpl(scope)
    fun hasSilentDeleteDirectory(scope: SilentDeleteScope): Boolean = hasSilentDeleteDirectoryImpl(scope)
    internal fun mergeSilentDeleteTreeUris(currentUris: List<String>, newScope: SilentDeleteScope, newTreeUri: String): List<String> = mergeSilentDeleteTreeUrisImpl(currentUris, newScope, newTreeUri)
    internal fun firstMissingSilentDeleteScope(treeUris: List<String>): SilentDeleteScope? = firstMissingSilentDeleteScopeImpl(treeUris)
    internal fun resolveAuthorizedTreeUriForScope(scope: SilentDeleteScope, treeUris: List<String>): Uri? = resolveAuthorizedTreeUriForScopeImpl(scope, treeUris)
    internal fun doesTreeCoverScope(treeUri: Uri, scope: SilentDeleteScope): Boolean = doesTreeCoverScopeImpl(treeUri, scope)
    internal fun isTreeExactScope(treeUri: Uri, scope: SilentDeleteScope): Boolean = isTreeExactScopeImpl(treeUri, scope)
    internal fun restoreSilentDeleteTreeUris(): List<String> = restoreSilentDeleteTreeUrisImpl()
    internal fun saveSilentDeleteTreeUris(treeUris: List<String>) = saveSilentDeleteTreeUrisImpl(treeUris)
    fun setAppLanguageTag(tag: String) = setAppLanguageTagImpl(tag)
    fun setSwipeLeftAction(action: SwipeAction) = setSwipeLeftActionImpl(action)
    fun setSwipeRightAction(action: SwipeAction) = setSwipeRightActionImpl(action)
    fun setSwipeUpAction(action: SwipeAction) = setSwipeUpActionImpl(action)
    fun setSwipeDownAction(action: SwipeAction) = setSwipeDownActionImpl(action)
    fun setDefaultBehaviorNoticeEnabled(enabled: Boolean) = setDefaultBehaviorNoticeEnabledImpl(enabled)
    fun checkForUpdatesOnLaunchIfNeeded() = checkForUpdatesOnLaunchIfNeededImpl()
    fun checkForUpdatesManually() = checkForUpdatesManuallyImpl()
    fun clearUpdateCheckFeedback() = clearUpdateCheckFeedbackImpl()
    fun dismissAvailableUpdatePrompt() = dismissAvailableUpdatePromptImpl()
    fun deferCurrentAvailableUpdate() = deferCurrentAvailableUpdateImpl()
    fun startUpdateInstallation() = startUpdateInstallationImpl()
    fun onUpdateInstallFlowFinished() = onUpdateInstallFlowFinishedImpl()
    fun onUpdateInstallLaunchFailed() = onUpdateInstallLaunchFailedImpl()
    override fun onCleared() {
        onClearedImpl()
        super.onCleared()
    }
    internal fun emitQueueState() = emitQueueStateImpl()
    internal fun preloadUpcomingImages() = preloadUpcomingImagesImpl()
    internal fun updateMediaCardCache(cards: List<MediaCard>) = updateMediaCardCacheImpl(cards)
    internal fun hasCachedCardsForVisibleWindow(): Boolean = hasCachedCardsForVisibleWindowImpl()
    internal fun isTopVisibleId(imageId: Long): Boolean = isTopVisibleIdImpl(imageId)
    internal fun canSwipeToPrevious(): Boolean = canSwipeToPreviousImpl()
    internal fun canSwipeToNext(): Boolean = canSwipeToNextImpl()
    internal fun restoreQueueIds(): MutableList<Long> = restoreQueueIdsImpl()
    internal fun saveQueueIds() = saveQueueIdsImpl()
    internal fun restoredSwipeAction(key: String): SwipeAction? = restoredSwipeActionImpl(key)
    internal fun restoredDefaultBehaviorNoticeMode(): DefaultBehaviorNoticeMode = restoredDefaultBehaviorNoticeModeImpl()
    internal suspend fun prepareDefaultBehaviorNoticeForSessionIfNeeded() = prepareDefaultBehaviorNoticeForSessionIfNeededImpl()
    internal fun checkForUpdates(initiatedByUser: Boolean) = checkForUpdatesImpl(initiatedByUser)
    internal fun handleUpdateCheckResult(release: AppReleaseInfo?, initiatedByUser: Boolean) = handleUpdateCheckResultImpl(release, initiatedByUser)
    internal fun clearPendingDownloadedUpdatePackage() = clearPendingDownloadedUpdatePackageImpl()
    internal fun currentMonthKey(): String = currentMonthKeyImpl()
    internal suspend fun buildSilentDeleteRequest(imageId: Long): IntentHelper.SilentDeleteRequest? = buildSilentDeleteRequestImpl(imageId)
    internal fun resolveAuthorizedTreeUriForEntry(mediaRelativePath: String?, treeUris: List<String>): Uri? = resolveAuthorizedTreeUriForEntryImpl(mediaRelativePath, treeUris)
    internal fun isEntryInsideTree(treeUri: Uri, normalizedMediaPath: String): Boolean = isEntryInsideTreeImpl(treeUri, normalizedMediaPath)
    internal fun createInitialUiState(): HomeUiState = createInitialUiStateImpl()
    internal fun defaultRestoredPermissionMode(): PermissionHelper.PermissionMode = defaultRestoredPermissionModeImpl()
    internal fun resolveDeleteUri(imageId: Long): Uri = resolveDeleteUriImpl(imageId)

    internal companion object {
        const val EXTERNAL_MEDIA_VOLUME = "external"
        const val KEY_QUEUE_IDS = "queue_ids"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_PERMISSION_MODE = "permission_mode"
        const val KEY_IS_SWIPE_DELETE_ENABLED = "is_swipe_delete_enabled"
        const val KEY_IS_DELETE_REMINDER_ENABLED = "is_delete_reminder_enabled"
        const val KEY_SWIPE_GESTURE_SENSITIVITY = "swipe_gesture_sensitivity"
        const val KEY_SHOW_FULL_IMAGE = "show_full_image"
        const val KEY_IS_TAP_IMAGE_TOGGLE_ENABLED = "is_tap_image_toggle_enabled"
        const val KEY_SHOW_FLOATING_DELETE_BUTTON = "show_floating_delete_button"
        const val KEY_IS_GESTURE_BALL_ENABLED = "is_gesture_ball_enabled"
        const val KEY_GESTURE_BALL_SIZE_SCALE = "gesture_ball_size_scale"
        const val KEY_IS_GESTURE_BALL_FEEDBACK_ENABLED = "is_gesture_ball_feedback_enabled"
        const val KEY_SHOW_GESTURE_BALL_ACTION_HINT = "show_gesture_ball_action_hint"
        const val KEY_IS_SILENT_DELETE_ENABLED = "is_silent_delete_enabled"
        const val KEY_SILENT_DELETE_TREE_URIS = "silent_delete_tree_uris"
        const val KEY_SILENT_DELETE_TREE_URI = "silent_delete_tree_uri"
        const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
        const val KEY_SWIPE_LEFT_ACTION = "swipe_left_action"
        const val KEY_SWIPE_RIGHT_ACTION = "swipe_right_action"
        const val KEY_SWIPE_UP_ACTION = "swipe_up_action"
        const val KEY_SWIPE_DOWN_ACTION = "swipe_down_action"
        const val KEY_DEFAULT_BEHAVIOR_NOTICE_MODE = "default_behavior_notice_mode"
        const val KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE = "should_show_default_behavior_notice"
        const val KEY_HAS_PREPARED_DEFAULT_BEHAVIOR_NOTICE = "has_prepared_default_behavior_notice"
        const val KEY_SKIPPED_UPDATE_VERSION = "skipped_update_version"
        const val PRELOAD_BEHIND_COUNT = 3
        const val PRELOAD_AHEAD_COUNT = 6
        const val DEFAULT_BEHAVIOR_NOTICE_MONTHLY_MAX_SHOWN = 5
    }
}

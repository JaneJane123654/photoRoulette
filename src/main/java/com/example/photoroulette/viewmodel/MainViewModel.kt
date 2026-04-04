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
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val imageLoader: ImageLoader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    data class DeleteReminderEvent(
        val deletedImageId: Long,
    )

    private data class SystemDeleteRequest(
        val imageId: Long,
        val request: IntentSenderRequest,
    )

    private data class PendingDeleteEntry(
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var updateCheckJob: Job? = null
    private var queueIds: MutableList<Long> = restoreQueueIds()
    private val mediaCardCache: MutableMap<Long, MediaCard> = mutableMapOf()

    private var currentIndex: Int = (savedStateHandle[KEY_CURRENT_INDEX] ?: 0).coerceAtLeast(0)
        set(value) {
            field = value.coerceAtLeast(0)
            savedStateHandle[KEY_CURRENT_INDEX] = field
        }

    private val pendingDeleteEntries: MutableMap<Long, PendingDeleteEntry> = mutableMapOf()
    private val pendingSystemDeleteRequests: ArrayDeque<SystemDeleteRequest> = ArrayDeque()
    private var activeSystemDeleteRequest: SystemDeleteRequest? = null

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _permissionMode = MutableStateFlow(
        savedStateHandle[KEY_PERMISSION_MODE] ?: defaultRestoredPermissionMode(),
    )
    val permissionMode: StateFlow<PermissionHelper.PermissionMode> = _permissionMode.asStateFlow()

    private val _isSwipeDeleteEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] ?: true,
    )
    val isSwipeDeleteEnabled: StateFlow<Boolean> = _isSwipeDeleteEnabled.asStateFlow()

    private val _isDeleteReminderEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_DELETE_REMINDER_ENABLED]
            ?: SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
    )
    val isDeleteReminderEnabled: StateFlow<Boolean> = _isDeleteReminderEnabled.asStateFlow()

    private val _swipeGestureSensitivity = MutableStateFlow(
        (savedStateHandle[KEY_SWIPE_GESTURE_SENSITIVITY]
            ?: SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY)
            .coerceIn(
                SettingsRepository.MIN_SWIPE_GESTURE_SENSITIVITY,
                SettingsRepository.MAX_SWIPE_GESTURE_SENSITIVITY,
            ),
    )
    val swipeGestureSensitivity: StateFlow<Float> = _swipeGestureSensitivity.asStateFlow()

    private val _showFullImage = MutableStateFlow(
        savedStateHandle[KEY_SHOW_FULL_IMAGE] ?: false,
    )
    val showFullImage: StateFlow<Boolean> = _showFullImage.asStateFlow()

    private val _isTapImageToggleEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_TAP_IMAGE_TOGGLE_ENABLED]
            ?: SettingsRepository.DEFAULT_TAP_IMAGE_TOGGLE_ENABLED,
    )
    val isTapImageToggleEnabled: StateFlow<Boolean> = _isTapImageToggleEnabled.asStateFlow()

    private val _showFloatingDeleteButton = MutableStateFlow(
        savedStateHandle[KEY_SHOW_FLOATING_DELETE_BUTTON] ?: false,
    )
    val showFloatingDeleteButton: StateFlow<Boolean> = _showFloatingDeleteButton.asStateFlow()

    private val _isGestureBallEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_GESTURE_BALL_ENABLED] ?: false,
    )
    val isGestureBallEnabled: StateFlow<Boolean> = _isGestureBallEnabled.asStateFlow()

    private val _gestureBallSizeScale = MutableStateFlow(
        (savedStateHandle[KEY_GESTURE_BALL_SIZE_SCALE] ?: SettingsRepository.DEFAULT_GESTURE_BALL_SIZE_SCALE)
            .coerceIn(
                SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE,
                SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
            ),
    )
    val gestureBallSizeScale: StateFlow<Float> = _gestureBallSizeScale.asStateFlow()

    private val _isGestureBallFeedbackEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_GESTURE_BALL_FEEDBACK_ENABLED]
            ?: SettingsRepository.DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED,
    )
    val isGestureBallFeedbackEnabled: StateFlow<Boolean> = _isGestureBallFeedbackEnabled.asStateFlow()

    private val _showGestureBallActionHint = MutableStateFlow(
        savedStateHandle[KEY_SHOW_GESTURE_BALL_ACTION_HINT]
            ?: SettingsRepository.DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED,
    )
    val showGestureBallActionHint: StateFlow<Boolean> = _showGestureBallActionHint.asStateFlow()

    private val _isSilentDeleteEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_SILENT_DELETE_ENABLED] ?: false,
    )
    val isSilentDeleteEnabled: StateFlow<Boolean> = _isSilentDeleteEnabled.asStateFlow()

    private val _silentDeleteTreeUris = MutableStateFlow(restoreSilentDeleteTreeUris())
    val silentDeleteTreeUris: StateFlow<List<String>> = _silentDeleteTreeUris.asStateFlow()

    private var pendingSilentDeleteScope: SilentDeleteScope? = null

    private val _silentDeleteDirectoryRequests = MutableSharedFlow<SilentDeleteScope>(extraBufferCapacity = 1)
    val silentDeleteDirectoryRequests: SharedFlow<SilentDeleteScope> = _silentDeleteDirectoryRequests.asSharedFlow()

    private val _appLanguageTag = MutableStateFlow(
        savedStateHandle[KEY_APP_LANGUAGE_TAG] ?: SettingsRepository.SYSTEM_LANGUAGE_TAG,
    )
    val appLanguageTag: StateFlow<String> = _appLanguageTag.asStateFlow()

    private val _swipeLeftAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_LEFT_ACTION) ?: SettingsRepository.DEFAULT_LEFT_ACTION,
    )
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_RIGHT_ACTION) ?: SettingsRepository.DEFAULT_RIGHT_ACTION,
    )
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    private val _swipeUpAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_UP_ACTION) ?: SettingsRepository.DEFAULT_UP_ACTION,
    )
    val swipeUpAction: StateFlow<SwipeAction> = _swipeUpAction.asStateFlow()

    private val _swipeDownAction = MutableStateFlow(
        restoredSwipeAction(KEY_SWIPE_DOWN_ACTION) ?: SettingsRepository.DEFAULT_DOWN_ACTION,
    )
    val swipeDownAction: StateFlow<SwipeAction> = _swipeDownAction.asStateFlow()

    private val _defaultBehaviorNoticeMode = MutableStateFlow(
        restoredDefaultBehaviorNoticeMode(),
    )
    val defaultBehaviorNoticeMode: StateFlow<DefaultBehaviorNoticeMode> =
        _defaultBehaviorNoticeMode.asStateFlow()

    private val _shouldShowDefaultBehaviorNotice = MutableStateFlow(
        savedStateHandle[KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE] ?: false,
    )
    val shouldShowDefaultBehaviorNotice: StateFlow<Boolean> =
        _shouldShowDefaultBehaviorNotice.asStateFlow()

    private var hasPreparedDefaultBehaviorNoticeForSession =
        savedStateHandle[KEY_HAS_PREPARED_DEFAULT_BEHAVIOR_NOTICE] ?: false

    private var pendingUpdatePackagePath: String? = null

    private val _skippedUpdateVersion = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SKIPPED_UPDATE_VERSION),
    )
    val skippedUpdateVersion: StateFlow<String?> = _skippedUpdateVersion.asStateFlow()

    private val _availableUpdateRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val availableUpdateRelease: StateFlow<AppReleaseInfo?> = _availableUpdateRelease.asStateFlow()

    private val _updateCheckFeedback = MutableStateFlow<UpdateCheckFeedback>(UpdateCheckFeedback.Idle)
    val updateCheckFeedback: StateFlow<UpdateCheckFeedback> = _updateCheckFeedback.asStateFlow()

    private val _isUpdateInstallInProgress = MutableStateFlow(false)
    val isUpdateInstallInProgress: StateFlow<Boolean> = _isUpdateInstallInProgress.asStateFlow()

    private val _updateInstallRequests = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val updateInstallRequests: SharedFlow<Uri> = _updateInstallRequests.asSharedFlow()

    private var hasCheckedUpdatesOnLaunch = false

    private val _deleteRequests = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val deleteRequests: SharedFlow<IntentSenderRequest> = _deleteRequests.asSharedFlow()

    private val _deleteReminderEvents = MutableSharedFlow<DeleteReminderEvent>(extraBufferCapacity = 1)
    val deleteReminderEvents: SharedFlow<DeleteReminderEvent> = _deleteReminderEvents.asSharedFlow()

    init {
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

    fun onPermissionModeChanged(mode: PermissionHelper.PermissionMode) {
        _permissionMode.value = mode
        savedStateHandle[KEY_PERMISSION_MODE] = mode

        when (mode) {
            PermissionHelper.PermissionMode.DENIED -> {
                loadJob?.cancel()
                _uiState.value = HomeUiState.PermissionDenied
            }

            PermissionHelper.PermissionMode.GRANTED_ALL,
            PermissionHelper.PermissionMode.GRANTED_PARTIAL,
            -> {
                scope.launch(ioDispatcher) {
                    prepareDefaultBehaviorNoticeForSessionIfNeeded()
                }

                if (queueIds.isEmpty() || !hasCachedCardsForVisibleWindow()) {
                    refreshMedia()
                } else {
                    emitQueueState()
                    preloadUpcomingImages()
                }
            }
        }
    }

    fun onPermissionResult(
        grants: Map<String, Boolean>,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ) {
        val mode = when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                (
                    grants[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                        grants[Manifest.permission.READ_MEDIA_VIDEO] == true
                    ) -> {
                PermissionHelper.PermissionMode.GRANTED_ALL
            }

            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                grants[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true -> {
                PermissionHelper.PermissionMode.GRANTED_PARTIAL
            }

            sdkInt >= Build.VERSION_CODES.TIRAMISU &&
                (
                    grants[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                        grants[Manifest.permission.READ_MEDIA_VIDEO] == true
                    ) -> {
                PermissionHelper.PermissionMode.GRANTED_ALL
            }

            sdkInt < Build.VERSION_CODES.TIRAMISU &&
                grants[Manifest.permission.READ_EXTERNAL_STORAGE] == true -> {
                PermissionHelper.PermissionMode.GRANTED_ALL
            }

            else -> PermissionHelper.PermissionMode.DENIED
        }

        onPermissionModeChanged(mode)
    }

    fun refreshMedia() {
        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            _uiState.value = HomeUiState.PermissionDenied
            return
        }

        loadJob?.cancel()
        loadJob = scope.launch(ioDispatcher) {
            val shuffledCards = mediaRepository.getShuffledMediaCards()
            val shuffledIds = shuffledCards.map { card -> card.id }
            val availableIds = shuffledIds.toSet()

            reconcilePendingDeletesWithAvailableIds(availableIds)

            queueIds = shuffledIds
                .filterNot { id -> pendingDeleteEntries.containsKey(id) }
                .toMutableList()
            updateMediaCardCache(shuffledCards)
            saveQueueIds()
            currentIndex = currentIndex.coerceIn(0, queueIds.size)

            emitQueueState()
            preloadUpcomingImages()
        }
    }

    fun swipeSkip(imageId: Long): Boolean {
        return swipeNext(imageId)
    }

    fun swipePrevious(imageId: Long): Boolean {
        if (!isTopVisibleId(imageId) || !canSwipeToPrevious()) {
            return false
        }

        currentIndex -= 1
        emitQueueState()
        preloadUpcomingImages()
        return true
    }

    fun swipeNext(imageId: Long): Boolean {
        if (!isTopVisibleId(imageId) || !canSwipeToNext()) {
            return false
        }

        currentIndex += 1
        emitQueueState()
        preloadUpcomingImages()
        return true
    }

    fun swipeDelete(imageId: Long): Boolean {
        if (!isTopVisibleId(imageId)) {
            return false
        }

        if (!_isSwipeDeleteEnabled.value) {
            return swipeSkip(imageId)
        }

        if (!dismissCardForDelete(imageId)) {
            return false
        }

        scope.launch {
            val application = getApplication<Application>()
            val mediaUri = mediaCardCache[imageId]?.previewUri
                ?: IntentHelper.buildMediaUri(
                    mediaId = imageId,
                    collectionUri = MediaStore.Files.getContentUri(EXTERNAL_MEDIA_VOLUME),
                )

            val result = withContext(ioDispatcher) {
                val silentDeleteRequest = buildSilentDeleteRequest(imageId)
                IntentHelper.prepareDelete(
                    context = application,
                    contentResolver = application.contentResolver,
                    imageUri = mediaUri,
                    silentDeleteRequest = silentDeleteRequest,
                )
            }

            when (
                result
            ) {
                IntentHelper.DeleteRequestResult.Deleted -> confirmDismissedDelete(imageId)
                is IntentHelper.DeleteRequestResult.Failed -> restoreDismissedDelete(imageId)
                is IntentHelper.DeleteRequestResult.LaunchRequest -> {
                    enqueueSystemDeleteRequest(
                        imageId = imageId,
                        request = result.request,
                    )
                }
            }
        }

        return true
    }

    fun performSwipeAction(action: SwipeAction, imageId: Long): Boolean {
        return when (action) {
            SwipeAction.Skip -> false
            SwipeAction.Delete -> swipeDelete(imageId)
            SwipeAction.Previous -> swipePrevious(imageId)
            SwipeAction.Next -> swipeNext(imageId)
        }
    }

    fun onSystemDeleteCancelled() {
        val activeRequest = activeSystemDeleteRequest ?: return
        activeSystemDeleteRequest = null
        restoreDismissedDelete(activeRequest.imageId)
        dispatchNextSystemDeleteRequest()
    }

    fun onSystemDeleteConfirmed() {
        val activeRequest = activeSystemDeleteRequest ?: return
        activeSystemDeleteRequest = null
        confirmDismissedDelete(activeRequest.imageId)
        dispatchNextSystemDeleteRequest()
    }

    private fun dismissCardForDelete(imageId: Long): Boolean {
        val topId = queueIds.getOrNull(currentIndex) ?: return false
        if (topId != imageId || pendingDeleteEntries.containsKey(imageId)) {
            return false
        }

        pendingDeleteEntries[imageId] = PendingDeleteEntry(
            originalIndex = currentIndex,
        )

        queueIds.removeAt(currentIndex)
        currentIndex = currentIndex.coerceAtMost(queueIds.size)
        saveQueueIds()
        emitQueueState()
        preloadUpcomingImages()
        return true
    }

    private fun confirmDismissedDelete(imageId: Long) {
        if (pendingDeleteEntries.remove(imageId) == null) {
            return
        }

        pendingSystemDeleteRequests.removeAll { request -> request.imageId == imageId }
        if (activeSystemDeleteRequest?.imageId == imageId) {
            activeSystemDeleteRequest = null
        }

        if (queueIds.remove(imageId)) {
            currentIndex = currentIndex.coerceAtMost(queueIds.size)
        }

        saveQueueIds()
        mediaCardCache.remove(imageId)
        emitQueueState()
        preloadUpcomingImages()

        if (_isDeleteReminderEnabled.value) {
            _deleteReminderEvents.tryEmit(DeleteReminderEvent(deletedImageId = imageId))
        }
    }

    private fun restoreDismissedDelete(imageId: Long) {
        val pendingEntry = pendingDeleteEntries.remove(imageId) ?: return

        pendingSystemDeleteRequests.removeAll { request -> request.imageId == imageId }
        if (activeSystemDeleteRequest?.imageId == imageId) {
            activeSystemDeleteRequest = null
        }

        if (!queueIds.contains(imageId)) {
            val insertionIndex = pendingEntry.originalIndex.coerceIn(0, queueIds.size)
            queueIds.add(insertionIndex, imageId)

            val updatedIndex = when {
                currentIndex == insertionIndex -> insertionIndex
                currentIndex > insertionIndex -> currentIndex + 1
                else -> currentIndex
            }
            currentIndex = updatedIndex.coerceAtMost(queueIds.size)
        }

        saveQueueIds()
        emitQueueState()
        preloadUpcomingImages()
    }

    private fun enqueueSystemDeleteRequest(
        imageId: Long,
        request: IntentSenderRequest,
    ) {
        if (!pendingDeleteEntries.containsKey(imageId)) {
            return
        }

        pendingSystemDeleteRequests.removeAll { pendingRequest -> pendingRequest.imageId == imageId }
        pendingSystemDeleteRequests.addLast(
            SystemDeleteRequest(
                imageId = imageId,
                request = request,
            ),
        )
        dispatchNextSystemDeleteRequest()
    }

    private fun dispatchNextSystemDeleteRequest() {
        if (activeSystemDeleteRequest != null) {
            return
        }

        while (pendingSystemDeleteRequests.isNotEmpty()) {
            val nextRequest = pendingSystemDeleteRequests.removeFirst()
            if (!pendingDeleteEntries.containsKey(nextRequest.imageId)) {
                continue
            }

            activeSystemDeleteRequest = nextRequest
            if (!_deleteRequests.tryEmit(nextRequest.request)) {
                scope.launch {
                    _deleteRequests.emit(nextRequest.request)
                }
            }
            return
        }
    }

    private fun reconcilePendingDeletesWithAvailableIds(availableIds: Set<Long>) {
        if (pendingDeleteEntries.isEmpty()) {
            return
        }

        val deletedIds = pendingDeleteEntries.keys
            .filterNot { id -> availableIds.contains(id) }
            .toSet()

        if (deletedIds.isEmpty()) {
            return
        }

        deletedIds.forEach { id -> pendingDeleteEntries.remove(id) }
        pendingSystemDeleteRequests.removeAll { request -> deletedIds.contains(request.imageId) }

        val activeRequest = activeSystemDeleteRequest
        if (activeRequest != null && deletedIds.contains(activeRequest.imageId)) {
            activeSystemDeleteRequest = null
            dispatchNextSystemDeleteRequest()
        }
    }

    fun setSwipeDeleteEnabled(enabled: Boolean) {
        _isSwipeDeleteEnabled.value = enabled
        savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeDeleteEnabled(enabled)
        }
    }

    fun setDeleteReminderEnabled(enabled: Boolean) {
        _isDeleteReminderEnabled.value = enabled
        savedStateHandle[KEY_IS_DELETE_REMINDER_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setDeleteReminderEnabled(enabled)
        }
    }

    fun setSwipeGestureSensitivity(sensitivity: Float) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeGestureSensitivity(sensitivity)
        }
    }

    fun setShowFullImage(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowFullImage(enabled)
        }
    }

    fun setTapImageToggleEnabled(enabled: Boolean) {
        _isTapImageToggleEnabled.value = enabled
        savedStateHandle[KEY_IS_TAP_IMAGE_TOGGLE_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setTapImageToggleEnabled(enabled)
        }
    }

    fun setShowFloatingDeleteButton(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowFloatingDeleteButton(enabled)
        }
    }

    fun setGestureBallEnabled(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallEnabled(enabled)
        }
    }

    fun setGestureBallSizeScale(scale: Float) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallSizeScale(scale)
        }
    }

    fun setGestureBallFeedbackEnabled(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallFeedbackEnabled(enabled)
        }
    }

    fun setShowGestureBallActionHint(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowGestureBallActionHint(enabled)
        }
    }

    fun setSilentDeleteEnabled(enabled: Boolean) {
        _isSilentDeleteEnabled.value = enabled
        savedStateHandle[KEY_IS_SILENT_DELETE_ENABLED] = enabled

        if (!enabled) {
            pendingSilentDeleteScope = null
            scope.launch(ioDispatcher) {
                settingsRepository.setSilentDeleteEnabled(false)
            }
            return
        }

        scope.launch(ioDispatcher) {
            settingsRepository.setSilentDeleteEnabled(true)
        }

        val missingScope = firstMissingSilentDeleteScope(_silentDeleteTreeUris.value) ?: return
        pendingSilentDeleteScope = missingScope
        if (!_silentDeleteDirectoryRequests.tryEmit(missingScope)) {
            pendingSilentDeleteScope = null
        }
    }

    fun requestSilentDeleteDirectorySelection(scope: SilentDeleteScope? = null) {
        val targetScope = scope
            ?: firstMissingSilentDeleteScope(_silentDeleteTreeUris.value)
            ?: SilentDeleteScope.Dcim

        pendingSilentDeleteScope = targetScope
        if (!_silentDeleteDirectoryRequests.tryEmit(targetScope)) {
            pendingSilentDeleteScope = null
        }
    }

    fun onSilentDeleteDirectoryGranted(scope: SilentDeleteScope, treeUri: Uri) {
        pendingSilentDeleteScope = null
        this.scope.launch(ioDispatcher) {
            val mergedTreeUris = mergeSilentDeleteTreeUris(
                currentUris = _silentDeleteTreeUris.value,
                newScope = scope,
                newTreeUri = treeUri.toString(),
            )
            settingsRepository.setSilentDeleteTreeUris(mergedTreeUris)
            settingsRepository.setSilentDeleteEnabled(true)

            val nextScope = firstMissingSilentDeleteScope(mergedTreeUris)
            if (nextScope != null) {
                pendingSilentDeleteScope = nextScope
                _silentDeleteDirectoryRequests.emit(nextScope)
            }
        }
    }

    fun onSilentDeleteDirectoryRequestCancelled(scope: SilentDeleteScope?) {
        if (scope != null && pendingSilentDeleteScope == scope) {
            pendingSilentDeleteScope = null
        }
    }

    fun getSilentDeleteDirectoryLabel(scope: SilentDeleteScope): String? {
        val treeUri = resolveAuthorizedTreeUriForScope(scope, _silentDeleteTreeUris.value)
            ?: return null

        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return null

        return treeDocumentId.substringAfter(':', treeDocumentId).ifBlank { treeDocumentId }
    }

    fun hasSilentDeleteDirectory(scope: SilentDeleteScope): Boolean {
        return resolveAuthorizedTreeUriForScope(scope, _silentDeleteTreeUris.value) != null
    }

    private fun mergeSilentDeleteTreeUris(
        currentUris: List<String>,
        newScope: SilentDeleteScope,
        newTreeUri: String,
    ): List<String> {
        if (newTreeUri.isBlank()) {
            return currentUris
        }

        val retainedUris = currentUris.filterNot { uriString ->
            val treeUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@filterNot false
            isTreeExactScope(treeUri, newScope)
        }

        return (retainedUris + newTreeUri).distinct()
    }

    private fun firstMissingSilentDeleteScope(treeUris: List<String>): SilentDeleteScope? {
        return SilentDeleteScope.entries.firstOrNull { scope ->
            resolveAuthorizedTreeUriForScope(scope, treeUris) == null
        }
    }

    private fun resolveAuthorizedTreeUriForScope(
        scope: SilentDeleteScope,
        treeUris: List<String>,
    ): Uri? {
        return treeUris
            .asSequence()
            .mapNotNull { treeUriString -> runCatching { Uri.parse(treeUriString) }.getOrNull() }
            .firstOrNull { treeUri -> doesTreeCoverScope(treeUri, scope) }
    }

    private fun doesTreeCoverScope(treeUri: Uri, scope: SilentDeleteScope): Boolean {
        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return false

        val treeDirectoryPath = IntentHelper.normalizeDirectoryPath(
            treeDocumentId.substringAfter(':', ""),
        )

        if (treeDirectoryPath.isNullOrBlank()) {
            return true
        }

        return treeDirectoryPath.equals(scope.directoryName, ignoreCase = true)
    }

    private fun isTreeExactScope(treeUri: Uri, scope: SilentDeleteScope): Boolean {
        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return false

        val treeDirectoryPath = IntentHelper.normalizeDirectoryPath(
            treeDocumentId.substringAfter(':', ""),
        ) ?: return false

        return treeDirectoryPath.equals(scope.directoryName, ignoreCase = true)
    }

    private fun restoreSilentDeleteTreeUris(): List<String> {
        val restoredList = savedStateHandle
            .get<ArrayList<String>>(KEY_SILENT_DELETE_TREE_URIS)
            ?.mapNotNull { uri -> uri.takeIf { it.isNotBlank() } }
            ?.distinct()
            .orEmpty()

        if (restoredList.isNotEmpty()) {
            return restoredList
        }

        val legacyUri = savedStateHandle
            .get<String>(KEY_SILENT_DELETE_TREE_URI)
            ?.takeIf { it.isNotBlank() }

        return legacyUri?.let(::listOf).orEmpty()
    }

    private fun saveSilentDeleteTreeUris(treeUris: List<String>) {
        val normalizedUris = treeUris
            .mapNotNull { uri -> uri.takeIf { it.isNotBlank() } }
            .distinct()

        savedStateHandle[KEY_SILENT_DELETE_TREE_URIS] = ArrayList(normalizedUris)
        savedStateHandle[KEY_SILENT_DELETE_TREE_URI] = normalizedUris.firstOrNull()
    }

    fun setAppLanguageTag(tag: String) {
        scope.launch(ioDispatcher) {
            settingsRepository.setAppLanguageTag(tag)
        }
    }

    fun setSwipeLeftAction(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeLeftAction(action)
        }
    }

    fun setSwipeRightAction(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeRightAction(action)
        }
    }

    fun setSwipeUpAction(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeUpAction(action)
        }
    }

    fun setSwipeDownAction(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeDownAction(action)
        }
    }

    fun setDefaultBehaviorNoticeEnabled(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setDefaultBehaviorNoticeEnabled(enabled)
            _shouldShowDefaultBehaviorNotice.value = enabled
            savedStateHandle[KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE] = enabled
        }
    }

    fun checkForUpdatesOnLaunchIfNeeded() {
        if (hasCheckedUpdatesOnLaunch) {
            return
        }
        hasCheckedUpdatesOnLaunch = true
        checkForUpdates(initiatedByUser = false)
    }

    fun checkForUpdatesManually() {
        checkForUpdates(initiatedByUser = true)
    }

    fun clearUpdateCheckFeedback() {
        _updateCheckFeedback.value = UpdateCheckFeedback.Idle
    }

    fun dismissAvailableUpdatePrompt() {
        _availableUpdateRelease.value = null
    }

    fun deferCurrentAvailableUpdate() {
        val release = _availableUpdateRelease.value ?: return
        val deferredVersion = release.normalizedVersion
        scope.launch(ioDispatcher) {
            settingsRepository.setSkippedUpdateVersion(deferredVersion)
        }
        _updateCheckFeedback.value = UpdateCheckFeedback.DeferredUntilNewer(deferredVersion)
        _availableUpdateRelease.value = null
    }

    fun startUpdateInstallation() {
        val release = _availableUpdateRelease.value ?: return
        scope.launch(ioDispatcher) {
            try {
                _isUpdateInstallInProgress.value = true
                val downloadedApk = appUpdateRepository.downloadReleaseApk(release)
                pendingUpdatePackagePath = downloadedApk.absolutePath
                val context = getApplication<Application>()
                val installUri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    downloadedApk,
                )
                _updateInstallRequests.emit(installUri)
                _availableUpdateRelease.value = null
            } catch (_: Throwable) {
                clearPendingDownloadedUpdatePackage()
                _updateCheckFeedback.value = UpdateCheckFeedback.Failed()
            } finally {
                _isUpdateInstallInProgress.value = false
            }
        }
    }

    fun onUpdateInstallFlowFinished() {
        clearPendingDownloadedUpdatePackage()
    }

    fun onUpdateInstallLaunchFailed() {
        clearPendingDownloadedUpdatePackage()
        _updateCheckFeedback.value = UpdateCheckFeedback.Failed()
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private fun emitQueueState() {
        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            _uiState.value = HomeUiState.PermissionDenied
            return
        }

        val visibleCards = queueIds
            .drop(currentIndex)
            .take(HomeUiState.MAX_VISIBLE_CARD_COUNT)
            .mapNotNull { id -> mediaCardCache[id] }

        _uiState.value = if (visibleCards.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Ready(
                visibleCards = visibleCards,
                canSwipeToPrevious = canSwipeToPrevious(),
                canSwipeToNext = canSwipeToNext(),
            )
        }
    }

    private fun preloadUpcomingImages() {
        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            return
        }

        val preloadCards = queueIds
            .drop(currentIndex + HomeUiState.MAX_VISIBLE_CARD_COUNT)
            .take(PRELOAD_AHEAD_COUNT)
            .mapNotNull { id -> mediaCardCache[id] }

        preloadCards.forEach { card ->
            val requestBuilder = ImageRequest.Builder(getApplication<Application>())
                .data(card.previewUri)

            if (card.isVideoLike) {
                requestBuilder.videoFrameMillis(0)
            }

            imageLoader.enqueue(
                requestBuilder.build(),
            )
        }
    }

    private fun updateMediaCardCache(cards: List<MediaCard>) {
        mediaCardCache.clear()
        cards.forEach { card ->
            mediaCardCache[card.id] = card
        }
    }

    private fun hasCachedCardsForVisibleWindow(): Boolean {
        val visibleIds = queueIds
            .drop(currentIndex)
            .take(HomeUiState.MAX_VISIBLE_CARD_COUNT)

        return visibleIds.isNotEmpty() && visibleIds.all { id -> mediaCardCache.containsKey(id) }
    }

    private fun isTopVisibleId(imageId: Long): Boolean = queueIds.getOrNull(currentIndex) == imageId

    private fun canSwipeToPrevious(): Boolean = currentIndex > 0

    private fun canSwipeToNext(): Boolean = currentIndex + 1 < queueIds.size

    private fun restoreQueueIds(): MutableList<Long> {
        return savedStateHandle.get<LongArray>(KEY_QUEUE_IDS)?.toMutableList() ?: mutableListOf()
    }

    private fun saveQueueIds() {
        savedStateHandle[KEY_QUEUE_IDS] = queueIds.toLongArray()
    }

    private fun restoredSwipeAction(key: String): SwipeAction? {
        return SwipeAction.fromStorageValue(savedStateHandle[key])
    }

    private fun restoredDefaultBehaviorNoticeMode(): DefaultBehaviorNoticeMode {
        return DefaultBehaviorNoticeMode.fromStorageValue(
            savedStateHandle[KEY_DEFAULT_BEHAVIOR_NOTICE_MODE],
        ) ?: DefaultBehaviorNoticeMode.Visible
    }

    private suspend fun prepareDefaultBehaviorNoticeForSessionIfNeeded() {
        if (hasPreparedDefaultBehaviorNoticeForSession) {
            return
        }

        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            return
        }

        val shouldShowNotice = settingsRepository.prepareDefaultBehaviorNoticeForSession(
            currentMonthKey = currentMonthKey(),
            monthlyDisplayLimit = DEFAULT_BEHAVIOR_NOTICE_MONTHLY_MAX_SHOWN,
        )
        hasPreparedDefaultBehaviorNoticeForSession = true
        savedStateHandle[KEY_HAS_PREPARED_DEFAULT_BEHAVIOR_NOTICE] = true
        _shouldShowDefaultBehaviorNotice.value = shouldShowNotice
        savedStateHandle[KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE] = shouldShowNotice
    }

    private fun checkForUpdates(
        initiatedByUser: Boolean,
    ) {
        updateCheckJob?.cancel()
        updateCheckJob = scope.launch(ioDispatcher) {
            _updateCheckFeedback.value = UpdateCheckFeedback.Checking

            runCatching {
                appUpdateRepository.fetchLatestRelease()
            }.onSuccess { release ->
                handleUpdateCheckResult(
                    release = release,
                    initiatedByUser = initiatedByUser,
                )
            }.onFailure {
                _availableUpdateRelease.value = null
                _updateCheckFeedback.value = if (initiatedByUser) {
                    UpdateCheckFeedback.Failed()
                } else {
                    UpdateCheckFeedback.Idle
                }
            }
        }
    }

    private fun handleUpdateCheckResult(
        release: AppReleaseInfo?,
        initiatedByUser: Boolean,
    ) {
        if (release == null) {
            _availableUpdateRelease.value = null
            _updateCheckFeedback.value = if (initiatedByUser) {
                UpdateCheckFeedback.UpToDate
            } else {
                UpdateCheckFeedback.Idle
            }
            return
        }

        if (!VersionNameUtils.isNewer(release.normalizedVersion, BuildConfig.VERSION_NAME)) {
            _availableUpdateRelease.value = null
            _updateCheckFeedback.value = if (initiatedByUser) {
                UpdateCheckFeedback.UpToDate
            } else {
                UpdateCheckFeedback.Idle
            }
            if (_skippedUpdateVersion.value != null) {
                scope.launch(ioDispatcher) {
                    settingsRepository.setSkippedUpdateVersion(null)
                }
            }
            return
        }

        val skippedVersion = _skippedUpdateVersion.value
        val isStillDeferred = skippedVersion != null &&
            VersionNameUtils.compare(release.normalizedVersion, skippedVersion) <= 0

        if (isStillDeferred) {
            _availableUpdateRelease.value = null
            _updateCheckFeedback.value = if (initiatedByUser) {
                UpdateCheckFeedback.DeferredUntilNewer(skippedVersion)
            } else {
                UpdateCheckFeedback.Idle
            }
            return
        }

        if (skippedVersion != null &&
            VersionNameUtils.compare(release.normalizedVersion, skippedVersion) > 0
        ) {
            scope.launch(ioDispatcher) {
                settingsRepository.setSkippedUpdateVersion(null)
            }
        }

        _availableUpdateRelease.value = release
        _updateCheckFeedback.value = UpdateCheckFeedback.Idle
    }

    private fun clearPendingDownloadedUpdatePackage() {
        pendingUpdatePackagePath?.let { path ->
            runCatching {
                val targetFile = File(path)
                if (targetFile.exists() && targetFile.name.startsWith("update-") && targetFile.name.endsWith(".apk")) {
                    targetFile.delete()
                }
            }
        }

        pendingUpdatePackagePath = null
        runCatching {
            appUpdateRepository.cleanupDownloadedApkPackages()
        }
    }

    private fun currentMonthKey(): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    private suspend fun buildSilentDeleteRequest(imageId: Long): IntentHelper.SilentDeleteRequest? {
        if (!_isSilentDeleteEnabled.value) {
            return null
        }

        val mediaEntry = mediaRepository.getSilentDeleteEntry(imageId) ?: return null
        val treeUri = resolveAuthorizedTreeUriForEntry(
            mediaRelativePath = mediaEntry.relativePath,
            treeUris = _silentDeleteTreeUris.value,
        ) ?: return null

        return IntentHelper.SilentDeleteRequest(
            treeUri = treeUri,
            displayName = mediaEntry.displayName,
            relativePath = mediaEntry.relativePath,
        )
    }

    private fun resolveAuthorizedTreeUriForEntry(
        mediaRelativePath: String?,
        treeUris: List<String>,
    ): Uri? {
        val normalizedMediaPath = IntentHelper.normalizeDirectoryPath(mediaRelativePath) ?: return null

        return treeUris
            .asSequence()
            .mapNotNull { treeUriString -> runCatching { Uri.parse(treeUriString) }.getOrNull() }
            .firstOrNull { treeUri ->
                isEntryInsideTree(treeUri = treeUri, normalizedMediaPath = normalizedMediaPath)
            }
    }

    private fun isEntryInsideTree(
        treeUri: Uri,
        normalizedMediaPath: String,
    ): Boolean {
        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return false

        val treeDirectoryPath = IntentHelper.normalizeDirectoryPath(
            treeDocumentId.substringAfter(':', ""),
        )

        if (treeDirectoryPath.isNullOrBlank()) {
            return true
        }

        if (normalizedMediaPath.equals(treeDirectoryPath, ignoreCase = true)) {
            return true
        }

        return normalizedMediaPath.startsWith("$treeDirectoryPath/", ignoreCase = true)
    }

    private fun createInitialUiState(): HomeUiState = when {
        queueIds.isNotEmpty() && currentIndex < queueIds.size && hasCachedCardsForVisibleWindow() -> {
            HomeUiState.Ready(
                visibleCards = queueIds
                    .drop(currentIndex)
                    .take(HomeUiState.MAX_VISIBLE_CARD_COUNT)
                    .mapNotNull { id -> mediaCardCache[id] },
                canSwipeToPrevious = canSwipeToPrevious(),
                canSwipeToNext = canSwipeToNext(),
            )
        }

        queueIds.isNotEmpty() -> HomeUiState.Loading
        pendingDeleteEntries.isNotEmpty() -> HomeUiState.Empty
        else -> HomeUiState.Loading
    }

    private fun defaultRestoredPermissionMode(): PermissionHelper.PermissionMode = when {
        queueIds.isNotEmpty() || pendingDeleteEntries.isNotEmpty() -> PermissionHelper.PermissionMode.GRANTED_ALL
        else -> PermissionHelper.PermissionMode.DENIED
    }

    private companion object {
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
        const val PRELOAD_AHEAD_COUNT = 6
        const val DEFAULT_BEHAVIOR_NOTICE_MONTHLY_MAX_SHOWN = 5
    }
}

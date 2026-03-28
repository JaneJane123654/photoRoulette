package com.example.photoroulette.viewmodel

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.photoroulette.data.datastore.SettingsRepository
import com.example.photoroulette.data.media.MediaRepository
import com.example.photoroulette.utils.IntentHelper
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.viewmodel.states.HomeUiState
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

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val imageLoader: ImageLoader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    constructor(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ) : this(
        application = application,
        savedStateHandle = savedStateHandle,
        settingsRepository = SettingsRepository(application),
        mediaRepository = MediaRepository(application),
        imageLoader = ImageLoader(application),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null
    private var queueIds: MutableList<Long> = restoreQueueIds()

    private var currentIndex: Int = (savedStateHandle[KEY_CURRENT_INDEX] ?: 0).coerceAtLeast(0)
        set(value) {
            field = value.coerceAtLeast(0)
            savedStateHandle[KEY_CURRENT_INDEX] = field
        }

    private var pendingDeleteId: Long? = savedStateHandle[KEY_PENDING_DELETE_ID]
        set(value) {
            field = value
            savedStateHandle[KEY_PENDING_DELETE_ID] = value
        }

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _permissionMode = MutableStateFlow(
        savedStateHandle[KEY_PERMISSION_MODE] ?: defaultRestoredPermissionMode(),
    )
    val permissionMode: StateFlow<PermissionHelper.PermissionMode> = _permissionMode.asStateFlow()

    private val _isSwipeDeleteEnabled = MutableStateFlow(
        savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] ?: false,
    )
    val isSwipeDeleteEnabled: StateFlow<Boolean> = _isSwipeDeleteEnabled.asStateFlow()

    private val _appLanguageTag = MutableStateFlow(
        savedStateHandle[KEY_APP_LANGUAGE_TAG] ?: SettingsRepository.SYSTEM_LANGUAGE_TAG,
    )
    val appLanguageTag: StateFlow<String> = _appLanguageTag.asStateFlow()

    private val _deleteRequests = MutableSharedFlow<IntentSenderRequest>(extraBufferCapacity = 1)
    val deleteRequests: SharedFlow<IntentSenderRequest> = _deleteRequests.asSharedFlow()

    init {
        scope.launch {
            settingsRepository.isSwipeDeleteEnabled.collect { enabled ->
                _isSwipeDeleteEnabled.value = enabled
                savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] = enabled
            }
        }

        scope.launch {
            settingsRepository.appLanguageTag.collect { tag ->
                _appLanguageTag.value = tag
                savedStateHandle[KEY_APP_LANGUAGE_TAG] = tag
            }
        }

        if (_permissionMode.value != PermissionHelper.PermissionMode.DENIED && queueIds.isNotEmpty()) {
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
                if (queueIds.isEmpty()) {
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
                grants[Manifest.permission.READ_MEDIA_IMAGES] == true -> {
                PermissionHelper.PermissionMode.GRANTED_ALL
            }

            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                grants[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true -> {
                PermissionHelper.PermissionMode.GRANTED_PARTIAL
            }

            sdkInt >= Build.VERSION_CODES.TIRAMISU &&
                grants[Manifest.permission.READ_MEDIA_IMAGES] == true -> {
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
            val shuffledIds = mediaRepository.getShuffledMediaIds()

            queueIds = shuffledIds.toMutableList()
            saveQueueIds()
            currentIndex = currentIndex.coerceIn(0, queueIds.size)

            if (pendingDeleteId != null && pendingDeleteId !in queueIds) {
                pendingDeleteId = null
            }

            emitQueueState()
            preloadUpcomingImages()
        }
    }

    fun swipeSkip(imageId: Long) {
        swipeNext(imageId)
    }

    fun swipePrevious(imageId: Long) {
        if (pendingDeleteId != null || !isTopVisibleId(imageId) || !canSwipeToPrevious()) {
            return
        }

        currentIndex -= 1
        emitQueueState()
        preloadUpcomingImages()
    }

    fun swipeNext(imageId: Long) {
        if (pendingDeleteId != null || !isTopVisibleId(imageId) || !canSwipeToNext()) {
            return
        }

        currentIndex += 1
        emitQueueState()
        preloadUpcomingImages()
    }

    fun swipeDelete(imageId: Long) {
        if (!isTopVisibleId(imageId)) {
            return
        }

        if (!_isSwipeDeleteEnabled.value) {
            swipeSkip(imageId)
            return
        }

        if (pendingDeleteId != null) {
            return
        }

        pendingDeleteId = imageId
        currentIndex += 1
        emitQueueState()
        preloadUpcomingImages()

        scope.launch(ioDispatcher) {
            when (val result = IntentHelper.prepareDelete(getApplication<Application>().contentResolver, imageId)) {
                IntentHelper.DeleteRequestResult.Deleted -> onSystemDeleteConfirmed()
                is IntentHelper.DeleteRequestResult.Failed -> rewindDismissedDelete()
                is IntentHelper.DeleteRequestResult.LaunchRequest -> _deleteRequests.emit(result.request)
            }
        }
    }

    fun onSystemDeleteCancelled() {
        rewindDismissedDelete()
    }

    fun rewindDismissedDelete() {
        val dismissedId = pendingDeleteId ?: return
        if (currentIndex > 0 && queueIds.getOrNull(currentIndex - 1) == dismissedId) {
            currentIndex -= 1
        } else if (!queueIds.contains(dismissedId)) {
            queueIds.add(currentIndex.coerceAtMost(queueIds.size), dismissedId)
            saveQueueIds()
        }

        pendingDeleteId = null
        emitQueueState()
        preloadUpcomingImages()
    }

    fun onSystemDeleteConfirmed() {
        val deletedId = pendingDeleteId ?: return

        if (currentIndex > 0 && queueIds.getOrNull(currentIndex - 1) == deletedId) {
            queueIds.removeAt(currentIndex - 1)
            currentIndex -= 1
            saveQueueIds()
        } else if (queueIds.remove(deletedId)) {
            currentIndex = currentIndex.coerceAtMost(queueIds.size)
            saveQueueIds()
        }

        pendingDeleteId = null
        emitQueueState()
        preloadUpcomingImages()
    }

    fun setSwipeDeleteEnabled(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeDeleteEnabled(enabled)
        }
    }

    fun setAppLanguageTag(tag: String) {
        scope.launch(ioDispatcher) {
            settingsRepository.setAppLanguageTag(tag)
        }
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

        val visibleIds = queueIds
            .drop(currentIndex)
            .take(HomeUiState.MAX_VISIBLE_CARD_COUNT)

        _uiState.value = if (visibleIds.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Ready(
                visibleIds = visibleIds,
                canSwipeToPrevious = canSwipeToPrevious(),
                canSwipeToNext = canSwipeToNext(),
            )
        }
    }

    private fun preloadUpcomingImages() {
        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            return
        }

        val preloadIds = queueIds
            .drop(currentIndex + HomeUiState.MAX_VISIBLE_CARD_COUNT)
            .take(PRELOAD_AHEAD_COUNT)

        preloadIds.forEach { imageId ->
            imageLoader.enqueue(
                ImageRequest.Builder(getApplication<Application>())
                    .data(IntentHelper.buildImageUri(imageId))
                    .build(),
            )
        }
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

    private fun createInitialUiState(): HomeUiState = when {
        queueIds.isNotEmpty() && currentIndex < queueIds.size -> {
            HomeUiState.Ready(
                visibleIds = queueIds
                    .drop(currentIndex)
                    .take(HomeUiState.MAX_VISIBLE_CARD_COUNT),
                canSwipeToPrevious = canSwipeToPrevious(),
                canSwipeToNext = canSwipeToNext(),
            )
        }

        queueIds.isNotEmpty() || pendingDeleteId != null -> HomeUiState.Empty
        else -> HomeUiState.Loading
    }

    private fun defaultRestoredPermissionMode(): PermissionHelper.PermissionMode = when {
        queueIds.isNotEmpty() || pendingDeleteId != null -> PermissionHelper.PermissionMode.GRANTED_ALL
        else -> PermissionHelper.PermissionMode.DENIED
    }

    private companion object {
        const val KEY_QUEUE_IDS = "queue_ids"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_PENDING_DELETE_ID = "pending_delete_id"
        const val KEY_PERMISSION_MODE = "permission_mode"
        const val KEY_IS_SWIPE_DELETE_ENABLED = "is_swipe_delete_enabled"
        const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
        const val PRELOAD_AHEAD_COUNT = 6
    }
}

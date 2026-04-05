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
import com.example.photoroulette.viewmodel.MainViewModel.DeleteReminderEvent
import com.example.photoroulette.viewmodel.MainViewModel.PendingDeleteEntry
import com.example.photoroulette.viewmodel.MainViewModel.SystemDeleteRequest

internal fun MainViewModel.onPermissionModeChangedImpl(mode: PermissionHelper.PermissionMode) {
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

internal fun MainViewModel.onPermissionResultImpl(grants: Map<String, Boolean>, sdkInt: Int = Build.VERSION.SDK_INT) {
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

internal fun MainViewModel.refreshMediaImpl() {
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

internal fun MainViewModel.swipeSkipImpl(imageId: Long): Boolean {
        return swipeNext(imageId)
    }

internal fun MainViewModel.swipePreviousImpl(imageId: Long): Boolean {
        if (!isTopVisibleId(imageId) || !canSwipeToPrevious()) {
            return false
        }

        currentIndex -= 1
        emitQueueState()
        preloadUpcomingImages()
        return true
    }

internal fun MainViewModel.swipeNextImpl(imageId: Long): Boolean {
        if (!isTopVisibleId(imageId) || !canSwipeToNext()) {
            return false
        }

        currentIndex += 1
        emitQueueState()
        preloadUpcomingImages()
        return true
    }

internal fun MainViewModel.swipeDeleteImpl(imageId: Long): Boolean {
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
            val mediaUri = resolveDeleteUri(imageId)

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

internal fun MainViewModel.performSwipeActionImpl(action: SwipeAction, imageId: Long): Boolean {
        return when (action) {
            SwipeAction.Skip -> false
            SwipeAction.Delete -> swipeDelete(imageId)
            SwipeAction.Previous -> swipePrevious(imageId)
            SwipeAction.Next -> swipeNext(imageId)
        }
    }

internal fun MainViewModel.onSystemDeleteCancelledImpl() {
        val activeRequest = activeSystemDeleteRequest ?: return
        activeSystemDeleteRequest = null
        restoreDismissedDelete(activeRequest.imageId)
        dispatchNextSystemDeleteRequest()
    }

internal fun MainViewModel.onSystemDeleteConfirmedImpl() {
        val activeRequest = activeSystemDeleteRequest ?: return
        activeSystemDeleteRequest = null
        confirmDismissedDelete(activeRequest.imageId)
        dispatchNextSystemDeleteRequest()
    }

internal fun MainViewModel.dismissCardForDeleteImpl(imageId: Long): Boolean {
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

internal fun MainViewModel.confirmDismissedDeleteImpl(imageId: Long) {
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

internal fun MainViewModel.restoreDismissedDeleteImpl(imageId: Long) {
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

internal fun MainViewModel.enqueueSystemDeleteRequestImpl(imageId: Long, request: IntentSenderRequest) {
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

internal fun MainViewModel.dispatchNextSystemDeleteRequestImpl() {
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

internal fun MainViewModel.reconcilePendingDeletesWithAvailableIdsImpl(availableIds: Set<Long>) {
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


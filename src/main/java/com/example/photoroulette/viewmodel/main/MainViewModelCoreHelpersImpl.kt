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

private const val PRELOAD_ANIMATED_SIZE_PX = 1080
private const val PRELOAD_VIDEO_FRAME_MS = 80L

internal fun MainViewModel.emitQueueStateImpl() {
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
                previousCard = queueIds
                    .getOrNull(currentIndex - 1)
                    ?.let { id -> mediaCardCache[id] },
                visibleCards = visibleCards,
                canSwipeToPrevious = canSwipeToPrevious(),
                canSwipeToNext = canSwipeToNext(),
            )
        }
    }

internal fun MainViewModel.preloadUpcomingImagesImpl() {
        if (_permissionMode.value == PermissionHelper.PermissionMode.DENIED) {
            return
        }

        if (queueIds.isEmpty()) {
            return
        }

        val preloadStartIndex = (currentIndex - PRELOAD_BEHIND_COUNT).coerceAtLeast(0)
        val preloadEndExclusive = (
            currentIndex + HomeUiState.MAX_VISIBLE_CARD_COUNT + PRELOAD_AHEAD_COUNT
        ).coerceAtMost(queueIds.size)

        if (preloadStartIndex >= preloadEndExclusive) {
            return
        }

        val preloadCards = queueIds
            .subList(preloadStartIndex, preloadEndExclusive)
            .mapNotNull { id -> mediaCardCache[id] }

        preloadCards.forEach { card ->
            val requestBuilder = ImageRequest.Builder(getApplication<Application>())
                .data(card.previewUri)

            if (card.isVideoLike) {
                requestBuilder.videoFrameMillis(PRELOAD_VIDEO_FRAME_MS)
            }

            if (card.kind == MediaKind.AnimatedImage) {
                requestBuilder.size(PRELOAD_ANIMATED_SIZE_PX, PRELOAD_ANIMATED_SIZE_PX)
            }

            imageLoader.enqueue(
                requestBuilder.build(),
            )
        }
    }

internal fun MainViewModel.updateMediaCardCacheImpl(cards: List<MediaCard>) {
        mediaCardCache.clear()
        cards.forEach { card ->
            mediaCardCache[card.id] = card
        }
    }

internal fun MainViewModel.hasCachedCardsForVisibleWindowImpl(): Boolean {
        val visibleIds = queueIds
            .drop(currentIndex)
            .take(HomeUiState.MAX_VISIBLE_CARD_COUNT)

        return visibleIds.isNotEmpty() && visibleIds.all { id -> mediaCardCache.containsKey(id) }
    }

internal fun MainViewModel.isTopVisibleIdImpl(imageId: Long): Boolean = queueIds.getOrNull(currentIndex) == imageId

internal fun MainViewModel.canSwipeToPreviousImpl(): Boolean = currentIndex > 0

internal fun MainViewModel.canSwipeToNextImpl(): Boolean = currentIndex + 1 < queueIds.size

internal fun MainViewModel.restoreQueueIdsImpl(): MutableList<Long> {
        return savedStateHandle.get<LongArray>(KEY_QUEUE_IDS)?.toMutableList() ?: mutableListOf()
    }

internal fun MainViewModel.saveQueueIdsImpl() {
        savedStateHandle[KEY_QUEUE_IDS] = queueIds.toLongArray()
    }

internal fun MainViewModel.restoredSwipeActionImpl(key: String): SwipeAction? {
        return SwipeAction.fromStorageValue(savedStateHandle[key])
    }

internal fun MainViewModel.restoredDefaultBehaviorNoticeModeImpl(): DefaultBehaviorNoticeMode {
        return DefaultBehaviorNoticeMode.fromStorageValue(
            savedStateHandle[KEY_DEFAULT_BEHAVIOR_NOTICE_MODE],
        ) ?: DefaultBehaviorNoticeMode.Visible
    }

internal suspend fun MainViewModel.prepareDefaultBehaviorNoticeForSessionIfNeededImpl() {
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

internal fun MainViewModel.checkForUpdatesImpl(initiatedByUser: Boolean) {
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

internal fun MainViewModel.handleUpdateCheckResultImpl(release: AppReleaseInfo?, initiatedByUser: Boolean) {
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

internal fun MainViewModel.clearPendingDownloadedUpdatePackageImpl() {
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

internal fun MainViewModel.currentMonthKeyImpl(): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

internal suspend fun MainViewModel.buildSilentDeleteRequestImpl(imageId: Long): IntentHelper.SilentDeleteRequest? {
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

internal fun MainViewModel.resolveAuthorizedTreeUriForEntryImpl(mediaRelativePath: String?, treeUris: List<String>): Uri? {
        val normalizedMediaPath = IntentHelper.normalizeDirectoryPath(mediaRelativePath) ?: return null

        return treeUris
            .asSequence()
            .mapNotNull { treeUriString -> runCatching { Uri.parse(treeUriString) }.getOrNull() }
            .firstOrNull { treeUri ->
                isEntryInsideTree(treeUri = treeUri, normalizedMediaPath = normalizedMediaPath)
            }
    }

internal fun MainViewModel.isEntryInsideTreeImpl(treeUri: Uri, normalizedMediaPath: String): Boolean {
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

internal fun MainViewModel.createInitialUiStateImpl(): HomeUiState = when {
        queueIds.isNotEmpty() && currentIndex < queueIds.size && hasCachedCardsForVisibleWindow() -> {
            HomeUiState.Ready(
                previousCard = queueIds
                    .getOrNull(currentIndex - 1)
                    ?.let { id -> mediaCardCache[id] },
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

internal fun MainViewModel.defaultRestoredPermissionModeImpl(): PermissionHelper.PermissionMode = when {
        queueIds.isNotEmpty() || pendingDeleteEntries.isNotEmpty() -> PermissionHelper.PermissionMode.GRANTED_ALL
        else -> PermissionHelper.PermissionMode.DENIED
    }

internal fun MainViewModel.resolveDeleteUriImpl(imageId: Long): Uri {
        val card = mediaCardCache[imageId]
        val collectionUri = when (card?.kind) {
            MediaKind.Video,
            MediaKind.LivePhoto,
            -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.Image,
            MediaKind.AnimatedImage,
            -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            null -> MediaStore.Files.getContentUri(EXTERNAL_MEDIA_VOLUME)
        }

        return IntentHelper.buildMediaUri(
            mediaId = imageId,
            collectionUri = collectionUri,
        )
    }


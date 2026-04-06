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
import com.example.photoroulette.utils.AppLanguageManager
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

internal fun MainViewModel.setSwipeDeleteEnabledImpl(enabled: Boolean) {
        _isSwipeDeleteEnabled.value = enabled
        savedStateHandle[KEY_IS_SWIPE_DELETE_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeDeleteEnabled(enabled)
        }
    }

internal fun MainViewModel.setDeleteReminderEnabledImpl(enabled: Boolean) {
        _isDeleteReminderEnabled.value = enabled
        savedStateHandle[KEY_IS_DELETE_REMINDER_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setDeleteReminderEnabled(enabled)
        }
    }

internal fun MainViewModel.setSwipeGestureSensitivityImpl(sensitivity: Float) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeGestureSensitivity(sensitivity)
        }
    }

internal fun MainViewModel.setShowFullImageImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowFullImage(enabled)
        }
    }

internal fun MainViewModel.setShowCardActionsButtonImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowCardActionsButton(enabled)
        }
    }

internal fun MainViewModel.setTapImageToggleEnabledImpl(enabled: Boolean) {
        _isTapImageToggleEnabled.value = enabled
        savedStateHandle[KEY_IS_TAP_IMAGE_TOGGLE_ENABLED] = enabled
        scope.launch(ioDispatcher) {
            settingsRepository.setTapImageToggleEnabled(enabled)
        }
    }

internal fun MainViewModel.setShowFloatingDeleteButtonImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowFloatingDeleteButton(enabled)
        }
    }

internal fun MainViewModel.setGestureBallEnabledImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallEnabled(enabled)
        }
    }

internal fun MainViewModel.setGestureBallSizeScaleImpl(scale: Float) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallSizeScale(scale)
        }
    }

internal fun MainViewModel.setGestureBallFeedbackEnabledImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setGestureBallFeedbackEnabled(enabled)
        }
    }

internal fun MainViewModel.setShowGestureBallActionHintImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setShowGestureBallActionHint(enabled)
        }
    }

internal fun MainViewModel.setSilentDeleteEnabledImpl(enabled: Boolean) {
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

internal fun MainViewModel.requestSilentDeleteDirectorySelectionImpl(scope: SilentDeleteScope? = null) {
        val targetScope = scope
            ?: firstMissingSilentDeleteScope(_silentDeleteTreeUris.value)
            ?: SilentDeleteScope.Dcim

        pendingSilentDeleteScope = targetScope
        if (!_silentDeleteDirectoryRequests.tryEmit(targetScope)) {
            pendingSilentDeleteScope = null
        }
    }

internal fun MainViewModel.onSilentDeleteDirectoryGrantedImpl(scope: SilentDeleteScope, treeUri: Uri) {
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

internal fun MainViewModel.onSilentDeleteDirectoryRequestCancelledImpl(scope: SilentDeleteScope?) {
        if (scope != null && pendingSilentDeleteScope == scope) {
            pendingSilentDeleteScope = null
        }
    }

internal fun MainViewModel.getSilentDeleteDirectoryLabelImpl(scope: SilentDeleteScope): String? {
        val treeUri = resolveAuthorizedTreeUriForScope(scope, _silentDeleteTreeUris.value)
            ?: return null

        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return null

        return treeDocumentId.substringAfter(':', treeDocumentId).ifBlank { treeDocumentId }
    }

internal fun MainViewModel.hasSilentDeleteDirectoryImpl(scope: SilentDeleteScope): Boolean {
        return resolveAuthorizedTreeUriForScope(scope, _silentDeleteTreeUris.value) != null
    }

internal fun MainViewModel.mergeSilentDeleteTreeUrisImpl(currentUris: List<String>, newScope: SilentDeleteScope, newTreeUri: String): List<String> {
        if (newTreeUri.isBlank()) {
            return currentUris
        }

        val retainedUris = currentUris.filterNot { uriString ->
            val treeUri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@filterNot false
            isTreeExactScope(treeUri, newScope)
        }

        return (retainedUris + newTreeUri).distinct()
    }

internal fun MainViewModel.firstMissingSilentDeleteScopeImpl(treeUris: List<String>): SilentDeleteScope? {
        return SilentDeleteScope.entries.firstOrNull { scope ->
            resolveAuthorizedTreeUriForScope(scope, treeUris) == null
        }
    }

internal fun MainViewModel.resolveAuthorizedTreeUriForScopeImpl(scope: SilentDeleteScope, treeUris: List<String>): Uri? {
        return treeUris
            .asSequence()
            .mapNotNull { treeUriString -> runCatching { Uri.parse(treeUriString) }.getOrNull() }
            .firstOrNull { treeUri -> doesTreeCoverScope(treeUri, scope) }
    }

internal fun MainViewModel.doesTreeCoverScopeImpl(treeUri: Uri, scope: SilentDeleteScope): Boolean {
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

internal fun MainViewModel.isTreeExactScopeImpl(treeUri: Uri, scope: SilentDeleteScope): Boolean {
        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return false

        val treeDirectoryPath = IntentHelper.normalizeDirectoryPath(
            treeDocumentId.substringAfter(':', ""),
        ) ?: return false

        return treeDirectoryPath.equals(scope.directoryName, ignoreCase = true)
    }

internal fun MainViewModel.restoreSilentDeleteTreeUrisImpl(): List<String> {
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

internal fun MainViewModel.saveSilentDeleteTreeUrisImpl(treeUris: List<String>) {
        val normalizedUris = treeUris
            .mapNotNull { uri -> uri.takeIf { it.isNotBlank() } }
            .distinct()

        savedStateHandle[KEY_SILENT_DELETE_TREE_URIS] = ArrayList(normalizedUris)
        savedStateHandle[KEY_SILENT_DELETE_TREE_URI] = normalizedUris.firstOrNull()
    }

internal fun MainViewModel.setAppLanguageTagImpl(tag: String) {
        val normalizedTag = AppLanguageManager.normalizeLanguageTag(tag)
        scope.launch(ioDispatcher) {
            settingsRepository.setAppLanguageTag(normalizedTag)
        }
    }

internal fun MainViewModel.setSwipeLeftActionImpl(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeLeftAction(action)
        }
    }

internal fun MainViewModel.setSwipeRightActionImpl(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeRightAction(action)
        }
    }

internal fun MainViewModel.setSwipeUpActionImpl(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeUpAction(action)
        }
    }

internal fun MainViewModel.setSwipeDownActionImpl(action: SwipeAction) {
        scope.launch(ioDispatcher) {
            settingsRepository.setSwipeDownAction(action)
        }
    }

internal fun MainViewModel.setDefaultBehaviorNoticeEnabledImpl(enabled: Boolean) {
        scope.launch(ioDispatcher) {
            settingsRepository.setDefaultBehaviorNoticeEnabled(enabled)
            _shouldShowDefaultBehaviorNotice.value = enabled
            savedStateHandle[KEY_SHOULD_SHOW_DEFAULT_BEHAVIOR_NOTICE] = enabled
        }
    }

internal fun MainViewModel.checkForUpdatesOnLaunchIfNeededImpl() {
        if (hasCheckedUpdatesOnLaunch) {
            return
        }
        hasCheckedUpdatesOnLaunch = true
        checkForUpdates(initiatedByUser = false)
    }

internal fun MainViewModel.checkForUpdatesManuallyImpl() {
        checkForUpdates(initiatedByUser = true)
    }

internal fun MainViewModel.clearUpdateCheckFeedbackImpl() {
        _updateCheckFeedback.value = UpdateCheckFeedback.Idle
    }

internal fun MainViewModel.dismissAvailableUpdatePromptImpl() {
        _availableUpdateRelease.value = null
    }

internal fun MainViewModel.deferCurrentAvailableUpdateImpl() {
        val release = _availableUpdateRelease.value ?: return
        val deferredVersion = release.normalizedVersion
        scope.launch(ioDispatcher) {
            settingsRepository.setSkippedUpdateVersion(deferredVersion)
        }
        _updateCheckFeedback.value = UpdateCheckFeedback.DeferredUntilNewer(deferredVersion)
        _availableUpdateRelease.value = null
    }

internal fun MainViewModel.startUpdateInstallationImpl() {
        val release = _availableUpdateRelease.value ?: return
        scope.launch(ioDispatcher) {
            _isUpdateInstallInProgress.value = true
            try {
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
                _isUpdateInstallInProgress.value = false
            }
        }
    }

internal fun MainViewModel.onUpdateInstallFlowFinishedImpl() {
        _isUpdateInstallInProgress.value = false
        pendingUpdatePackagePath = null
    }

internal fun MainViewModel.onUpdateInstallLaunchFailedImpl() {
        _isUpdateInstallInProgress.value = false
        clearPendingDownloadedUpdatePackage()
        _updateCheckFeedback.value = UpdateCheckFeedback.Failed()
    }

internal fun MainViewModel.onClearedImpl() {
        scope.cancel()
    }


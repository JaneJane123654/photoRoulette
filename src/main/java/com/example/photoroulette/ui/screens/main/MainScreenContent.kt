package com.example.photoroulette.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.photoroulette.R
import com.example.photoroulette.BuildConfig
import com.example.photoroulette.data.datastore.SettingsRepository
import com.example.photoroulette.model.AppReleaseInfo
import com.example.photoroulette.model.DefaultBehaviorNoticeMode
import com.example.photoroulette.model.MediaCard
import com.example.photoroulette.model.MediaKind
import com.example.photoroulette.model.SilentDeleteScope
import com.example.photoroulette.model.SwipeAction
import com.example.photoroulette.model.UpdateCheckFeedback
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.photoroulette.ui.components.EmptyGalleryScreen
import com.example.photoroulette.ui.components.PermissionDeniedState
import com.example.photoroulette.ui.components.SwipeDirection
import com.example.photoroulette.ui.components.SwipeableCard
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.viewmodel.MainViewModel
import com.example.photoroulette.viewmodel.states.HomeUiState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun MainScreenContent(
    state: HomeUiState,
    permissionMode: PermissionHelper.PermissionMode,
    isSwipeDeleteEnabled: Boolean,
    isDeleteReminderEnabled: Boolean,
    swipeGestureSensitivity: Float,
    showFullImage: Boolean,
    isTapImageToggleEnabled: Boolean,
    showFloatingDeleteButton: Boolean,
    isGestureBallEnabled: Boolean,
    gestureBallSizeScale: Float,
    isGestureBallFeedbackEnabled: Boolean,
    showGestureBallActionHint: Boolean,
    isSilentDeleteEnabled: Boolean,
    silentDeleteDcimLabel: String?,
    silentDeletePicturesLabel: String?,
    hasDcimDirectoryAuthorized: Boolean,
    hasPicturesDirectoryAuthorized: Boolean,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    defaultBehaviorNoticeMode: DefaultBehaviorNoticeMode,
    shouldShowDefaultBehaviorNotice: Boolean,
    availableUpdateRelease: AppReleaseInfo?,
    updateCheckFeedback: UpdateCheckFeedback,
    isUpdateInstallInProgress: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionRationaleDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onSwipeDeleteEnabledChange: (Boolean) -> Unit,
    onDeleteReminderEnabledChange: (Boolean) -> Unit,
    onSwipeGestureSensitivityChange: (Float) -> Unit,
    onShowFullImageChange: (Boolean) -> Unit,
    onTapImageToggleEnabledChange: (Boolean) -> Unit,
    onShowFloatingDeleteButtonChange: (Boolean) -> Unit,
    onGestureBallEnabledChange: (Boolean) -> Unit,
    onGestureBallSizeScaleChange: (Float) -> Unit,
    onGestureBallFeedbackEnabledChange: (Boolean) -> Unit,
    onShowGestureBallActionHintChange: (Boolean) -> Unit,
    onSilentDeleteEnabledChange: (Boolean) -> Unit,
    onConfigureSilentDeleteDcimDirectory: () -> Unit,
    onConfigureSilentDeletePicturesDirectory: () -> Unit,
    onSwipeLeftActionChange: (SwipeAction) -> Unit,
    onSwipeRightActionChange: (SwipeAction) -> Unit,
    onSwipeUpActionChange: (SwipeAction) -> Unit,
    onSwipeDownActionChange: (SwipeAction) -> Unit,
    onDefaultBehaviorNoticeEnabledChange: (Boolean) -> Unit,
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    onSwipeAction: (SwipeAction, Long) -> Boolean,
    onManualUpdateCheck: () -> Unit,
    onClearUpdateFeedback: () -> Unit,
    onDismissAvailableUpdate: () -> Unit,
    onDeferCurrentUpdate: () -> Unit,
    onStartUpdateInstallation: () -> Unit,
    showPermissionRationale: Boolean,
    modifier: Modifier = Modifier,
) {
    if (
        permissionMode == PermissionHelper.PermissionMode.DENIED &&
        showPermissionRationale
    ) {
        PermissionRationaleScreen(
            modifier = modifier,
            onRequestClicked = onRequestPermission,
            onMaybeLaterClicked = onPermissionRationaleDismissed,
        )
        return
    }

    val effectiveState = if (permissionMode == PermissionHelper.PermissionMode.DENIED) {
        HomeUiState.PermissionDenied
    } else {
        state
    }

    var isSettingsDialogVisible by rememberSaveable { mutableStateOf(false) }
    var isDefaultNoticeDismissedBySession by rememberSaveable { mutableStateOf(false) }
    var isDefaultNoticeExpanded by rememberSaveable { mutableStateOf(true) }
    var hasDefaultNoticeAutoCollapsed by rememberSaveable { mutableStateOf(false) }
    val topVisibleImageId = (effectiveState as? HomeUiState.Ready)?.visibleCards?.firstOrNull()?.id
    val canSwipePrevious = (effectiveState as? HomeUiState.Ready)?.canSwipeToPrevious == true
    val canSwipeNext = (effectiveState as? HomeUiState.Ready)?.canSwipeToNext == true
    val shouldShowDefaultNotice =
        effectiveState != HomeUiState.PermissionDenied &&
            shouldShowDefaultBehaviorNotice &&
            !isDefaultNoticeDismissedBySession

    LaunchedEffect(shouldShowDefaultBehaviorNotice) {
        if (!shouldShowDefaultBehaviorNotice) {
            isDefaultNoticeDismissedBySession = false
            isDefaultNoticeExpanded = true
            hasDefaultNoticeAutoCollapsed = false
        }
    }

    LaunchedEffect(shouldShowDefaultNotice, hasDefaultNoticeAutoCollapsed) {
        if (!shouldShowDefaultNotice || hasDefaultNoticeAutoCollapsed) {
            return@LaunchedEffect
        }

        delay(DEFAULT_BEHAVIOR_NOTICE_AUTO_COLLAPSE_DELAY_MS)
        if (!isDefaultNoticeDismissedBySession) {
            isDefaultNoticeExpanded = false
            hasDefaultNoticeAutoCollapsed = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MainHeader(
                    onRefresh = onRefresh,
                    canRefresh = effectiveState != HomeUiState.PermissionDenied,
                )

                AnimatedVisibility(
                    visible = permissionMode == PermissionHelper.PermissionMode.GRANTED_PARTIAL,
                ) {
                    PartialAccessBanner(onExpandAccessClick = onRequestPermission)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    when (effectiveState) {
                        HomeUiState.Loading -> LoadingState()
                        HomeUiState.PermissionDenied -> PermissionDeniedState(
                            onOpenSettingsClick = onOpenSettings,
                        )

                        HomeUiState.Empty -> EmptyGalleryScreen()
                        is HomeUiState.Ready -> PhotoDeck(
                            previousCard = effectiveState.previousCard,
                            visibleCards = effectiveState.visibleCards,
                            canSwipePrevious = effectiveState.canSwipeToPrevious,
                            canSwipeNext = effectiveState.canSwipeToNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                            swipeGestureSensitivity = swipeGestureSensitivity,
                            swipeLeftAction = swipeLeftAction,
                            swipeRightAction = swipeRightAction,
                            swipeUpAction = swipeUpAction,
                            swipeDownAction = swipeDownAction,
                            showFullImage = showFullImage,
                            isTapImageToggleEnabled = isTapImageToggleEnabled,
                            onSwipeAction = onSwipeAction,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (shouldShowDefaultNotice) {
                        DefaultBehaviorNotice(
                            expanded = isDefaultNoticeExpanded,
                            onToggleExpanded = {
                                isDefaultNoticeExpanded = !isDefaultNoticeExpanded
                            },
                            onDismiss = {
                                isDefaultNoticeDismissedBySession = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(
                                    if (isDefaultNoticeExpanded) {
                                        1f
                                    } else {
                                        0.72f
                                    },
                                )
                                .padding(top = 10.dp),
                        )
                    }
                }

                if (effectiveState != HomeUiState.PermissionDenied) {
                    SettingsEntryCard(
                        onClick = { isSettingsDialogVisible = true },
                    )
                }
            }

            if (
                effectiveState != HomeUiState.PermissionDenied &&
                isGestureBallEnabled &&
                topVisibleImageId != null
            ) {
                GestureBallOverlay(
                    topImageId = topVisibleImageId,
                    swipeLeftAction = swipeLeftAction,
                    swipeRightAction = swipeRightAction,
                    swipeUpAction = swipeUpAction,
                    swipeDownAction = swipeDownAction,
                    canSwipePrevious = canSwipePrevious,
                    canSwipeNext = canSwipeNext,
                    isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                    ballSizeScale = gestureBallSizeScale,
                    isFeedbackEnabled = isGestureBallFeedbackEnabled,
                    showActionHint = showGestureBallActionHint,
                    onSwipeAction = onSwipeAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (
                effectiveState != HomeUiState.PermissionDenied &&
                showFloatingDeleteButton &&
                topVisibleImageId != null
            ) {
                DraggableFloatingDeleteButton(
                    onDeleteClick = {
                        onSwipeAction(SwipeAction.Delete, topVisibleImageId)
                    },
                    isGestureBallEnabled = isGestureBallEnabled,
                    gestureBallSizeScale = gestureBallSizeScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp),
            )
        }
    }

    if (effectiveState != HomeUiState.PermissionDenied && isSettingsDialogVisible) {
        SettingsDialog(
            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
            isDeleteReminderEnabled = isDeleteReminderEnabled,
            swipeGestureSensitivity = swipeGestureSensitivity,
            showFullImage = showFullImage,
            isTapImageToggleEnabled = isTapImageToggleEnabled,
            showFloatingDeleteButton = showFloatingDeleteButton,
            isGestureBallEnabled = isGestureBallEnabled,
            gestureBallSizeScale = gestureBallSizeScale,
            isGestureBallFeedbackEnabled = isGestureBallFeedbackEnabled,
            showGestureBallActionHint = showGestureBallActionHint,
            isSilentDeleteEnabled = isSilentDeleteEnabled,
            silentDeleteDcimLabel = silentDeleteDcimLabel,
            silentDeletePicturesLabel = silentDeletePicturesLabel,
            hasDcimDirectoryAuthorized = hasDcimDirectoryAuthorized,
            hasPicturesDirectoryAuthorized = hasPicturesDirectoryAuthorized,
            swipeLeftAction = swipeLeftAction,
            swipeRightAction = swipeRightAction,
            swipeUpAction = swipeUpAction,
            swipeDownAction = swipeDownAction,
            selectedLanguageTag = selectedLanguageTag,
            onDismiss = { isSettingsDialogVisible = false },
            onSwipeDeleteEnabledChange = onSwipeDeleteEnabledChange,
            onDeleteReminderEnabledChange = onDeleteReminderEnabledChange,
            onSwipeGestureSensitivityChange = onSwipeGestureSensitivityChange,
            onShowFullImageChange = onShowFullImageChange,
            onTapImageToggleEnabledChange = onTapImageToggleEnabledChange,
            onShowFloatingDeleteButtonChange = onShowFloatingDeleteButtonChange,
            onGestureBallEnabledChange = onGestureBallEnabledChange,
            onGestureBallSizeScaleChange = onGestureBallSizeScaleChange,
            onGestureBallFeedbackEnabledChange = onGestureBallFeedbackEnabledChange,
            onShowGestureBallActionHintChange = onShowGestureBallActionHintChange,
            onSilentDeleteEnabledChange = onSilentDeleteEnabledChange,
            onConfigureSilentDeleteDcimDirectory = onConfigureSilentDeleteDcimDirectory,
            onConfigureSilentDeletePicturesDirectory = onConfigureSilentDeletePicturesDirectory,
            onSwipeLeftActionChange = onSwipeLeftActionChange,
            onSwipeRightActionChange = onSwipeRightActionChange,
            onSwipeUpActionChange = onSwipeUpActionChange,
            onSwipeDownActionChange = onSwipeDownActionChange,
            defaultBehaviorNoticeMode = defaultBehaviorNoticeMode,
            onDefaultBehaviorNoticeEnabledChange = onDefaultBehaviorNoticeEnabledChange,
            onLanguageTagChange = onLanguageTagChange,
            onCheckUpdateClick = onManualUpdateCheck,
        )
    }

    if (availableUpdateRelease != null) {
        UpdateAvailableDialog(
            release = availableUpdateRelease,
            isInstalling = isUpdateInstallInProgress,
            onDismiss = onDismissAvailableUpdate,
            onLater = onDeferCurrentUpdate,
            onUpdateNow = onStartUpdateInstallation,
        )
    }

    when (val feedback = updateCheckFeedback) {
        UpdateCheckFeedback.Idle,
        UpdateCheckFeedback.Checking,
        -> Unit

        UpdateCheckFeedback.UpToDate,
        is UpdateCheckFeedback.DeferredUntilNewer,
        is UpdateCheckFeedback.Failed,
        -> {
            UpdateStatusDialog(
                feedback = feedback,
                onDismiss = onClearUpdateFeedback,
            )
        }
    }
}



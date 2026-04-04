package com.example.photoroulette.ui.screens

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photoroulette.R
import com.example.photoroulette.BuildConfig
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.compose.zoom.isNotEmpty
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType
import com.github.panpf.zoomimage.zoom.ScalesCalculator
import com.example.photoroulette.data.datastore.SettingsRepository
import com.example.photoroulette.model.AppReleaseInfo
import com.example.photoroulette.model.DefaultBehaviorNoticeMode
import com.example.photoroulette.model.SilentDeleteScope
import com.example.photoroulette.model.SwipeAction
import com.example.photoroulette.model.UpdateCheckFeedback
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.photoroulette.ui.components.EmptyGalleryScreen
import com.example.photoroulette.ui.components.PermissionDeniedState
import com.example.photoroulette.ui.components.SwipeDirection
import com.example.photoroulette.ui.components.SwipeableCard
import com.example.photoroulette.utils.IntentHelper
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
fun MainScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    showPermissionRationale: Boolean = false,
    onPermissionRationaleDismissed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionMode by viewModel.permissionMode.collectAsStateWithLifecycle()
    val isSwipeDeleteEnabled by viewModel.isSwipeDeleteEnabled.collectAsStateWithLifecycle()
    val isDeleteReminderEnabled by viewModel.isDeleteReminderEnabled.collectAsStateWithLifecycle()
    val swipeGestureSensitivity by viewModel.swipeGestureSensitivity.collectAsStateWithLifecycle()
    val showFullImage by viewModel.showFullImage.collectAsStateWithLifecycle()
    val isTapImageToggleEnabled by viewModel.isTapImageToggleEnabled.collectAsStateWithLifecycle()
    val showFloatingDeleteButton by viewModel.showFloatingDeleteButton.collectAsStateWithLifecycle()
    val isGestureBallEnabled by viewModel.isGestureBallEnabled.collectAsStateWithLifecycle()
    val gestureBallSizeScale by viewModel.gestureBallSizeScale.collectAsStateWithLifecycle()
    val isGestureBallFeedbackEnabled by viewModel.isGestureBallFeedbackEnabled.collectAsStateWithLifecycle()
    val showGestureBallActionHint by viewModel.showGestureBallActionHint.collectAsStateWithLifecycle()
    val isSilentDeleteEnabled by viewModel.isSilentDeleteEnabled.collectAsStateWithLifecycle()
    val silentDeleteTreeUris by viewModel.silentDeleteTreeUris.collectAsStateWithLifecycle()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRightAction by viewModel.swipeRightAction.collectAsStateWithLifecycle()
    val swipeUpAction by viewModel.swipeUpAction.collectAsStateWithLifecycle()
    val swipeDownAction by viewModel.swipeDownAction.collectAsStateWithLifecycle()
    val defaultBehaviorNoticeMode by viewModel.defaultBehaviorNoticeMode.collectAsStateWithLifecycle()
    val shouldShowDefaultBehaviorNotice by viewModel.shouldShowDefaultBehaviorNotice.collectAsStateWithLifecycle()
    val availableUpdateRelease by viewModel.availableUpdateRelease.collectAsStateWithLifecycle()
    val updateCheckFeedback by viewModel.updateCheckFeedback.collectAsStateWithLifecycle()
    val isUpdateInstallInProgress by viewModel.isUpdateInstallInProgress.collectAsStateWithLifecycle()
    val silentDeleteDcimLabel = remember(silentDeleteTreeUris) {
        viewModel.getSilentDeleteDirectoryLabel(SilentDeleteScope.Dcim)
    }
    val silentDeletePicturesLabel = remember(silentDeleteTreeUris) {
        viewModel.getSilentDeleteDirectoryLabel(SilentDeleteScope.Pictures)
    }
    val hasDcimDirectoryAuthorized = remember(silentDeleteTreeUris) {
        viewModel.hasSilentDeleteDirectory(SilentDeleteScope.Dcim)
    }
    val hasPicturesDirectoryAuthorized = remember(silentDeleteTreeUris) {
        viewModel.hasSilentDeleteDirectory(SilentDeleteScope.Pictures)
    }
    val deleteReminderHostState = remember { SnackbarHostState() }

    MainScreenContent(
        state = uiState,
        permissionMode = permissionMode,
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
        defaultBehaviorNoticeMode = defaultBehaviorNoticeMode,
        shouldShowDefaultBehaviorNotice = shouldShowDefaultBehaviorNotice,
        availableUpdateRelease = availableUpdateRelease,
        updateCheckFeedback = updateCheckFeedback,
        isUpdateInstallInProgress = isUpdateInstallInProgress,
        snackbarHostState = deleteReminderHostState,
        onRequestPermission = onRequestPermission,
        onOpenSettings = onOpenSettings,
        onPermissionRationaleDismissed = onPermissionRationaleDismissed,
        onRefresh = viewModel::refreshMedia,
        onSwipeDeleteEnabledChange = viewModel::setSwipeDeleteEnabled,
        onDeleteReminderEnabledChange = viewModel::setDeleteReminderEnabled,
        onSwipeGestureSensitivityChange = viewModel::setSwipeGestureSensitivity,
        onShowFullImageChange = viewModel::setShowFullImage,
        onTapImageToggleEnabledChange = viewModel::setTapImageToggleEnabled,
        onShowFloatingDeleteButtonChange = viewModel::setShowFloatingDeleteButton,
        onGestureBallEnabledChange = viewModel::setGestureBallEnabled,
        onGestureBallSizeScaleChange = viewModel::setGestureBallSizeScale,
        onGestureBallFeedbackEnabledChange = viewModel::setGestureBallFeedbackEnabled,
        onShowGestureBallActionHintChange = viewModel::setShowGestureBallActionHint,
        onSilentDeleteEnabledChange = viewModel::setSilentDeleteEnabled,
        onConfigureSilentDeleteDcimDirectory = {
            viewModel.requestSilentDeleteDirectorySelection(SilentDeleteScope.Dcim)
        },
        onConfigureSilentDeletePicturesDirectory = {
            viewModel.requestSilentDeleteDirectorySelection(SilentDeleteScope.Pictures)
        },
        onSwipeLeftActionChange = viewModel::setSwipeLeftAction,
        onSwipeRightActionChange = viewModel::setSwipeRightAction,
        onSwipeUpActionChange = viewModel::setSwipeUpAction,
        onSwipeDownActionChange = viewModel::setSwipeDownAction,
        onDefaultBehaviorNoticeEnabledChange = viewModel::setDefaultBehaviorNoticeEnabled,
        selectedLanguageTag = selectedLanguageTag,
        onLanguageTagChange = onLanguageTagChange,
        onSwipeAction = viewModel::performSwipeAction,
        onManualUpdateCheck = viewModel::checkForUpdatesManually,
        onClearUpdateFeedback = viewModel::clearUpdateCheckFeedback,
        onDismissAvailableUpdate = viewModel::dismissAvailableUpdatePrompt,
        onDeferCurrentUpdate = viewModel::deferCurrentAvailableUpdate,
        onStartUpdateInstallation = viewModel::startUpdateInstallation,
        showPermissionRationale = showPermissionRationale,
        modifier = modifier,
    )

    val context = LocalContext.current
    LaunchedEffect(viewModel, context, deleteReminderHostState) {
        viewModel.deleteReminderEvents.collectLatest {
            deleteReminderHostState.currentSnackbarData?.dismiss()
            deleteReminderHostState.showSnackbar(
                message = context.getString(R.string.delete_reminder_toast),
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdatesOnLaunchIfNeeded()
    }
}

@Composable
private fun MainScreenContent(
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
    val topVisibleImageId = (effectiveState as? HomeUiState.Ready)?.visibleIds?.firstOrNull()
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
                            visibleIds = effectiveState.visibleIds,
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

@Composable
private fun MainHeader(
    onRefresh: () -> Unit,
    canRefresh: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.home_header_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(id = R.string.home_header_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                enabled = canRefresh,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(id = R.string.refresh_button_content_description),
                )
                Text(
                    text = stringResource(id = R.string.refresh_button_label),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(
    onExpandAccessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = stringResource(id = R.string.partial_access_icon_content_description),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.partial_access_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.partial_access_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            Button(onClick = onExpandAccessClick) {
                Text(text = stringResource(id = R.string.partial_access_expand))
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(id = R.string.loading_deck),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DefaultBehaviorNotice(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = if (expanded) RoundedCornerShape(22.dp) else RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(
                    horizontal = if (expanded) 16.dp else 14.dp,
                    vertical = if (expanded) 12.dp else 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpanded),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.default_behavior_title),
                        style = if (expanded) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (!expanded) {
                        Text(
                            text = stringResource(id = R.string.default_behavior_collapsed_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f),
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpanded,
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = stringResource(
                            id = if (expanded) {
                                R.string.default_behavior_collapse
                            } else {
                                R.string.default_behavior_expand
                            },
                        ),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.default_behavior_close),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_up_next),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_down_previous),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_left_delete),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_delete_prerequisite),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_direct_delete),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_tap_toggle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_delete_delay),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    )
                    Text(
                        text = stringResource(id = R.string.default_behavior_line_visibility_rule),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_entry_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = R.string.settings_entry_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(onClick = onClick) {
                Text(text = stringResource(id = R.string.settings_entry_open))
            }
        }
    }
}

@Composable
private fun SettingsDialog(
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
    selectedLanguageTag: String,
    onDismiss: () -> Unit,
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
    onLanguageTagChange: (String) -> Unit,
    onCheckUpdateClick: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_dialog_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.settings_dialog_done))
                    }
                }

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_guide))
                UsageGuideCard()
                DefaultBehaviorNoticeControls(
                    mode = defaultBehaviorNoticeMode,
                    onCheckedChange = onDefaultBehaviorNoticeEnabledChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_display))
                ImageDisplayControls(
                    showFullImage = showFullImage,
                    onCheckedChange = onShowFullImageChange,
                )
                TapImageToggleControls(
                    isEnabled = isTapImageToggleEnabled,
                    onCheckedChange = onTapImageToggleEnabledChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_swipe))
                SwipeBehaviorControls(
                    swipeLeftAction = swipeLeftAction,
                    swipeRightAction = swipeRightAction,
                    swipeUpAction = swipeUpAction,
                    swipeDownAction = swipeDownAction,
                    swipeGestureSensitivity = swipeGestureSensitivity,
                    onSwipeLeftActionChange = onSwipeLeftActionChange,
                    onSwipeRightActionChange = onSwipeRightActionChange,
                    onSwipeUpActionChange = onSwipeUpActionChange,
                    onSwipeDownActionChange = onSwipeDownActionChange,
                    onSwipeGestureSensitivityChange = onSwipeGestureSensitivityChange,
                )
                GestureBallControls(
                    isGestureBallEnabled = isGestureBallEnabled,
                    sizeScale = gestureBallSizeScale,
                    isFeedbackEnabled = isGestureBallFeedbackEnabled,
                    showActionHint = showGestureBallActionHint,
                    onCheckedChange = onGestureBallEnabledChange,
                    onSizeScaleChange = onGestureBallSizeScaleChange,
                    onFeedbackEnabledChange = onGestureBallFeedbackEnabledChange,
                    onActionHintEnabledChange = onShowGestureBallActionHintChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_delete))
                SwipeDeleteControls(
                    isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                    onCheckedChange = onSwipeDeleteEnabledChange,
                )
                DeleteReminderControls(
                    isEnabled = isDeleteReminderEnabled,
                    onCheckedChange = onDeleteReminderEnabledChange,
                )
                FloatingDeleteButtonControls(
                    isEnabled = showFloatingDeleteButton,
                    onCheckedChange = onShowFloatingDeleteButtonChange,
                )
                SilentDeleteControls(
                    isSilentDeleteEnabled = isSilentDeleteEnabled,
                    dcimLabel = silentDeleteDcimLabel,
                    picturesLabel = silentDeletePicturesLabel,
                    hasDcimDirectoryAuthorized = hasDcimDirectoryAuthorized,
                    hasPicturesDirectoryAuthorized = hasPicturesDirectoryAuthorized,
                    onCheckedChange = onSilentDeleteEnabledChange,
                    onConfigureDcimDirectory = onConfigureSilentDeleteDcimDirectory,
                    onConfigurePicturesDirectory = onConfigureSilentDeletePicturesDirectory,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_language))
                LanguageSettingsControls(
                    selectedLanguageTag = selectedLanguageTag,
                    onLanguageTagChange = onLanguageTagChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_update))
                UpdateControls(
                    onCheckUpdateClick = onCheckUpdateClick,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun UsageGuideCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.usage_guide_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_one),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_two),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_three),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_four),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_five),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DefaultBehaviorNoticeControls(
    mode: DefaultBehaviorNoticeMode,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = mode == DefaultBehaviorNoticeMode.Visible
    val descriptionRes = when (mode) {
        DefaultBehaviorNoticeMode.Visible -> R.string.default_behavior_visibility_visible_description
        DefaultBehaviorNoticeMode.AutoHidden -> R.string.default_behavior_visibility_auto_hidden_description
        DefaultBehaviorNoticeMode.UserHidden -> R.string.default_behavior_visibility_user_hidden_description
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.default_behavior_visibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun UpdateControls(
    onCheckUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.update_controls_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = R.string.update_controls_description, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledTonalButton(
                onClick = onCheckUpdateClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.update_controls_check_button))
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    release: AppReleaseInfo,
    isInstalling: Boolean,
    onDismiss: () -> Unit,
    onLater: () -> Unit,
    onUpdateNow: () -> Unit,
) {
    Dialog(
        onDismissRequest = if (isInstalling) {
            {}
        } else {
            onDismiss
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = stringResource(
                        id = R.string.update_dialog_message,
                        release.normalizedVersion,
                        BuildConfig.VERSION_NAME,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                release.notes
                    ?.lineSequence()
                    ?.firstOrNull { line -> line.isNotBlank() }
                    ?.let { firstLine ->
                        Text(
                            text = firstLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                release.releasePageUrl
                    ?.takeIf { url -> url.isNotBlank() }
                    ?.let { releaseUrl ->
                        Text(
                            text = releaseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_not_now))
                    }

                    OutlinedButton(
                        onClick = onLater,
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_later))
                    }

                    Button(
                        onClick = onUpdateNow,
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.update_dialog_update_now),
                            modifier = Modifier.padding(start = if (isInstalling) 8.dp else 0.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusDialog(
    feedback: UpdateCheckFeedback,
    onDismiss: () -> Unit,
) {
    val messageRes = when (feedback) {
        UpdateCheckFeedback.UpToDate -> R.string.update_status_latest
        is UpdateCheckFeedback.DeferredUntilNewer -> R.string.update_status_deferred
        is UpdateCheckFeedback.Failed -> R.string.update_status_failed
        UpdateCheckFeedback.Idle,
        UpdateCheckFeedback.Checking,
        -> return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.update_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val message = when (feedback) {
                    UpdateCheckFeedback.UpToDate -> stringResource(id = messageRes)
                    is UpdateCheckFeedback.DeferredUntilNewer -> stringResource(
                        id = messageRes,
                        feedback.deferredVersion,
                    )
                    is UpdateCheckFeedback.Failed -> stringResource(id = messageRes)
                    UpdateCheckFeedback.Idle,
                    UpdateCheckFeedback.Checking,
                    -> ""
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.settings_dialog_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageDisplayControls(
    showFullImage: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.show_full_image_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (showFullImage) {
                        stringResource(id = R.string.show_full_image_enabled_description)
                    } else {
                        stringResource(id = R.string.show_full_image_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = showFullImage,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun FloatingDeleteButtonControls(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.floating_delete_button_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isEnabled) {
                        stringResource(id = R.string.floating_delete_button_enabled_description)
                    } else {
                        stringResource(id = R.string.floating_delete_button_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun GestureBallControls(
    isGestureBallEnabled: Boolean,
    sizeScale: Float,
    isFeedbackEnabled: Boolean,
    showActionHint: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSizeScaleChange: (Float) -> Unit,
    onFeedbackEnabledChange: (Boolean) -> Unit,
    onActionHintEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.gesture_ball_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isGestureBallEnabled) {
                            stringResource(id = R.string.gesture_ball_enabled_description)
                        } else {
                            stringResource(id = R.string.gesture_ball_disabled_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isGestureBallEnabled,
                    onCheckedChange = onCheckedChange,
                )
            }

            Text(
                text = stringResource(
                    id = R.string.gesture_ball_size_label,
                    (sizeScale * 100f).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            Slider(
                value = sizeScale,
                onValueChange = onSizeScaleChange,
                valueRange = SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE..SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
                enabled = isGestureBallEnabled,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.gesture_ball_feedback_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isFeedbackEnabled) {
                            stringResource(id = R.string.gesture_ball_feedback_enabled_description)
                        } else {
                            stringResource(id = R.string.gesture_ball_feedback_disabled_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isFeedbackEnabled,
                    onCheckedChange = onFeedbackEnabledChange,
                    enabled = isGestureBallEnabled,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.gesture_ball_hint_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (showActionHint) {
                            stringResource(id = R.string.gesture_ball_hint_enabled_description)
                        } else {
                            stringResource(id = R.string.gesture_ball_hint_disabled_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = showActionHint,
                    onCheckedChange = onActionHintEnabledChange,
                    enabled = isGestureBallEnabled,
                )
            }
        }
    }
}

@Composable
private fun DraggableFloatingDeleteButton(
    onDeleteClick: () -> Unit,
    isGestureBallEnabled: Boolean,
    gestureBallSizeScale: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val buttonSize = FLOATING_DELETE_BUTTON_SIZE
    val margin = 18.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val buttonRadiusPx = buttonSizePx / 2f
    val marginPx = with(density) { margin.toPx() }
    val normalizedGestureBallSizeScale = gestureBallSizeScale.coerceIn(
        SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE,
        SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
    )
    val gestureBallRadiusPx = with(density) {
        (GESTURE_BALL_BASE_OUTER_RADIUS * normalizedGestureBallSizeScale).toPx()
    }
    val gestureBallMarginPx = with(density) { GESTURE_BALL_MARGIN.toPx() }
    val gestureBallClearancePx = with(density) { FLOATING_DELETE_BUTTON_GESTURE_BALL_CLEARANCE.toPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var positioned by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var suppressTapUntilRelease by remember { mutableStateOf(false) }
    val currentOnDeleteClick by rememberUpdatedState(onDeleteClick)

    fun clampX(value: Float): Float {
        val maxX = (containerSize.width.toFloat() - buttonSizePx - marginPx).coerceAtLeast(marginPx)
        return value.coerceIn(marginPx, maxX)
    }

    fun clampY(value: Float): Float {
        val maxY = (containerSize.height.toFloat() - buttonSizePx - marginPx).coerceAtLeast(marginPx)
        return value.coerceIn(marginPx, maxY)
    }

    fun clampCenter(target: Offset): Offset {
        val minX = marginPx + buttonRadiusPx
        val maxX = (containerSize.width.toFloat() - marginPx - buttonRadiusPx).coerceAtLeast(minX)
        val minY = marginPx + buttonRadiusPx
        val maxY = (containerSize.height.toFloat() - marginPx - buttonRadiusPx).coerceAtLeast(minY)
        return Offset(
            x = target.x.coerceIn(minX, maxX),
            y = target.y.coerceIn(minY, maxY),
        )
    }

    if (!positioned && containerSize.width > 0 && containerSize.height > 0) {
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        val desiredCenter = clampCenter(
            Offset(
                x = containerWidth - marginPx - buttonRadiusPx,
                y = containerHeight * FLOATING_DELETE_BUTTON_DEFAULT_CENTER_Y_RATIO,
            ),
        )
        var initialCenter = desiredCenter

        if (isGestureBallEnabled) {
            val gestureBallCenter = Offset(
                x = containerWidth - gestureBallRadiusPx - gestureBallMarginPx,
                y = containerHeight * GESTURE_BALL_DEFAULT_CENTER_Y_RATIO,
            )
            val minimumSafeDistance = gestureBallRadiusPx + buttonRadiusPx + gestureBallClearancePx

            if ((initialCenter - gestureBallCenter).getDistanceValue() < minimumSafeDistance) {
                val preferredLowerCenter = clampCenter(
                    Offset(
                        x = initialCenter.x,
                        y = gestureBallCenter.y + minimumSafeDistance,
                    ),
                )
                val fallbackUpperCenter = clampCenter(
                    Offset(
                        x = initialCenter.x,
                        y = gestureBallCenter.y - minimumSafeDistance,
                    ),
                )

                initialCenter = when {
                    (preferredLowerCenter - gestureBallCenter).getDistanceValue() >=
                        minimumSafeDistance -> preferredLowerCenter

                    (fallbackUpperCenter - gestureBallCenter).getDistanceValue() >=
                        minimumSafeDistance -> fallbackUpperCenter

                    abs(preferredLowerCenter.y - desiredCenter.y) <=
                        abs(fallbackUpperCenter.y - desiredCenter.y) -> preferredLowerCenter

                    else -> fallbackUpperCenter
                }
            }
        }

        offsetX = clampX(initialCenter.x - buttonRadiusPx)
        offsetY = clampY(initialCenter.y - buttonRadiusPx)
        positioned = true
    }

    Box(
        modifier = modifier.onSizeChanged { containerSize = it },
    ) {
        Surface(
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(containerSize) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            suppressTapUntilRelease = true
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = clampX(offsetX + dragAmount.x)
                        offsetY = clampY(offsetY + dragAmount.y)
                    }
                }
                .pointerInput(isDragging, currentOnDeleteClick) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isTap = true

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (isTap && !isDragging && !suppressTapUntilRelease) {
                                    currentOnDeleteClick()
                                }
                                suppressTapUntilRelease = false
                                break
                            }

                            if (change.positionChange() != Offset.Zero) {
                                isTap = false
                            }
                        }
                    }
                },
            shape = CircleShape,
            color = Color(0xFFD32F2F),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.floating_delete_button_title),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun GestureBallOverlay(
    topImageId: Long,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    isSwipeDeleteEnabled: Boolean,
    ballSizeScale: Float,
    isFeedbackEnabled: Boolean,
    showActionHint: Boolean,
    onSwipeAction: (SwipeAction, Long) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hostView = LocalView.current
    val sizeScale = ballSizeScale.coerceIn(
        SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE,
        SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
    )
    val outerRadius = GESTURE_BALL_BASE_OUTER_RADIUS * sizeScale
    val innerRadius = 30.dp * sizeScale
    val strokeWidth = 18.dp * sizeScale
    val margin = GESTURE_BALL_MARGIN
    val outerRadiusPx = with(density) { outerRadius.toPx() }
    val innerRadiusPx = with(density) { innerRadius.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val marginPx = with(density) { margin.toPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isRelocating by remember { mutableStateOf(false) }
    var hoveredSegment by remember { mutableStateOf<GestureBallHover?>(null) }

    fun clampCenter(target: Offset, bounds: IntSize = containerSize): Offset {
        val width = bounds.width.toFloat()
        val height = bounds.height.toFloat()
        if (width <= 0f || height <= 0f) {
            return target
        }

        val minX = outerRadiusPx + marginPx
        val maxX = (width - outerRadiusPx - marginPx).coerceAtLeast(minX)
        val minY = outerRadiusPx + marginPx
        val maxY = (height - outerRadiusPx - marginPx).coerceAtLeast(minY)

        return Offset(
            x = target.x.coerceIn(minX, maxX),
            y = target.y.coerceIn(minY, maxY),
        )
    }

    if (!initialized && containerSize.width > 0 && containerSize.height > 0) {
        center = clampCenter(
            target = Offset(
                x = containerSize.width - outerRadiusPx - marginPx,
                y = containerSize.height * GESTURE_BALL_DEFAULT_CENTER_Y_RATIO,
            ),
        )
        initialized = true
    } else if (initialized && containerSize.width > 0 && containerSize.height > 0) {
        center = clampCenter(center)
    }

    val previewAction = hoveredSegment?.action
    val canExecutePreview = !isRelocating && previewAction != null && canExecuteAction(
        action = previewAction,
        canSwipePrevious = canSwipePrevious,
        canSwipeNext = canSwipeNext,
        isSwipeDeleteEnabled = isSwipeDeleteEnabled,
    )
    val segmentVisuals = remember(
        swipeLeftAction,
        swipeUpAction,
        swipeRightAction,
        swipeDownAction,
    ) {
        listOf(
            GestureBallSegmentVisual(
                segment = GestureBallSegment.Left,
                startAngle = 135f,
                color = gestureActionColor(swipeLeftAction),
            ),
            GestureBallSegmentVisual(
                segment = GestureBallSegment.Up,
                startAngle = 225f,
                color = gestureActionColor(swipeUpAction),
            ),
            GestureBallSegmentVisual(
                segment = GestureBallSegment.Right,
                startAngle = 315f,
                color = gestureActionColor(swipeRightAction),
            ),
            GestureBallSegmentVisual(
                segment = GestureBallSegment.Down,
                startAngle = 45f,
                color = gestureActionColor(swipeDownAction),
            ),
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                containerSize = it
                if (initialized) {
                    center = clampCenter(center, it)
                }
            },
    ) {
        if (initialized) {
            val sizePx = outerRadiusPx * 2f
            val ballSizeDp = with(density) { sizePx.toDp() }
            val centerFillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            Canvas(
                modifier = Modifier
                    .size(ballSizeDp)
                    .graphicsLayer {
                        translationX = center.x - outerRadiusPx
                        translationY = center.y - outerRadiusPx
                    }
                    .pointerInput(
                        topImageId,
                        swipeLeftAction,
                        swipeRightAction,
                        swipeUpAction,
                        swipeDownAction,
                        canSwipePrevious,
                        canSwipeNext,
                        isSwipeDeleteEnabled,
                        isFeedbackEnabled,
                        outerRadiusPx,
                        innerRadiusPx,
                    ) {
                        awaitEachGesture {
                            val localCenter = Offset(outerRadiusPx, outerRadiusPx)
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downDistance = (down.position - localCenter).getDistanceValue()
                            if (downDistance > innerRadiusPx) {
                                return@awaitEachGesture
                            }

                            fun toContainerPosition(pointerPosition: Offset): Offset {
                                return Offset(
                                    x = center.x - outerRadiusPx + pointerPosition.x,
                                    y = center.y - outerRadiusPx + pointerPosition.y,
                                )
                            }

                            isDragging = true
                            isRelocating = false
                            hoveredSegment = null
                            var activePointerId = down.id
                            var lastContainerPointer = toContainerPosition(down.position)
                            var hasRelocatedDuringGesture = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val activeChange = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull { it.pressed }
                                    ?: break

                                if (!activeChange.pressed) {
                                    break
                                }

                                val switchedPointer = activeChange.id != activePointerId
                                activePointerId = activeChange.id
                                val currentPosition = activeChange.position
                                val currentContainerPointer = toContainerPosition(currentPosition)
                                val movementFromCenter = (currentPosition - localCenter).getDistanceValue()
                                val holdDuration = activeChange.uptimeMillis - down.uptimeMillis

                                if (
                                    !hasRelocatedDuringGesture &&
                                    holdDuration >= GESTURE_BALL_HOLD_TO_DRAG_MS &&
                                    movementFromCenter <= innerRadiusPx * GESTURE_BALL_DRAG_CENTER_RATIO
                                ) {
                                    hasRelocatedDuringGesture = true
                                    isRelocating = true
                                    hoveredSegment = null
                                    hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    if (isFeedbackEnabled) {
                                        hostView.playSoundEffect(SoundEffectConstants.CLICK)
                                    }
                                    lastContainerPointer = currentContainerPointer
                                }

                                if (hasRelocatedDuringGesture) {
                                    isRelocating = true
                                    hoveredSegment = null
                                    if (!switchedPointer) {
                                        val dragDelta = currentContainerPointer - lastContainerPointer
                                        if (dragDelta != Offset.Zero) {
                                            center = clampCenter(center + dragDelta)
                                        }
                                    }
                                } else {
                                    isRelocating = false
                                    val nextHoveredSegment = resolveGestureBallSegment(
                                        center = localCenter,
                                        pointer = currentPosition,
                                        innerRadius = innerRadiusPx,
                                        leftAction = swipeLeftAction,
                                        rightAction = swipeRightAction,
                                        upAction = swipeUpAction,
                                        downAction = swipeDownAction,
                                    )
                                    if (nextHoveredSegment != hoveredSegment) {
                                        hoveredSegment = nextHoveredSegment
                                        if (nextHoveredSegment != null) {
                                            hostView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            if (isFeedbackEnabled) {
                                                hostView.playSoundEffect(SoundEffectConstants.CLICK)
                                            }
                                        }
                                    }
                                }

                                activeChange.consume()
                                lastContainerPointer = currentContainerPointer
                            }

                            val action = hoveredSegment?.action
                            if (
                                !hasRelocatedDuringGesture &&
                                action != null &&
                                canExecuteAction(
                                    action = action,
                                    canSwipePrevious = canSwipePrevious,
                                    canSwipeNext = canSwipeNext,
                                    isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                                )
                            ) {
                                onSwipeAction(action, topImageId)
                            }

                            isDragging = false
                            isRelocating = false
                            hoveredSegment = null
                        }
                    },
            ) {
                val ringRadius = (size.minDimension - strokeWidthPx) / 2f
                val topLeft = Offset(
                    x = size.width / 2f - ringRadius,
                    y = size.height / 2f - ringRadius,
                )
                val arcSize = Size(ringRadius * 2f, ringRadius * 2f)

                segmentVisuals.forEach { visual ->
                    val highlightScale = if (hoveredSegment?.segment == visual.segment) 1f else 0.76f
                    drawArc(
                        color = visual.color.copy(alpha = highlightScale),
                        startAngle = visual.startAngle,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                    )
                }

                val localCenter = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = centerFillColor,
                    radius = innerRadiusPx,
                    center = localCenter,
                )
            }

            if (showActionHint && isDragging && previewAction != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = if (canExecutePreview) {
                            stringResource(
                                id = R.string.gesture_ball_preview_execute,
                                stringResource(id = swipeActionLabelRes(previewAction)),
                            )
                        } else {
                            stringResource(
                                id = R.string.gesture_ball_preview_unavailable,
                                stringResource(id = swipeActionLabelRes(previewAction)),
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private data class GestureBallSegmentVisual(
    val segment: GestureBallSegment,
    val startAngle: Float,
    val color: Color,
)

private data class GestureBallHover(
    val segment: GestureBallSegment,
    val action: SwipeAction,
)

private enum class GestureBallSegment {
    Left,
    Up,
    Right,
    Down,
}

private fun resolveGestureBallSegment(
    center: Offset,
    pointer: Offset,
    innerRadius: Float,
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    upAction: SwipeAction,
    downAction: SwipeAction,
): GestureBallHover? {
    val delta = pointer - center
    val distance = delta.getDistance()

    if (distance < innerRadius) {
        return null
    }

    val segment = if (abs(delta.x) >= abs(delta.y)) {
        if (delta.x < 0f) {
            GestureBallSegment.Left
        } else {
            GestureBallSegment.Right
        }
    } else {
        if (delta.y < 0f) {
            GestureBallSegment.Up
        } else {
            GestureBallSegment.Down
        }
    }

    val action = when (segment) {
        GestureBallSegment.Left -> leftAction
        GestureBallSegment.Right -> rightAction
        GestureBallSegment.Up -> upAction
        GestureBallSegment.Down -> downAction
    }

    return GestureBallHover(segment = segment, action = action)
}

private fun gestureActionColor(action: SwipeAction): Color {
    return when (action) {
        SwipeAction.Delete -> Color(0xFFD32F2F)
        SwipeAction.Next -> Color(0xFF2E7D32)
        SwipeAction.Previous -> Color.White
        SwipeAction.Skip -> Color(0xFFD7D7D7)
    }
}

private fun swipeActionLabelRes(action: SwipeAction): Int {
    return when (action) {
        SwipeAction.Skip -> R.string.swipe_action_skip
        SwipeAction.Delete -> R.string.swipe_action_delete
        SwipeAction.Previous -> R.string.swipe_action_previous
        SwipeAction.Next -> R.string.swipe_action_next
    }
}

private fun canExecuteAction(
    action: SwipeAction,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    isSwipeDeleteEnabled: Boolean,
): Boolean {
    if (action == SwipeAction.Skip) {
        return true
    }

    return canSwipeForAction(
        action = action,
        canSwipePrevious = canSwipePrevious,
        canSwipeNext = canSwipeNext,
        isSwipeDeleteEnabled = isSwipeDeleteEnabled,
    )
}

private fun Offset.getDistanceValue(): Float {
    return kotlin.math.sqrt((x * x) + (y * y))
}

@Composable
private fun SwipeDeleteControls(
    isSwipeDeleteEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.swipe_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isSwipeDeleteEnabled) {
                        stringResource(id = R.string.swipe_delete_enabled_description)
                    } else {
                        stringResource(id = R.string.swipe_delete_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isSwipeDeleteEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun TapImageToggleControls(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.tap_image_toggle_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isEnabled) {
                        stringResource(id = R.string.tap_image_toggle_enabled_description)
                    } else {
                        stringResource(id = R.string.tap_image_toggle_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun DeleteReminderControls(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.delete_reminder_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isEnabled) {
                        stringResource(id = R.string.delete_reminder_enabled_description)
                    } else {
                        stringResource(id = R.string.delete_reminder_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SilentDeleteControls(
    isSilentDeleteEnabled: Boolean,
    dcimLabel: String?,
    picturesLabel: String?,
    hasDcimDirectoryAuthorized: Boolean,
    hasPicturesDirectoryAuthorized: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onConfigureDcimDirectory: () -> Unit,
    onConfigurePicturesDirectory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAnyDirectory = hasDcimDirectoryAuthorized || hasPicturesDirectoryAuthorized
    val dcimDisplayLabel = dcimLabel ?: stringResource(id = R.string.silent_delete_scope_dcim)
    val picturesDisplayLabel = picturesLabel ?: stringResource(id = R.string.silent_delete_scope_pictures)

    val grantedSummary = listOfNotNull(
        dcimDisplayLabel.takeIf { hasDcimDirectoryAuthorized },
        picturesDisplayLabel.takeIf { hasPicturesDirectoryAuthorized },
    ).joinToString(separator = " / ")

    val description = when {
        isSilentDeleteEnabled && hasAnyDirectory -> stringResource(
            id = R.string.silent_delete_enabled_description,
            grantedSummary,
        )
        isSilentDeleteEnabled -> stringResource(id = R.string.silent_delete_pending_description)
        hasAnyDirectory -> stringResource(
            id = R.string.silent_delete_directory_ready_description,
            grantedSummary,
        )
        else -> stringResource(id = R.string.silent_delete_disabled_description)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.silent_delete_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isSilentDeleteEnabled,
                    onCheckedChange = onCheckedChange,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.silent_delete_scope_dcim),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (hasDcimDirectoryAuthorized) {
                            stringResource(id = R.string.silent_delete_scope_ready_description, dcimDisplayLabel)
                        } else {
                            stringResource(id = R.string.silent_delete_scope_missing_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedButton(onClick = onConfigureDcimDirectory) {
                    Text(
                        text = stringResource(
                            id = if (hasDcimDirectoryAuthorized) {
                                R.string.silent_delete_change_directory
                            } else {
                                R.string.silent_delete_select_directory
                            },
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.silent_delete_scope_pictures),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (hasPicturesDirectoryAuthorized) {
                            stringResource(id = R.string.silent_delete_scope_ready_description, picturesDisplayLabel)
                        } else {
                            stringResource(id = R.string.silent_delete_scope_missing_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedButton(onClick = onConfigurePicturesDirectory) {
                    Text(
                        text = stringResource(
                            id = if (hasPicturesDirectoryAuthorized) {
                                R.string.silent_delete_change_directory
                            } else {
                                R.string.silent_delete_select_directory
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeBehaviorControls(
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    swipeGestureSensitivity: Float,
    onSwipeLeftActionChange: (SwipeAction) -> Unit,
    onSwipeRightActionChange: (SwipeAction) -> Unit,
    onSwipeUpActionChange: (SwipeAction) -> Unit,
    onSwipeDownActionChange: (SwipeAction) -> Unit,
    onSwipeGestureSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        SwipeActionUiOption(SwipeAction.Skip, stringResource(id = R.string.swipe_action_skip)),
        SwipeActionUiOption(SwipeAction.Delete, stringResource(id = R.string.swipe_action_delete)),
        SwipeActionUiOption(SwipeAction.Previous, stringResource(id = R.string.swipe_action_previous)),
        SwipeActionUiOption(SwipeAction.Next, stringResource(id = R.string.swipe_action_next)),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(id = R.string.swipe_behavior_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(id = R.string.swipe_behavior_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(
                    id = R.string.swipe_sensitivity_label,
                    (swipeGestureSensitivity * 100f).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            Slider(
                value = swipeGestureSensitivity,
                onValueChange = onSwipeGestureSensitivityChange,
                valueRange = SettingsRepository.MIN_SWIPE_GESTURE_SENSITIVITY..
                    SettingsRepository.MAX_SWIPE_GESTURE_SENSITIVITY,
            )

            Text(
                text = stringResource(id = R.string.swipe_sensitivity_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SwipeActionSelectorRow(
                directionLabel = stringResource(id = R.string.swipe_direction_left),
                selectedAction = swipeLeftAction,
                options = options,
                onActionChange = onSwipeLeftActionChange,
            )

            SwipeActionSelectorRow(
                directionLabel = stringResource(id = R.string.swipe_direction_right),
                selectedAction = swipeRightAction,
                options = options,
                onActionChange = onSwipeRightActionChange,
            )

            SwipeActionSelectorRow(
                directionLabel = stringResource(id = R.string.swipe_direction_up),
                selectedAction = swipeUpAction,
                options = options,
                onActionChange = onSwipeUpActionChange,
            )

            SwipeActionSelectorRow(
                directionLabel = stringResource(id = R.string.swipe_direction_down),
                selectedAction = swipeDownAction,
                options = options,
                onActionChange = onSwipeDownActionChange,
            )
        }
    }
}

@Composable
private fun SwipeActionSelectorRow(
    directionLabel: String,
    selectedAction: SwipeAction,
    options: List<SwipeActionUiOption>,
    onActionChange: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(directionLabel) { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.action == selectedAction } ?: options.first()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = directionLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(text = selectedOption.label)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label) },
                        onClick = {
                            expanded = false
                            onActionChange(option.action)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSettingsControls(
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        LanguageUiOption(SettingsRepository.SYSTEM_LANGUAGE_TAG, stringResource(id = R.string.language_auto)),
        LanguageUiOption("ar", stringResource(id = R.string.language_name_arabic)),
        LanguageUiOption("en", stringResource(id = R.string.language_name_english)),
        LanguageUiOption("es", stringResource(id = R.string.language_name_spanish)),
        LanguageUiOption("fr", stringResource(id = R.string.language_name_french)),
        LanguageUiOption("ru", stringResource(id = R.string.language_name_russian)),
        LanguageUiOption("zh", stringResource(id = R.string.language_name_chinese)),
    )

    val effectiveTag = selectedLanguageTag.substringBefore('-').lowercase().ifBlank {
        SettingsRepository.SYSTEM_LANGUAGE_TAG
    }
    val selectedOption = options.firstOrNull { it.tag == effectiveTag } ?: options.first()
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.language_option),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = selectedOption.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = stringResource(id = R.string.language_change_action))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.label) },
                            onClick = {
                                expanded = false
                                onLanguageTagChange(option.tag)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoDeck(
    visibleIds: List<Long>,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    isSwipeDeleteEnabled: Boolean,
    swipeGestureSensitivity: Float,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    showFullImage: Boolean,
    isTapImageToggleEnabled: Boolean,
    onSwipeAction: (SwipeAction, Long) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var topCardDragProgress by remember { mutableFloatStateOf(0f) }
    var isTopCardForceFullImage by remember { mutableStateOf(false) }
    var isTopCardImageGestureLocked by remember { mutableStateOf(false) }

    LaunchedEffect(visibleIds.firstOrNull()) {
        topCardDragProgress = 0f
        isTopCardForceFullImage = false
        isTopCardImageGestureLocked = false
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val deckWidth = minOf(maxWidth, maxHeight * CARD_ASPECT_RATIO).coerceAtMost(MAX_DECK_WIDTH)
        val deckHeight = deckWidth / CARD_ASPECT_RATIO

        Box(
            modifier = Modifier.size(
                width = deckWidth,
                height = deckHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            val topCardShowFullImage = showFullImage || isTopCardForceFullImage

            for (index in visibleIds.indices.reversed()) {
                val imageId = visibleIds[index]
                val isTopCard = index == 0
                val layerInset = CARD_LAYER_INSET * index.toFloat()
                val horizontalInset = layerInset
                val verticalInset = layerInset * 1.2f
                val cardZIndex = (visibleIds.size - index).toFloat()

                key(imageId) {
                    SwipeableCard(
                        onSwiped = { direction ->
                            val action = when (direction) {
                                SwipeDirection.Left -> swipeLeftAction
                                SwipeDirection.Right -> swipeRightAction
                                SwipeDirection.Up -> swipeUpAction
                                SwipeDirection.Down -> swipeDownAction
                            }
                            onSwipeAction(action, imageId)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = horizontalInset,
                                vertical = verticalInset,
                            )
                            .zIndex(cardZIndex),
                        enabled = isTopCard && !isTopCardImageGestureLocked,
                        gestureSensitivity = swipeGestureSensitivity,
                        canSwipeLeft = isTopCard && canSwipeForAction(
                            action = swipeLeftAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeRight = isTopCard && canSwipeForAction(
                            action = swipeRightAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeUp = isTopCard && canSwipeForAction(
                            action = swipeUpAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeDown = isTopCard && canSwipeForAction(
                            action = swipeDownAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        restingScale = when (index) {
                            0 -> 1f
                            1 -> 0.96f
                            else -> 0.92f
                        },
                        revealProgress = when (index) {
                            0 -> 0f
                            1 -> topCardDragProgress
                            else -> topCardDragProgress * BACK_CARD_REVEAL_MULTIPLIER
                        },
                        onDragProgressChanged = if (isTopCard) {
                            { topCardDragProgress = it }
                        } else {
                            {}
                        },
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        PhotoCardImage(
                            imageId = imageId,
                            showFullImage = if (isTopCard) topCardShowFullImage else showFullImage,
                            enableTwoFingerTransform = isTopCard,
                            enableTapToggle = isTapImageToggleEnabled,
                            onGestureLockChanged = if (isTopCard) {
                                { isTopCardImageGestureLocked = it }
                            } else {
                                {}
                            },
                            onTapWhenIdle = if (isTopCard) {
                                { isTopCardForceFullImage = !isTopCardForceFullImage }
                            } else {
                                {}
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCardImage(
    imageId: Long,
    showFullImage: Boolean,
    enableTwoFingerTransform: Boolean,
    enableTapToggle: Boolean,
    onGestureLockChanged: (Boolean) -> Unit,
    onTapWhenIdle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(context, imageId) {
        ImageRequest.Builder(context)
            .data(IntentHelper.buildImageUri(imageId))
            .crossfade(true)
            .build()
    }

    var visualState by remember(imageId) { mutableStateOf(PhotoVisualState.Loading) }
    val currentOnGestureLockChanged by rememberUpdatedState(onGestureLockChanged)
    val currentOnTapWhenIdle by rememberUpdatedState(onTapWhenIdle)
    val placeholderPainter = ColorPainter(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    )
    val transparentPainter = remember { ColorPainter(Color.Transparent) }

    LaunchedEffect(enableTwoFingerTransform) {
        if (!enableTwoFingerTransform) {
            currentOnGestureLockChanged(false)
        }
    }

    DisposableEffect(currentOnGestureLockChanged) {
        onDispose {
            currentOnGestureLockChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            ),
    ) {
        if (visualState != PhotoVisualState.Ready) {
            PhotoFallbackContent(
                isError = visualState == PhotoVisualState.Error,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (enableTwoFingerTransform) {
            val zoomState = rememberCoilZoomState()
            val zoomableState = zoomState.zoomable
            val hasUserTransform = zoomableState.userTransform.isNotEmpty()
            val cardGestureLocked =
                hasUserTransform ||
                    zoomableState.continuousTransformType != ContinuousTransformType.NONE

            LaunchedEffect(zoomableState) {
                zoomableState.setLimitOffsetWithinBaseVisibleRect(true)
                zoomableState.setScalesCalculator(ScalesCalculator.fixed(2f))
                zoomableState.setThreeStepScale(false)
                zoomableState.setRubberBandScale(false)
                zoomableState.setKeepTransformWhenSameAspectRatioContentSizeChanged(false)
                zoomableState.setDisabledGestureTypes(
                    GestureType.ONE_FINGER_SCALE or GestureType.DOUBLE_TAP_SCALE,
                )
            }

            LaunchedEffect(cardGestureLocked) {
                currentOnGestureLockChanged(cardGestureLocked)
            }

            CoilZoomAsyncImage(
                model = request,
                contentDescription = stringResource(id = R.string.photo_content_description),
                imageLoader = context.imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (showFullImage) ContentScale.Fit else ContentScale.Crop,
                placeholder = placeholderPainter,
                error = transparentPainter,
                fallback = transparentPainter,
                zoomState = zoomState,
                scrollBar = null,
                onLoading = { visualState = PhotoVisualState.Loading },
                onSuccess = { visualState = PhotoVisualState.Ready },
                onError = { visualState = PhotoVisualState.Error },
                onTap = {
                    if (zoomableState.userTransform.isNotEmpty()) {
                        zoomableState.reset()
                    } else if (enableTapToggle) {
                        currentOnTapWhenIdle()
                    }
                },
            )
        } else {
            AsyncImage(
                model = request,
                contentDescription = stringResource(id = R.string.photo_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = if (showFullImage) ContentScale.Fit else ContentScale.Crop,
                placeholder = placeholderPainter,
                error = transparentPainter,
                fallback = transparentPainter,
                onLoading = { visualState = PhotoVisualState.Loading },
                onSuccess = { visualState = PhotoVisualState.Ready },
                onError = { visualState = PhotoVisualState.Error },
            )
        }
    }
}

@Composable
private fun PhotoFallbackContent(
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        ) {
            Box(
                modifier = Modifier.padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!isError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = if (isError) {
                stringResource(id = R.string.photo_load_error_title)
            } else {
                stringResource(id = R.string.photo_loading_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (isError) {
                stringResource(id = R.string.photo_load_error_description)
            } else {
                stringResource(id = R.string.photo_loading_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class PhotoVisualState {
    Loading,
    Ready,
    Error,
}

private data class LanguageUiOption(
    val tag: String,
    val label: String,
)

private data class SwipeActionUiOption(
    val action: SwipeAction,
    val label: String,
)

private fun canSwipeForAction(
    action: SwipeAction,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    isSwipeDeleteEnabled: Boolean,
): Boolean {
    return when (action) {
        SwipeAction.Skip -> false
        SwipeAction.Previous -> canSwipePrevious
        SwipeAction.Next -> canSwipeNext
        SwipeAction.Delete -> isSwipeDeleteEnabled || canSwipeNext
    }
}

private const val CARD_ASPECT_RATIO = 0.72f
private const val BACK_CARD_REVEAL_MULTIPLIER = 0.72f
private val CARD_LAYER_INSET = 12.dp
private val MAX_DECK_WIDTH = 460.dp
private val FLOATING_DELETE_BUTTON_SIZE = 54.dp
private const val FLOATING_DELETE_BUTTON_DEFAULT_CENTER_Y_RATIO = 0.74f
private val FLOATING_DELETE_BUTTON_GESTURE_BALL_CLEARANCE = 18.dp
private val GESTURE_BALL_BASE_OUTER_RADIUS = 62.dp
private val GESTURE_BALL_MARGIN = 20.dp
private const val GESTURE_BALL_DEFAULT_CENTER_Y_RATIO = 0.55f
private const val GESTURE_BALL_DRAG_CENTER_RATIO = 0.55f
private const val GESTURE_BALL_HOLD_TO_DRAG_MS = 180L
private const val DEFAULT_BEHAVIOR_NOTICE_AUTO_COLLAPSE_DELAY_MS = 5_000L

private val PREVIEW_DEFAULT_LEFT_ACTION = SettingsRepository.DEFAULT_LEFT_ACTION
private val PREVIEW_DEFAULT_RIGHT_ACTION = SettingsRepository.DEFAULT_RIGHT_ACTION
private val PREVIEW_DEFAULT_UP_ACTION = SettingsRepository.DEFAULT_UP_ACTION
private val PREVIEW_DEFAULT_DOWN_ACTION = SettingsRepository.DEFAULT_DOWN_ACTION

@Preview(showBackground = true, backgroundColor = 0xFFF4F1EA)
@Composable
private fun MainScreenReadyPreview() {
    MaterialTheme {
        val previewSnackbarHostState = remember { SnackbarHostState() }
        MainScreenContent(
            state = HomeUiState.Ready(
                visibleIds = listOf(11L, 12L, 13L),
                canSwipeToPrevious = true,
                canSwipeToNext = true,
            ),
            permissionMode = PermissionHelper.PermissionMode.GRANTED_PARTIAL,
            isSwipeDeleteEnabled = true,
            isDeleteReminderEnabled = SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
            swipeGestureSensitivity = SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY,
            showFullImage = false,
            isTapImageToggleEnabled = SettingsRepository.DEFAULT_TAP_IMAGE_TOGGLE_ENABLED,
            showFloatingDeleteButton = true,
            isGestureBallEnabled = true,
            gestureBallSizeScale = SettingsRepository.DEFAULT_GESTURE_BALL_SIZE_SCALE,
            isGestureBallFeedbackEnabled = SettingsRepository.DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED,
            showGestureBallActionHint = SettingsRepository.DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED,
            isSilentDeleteEnabled = true,
            silentDeleteDcimLabel = "DCIM",
            silentDeletePicturesLabel = "Pictures",
            hasDcimDirectoryAuthorized = true,
            hasPicturesDirectoryAuthorized = true,
            swipeLeftAction = PREVIEW_DEFAULT_LEFT_ACTION,
            swipeRightAction = PREVIEW_DEFAULT_RIGHT_ACTION,
            swipeUpAction = PREVIEW_DEFAULT_UP_ACTION,
            swipeDownAction = PREVIEW_DEFAULT_DOWN_ACTION,
            defaultBehaviorNoticeMode = DefaultBehaviorNoticeMode.Visible,
            shouldShowDefaultBehaviorNotice = true,
            availableUpdateRelease = null,
            updateCheckFeedback = UpdateCheckFeedback.Idle,
            isUpdateInstallInProgress = false,
            snackbarHostState = previewSnackbarHostState,
            onRequestPermission = {},
            onOpenSettings = {},
            onPermissionRationaleDismissed = {},
            onRefresh = {},
            onSwipeDeleteEnabledChange = {},
            onDeleteReminderEnabledChange = {},
            onSwipeGestureSensitivityChange = {},
            onShowFullImageChange = {},
            onTapImageToggleEnabledChange = {},
            onShowFloatingDeleteButtonChange = {},
            onGestureBallEnabledChange = {},
            onGestureBallSizeScaleChange = {},
            onGestureBallFeedbackEnabledChange = {},
            onShowGestureBallActionHintChange = {},
            onSilentDeleteEnabledChange = {},
            onConfigureSilentDeleteDcimDirectory = {},
            onConfigureSilentDeletePicturesDirectory = {},
            onSwipeLeftActionChange = {},
            onSwipeRightActionChange = {},
            onSwipeUpActionChange = {},
            onSwipeDownActionChange = {},
            onDefaultBehaviorNoticeEnabledChange = {},
            selectedLanguageTag = SettingsRepository.SYSTEM_LANGUAGE_TAG,
            onLanguageTagChange = {},
            onSwipeAction = { _, _ -> true },
            onManualUpdateCheck = {},
            onClearUpdateFeedback = {},
            onDismissAvailableUpdate = {},
            onDeferCurrentUpdate = {},
            onStartUpdateInstallation = {},
            showPermissionRationale = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F1EA)
@Composable
private fun MainScreenPermissionPreview() {
    MaterialTheme {
        val previewSnackbarHostState = remember { SnackbarHostState() }
        MainScreenContent(
            state = HomeUiState.PermissionDenied,
            permissionMode = PermissionHelper.PermissionMode.DENIED,
            isSwipeDeleteEnabled = false,
            isDeleteReminderEnabled = SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
            swipeGestureSensitivity = SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY,
            showFullImage = false,
            isTapImageToggleEnabled = SettingsRepository.DEFAULT_TAP_IMAGE_TOGGLE_ENABLED,
            showFloatingDeleteButton = false,
            isGestureBallEnabled = false,
            gestureBallSizeScale = SettingsRepository.DEFAULT_GESTURE_BALL_SIZE_SCALE,
            isGestureBallFeedbackEnabled = SettingsRepository.DEFAULT_GESTURE_BALL_FEEDBACK_ENABLED,
            showGestureBallActionHint = SettingsRepository.DEFAULT_GESTURE_BALL_ACTION_HINT_ENABLED,
            isSilentDeleteEnabled = false,
            silentDeleteDcimLabel = null,
            silentDeletePicturesLabel = null,
            hasDcimDirectoryAuthorized = false,
            hasPicturesDirectoryAuthorized = false,
            swipeLeftAction = PREVIEW_DEFAULT_LEFT_ACTION,
            swipeRightAction = PREVIEW_DEFAULT_RIGHT_ACTION,
            swipeUpAction = PREVIEW_DEFAULT_UP_ACTION,
            swipeDownAction = PREVIEW_DEFAULT_DOWN_ACTION,
            defaultBehaviorNoticeMode = DefaultBehaviorNoticeMode.Visible,
            shouldShowDefaultBehaviorNotice = true,
            availableUpdateRelease = null,
            updateCheckFeedback = UpdateCheckFeedback.Idle,
            isUpdateInstallInProgress = false,
            snackbarHostState = previewSnackbarHostState,
            onRequestPermission = {},
            onOpenSettings = {},
            onPermissionRationaleDismissed = {},
            onRefresh = {},
            onSwipeDeleteEnabledChange = {},
            onDeleteReminderEnabledChange = {},
            onSwipeGestureSensitivityChange = {},
            onShowFullImageChange = {},
            onTapImageToggleEnabledChange = {},
            onShowFloatingDeleteButtonChange = {},
            onGestureBallEnabledChange = {},
            onGestureBallSizeScaleChange = {},
            onGestureBallFeedbackEnabledChange = {},
            onShowGestureBallActionHintChange = {},
            onSilentDeleteEnabledChange = {},
            onConfigureSilentDeleteDcimDirectory = {},
            onConfigureSilentDeletePicturesDirectory = {},
            onSwipeLeftActionChange = {},
            onSwipeRightActionChange = {},
            onSwipeUpActionChange = {},
            onSwipeDownActionChange = {},
            onDefaultBehaviorNoticeEnabledChange = {},
            selectedLanguageTag = SettingsRepository.SYSTEM_LANGUAGE_TAG,
            onLanguageTagChange = {},
            onSwipeAction = { _, _ -> true },
            onManualUpdateCheck = {},
            onClearUpdateFeedback = {},
            onDismissAvailableUpdate = {},
            onDeferCurrentUpdate = {},
            onStartUpdateInstallation = {},
            showPermissionRationale = true,
        )
    }
}

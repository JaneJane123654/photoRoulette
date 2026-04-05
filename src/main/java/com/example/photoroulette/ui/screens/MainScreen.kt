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


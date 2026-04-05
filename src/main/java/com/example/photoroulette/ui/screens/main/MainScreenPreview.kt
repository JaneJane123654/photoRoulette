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
internal fun MainScreenReadyPreview() {
    MaterialTheme {
        val previewSnackbarHostState = remember { SnackbarHostState() }
        MainScreenContent(
            state = HomeUiState.Ready(
                previousCard = MediaCard(
                    id = 10L,
                    mimeType = "image/jpeg",
                    kind = MediaKind.Image,
                    previewUri = Uri.EMPTY,
                ),
                visibleCards = listOf(
                    MediaCard(
                        id = 11L,
                        mimeType = "image/jpeg",
                        kind = MediaKind.Image,
                        previewUri = Uri.EMPTY,
                    ),
                    MediaCard(
                        id = 12L,
                        mimeType = "image/gif",
                        kind = MediaKind.AnimatedImage,
                        previewUri = Uri.EMPTY,
                    ),
                    MediaCard(
                        id = 13L,
                        mimeType = "video/mp4",
                        kind = MediaKind.Video,
                        previewUri = Uri.EMPTY,
                        playbackUri = Uri.EMPTY,
                        durationMs = 800L,
                    ),
                ),
                canSwipeToPrevious = true,
                canSwipeToNext = true,
            ),
            permissionMode = PermissionHelper.PermissionMode.GRANTED_PARTIAL,
            isSwipeDeleteEnabled = true,
            isDeleteReminderEnabled = SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
            swipeGestureSensitivity = SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY,
            showFullImage = false,
            showCardActionsButton = true,
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
            onShowCardActionsButtonChange = {},
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
internal fun MainScreenPermissionPreview() {
    MaterialTheme {
        val previewSnackbarHostState = remember { SnackbarHostState() }
        MainScreenContent(
            state = HomeUiState.PermissionDenied,
            permissionMode = PermissionHelper.PermissionMode.DENIED,
            isSwipeDeleteEnabled = false,
            isDeleteReminderEnabled = SettingsRepository.DEFAULT_DELETE_REMINDER_ENABLED,
            swipeGestureSensitivity = SettingsRepository.DEFAULT_SWIPE_GESTURE_SENSITIVITY,
            showFullImage = false,
            showCardActionsButton = false,
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
            onShowCardActionsButtonChange = {},
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


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
internal fun PhotoFallbackContent(
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

internal enum class PhotoVisualState {
    Loading,
    Ready,
    Error,
}

internal class DeckPlayerPool(context: android.content.Context) {
    private val appContext = context.applicationContext

    private var internalActivePlayer by mutableStateOf(buildPlayer(looping = true))
    private var internalWarmupPlayer: ExoPlayer = buildPlayer(looping = false)
    val activePlayer: ExoPlayer
        get() = internalActivePlayer

    private var activeUri: Uri? = null
    private var warmupUri: Uri? = null

    fun activate(
        uri: Uri,
        autoPlay: Boolean,
    ) {
        if (activeUri == uri) {
            applyActivePlayback(autoPlay)
            return
        }

        if (warmupUri == uri) {
            val promotedPlayer = internalWarmupPlayer
            val demotedPlayer = internalActivePlayer

            internalActivePlayer = promotedPlayer
            internalWarmupPlayer = demotedPlayer
            activeUri = uri
            warmupUri = null

            applyActivePlayback(autoPlay)
            clearPlayer(internalWarmupPlayer)
            internalWarmupPlayer.repeatMode = Player.REPEAT_MODE_OFF
            return
        }

        activeUri = uri
        clearPlayer(internalActivePlayer)
        internalActivePlayer.repeatMode = Player.REPEAT_MODE_ALL
        internalActivePlayer.setMediaItem(MediaItem.fromUri(uri))
        internalActivePlayer.prepare()
        applyActivePlayback(autoPlay)
    }

    fun warmup(uri: Uri?) {
        if (uri == null) {
            clearWarmup()
            return
        }

        if (uri == activeUri || uri == warmupUri) {
            return
        }

        warmupUri = uri
        clearPlayer(internalWarmupPlayer)
        internalWarmupPlayer.repeatMode = Player.REPEAT_MODE_OFF
        internalWarmupPlayer.setMediaItem(MediaItem.fromUri(uri))
        internalWarmupPlayer.prepare()
        internalWarmupPlayer.playWhenReady = false
    }

    fun clearActive() {
        activeUri = null
        internalActivePlayer.playWhenReady = false
        clearPlayer(internalActivePlayer)
    }

    fun isActiveUri(uri: Uri?): Boolean = activeUri != null && activeUri == uri

    fun release() {
        internalActivePlayer.release()
        internalWarmupPlayer.release()
        activeUri = null
        warmupUri = null
    }

    private fun clearWarmup() {
        warmupUri = null
        internalWarmupPlayer.playWhenReady = false
        clearPlayer(internalWarmupPlayer)
    }

    private fun applyActivePlayback(autoPlay: Boolean) {
        internalActivePlayer.repeatMode = Player.REPEAT_MODE_ALL
        internalActivePlayer.playWhenReady = autoPlay
        if (autoPlay) {
            internalActivePlayer.play()
        } else {
            internalActivePlayer.pause()
            internalActivePlayer.seekTo(0L)
        }
    }

    private fun clearPlayer(player: ExoPlayer) {
        player.stop()
        player.clearMediaItems()
    }

    private fun buildPlayer(looping: Boolean): ExoPlayer {
        return ExoPlayer.Builder(appContext)
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                repeatMode = if (looping) {
                    Player.REPEAT_MODE_ALL
                } else {
                    Player.REPEAT_MODE_OFF
                }
                volume = 0f
                playWhenReady = false
            }
    }
}

internal data class LanguageUiOption(
    val tag: String,
    val label: String,
)

internal data class SwipeActionUiOption(
    val action: SwipeAction,
    val label: String,
)

internal fun canSwipeForAction(
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

internal const val CARD_ASPECT_RATIO = 0.72f
internal const val BACK_CARD_REVEAL_MULTIPLIER = 0.72f
internal const val DOWN_SWIPE_COVER_VISIBILITY_EPSILON = 0.001f
internal const val RIGHT_SWIPE_COVER_VISIBILITY_EPSILON = 0.001f
internal const val MIN_PHOTO_GESTURE_SCALE = 1f
internal const val MAX_PHOTO_GESTURE_SCALE = 4f
internal const val PHOTO_GESTURE_EPSILON = 0.0001f
internal const val PHOTO_GESTURE_LOCK_SCALE_EPSILON = 0.02f
internal const val PHOTO_GESTURE_OFFSET_LOCK_EPSILON = 0.5f
internal const val PHOTO_GESTURE_AXIS_UNLOCK_EPSILON = 0.8f
internal const val PHOTO_GESTURE_RESET_SNAP_EPSILON = 0.03f
internal val CARD_LAYER_INSET = 12.dp
internal val MAX_DECK_WIDTH = 460.dp
internal val FLOATING_DELETE_BUTTON_SIZE = 54.dp
internal const val FLOATING_DELETE_BUTTON_DEFAULT_CENTER_Y_RATIO = 0.74f
internal val FLOATING_DELETE_BUTTON_GESTURE_BALL_CLEARANCE = 18.dp
internal val GESTURE_BALL_BASE_OUTER_RADIUS = 62.dp
internal val GESTURE_BALL_MARGIN = 20.dp
internal const val GESTURE_BALL_DEFAULT_CENTER_Y_RATIO = 0.55f
internal const val GESTURE_BALL_DRAG_CENTER_RATIO = 0.55f
internal const val GESTURE_BALL_HOLD_TO_DRAG_MS = 180L
internal const val DEFAULT_BEHAVIOR_NOTICE_AUTO_COLLAPSE_DELAY_MS = 5_000L
internal const val ANIMATED_FALLBACK_SIZE_PX = 1080

internal val PREVIEW_DEFAULT_LEFT_ACTION = SettingsRepository.DEFAULT_LEFT_ACTION
internal val PREVIEW_DEFAULT_RIGHT_ACTION = SettingsRepository.DEFAULT_RIGHT_ACTION
internal val PREVIEW_DEFAULT_UP_ACTION = SettingsRepository.DEFAULT_UP_ACTION
internal val PREVIEW_DEFAULT_DOWN_ACTION = SettingsRepository.DEFAULT_DOWN_ACTION


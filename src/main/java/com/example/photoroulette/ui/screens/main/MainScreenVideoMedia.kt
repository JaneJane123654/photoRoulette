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
internal fun VideoCardContent(
    card: MediaCard,
    isTopCard: Boolean,
    playerPool: DeckPlayerPool,
    showFullImage: Boolean,
    onGestureLockChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playbackUri = card.playbackUri
    val activePlayer = playerPool.activePlayer
    val isLivePhoto = card.kind == MediaKind.LivePhoto
    val shouldAttachPlayer = isTopCard && playbackUri != null && playerPool.isActiveUri(playbackUri)

    var visualState by remember(card.id) { mutableStateOf(PhotoVisualState.Loading) }
    var isCoverVisible by remember(card.id) { mutableStateOf(true) }
    var isLiveMotionActive by remember(card.id) { mutableStateOf(false) }

    val currentIsLivePhoto by rememberUpdatedState(isLivePhoto)
    val currentIsLiveMotionActive by rememberUpdatedState(isLiveMotionActive)

    val previewRequest = remember(context, card.id, card.previewUri) {
        ImageRequest.Builder(context)
            .data(card.previewUri)
            .crossfade(false)
            .videoFrameMillis(0)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build()
    }

    val transparentPainter = remember { ColorPainter(Color.Transparent) }

    LaunchedEffect(Unit) {
        onGestureLockChanged(false)
    }

    LaunchedEffect(card.id, isTopCard, playbackUri) {
        isCoverVisible = true
        isLiveMotionActive = false
        visualState = PhotoVisualState.Loading

        if (!isTopCard || playbackUri == null || !playerPool.isActiveUri(playbackUri)) {
            return@LaunchedEffect
        }
    }

    DisposableEffect(activePlayer, playbackUri, shouldAttachPlayer) {
        if (!shouldAttachPlayer) {
            onDispose { }
        } else {
            val targetUri = playbackUri
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    if (!playerPool.isActiveUri(targetUri)) {
                        return
                    }

                    if (!currentIsLivePhoto || currentIsLiveMotionActive) {
                        isCoverVisible = false
                    }
                    visualState = PhotoVisualState.Ready
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (!playerPool.isActiveUri(targetUri)) {
                        return
                    }

                    isCoverVisible = true
                    isLiveMotionActive = false
                    visualState = PhotoVisualState.Error
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!playerPool.isActiveUri(targetUri)) {
                        return
                    }

                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            if (visualState != PhotoVisualState.Error) {
                                visualState = PhotoVisualState.Loading
                            }
                        }

                        else -> Unit
                    }
                }
            }

            activePlayer.addListener(listener)
            onDispose {
                activePlayer.removeListener(listener)
            }
        }
    }

    val interactionModifier = when {
        !isTopCard || playbackUri == null -> Modifier

        isLivePhoto -> Modifier.pointerInput(card.id, activePlayer, playbackUri) {
            detectTapGestures(
                onLongPress = {
                    if (!playerPool.isActiveUri(playbackUri)) {
                        return@detectTapGestures
                    }
                    isLiveMotionActive = true
                    isCoverVisible = true
                    visualState = PhotoVisualState.Loading
                    activePlayer.playWhenReady = true
                    activePlayer.play()
                },
                onPress = {
                    tryAwaitRelease()
                    if (isLiveMotionActive && playerPool.isActiveUri(playbackUri)) {
                        isLiveMotionActive = false
                        isCoverVisible = true
                        activePlayer.pause()
                        activePlayer.seekTo(0L)
                    }
                },
            )
        }

        else -> Modifier.pointerInput(card.id, activePlayer, playbackUri) {
            detectTapGestures(
                onTap = {
                    if (!playerPool.isActiveUri(playbackUri)) {
                        return@detectTapGestures
                    }
                    if (activePlayer.isPlaying) {
                        activePlayer.pause()
                    } else {
                        activePlayer.playWhenReady = true
                        activePlayer.play()
                    }
                },
            )
        }
    }

    DisposableEffect(playbackUri, isTopCard, isLivePhoto) {
        onDispose {
            if (isTopCard && playbackUri != null && playerPool.isActiveUri(playbackUri)) {
                if (isLivePhoto) {
                    activePlayer.pause()
                    activePlayer.seekTo(0L)
                } else {
                    activePlayer.pause()
                }
            }
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
            )
            .then(interactionModifier),
    ) {
        if (isTopCard && playbackUri != null) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                        setKeepContentOnPlayerReset(true)
                        resizeMode = if (showFullImage) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = if (showFullImage) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    playerView.player = if (playerPool.isActiveUri(playbackUri)) {
                        activePlayer
                    } else {
                        null
                    }
                },
            )
        }

        if (!isTopCard || isCoverVisible) {
            AsyncImage(
                model = previewRequest,
                contentDescription = stringResource(id = R.string.photo_content_description),
                modifier = Modifier.matchParentSize(),
                contentScale = if (showFullImage) ContentScale.Fit else ContentScale.Crop,
                placeholder = transparentPainter,
                error = transparentPainter,
                fallback = transparentPainter,
                onLoading = {
                    if (visualState != PhotoVisualState.Error) {
                        visualState = PhotoVisualState.Loading
                    }
                },
                onSuccess = {
                    if (visualState != PhotoVisualState.Error) {
                        visualState = PhotoVisualState.Ready
                    }
                },
                onError = {
                    if (!isTopCard || playbackUri == null) {
                        visualState = PhotoVisualState.Error
                    } else if (visualState != PhotoVisualState.Error) {
                        visualState = PhotoVisualState.Loading
                    }
                },
            )
        }

        if (visualState == PhotoVisualState.Error) {
            PhotoFallbackContent(
                isError = true,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}



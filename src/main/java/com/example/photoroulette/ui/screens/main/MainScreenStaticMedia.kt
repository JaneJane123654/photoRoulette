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
internal fun StaticPhotoCardImage(
    card: MediaCard,
    showFullImage: Boolean,
    enableTwoFingerTransform: Boolean,
    enableTapToggle: Boolean,
    onGestureLockChanged: (Boolean) -> Unit,
    onTapWhenIdle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val gestureScope = rememberCoroutineScope()
    val request = remember(context, card.id, card.previewUri) {
        ImageRequest.Builder(context)
            .data(card.previewUri)
            .crossfade(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build()
    }

    var visualState by remember(card.id) { mutableStateOf(PhotoVisualState.Loading) }
    var imageScale by remember(card.id) { mutableFloatStateOf(MIN_PHOTO_GESTURE_SCALE) }
    var imageOffset by remember(card.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(card.id) { mutableStateOf(IntSize.Zero) }
    var hasTwoFingerTransformActive by remember(card.id) { mutableStateOf(false) }
    var resetTransformJob by remember(card.id) { mutableStateOf<Job?>(null) }
    val currentOnGestureLockChanged by rememberUpdatedState(onGestureLockChanged)
    val currentOnTapWhenIdle by rememberUpdatedState(onTapWhenIdle)
    val transparentPainter = remember { ColorPainter(Color.Transparent) }

    fun clampImageOffset(target: Offset, scale: Float): Offset {
        if (containerSize.width <= 0 || containerSize.height <= 0) {
            return Offset.Zero
        }

        val maxX = (containerSize.width.toFloat() * (scale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (containerSize.height.toFloat() * (scale - 1f) / 2f).coerceAtLeast(0f)

        return Offset(
            x = target.x.coerceIn(-maxX, maxX),
            y = target.y.coerceIn(-maxY, maxY),
        )
    }

    fun shouldLockCardGestures(
        scale: Float,
        offset: Offset,
        hasTwoFingerGestureActive: Boolean,
    ): Boolean {
        val offsetDistance = offset.getDistanceValue()
        val horizontalOffsetAbs = abs(offset.x)
        val verticalOffsetAbs = abs(offset.y)

        val scaleLocked = scale > MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_LOCK_SCALE_EPSILON
        val horizontalStillAtEdge = horizontalOffsetAbs <= PHOTO_GESTURE_AXIS_UNLOCK_EPSILON
        val verticalStillAtEdge = verticalOffsetAbs <= PHOTO_GESTURE_AXIS_UNLOCK_EPSILON

        return hasTwoFingerGestureActive ||
            scaleLocked ||
            (offsetDistance > PHOTO_GESTURE_OFFSET_LOCK_EPSILON &&
                !horizontalStillAtEdge &&
                !verticalStillAtEdge)
    }

    fun cancelResetTransformAnimation() {
        resetTransformJob?.cancel()
        resetTransformJob = null
    }

    suspend fun animateResetTransform() {
        val startScale = imageScale
        val startOffset = imageOffset

        if (
            startScale <= MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_LOCK_SCALE_EPSILON &&
            startOffset.getDistanceValue() <= PHOTO_GESTURE_OFFSET_LOCK_EPSILON
        ) {
            imageScale = MIN_PHOTO_GESTURE_SCALE
            imageOffset = Offset.Zero
            return
        }

        coroutineScope {
            launch {
                val scaleAnim = Animatable(startScale)
                scaleAnim.animateTo(
                    targetValue = MIN_PHOTO_GESTURE_SCALE,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) {
                    imageScale = value
                }
            }

            launch {
                val offsetAnim = Animatable(
                    initialValue = startOffset,
                    typeConverter = Offset.VectorConverter,
                )
                offsetAnim.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) {
                    imageOffset = value
                }
            }
        }

        imageScale = MIN_PHOTO_GESTURE_SCALE
        imageOffset = Offset.Zero
    }

    val cardGestureLocked =
        enableTwoFingerTransform && shouldLockCardGestures(
            scale = imageScale,
            offset = imageOffset,
            hasTwoFingerGestureActive = hasTwoFingerTransformActive,
        )

    val canTapToResetTransform =
        imageScale > MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_LOCK_SCALE_EPSILON ||
            imageOffset.getDistanceValue() > PHOTO_GESTURE_OFFSET_LOCK_EPSILON

    LaunchedEffect(enableTwoFingerTransform, card.id) {
        if (!enableTwoFingerTransform) {
            cancelResetTransformAnimation()
            hasTwoFingerTransformActive = false
            imageScale = MIN_PHOTO_GESTURE_SCALE
            imageOffset = Offset.Zero
            currentOnGestureLockChanged(false)
        }
    }

    LaunchedEffect(cardGestureLocked) {
        currentOnGestureLockChanged(cardGestureLocked)
    }

    DisposableEffect(currentOnGestureLockChanged) {
        onDispose {
            cancelResetTransformAnimation()
            currentOnGestureLockChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize = size
                imageOffset = clampImageOffset(imageOffset, imageScale)
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
            .pointerInput(enableTwoFingerTransform, card.id, containerSize) {
                if (!enableTwoFingerTransform) {
                    hasTwoFingerTransformActive = false
                    return@pointerInput
                }

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    cancelResetTransformAnimation()
                    hasTwoFingerTransformActive = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.count { it.pressed }
                        if (activePointers == 0) {
                            break
                        }

                        val shouldTransform =
                            activePointers >= 2 &&
                                containerSize.width > 0 &&
                                containerSize.height > 0
                        hasTwoFingerTransformActive = shouldTransform

                        if (shouldTransform) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (!zoomChange.isFinite() || zoomChange <= 0f || !centroid.isSpecified) {
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        change.consume()
                                    }
                                }
                                continue
                            }

                            val currentScale = imageScale
                            val rawNextScale = (currentScale * zoomChange).coerceIn(
                                MIN_PHOTO_GESTURE_SCALE,
                                MAX_PHOTO_GESTURE_SCALE,
                            )
                            val nextScale = if (
                                rawNextScale <= MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_RESET_SNAP_EPSILON
                            ) {
                                MIN_PHOTO_GESTURE_SCALE
                            } else {
                                rawNextScale
                            }
                            val scaleRatio = if (currentScale > PHOTO_GESTURE_EPSILON) {
                                nextScale / currentScale
                            } else {
                                1f
                            }
                            val containerCenter = Offset(
                                x = containerSize.width / 2f,
                                y = containerSize.height / 2f,
                            )
                            val nextOffset = if (
                                nextScale <= MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_EPSILON
                            ) {
                                Offset.Zero
                            } else {
                                clampImageOffset(
                                    target =
                                        (imageOffset * scaleRatio) +
                                            ((centroid - containerCenter) * (1f - scaleRatio)) +
                                            panChange,
                                    scale = nextScale,
                                )
                            }

                            imageScale = nextScale
                            imageOffset = nextOffset
                            event.changes.forEach { change -> change.consume() }
                        } else if (imageScale > MIN_PHOTO_GESTURE_SCALE + PHOTO_GESTURE_LOCK_SCALE_EPSILON) {
                            event.changes.forEach { change ->
                                if (change.positionChange() != Offset.Zero) {
                                    change.consume()
                                }
                            }
                        }
                    }

                    hasTwoFingerTransformActive = false
                }
            }
            .pointerInput(enableTwoFingerTransform, enableTapToggle, canTapToResetTransform, card.id) {
                if (!enableTwoFingerTransform && !enableTapToggle) {
                    return@pointerInput
                }

                if (!canTapToResetTransform && !enableTapToggle) {
                    return@pointerInput
                }

                detectTapGestures(
                    onTap = {
                        if (canTapToResetTransform) {
                            cancelResetTransformAnimation()
                            resetTransformJob = gestureScope.launch {
                                animateResetTransform()
                            }.also { job ->
                                job.invokeOnCompletion {
                                    if (resetTransformJob == job) {
                                        resetTransformJob = null
                                    }
                                }
                            }
                        } else if (enableTapToggle) {
                            currentOnTapWhenIdle()
                        }
                    },
                )
            },
    ) {
        if (visualState == PhotoVisualState.Error) {
            PhotoFallbackContent(
                isError = true,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        AsyncImage(
            model = request,
            contentDescription = stringResource(id = R.string.photo_content_description),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = imageScale
                    scaleY = imageScale
                    translationX = imageOffset.x
                    translationY = imageOffset.y
                },
            contentScale = if (showFullImage) ContentScale.Fit else ContentScale.Crop,
            placeholder = transparentPainter,
            error = transparentPainter,
            fallback = transparentPainter,
            onLoading = { visualState = PhotoVisualState.Loading },
            onSuccess = { visualState = PhotoVisualState.Ready },
            onError = { visualState = PhotoVisualState.Error },
        )
    }
}



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
internal fun GestureBallOverlay(
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


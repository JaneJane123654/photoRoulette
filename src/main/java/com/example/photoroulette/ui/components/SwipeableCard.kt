package com.example.photoroulette.ui.components

import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.photoroulette.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down,
}

@Composable
fun SwipeableCard(
    onSwiped: (SwipeDirection) -> Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gestureSensitivity: Float = DEFAULT_GESTURE_SENSITIVITY,
    canSwipeLeft: Boolean = true,
    canSwipeRight: Boolean = true,
    canSwipeUp: Boolean = true,
    canSwipeDown: Boolean = true,
    restingScale: Float = 1f,
    revealProgress: Float = 0f,
    onDragProgressChanged: (Float) -> Unit = {},
    isDownSwipeCoverEnabled: Boolean = false,
    downSwipeCoverTriggerDirection: SwipeDirection = SwipeDirection.Down,
    onDownSwipeCoverProgressChanged: (Float) -> Unit = {},
    isRightSwipeCoverEnabled: Boolean = false,
    rightSwipeCoverTriggerDirection: SwipeDirection = SwipeDirection.Right,
    onRightSwipeCoverProgressChanged: (Float) -> Unit = {},
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hostView = LocalView.current

    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentOnDragProgressChanged by rememberUpdatedState(onDragProgressChanged)
    val currentCanSwipeLeft by rememberUpdatedState(canSwipeLeft)
    val currentCanSwipeRight by rememberUpdatedState(canSwipeRight)
    val currentCanSwipeUp by rememberUpdatedState(canSwipeUp)
    val currentCanSwipeDown by rememberUpdatedState(canSwipeDown)
    val currentIsDownSwipeCoverEnabled by rememberUpdatedState(isDownSwipeCoverEnabled)
    val currentDownSwipeCoverTriggerDirection by rememberUpdatedState(downSwipeCoverTriggerDirection)
    val currentIsRightSwipeCoverEnabled by rememberUpdatedState(isRightSwipeCoverEnabled)
    val currentRightSwipeCoverTriggerDirection by rememberUpdatedState(rightSwipeCoverTriggerDirection)
    val currentGestureSensitivity by rememberUpdatedState(
        gestureSensitivity.coerceIn(MIN_GESTURE_SENSITIVITY, MAX_GESTURE_SENSITIVITY),
    )
    val currentOnDownSwipeCoverProgressChanged by rememberUpdatedState(onDownSwipeCoverProgressChanged)
    val currentOnRightSwipeCoverProgressChanged by rememberUpdatedState(onRightSwipeCoverProgressChanged)

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetXState = remember { mutableFloatStateOf(0f) }
    var offsetX by offsetXState
    val offsetYState = remember { mutableFloatStateOf(0f) }
    var offsetY by offsetYState
    val isSettlingState = remember { mutableStateOf(false) }
    var isSettling by isSettlingState
    val settleJobState = remember { mutableStateOf<Job?>(null) }
    var settleJob by settleJobState
    var lastReportedProgress by remember { mutableFloatStateOf(0f) }
    val downSwipeCoverOffsetYState = remember { mutableFloatStateOf(0f) }
    var downSwipeCoverOffsetY by downSwipeCoverOffsetYState
    var lastReportedDownSwipeCoverProgress by remember { mutableFloatStateOf(0f) }
    val rightSwipeCoverOffsetXState = remember { mutableFloatStateOf(0f) }
    var rightSwipeCoverOffsetX by rightSwipeCoverOffsetXState
    var lastReportedRightSwipeCoverProgress by remember { mutableFloatStateOf(0f) }
    var gestureExclusionRects by remember { mutableStateOf<List<Rect>>(emptyList()) }

    fun reportDragProgress(progress: Float, force: Boolean = false) {
        if (force || abs(progress - lastReportedProgress) >= DRAG_PROGRESS_EPSILON) {
            lastReportedProgress = progress
            currentOnDragProgressChanged(progress)
        }
    }

    fun reportDownSwipeCoverProgress(progress: Float, force: Boolean = false) {
        if (force || abs(progress - lastReportedDownSwipeCoverProgress) >= DRAG_PROGRESS_EPSILON) {
            lastReportedDownSwipeCoverProgress = progress
            currentOnDownSwipeCoverProgressChanged(progress)
        }
    }

    fun reportRightSwipeCoverProgress(progress: Float, force: Boolean = false) {
        if (force || abs(progress - lastReportedRightSwipeCoverProgress) >= DRAG_PROGRESS_EPSILON) {
            lastReportedRightSwipeCoverProgress = progress
            currentOnRightSwipeCoverProgressChanged(progress)
        }
    }

    if (enabled) {
        DisposableEffect(currentOnDragProgressChanged) {
            onDispose {
                reportDragProgress(0f, force = true)
            }
        }

        DisposableEffect(currentOnDownSwipeCoverProgressChanged) {
            onDispose {
                reportDownSwipeCoverProgress(0f, force = true)
            }
        }

        DisposableEffect(currentOnRightSwipeCoverProgressChanged) {
            onDispose {
                reportRightSwipeCoverProgress(0f, force = true)
            }
        }
    }

    LaunchedEffect(enabled, isDownSwipeCoverEnabled, isRightSwipeCoverEnabled) {
        if (!enabled || (!isDownSwipeCoverEnabled && !isRightSwipeCoverEnabled)) {
            settleJob?.cancel()
            settleJob = null
            isSettling = false
            if (offsetX != 0f || offsetY != 0f) {
                offsetX = 0f
                offsetY = 0f
            }
            if (downSwipeCoverOffsetY != 0f) {
                downSwipeCoverOffsetY = 0f
            }
            if (rightSwipeCoverOffsetX != 0f) {
                rightSwipeCoverOffsetX = 0f
            }
            reportDragProgress(0f, force = true)
            reportDownSwipeCoverProgress(0f, force = true)
            reportRightSwipeCoverProgress(0f, force = true)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled) {
        SideEffect {
            hostView.systemGestureExclusionRects = gestureExclusionRects
        }

        DisposableEffect(hostView) {
            onDispose {
                hostView.systemGestureExclusionRects = emptyList()
            }
        }
    }

    val underlayScale = restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f) +
        ((1f - restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f)) * revealProgress.coerceIn(0f, 1f))

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
            .onGloballyPositioned { coordinates ->
                if (!enabled) {
                    gestureExclusionRects = emptyList()
                    return@onGloballyPositioned
                }

                val bounds = coordinates.boundsInRoot()
                if (bounds.width <= 0f || bounds.height <= 0f) {
                    gestureExclusionRects = emptyList()
                    return@onGloballyPositioned
                }

                val top = bounds.top.toInt()
                val bottom = bounds.bottom.toInt()
                val rootWidth = hostView.width.coerceAtLeast(bounds.right.toInt()).coerceAtLeast(1)

                gestureExclusionRects = listOf(
                    Rect(0, top, rootWidth, bottom),
                ).filter { rect ->
                    rect.right > rect.left && rect.bottom > rect.top
                }
            }
            .graphicsLayer {
                translationX = if (enabled) offsetX else 0f
                translationY = if (enabled) offsetY else 0f
                rotationZ = if (enabled) offsetX / ROTATION_DIVISOR else 0f
                scaleX = if (enabled) 1f else underlayScale
                scaleY = if (enabled) 1f else underlayScale
                clip = true
                this.shape = shape
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (enabled) {
                    Modifier.swipeableCardPointerInputModifier(
                        enabled = enabled,
                        cardSize = cardSize,
                        gestureSensitivity = gestureSensitivity,
                        canSwipeLeft = canSwipeLeft,
                        canSwipeRight = canSwipeRight,
                        canSwipeUp = canSwipeUp,
                        canSwipeDown = canSwipeDown,
                        isDownSwipeCoverEnabled = isDownSwipeCoverEnabled,
                        downSwipeCoverTriggerDirection = downSwipeCoverTriggerDirection,
                        isRightSwipeCoverEnabled = isRightSwipeCoverEnabled,
                        rightSwipeCoverTriggerDirection = rightSwipeCoverTriggerDirection,
                        scope = scope,
                        hostView = hostView,
                        currentOnSwiped = currentOnSwiped,
                        currentCanSwipeLeft = currentCanSwipeLeft,
                        currentCanSwipeRight = currentCanSwipeRight,
                        currentCanSwipeUp = currentCanSwipeUp,
                        currentCanSwipeDown = currentCanSwipeDown,
                        currentIsDownSwipeCoverEnabled = currentIsDownSwipeCoverEnabled,
                        currentDownSwipeCoverTriggerDirection =
                            currentDownSwipeCoverTriggerDirection,
                        currentIsRightSwipeCoverEnabled = currentIsRightSwipeCoverEnabled,
                        currentRightSwipeCoverTriggerDirection =
                            currentRightSwipeCoverTriggerDirection,
                        currentGestureSensitivity = currentGestureSensitivity,
                        offsetXState = offsetXState,
                        offsetYState = offsetYState,
                        isSettlingState = isSettlingState,
                        settleJobState = settleJobState,
                        downSwipeCoverOffsetYState = downSwipeCoverOffsetYState,
                        rightSwipeCoverOffsetXState = rightSwipeCoverOffsetXState,
                        reportDragProgressCallback = ::reportDragProgress,
                        reportDownSwipeCoverProgressCallback = ::reportDownSwipeCoverProgress,
                        reportRightSwipeCoverProgressCallback = ::reportRightSwipeCoverProgress,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

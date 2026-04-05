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

private enum class DragAxis {
    Horizontal,
    Vertical,
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
    onDownSwipeCoverProgressChanged: (Float) -> Unit = {},
    isRightSwipeCoverEnabled: Boolean = false,
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
    val currentIsRightSwipeCoverEnabled by rememberUpdatedState(isRightSwipeCoverEnabled)
    val currentGestureSensitivity by rememberUpdatedState(
        gestureSensitivity.coerceIn(MIN_GESTURE_SENSITIVITY, MAX_GESTURE_SENSITIVITY),
    )
    val currentOnDownSwipeCoverProgressChanged by rememberUpdatedState(onDownSwipeCoverProgressChanged)
    val currentOnRightSwipeCoverProgressChanged by rememberUpdatedState(onRightSwipeCoverProgressChanged)

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isSettling by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var lastReportedProgress by remember { mutableFloatStateOf(0f) }
    var downSwipeCoverOffsetY by remember { mutableFloatStateOf(0f) }
    var lastReportedDownSwipeCoverProgress by remember { mutableFloatStateOf(0f) }
    var rightSwipeCoverOffsetX by remember { mutableFloatStateOf(0f) }
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
                    Modifier.pointerInput(
                        enabled,
                        cardSize,
                        gestureSensitivity,
                        canSwipeLeft,
                        canSwipeRight,
                        canSwipeUp,
                        canSwipeDown,
                        isDownSwipeCoverEnabled,
                        isRightSwipeCoverEnabled,
                    ) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            settleJob?.cancel()
                            isSettling = false

                            var activePointerId = down.id
                            val touchSlopScale = 1f +
                                ((currentGestureSensitivity - DEFAULT_GESTURE_SENSITIVITY) *
                                    TOUCH_SLOP_SENSITIVITY_FACTOR)
                            val touchSlop =
                                (viewConfiguration.touchSlop / touchSlopScale).coerceAtLeast(MIN_TOUCH_SLOP)
                            var accumulatedDrag = Offset.Zero
                            var hasCrossedTouchSlop = false
                            var hasDraggedCard = false
                            var cancelledByMultiTouch = false
                            var dominantAxis: DragAxis? = null

                            fun resolveDominantAxis(delta: Offset): DragAxis? {
                                val absX = abs(delta.x)
                                val absY = abs(delta.y)
                                return when {
                                    absX >= absY * AXIS_LOCK_DOMINANCE_RATIO -> DragAxis.Horizontal
                                    absY >= absX * AXIS_LOCK_DOMINANCE_RATIO -> DragAxis.Vertical
                                    else -> null
                                }
                            }

                            fun applyDragDelta(rawDelta: Offset) {
                                val normalizedDelta = when (dominantAxis) {
                                    DragAxis.Horizontal -> Offset(
                                        x = rawDelta.x,
                                        y = rawDelta.y * AXIS_LOCK_CROSS_DAMPING,
                                    )

                                    DragAxis.Vertical -> Offset(
                                        x = rawDelta.x * AXIS_LOCK_CROSS_DAMPING,
                                        y = rawDelta.y,
                                    )

                                    null -> rawDelta
                                }

                                val canDriveDownSwipeCover =
                                    currentIsDownSwipeCoverEnabled && currentCanSwipeDown
                                val shouldStartDownSwipeCover =
                                    normalizedDelta.y > 0f && abs(normalizedDelta.y) >= abs(normalizedDelta.x)
                                val maxCoverOffset =
                                    cardSize.height.toFloat().coerceAtLeast(1f) *
                                        DOWN_SWIPE_COVER_MAX_PULL_MULTIPLIER
                                val canDriveRightSwipeCover =
                                    currentIsRightSwipeCoverEnabled && currentCanSwipeRight
                                val shouldStartRightSwipeCover =
                                    normalizedDelta.x > 0f && abs(normalizedDelta.x) >= abs(normalizedDelta.y)
                                val maxRightCoverOffset =
                                    cardSize.width.toFloat().coerceAtLeast(1f) *
                                        RIGHT_SWIPE_COVER_MAX_PULL_MULTIPLIER

                                if (
                                    canDriveDownSwipeCover &&
                                    (downSwipeCoverOffsetY > 0f ||
                                        (rightSwipeCoverOffsetX == 0f && shouldStartDownSwipeCover))
                                ) {
                                    downSwipeCoverOffsetY =
                                        (downSwipeCoverOffsetY + normalizedDelta.y)
                                            .coerceIn(0f, maxCoverOffset)
                                    hasDraggedCard = true

                                    if (rightSwipeCoverOffsetX != 0f) {
                                        rightSwipeCoverOffsetX = 0f
                                        reportRightSwipeCoverProgress(0f, force = true)
                                    }

                                    if (offsetX != 0f || offsetY != 0f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                        reportDragProgress(0f, force = true)
                                    }

                                    reportDownSwipeCoverProgress(
                                        progress = calculateDownSwipeCoverProgress(
                                            downSwipeCoverOffsetY = downSwipeCoverOffsetY,
                                            cardSize = cardSize,
                                        ),
                                    )
                                    reportRightSwipeCoverProgress(0f)
                                    return
                                }

                                if (
                                    canDriveRightSwipeCover &&
                                    (rightSwipeCoverOffsetX > 0f ||
                                        (downSwipeCoverOffsetY == 0f && shouldStartRightSwipeCover))
                                ) {
                                    rightSwipeCoverOffsetX =
                                        (rightSwipeCoverOffsetX + normalizedDelta.x)
                                            .coerceIn(0f, maxRightCoverOffset)
                                    hasDraggedCard = true

                                    if (downSwipeCoverOffsetY != 0f) {
                                        downSwipeCoverOffsetY = 0f
                                        reportDownSwipeCoverProgress(0f, force = true)
                                    }

                                    if (offsetX != 0f || offsetY != 0f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                        reportDragProgress(0f, force = true)
                                    }

                                    reportRightSwipeCoverProgress(
                                        progress = calculateRightSwipeCoverProgress(
                                            rightSwipeCoverOffsetX = rightSwipeCoverOffsetX,
                                            cardSize = cardSize,
                                        ),
                                    )
                                    reportDownSwipeCoverProgress(0f)
                                    return
                                }

                                if (downSwipeCoverOffsetY != 0f) {
                                    downSwipeCoverOffsetY = 0f
                                    reportDownSwipeCoverProgress(0f, force = true)
                                }

                                if (rightSwipeCoverOffsetX != 0f) {
                                    rightSwipeCoverOffsetX = 0f
                                    reportRightSwipeCoverProgress(0f, force = true)
                                }

                                val dampedDeltaX = when {
                                    normalizedDelta.x < 0f && !currentCanSwipeLeft -> {
                                        normalizedDelta.x * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    normalizedDelta.x > 0f && !currentCanSwipeRight -> {
                                        normalizedDelta.x * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    else -> normalizedDelta.x
                                }

                                val dampedDeltaY = when {
                                    normalizedDelta.y < 0f && !currentCanSwipeUp -> {
                                        normalizedDelta.y * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    normalizedDelta.y > 0f && !currentCanSwipeDown -> {
                                        normalizedDelta.y * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    else -> normalizedDelta.y
                                }

                                offsetX += dampedDeltaX
                                offsetY += dampedDeltaY
                                hasDraggedCard = true

                                reportDragProgress(
                                    progress = calculateDragProgress(
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        cardSize = cardSize,
                                        gestureSensitivity = currentGestureSensitivity,
                                    ),
                                )

                                reportDownSwipeCoverProgress(0f)
                                reportRightSwipeCoverProgress(0f)
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }

                                if (pressedChanges.isEmpty()) {
                                    break
                                }

                                if (pressedChanges.size > 1) {
                                    cancelledByMultiTouch = true
                                    break
                                }

                                val activeChange = pressedChanges
                                    .firstOrNull { it.id == activePointerId }
                                    ?: pressedChanges.first().also { activePointerId = it.id }

                                val delta = activeChange.positionChange()
                                if (delta != Offset.Zero) {
                                    if (!hasCrossedTouchSlop) {
                                        accumulatedDrag += delta
                                        val dragDistance = hypot(
                                            accumulatedDrag.x.toDouble(),
                                            accumulatedDrag.y.toDouble(),
                                        ).toFloat()

                                        if (dragDistance >= touchSlop) {
                                            hasCrossedTouchSlop = true
                                            val directionX = accumulatedDrag.x / dragDistance
                                            val directionY = accumulatedDrag.y / dragDistance
                                            val overSlop = Offset(
                                                x = accumulatedDrag.x - (directionX * touchSlop),
                                                y = accumulatedDrag.y - (directionY * touchSlop),
                                            )
                                            dominantAxis = resolveDominantAxis(accumulatedDrag)
                                            if (dominantAxis == DragAxis.Horizontal) {
                                                hostView.parent?.requestDisallowInterceptTouchEvent(true)
                                            }
                                            applyDragDelta(overSlop)
                                            accumulatedDrag = Offset.Zero
                                        }
                                    } else {
                                        if (dominantAxis == null) {
                                            dominantAxis = resolveDominantAxis(delta)
                                            if (dominantAxis == DragAxis.Horizontal) {
                                                hostView.parent?.requestDisallowInterceptTouchEvent(true)
                                            }
                                        }
                                        applyDragDelta(delta)
                                    }

                                    activeChange.consume()
                                }
                            }

                            if (!hasDraggedCard) {
                                reportDragProgress(0f, force = true)
                                return@awaitEachGesture
                            }

                            settleJob = scope.launch {
                                isSettling = true

                                val downSwipeCoverThreshold = cardSize.height.toFloat()
                                    .coerceAtLeast(1f) * DOWN_SWIPE_COVER_CONFIRM_FRACTION
                                val rightSwipeCoverThreshold = cardSize.width.toFloat()
                                    .coerceAtLeast(1f) * RIGHT_SWIPE_COVER_CONFIRM_FRACTION

                                if (
                                    currentIsDownSwipeCoverEnabled &&
                                        currentCanSwipeDown &&
                                        downSwipeCoverOffsetY > 0f
                                ) {
                                    val shouldConfirmDownSwipeCover =
                                        downSwipeCoverOffsetY >= downSwipeCoverThreshold

                                    if (shouldConfirmDownSwipeCover) {
                                        animateValueTo(
                                            startValue = downSwipeCoverOffsetY,
                                            targetValue = cardSize.height.toFloat().coerceAtLeast(1f),
                                            forDismiss = true,
                                        ) { value ->
                                            downSwipeCoverOffsetY = value
                                            reportDownSwipeCoverProgress(
                                                progress = calculateDownSwipeCoverProgress(
                                                    downSwipeCoverOffsetY = value,
                                                    cardSize = cardSize,
                                                ),
                                            )
                                        }

                                        val handled = currentOnSwiped(SwipeDirection.Down)
                                        if (!handled) {
                                            animateValueTo(
                                                startValue = downSwipeCoverOffsetY,
                                                targetValue = 0f,
                                            ) { value ->
                                                downSwipeCoverOffsetY = value
                                                reportDownSwipeCoverProgress(
                                                    progress = calculateDownSwipeCoverProgress(
                                                        downSwipeCoverOffsetY = value,
                                                        cardSize = cardSize,
                                                    ),
                                                )
                                            }
                                        } else {
                                            downSwipeCoverOffsetY = 0f
                                            reportDownSwipeCoverProgress(0f, force = true)
                                        }
                                    } else {
                                        animateValueTo(
                                            startValue = downSwipeCoverOffsetY,
                                            targetValue = 0f,
                                        ) { value ->
                                            downSwipeCoverOffsetY = value
                                            reportDownSwipeCoverProgress(
                                                progress = calculateDownSwipeCoverProgress(
                                                    downSwipeCoverOffsetY = value,
                                                    cardSize = cardSize,
                                                ),
                                            )
                                        }
                                    }

                                    isSettling = false
                                    settleJob = null
                                    return@launch
                                }

                                if (
                                    currentIsRightSwipeCoverEnabled &&
                                        currentCanSwipeRight &&
                                        rightSwipeCoverOffsetX > 0f
                                ) {
                                    val shouldConfirmRightSwipeCover =
                                        rightSwipeCoverOffsetX >= rightSwipeCoverThreshold

                                    if (shouldConfirmRightSwipeCover) {
                                        animateValueTo(
                                            startValue = rightSwipeCoverOffsetX,
                                            targetValue = cardSize.width.toFloat().coerceAtLeast(1f),
                                            forDismiss = true,
                                        ) { value ->
                                            rightSwipeCoverOffsetX = value
                                            reportRightSwipeCoverProgress(
                                                progress = calculateRightSwipeCoverProgress(
                                                    rightSwipeCoverOffsetX = value,
                                                    cardSize = cardSize,
                                                ),
                                            )
                                        }

                                        val handled = currentOnSwiped(SwipeDirection.Right)
                                        if (!handled) {
                                            animateValueTo(
                                                startValue = rightSwipeCoverOffsetX,
                                                targetValue = 0f,
                                            ) { value ->
                                                rightSwipeCoverOffsetX = value
                                                reportRightSwipeCoverProgress(
                                                    progress = calculateRightSwipeCoverProgress(
                                                        rightSwipeCoverOffsetX = value,
                                                        cardSize = cardSize,
                                                    ),
                                                )
                                            }
                                        } else {
                                            rightSwipeCoverOffsetX = 0f
                                            reportRightSwipeCoverProgress(0f, force = true)
                                        }
                                    } else {
                                        animateValueTo(
                                            startValue = rightSwipeCoverOffsetX,
                                            targetValue = 0f,
                                        ) { value ->
                                            rightSwipeCoverOffsetX = value
                                            reportRightSwipeCoverProgress(
                                                progress = calculateRightSwipeCoverProgress(
                                                    rightSwipeCoverOffsetX = value,
                                                    cardSize = cardSize,
                                                ),
                                            )
                                        }
                                    }

                                    isSettling = false
                                    settleJob = null
                                    return@launch
                                }

                                if (cancelledByMultiTouch) {
                                    animateCardTo(
                                        startX = offsetX,
                                        startY = offsetY,
                                        targetX = 0f,
                                        targetY = 0f,
                                    ) { x, y ->
                                        offsetX = x
                                        offsetY = y
                                        reportDragProgress(
                                            progress = calculateDragProgress(
                                                offsetX = x,
                                                offsetY = y,
                                                cardSize = cardSize,
                                                gestureSensitivity = currentGestureSensitivity,
                                            ),
                                        )
                                    }

                                    isSettling = false
                                    settleJob = null
                                    return@launch
                                }

                                val decision = resolveSwipeDecision(
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    cardSize = cardSize,
                                    canSwipeLeft = currentCanSwipeLeft,
                                    canSwipeRight = currentCanSwipeRight,
                                    canSwipeUp = currentCanSwipeUp,
                                    canSwipeDown = currentCanSwipeDown,
                                    gestureSensitivity = currentGestureSensitivity,
                                )

                                if (decision == null) {
                                    animateCardTo(
                                        startX = offsetX,
                                        startY = offsetY,
                                        targetX = 0f,
                                        targetY = 0f,
                                    ) { x, y ->
                                        offsetX = x
                                        offsetY = y
                                        reportDragProgress(
                                            progress = calculateDragProgress(
                                                offsetX = x,
                                                offsetY = y,
                                                cardSize = cardSize,
                                                gestureSensitivity = currentGestureSensitivity,
                                            ),
                                        )
                                    }
                                    isSettling = false
                                    settleJob = null
                                    return@launch
                                }

                                animateCardTo(
                                    startX = offsetX,
                                    startY = offsetY,
                                    targetX = decision.targetX,
                                    targetY = decision.targetY,
                                    forDismiss = true,
                                ) { x, y ->
                                    offsetX = x
                                    offsetY = y
                                    reportDragProgress(
                                        progress = calculateDragProgress(
                                            offsetX = x,
                                            offsetY = y,
                                            cardSize = cardSize,
                                            gestureSensitivity = currentGestureSensitivity,
                                        ),
                                    )
                                }

                                val handled = currentOnSwiped(decision.direction)
                                if (!handled) {
                                    animateCardTo(
                                        startX = offsetX,
                                        startY = offsetY,
                                        targetX = 0f,
                                        targetY = 0f,
                                    ) { x, y ->
                                        offsetX = x
                                        offsetY = y
                                        reportDragProgress(
                                            progress = calculateDragProgress(
                                                offsetX = x,
                                                offsetY = y,
                                                cardSize = cardSize,
                                                gestureSensitivity = currentGestureSensitivity,
                                            ),
                                        )
                                    }
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                    reportDragProgress(0f, force = true)
                                }

                                isSettling = false
                                settleJob = null
                            }

                            hostView.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private data class SwipeDecision(
    val direction: SwipeDirection,
    val targetX: Float,
    val targetY: Float,
)

private fun resolveSwipeDecision(
    offsetX: Float,
    offsetY: Float,
    cardSize: IntSize,
    canSwipeLeft: Boolean,
    canSwipeRight: Boolean,
    canSwipeUp: Boolean,
    canSwipeDown: Boolean,
    gestureSensitivity: Float,
): SwipeDecision? {
    val width = cardSize.width.toFloat()
    val height = cardSize.height.toFloat()

    if (width <= 0f || height <= 0f) {
        return null
    }

    val thresholdScale =
        (DEFAULT_GESTURE_SENSITIVITY / gestureSensitivity.coerceAtLeast(MIN_GESTURE_SENSITIVITY))
            .coerceIn(MIN_THRESHOLD_SCALE, MAX_THRESHOLD_SCALE)

    val horizontalThreshold = width * HORIZONTAL_DISMISS_THRESHOLD_FRACTION * thresholdScale
    val verticalThreshold = height * VERTICAL_DISMISS_THRESHOLD_FRACTION * thresholdScale

    val horizontalDecision = when {
        offsetX <= -horizontalThreshold && canSwipeLeft -> SwipeDecision(
            direction = SwipeDirection.Left,
            targetX = -width * SWIPE_OUT_DISTANCE_MULTIPLIER,
            targetY = offsetY * OUT_SWIPE_CROSS_AXIS_FACTOR,
        )

        offsetX >= horizontalThreshold && canSwipeRight -> SwipeDecision(
            direction = SwipeDirection.Right,
            targetX = width * SWIPE_OUT_DISTANCE_MULTIPLIER,
            targetY = offsetY * OUT_SWIPE_CROSS_AXIS_FACTOR,
        )

        else -> null
    }

    val verticalDecision = when {
        offsetY <= -verticalThreshold && canSwipeUp -> SwipeDecision(
            direction = SwipeDirection.Up,
            targetX = offsetX * OUT_SWIPE_CROSS_AXIS_FACTOR,
            targetY = -height * SWIPE_OUT_DISTANCE_MULTIPLIER,
        )

        offsetY >= verticalThreshold && canSwipeDown -> SwipeDecision(
            direction = SwipeDirection.Down,
            targetX = offsetX * OUT_SWIPE_CROSS_AXIS_FACTOR,
            targetY = height * SWIPE_OUT_DISTANCE_MULTIPLIER,
        )

        else -> null
    }

    return when {
        horizontalDecision == null && verticalDecision == null -> null
        horizontalDecision != null && verticalDecision != null -> {
            val horizontalProgress = abs(offsetX) / horizontalThreshold.coerceAtLeast(1f)
            val verticalProgress = abs(offsetY) / verticalThreshold.coerceAtLeast(1f)
            if (horizontalProgress >= verticalProgress) horizontalDecision else verticalDecision
        }

        horizontalDecision != null -> horizontalDecision
        else -> verticalDecision
    }
}

private suspend fun animateCardTo(
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    forDismiss: Boolean = false,
    onFrame: (Float, Float) -> Unit,
) {
    val offsetAnim = Animatable(
        initialValue = Offset(startX, startY),
        typeConverter = Offset.VectorConverter,
    )

    offsetAnim.animateTo(
        targetValue = Offset(targetX, targetY),
        animationSpec = spring(
            dampingRatio = if (forDismiss) {
                Spring.DampingRatioNoBouncy
            } else {
                Spring.DampingRatioMediumBouncy
            },
            stiffness = if (forDismiss) {
                Spring.StiffnessHigh
            } else {
                Spring.StiffnessMedium
            },
        ),
    ) {
        onFrame(value.x, value.y)
    }
}

private suspend fun animateValueTo(
    startValue: Float,
    targetValue: Float,
    forDismiss: Boolean = false,
    onFrame: (Float) -> Unit,
) {
    val anim = Animatable(initialValue = startValue)
    anim.animateTo(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = if (forDismiss) {
                Spring.DampingRatioNoBouncy
            } else {
                Spring.DampingRatioMediumBouncy
            },
            stiffness = if (forDismiss) {
                Spring.StiffnessHigh
            } else {
                Spring.StiffnessMedium
            },
        ),
    ) {
        onFrame(value)
    }
}

private fun calculateDownSwipeCoverProgress(
    downSwipeCoverOffsetY: Float,
    cardSize: IntSize,
): Float {
    val height = cardSize.height.toFloat().coerceAtLeast(1f)
    return (downSwipeCoverOffsetY / height).coerceIn(0f, 1f)
}

private fun calculateRightSwipeCoverProgress(
    rightSwipeCoverOffsetX: Float,
    cardSize: IntSize,
): Float {
    val width = cardSize.width.toFloat().coerceAtLeast(1f)
    return (rightSwipeCoverOffsetX / width).coerceIn(0f, 1f)
}

private fun calculateDragProgress(
    offsetX: Float,
    offsetY: Float,
    cardSize: IntSize,
    gestureSensitivity: Float,
): Float {
    val width = cardSize.width.toFloat().coerceAtLeast(1f)
    val height = cardSize.height.toFloat().coerceAtLeast(1f)
    val distance = hypot(offsetX.toDouble(), offsetY.toDouble()).toFloat()
    val thresholdScale =
        (DEFAULT_GESTURE_SENSITIVITY / gestureSensitivity.coerceAtLeast(MIN_GESTURE_SENSITIVITY))
            .coerceIn(MIN_THRESHOLD_SCALE, MAX_THRESHOLD_SCALE)
    val dismissDistance = hypot(
        (width * HORIZONTAL_DISMISS_THRESHOLD_FRACTION * thresholdScale).toDouble(),
        (height * VERTICAL_DISMISS_THRESHOLD_FRACTION * thresholdScale).toDouble(),
    ).toFloat().coerceAtLeast(1f)

    return (distance / dismissDistance).coerceIn(0f, 1f)
}

private const val HORIZONTAL_DISMISS_THRESHOLD_FRACTION = 0.22f
private const val VERTICAL_DISMISS_THRESHOLD_FRACTION = 0.22f
private const val SWIPE_OUT_DISTANCE_MULTIPLIER = 1.38f
private const val BLOCKED_DIRECTION_DRAG_FRICTION = 0.18f
private const val OUT_SWIPE_CROSS_AXIS_FACTOR = 0.35f
private const val ROTATION_DIVISOR = 18f
private const val MIN_UNDERLAY_SCALE = 0.90f
private const val DRAG_PROGRESS_EPSILON = 0.006f
private const val MIN_GESTURE_SENSITIVITY = 0.8f
private const val MAX_GESTURE_SENSITIVITY = 1.35f
private const val DEFAULT_GESTURE_SENSITIVITY = 1f
private const val MIN_TOUCH_SLOP = 6f
private const val TOUCH_SLOP_SENSITIVITY_FACTOR = 1.28f
private const val AXIS_LOCK_DOMINANCE_RATIO = 1.28f
private const val AXIS_LOCK_CROSS_DAMPING = 0.28f
private const val DOWN_SWIPE_COVER_MAX_PULL_MULTIPLIER = 1.18f
private const val DOWN_SWIPE_COVER_CONFIRM_FRACTION = 0.33f
private const val RIGHT_SWIPE_COVER_MAX_PULL_MULTIPLIER = 1.18f
private const val RIGHT_SWIPE_COVER_CONFIRM_FRACTION = 0.33f
private const val MIN_THRESHOLD_SCALE = 0.8f
private const val MAX_THRESHOLD_SCALE = 1.25f

@Preview(showBackground = true, backgroundColor = 0xFFF2EFE8)
@Composable
private fun SwipeableCardPreview() {
    MaterialTheme {
        SwipeableCard(
            onSwiped = { true },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = R.string.preview_photo_card),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

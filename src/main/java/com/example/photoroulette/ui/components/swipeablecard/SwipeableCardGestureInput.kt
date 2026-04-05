package com.example.photoroulette.ui.components
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
internal enum class DragAxis {
    Horizontal,
    Vertical,
}
internal fun Modifier.swipeableCardPointerInputModifier(
    enabled: Boolean,
    cardSize: IntSize,
    gestureSensitivity: Float,
    canSwipeLeft: Boolean,
    canSwipeRight: Boolean,
    canSwipeUp: Boolean,
    canSwipeDown: Boolean,
    isDownSwipeCoverEnabled: Boolean,
    isRightSwipeCoverEnabled: Boolean,
    scope: CoroutineScope,
    hostView: android.view.View,
    currentOnSwiped: (SwipeDirection) -> Boolean,
    currentCanSwipeLeft: Boolean,
    currentCanSwipeRight: Boolean,
    currentCanSwipeUp: Boolean,
    currentCanSwipeDown: Boolean,
    currentIsDownSwipeCoverEnabled: Boolean,
    currentIsRightSwipeCoverEnabled: Boolean,
    currentGestureSensitivity: Float,
    offsetXState: MutableFloatState,
    offsetYState: MutableFloatState,
    isSettlingState: MutableState<Boolean>,
    settleJobState: MutableState<Job?>,
    downSwipeCoverOffsetYState: MutableFloatState,
    rightSwipeCoverOffsetXState: MutableFloatState,
    reportDragProgressCallback: (Float, Boolean) -> Unit,
    reportDownSwipeCoverProgressCallback: (Float, Boolean) -> Unit,
    reportRightSwipeCoverProgressCallback: (Float, Boolean) -> Unit,
): Modifier {
    return pointerInput(
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
        var offsetX by offsetXState
        var offsetY by offsetYState
        var isSettling by isSettlingState
        var settleJob by settleJobState
        var downSwipeCoverOffsetY by downSwipeCoverOffsetYState
        var rightSwipeCoverOffsetX by rightSwipeCoverOffsetXState
        fun reportDragProgress(progress: Float, force: Boolean = false) {
            reportDragProgressCallback(progress, force)
        }
        fun reportDownSwipeCoverProgress(progress: Float, force: Boolean = false) {
            reportDownSwipeCoverProgressCallback(progress, force)
        }
        fun reportRightSwipeCoverProgress(progress: Float, force: Boolean = false) {
            reportRightSwipeCoverProgressCallback(progress, force)
        }
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
}

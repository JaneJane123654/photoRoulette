package com.example.photoroulette.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
    canSwipeLeft: Boolean = true,
    canSwipeRight: Boolean = true,
    canSwipeUp: Boolean = true,
    canSwipeDown: Boolean = true,
    restingScale: Float = 1f,
    revealProgress: Float = 0f,
    onDragProgressChanged: (Float) -> Unit = {},
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()

    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentOnDragProgressChanged by rememberUpdatedState(onDragProgressChanged)
    val currentCanSwipeLeft by rememberUpdatedState(canSwipeLeft)
    val currentCanSwipeRight by rememberUpdatedState(canSwipeRight)
    val currentCanSwipeUp by rememberUpdatedState(canSwipeUp)
    val currentCanSwipeDown by rememberUpdatedState(canSwipeDown)

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isSettling by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var lastReportedProgress by remember { mutableFloatStateOf(0f) }

    fun reportDragProgress(progress: Float, force: Boolean = false) {
        if (force || abs(progress - lastReportedProgress) >= DRAG_PROGRESS_EPSILON) {
            lastReportedProgress = progress
            currentOnDragProgressChanged(progress)
        }
    }

    if (enabled) {
        DisposableEffect(currentOnDragProgressChanged) {
            onDispose {
                reportDragProgress(0f, force = true)
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            settleJob?.cancel()
            settleJob = null
            isSettling = false
            if (offsetX != 0f || offsetY != 0f) {
                offsetX = 0f
                offsetY = 0f
            }
            reportDragProgress(0f, force = true)
        }
    }

    val underlayScale = restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f) +
        ((1f - restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f)) * revealProgress.coerceIn(0f, 1f))

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
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
                if (enabled && !isSettling) {
                    Modifier.pointerInput(
                        enabled,
                        cardSize,
                        canSwipeLeft,
                        canSwipeRight,
                        canSwipeUp,
                        canSwipeDown,
                    ) {
                        detectDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                                isSettling = false
                            },
                            onDragEnd = {
                                settleJob = scope.launch {
                                    isSettling = true

                                    val decision = resolveSwipeDecision(
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        cardSize = cardSize,
                                        canSwipeLeft = currentCanSwipeLeft,
                                        canSwipeRight = currentCanSwipeRight,
                                        canSwipeUp = currentCanSwipeUp,
                                        canSwipeDown = currentCanSwipeDown,
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
                            },
                            onDragCancel = {
                                settleJob = scope.launch {
                                    isSettling = true
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
                                            ),
                                        )
                                    }
                                    isSettling = false
                                    settleJob = null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                val dampedDeltaX = when {
                                    dragAmount.x < 0f && !currentCanSwipeLeft -> {
                                        dragAmount.x * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    dragAmount.x > 0f && !currentCanSwipeRight -> {
                                        dragAmount.x * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    else -> dragAmount.x
                                }

                                val dampedDeltaY = when {
                                    dragAmount.y < 0f && !currentCanSwipeUp -> {
                                        dragAmount.y * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    dragAmount.y > 0f && !currentCanSwipeDown -> {
                                        dragAmount.y * BLOCKED_DIRECTION_DRAG_FRICTION
                                    }

                                    else -> dragAmount.y
                                }

                                offsetX += dampedDeltaX
                                offsetY += dampedDeltaY

                                reportDragProgress(
                                    progress = calculateDragProgress(
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        cardSize = cardSize,
                                    ),
                                )
                            },
                        )
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
): SwipeDecision? {
    val width = cardSize.width.toFloat()
    val height = cardSize.height.toFloat()

    if (width <= 0f || height <= 0f) {
        return null
    }

    val horizontalThreshold = width * HORIZONTAL_DISMISS_THRESHOLD_FRACTION
    val verticalThreshold = height * VERTICAL_DISMISS_THRESHOLD_FRACTION

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

private fun calculateDragProgress(
    offsetX: Float,
    offsetY: Float,
    cardSize: IntSize,
): Float {
    val width = cardSize.width.toFloat().coerceAtLeast(1f)
    val height = cardSize.height.toFloat().coerceAtLeast(1f)
    val distance = hypot(offsetX.toDouble(), offsetY.toDouble()).toFloat()
    val dismissDistance = hypot(
        (width * HORIZONTAL_DISMISS_THRESHOLD_FRACTION).toDouble(),
        (height * VERTICAL_DISMISS_THRESHOLD_FRACTION).toDouble(),
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

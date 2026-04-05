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


internal data class SwipeDecision(
    val direction: SwipeDirection,
    val targetX: Float,
    val targetY: Float,
)

internal fun resolveSwipeDecision(
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

internal suspend fun animateCardTo(
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

internal suspend fun animateValueTo(
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

internal fun calculateDownSwipeCoverProgress(
    downSwipeCoverOffsetY: Float,
    cardSize: IntSize,
): Float {
    val height = cardSize.height.toFloat().coerceAtLeast(1f)
    return (downSwipeCoverOffsetY / height).coerceIn(0f, 1f)
}

internal fun calculateRightSwipeCoverProgress(
    rightSwipeCoverOffsetX: Float,
    cardSize: IntSize,
): Float {
    val width = cardSize.width.toFloat().coerceAtLeast(1f)
    return (rightSwipeCoverOffsetX / width).coerceIn(0f, 1f)
}

internal fun calculateDragProgress(
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

internal const val HORIZONTAL_DISMISS_THRESHOLD_FRACTION = 0.22f
internal const val VERTICAL_DISMISS_THRESHOLD_FRACTION = 0.22f
internal const val SWIPE_OUT_DISTANCE_MULTIPLIER = 1.38f
internal const val BLOCKED_DIRECTION_DRAG_FRICTION = 0.18f
internal const val OUT_SWIPE_CROSS_AXIS_FACTOR = 0.35f
internal const val ROTATION_DIVISOR = 18f
internal const val MIN_UNDERLAY_SCALE = 0.90f
internal const val DRAG_PROGRESS_EPSILON = 0.006f
internal const val MIN_GESTURE_SENSITIVITY = 0.8f
internal const val MAX_GESTURE_SENSITIVITY = 1.35f
internal const val DEFAULT_GESTURE_SENSITIVITY = 1f
internal const val MIN_TOUCH_SLOP = 6f
internal const val TOUCH_SLOP_SENSITIVITY_FACTOR = 1.28f
internal const val AXIS_LOCK_DOMINANCE_RATIO = 1.28f
internal const val AXIS_LOCK_CROSS_DAMPING = 0.28f
internal const val DOWN_SWIPE_COVER_MAX_PULL_MULTIPLIER = 1.18f
internal const val DOWN_SWIPE_COVER_CONFIRM_FRACTION = 0.33f
internal const val RIGHT_SWIPE_COVER_MAX_PULL_MULTIPLIER = 1.18f
internal const val RIGHT_SWIPE_COVER_CONFIRM_FRACTION = 0.33f
internal const val MIN_THRESHOLD_SCALE = 0.8f
internal const val MAX_THRESHOLD_SCALE = 1.25f

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

package com.example.photoroulette.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.photoroulette.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SwipeableCard(
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onSwipedUp: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canSwipeLeft: Boolean = true,
    canSwipeRight: Boolean = true,
    restingScale: Float = 1f,
    revealProgress: Float = 0f,
    onDragProgressChanged: (Float) -> Unit = {},
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val currentOnSwipedLeft by rememberUpdatedState(onSwipedLeft)
    val currentOnSwipedRight by rememberUpdatedState(onSwipedRight)
    val currentOnSwipedUp by rememberUpdatedState(onSwipedUp)
    val currentOnDragProgressChanged by rememberUpdatedState(onDragProgressChanged)
    val currentCanSwipeLeft by rememberUpdatedState(canSwipeLeft)
    val currentCanSwipeRight by rememberUpdatedState(canSwipeRight)

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var isSettling by remember { mutableStateOf(false) }

    if (enabled) {
        DisposableEffect(currentOnDragProgressChanged) {
            onDispose {
                currentOnDragProgressChanged(0f)
            }
        }
    }

    val underlayScale = restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f) +
        ((1f - restingScale.coerceIn(MIN_UNDERLAY_SCALE, 1f)) * revealProgress.coerceIn(0f, 1f))

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
            .graphicsLayer {
                translationX = if (enabled) offsetX.value else 0f
                translationY = if (enabled) offsetY.value else 0f
                rotationZ = if (enabled) offsetX.value / ROTATION_DIVISOR else 0f
                scaleX = if (enabled) 1f else underlayScale
                scaleY = if (enabled) 1f else underlayScale
                clip = true
                this.shape = shape
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (enabled && !isSettling) {
                    Modifier.pointerInput(enabled, cardSize, canSwipeLeft, canSwipeRight) {
                        detectDragGestures(
                            onDragStart = {
                                scope.launch {
                                    isSettling = false
                                    offsetX.stop()
                                    offsetY.stop()
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    isSettling = true

                                    val width = cardSize.width.toFloat()
                                    val height = cardSize.height.toFloat()

                                    if (width <= 0f || height <= 0f) {
                                        animateCardTo(
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            targetX = 0f,
                                            targetY = 0f,
                                            cardSize = cardSize,
                                            onDragProgressChanged = currentOnDragProgressChanged,
                                        )
                                        isSettling = false
                                        return@launch
                                    }

                                    val horizontalThreshold = width * HORIZONTAL_DISMISS_THRESHOLD_FRACTION
                                    val verticalThreshold = height * VERTICAL_DISMISS_THRESHOLD_FRACTION
                                    val verticalDominates = verticalThreshold > 0f &&
                                        abs(offsetY.value) / verticalThreshold >=
                                        abs(offsetX.value) / horizontalThreshold.coerceAtLeast(1f)

                                    when {
                                        offsetY.value <= -verticalThreshold && verticalDominates -> {
                                            animateCardTo(
                                                offsetX = offsetX,
                                                offsetY = offsetY,
                                                targetX = offsetX.value,
                                                targetY = -height * SWIPE_OUT_DISTANCE_MULTIPLIER,
                                                cardSize = cardSize,
                                                onDragProgressChanged = currentOnDragProgressChanged,
                                            )
                                            currentOnSwipedUp()
                                        }

                                        offsetX.value >= horizontalThreshold -> {
                                            if (currentCanSwipeRight) {
                                                animateCardTo(
                                                    offsetX = offsetX,
                                                    offsetY = offsetY,
                                                    targetX = width * SWIPE_OUT_DISTANCE_MULTIPLIER,
                                                    targetY = offsetY.value,
                                                    cardSize = cardSize,
                                                    onDragProgressChanged = currentOnDragProgressChanged,
                                                )
                                                currentOnSwipedRight()
                                            } else {
                                                animateCardTo(
                                                    offsetX = offsetX,
                                                    offsetY = offsetY,
                                                    targetX = 0f,
                                                    targetY = 0f,
                                                    cardSize = cardSize,
                                                    onDragProgressChanged = currentOnDragProgressChanged,
                                                )
                                                isSettling = false
                                            }
                                        }

                                        offsetX.value <= -horizontalThreshold -> {
                                            if (currentCanSwipeLeft) {
                                                animateCardTo(
                                                    offsetX = offsetX,
                                                    offsetY = offsetY,
                                                    targetX = -width * SWIPE_OUT_DISTANCE_MULTIPLIER,
                                                    targetY = offsetY.value,
                                                    cardSize = cardSize,
                                                    onDragProgressChanged = currentOnDragProgressChanged,
                                                )
                                                currentOnSwipedLeft()
                                            } else {
                                                animateCardTo(
                                                    offsetX = offsetX,
                                                    offsetY = offsetY,
                                                    targetX = 0f,
                                                    targetY = 0f,
                                                    cardSize = cardSize,
                                                    onDragProgressChanged = currentOnDragProgressChanged,
                                                )
                                                isSettling = false
                                            }
                                        }

                                        else -> {
                                            animateCardTo(
                                                offsetX = offsetX,
                                                offsetY = offsetY,
                                                targetX = 0f,
                                                targetY = 0f,
                                                cardSize = cardSize,
                                                onDragProgressChanged = currentOnDragProgressChanged,
                                            )
                                            isSettling = false
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    isSettling = true
                                    animateCardTo(
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        targetX = 0f,
                                        targetY = 0f,
                                        cardSize = cardSize,
                                        onDragProgressChanged = currentOnDragProgressChanged,
                                    )
                                    isSettling = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                    currentOnDragProgressChanged(
                                        calculateDragProgress(
                                            offsetX = offsetX.value,
                                            offsetY = offsetY.value,
                                            cardSize = cardSize,
                                        ),
                                    )
                                }
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

private suspend fun animateCardTo(
    offsetX: Animatable<Float, *>,
    offsetY: Animatable<Float, *>,
    targetX: Float,
    targetY: Float,
    cardSize: IntSize,
    onDragProgressChanged: (Float) -> Unit,
) = coroutineScope {
    launch {
        offsetX.animateTo(
            targetValue = targetX,
            animationSpec = swipeSpringSpec(),
        ) {
            onDragProgressChanged(
                calculateDragProgress(
                    offsetX = value,
                    offsetY = offsetY.value,
                    cardSize = cardSize,
                ),
            )
        }
    }

    launch {
        offsetY.animateTo(
            targetValue = targetY,
            animationSpec = swipeSpringSpec(),
        ) {
            onDragProgressChanged(
                calculateDragProgress(
                    offsetX = offsetX.value,
                    offsetY = value,
                    cardSize = cardSize,
                ),
            )
        }
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

private fun swipeSpringSpec(): SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium,
)

private const val HORIZONTAL_DISMISS_THRESHOLD_FRACTION = 0.30f
private const val VERTICAL_DISMISS_THRESHOLD_FRACTION = 0.30f
private const val SWIPE_OUT_DISTANCE_MULTIPLIER = 1.35f
private const val ROTATION_DIVISOR = 20f
private const val MIN_UNDERLAY_SCALE = 0.90f

@Preview(showBackground = true, backgroundColor = 0xFFF2EFE8)
@Composable
private fun SwipeableCardPreview() {
    MaterialTheme {
        SwipeableCard(
            onSwipedLeft = {},
            onSwipedRight = {},
            onSwipedUp = {},
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

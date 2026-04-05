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
internal fun DraggableFloatingDeleteButton(
    onDeleteClick: () -> Unit,
    isGestureBallEnabled: Boolean,
    gestureBallSizeScale: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val buttonSize = FLOATING_DELETE_BUTTON_SIZE
    val margin = 18.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val buttonRadiusPx = buttonSizePx / 2f
    val marginPx = with(density) { margin.toPx() }
    val normalizedGestureBallSizeScale = gestureBallSizeScale.coerceIn(
        SettingsRepository.MIN_GESTURE_BALL_SIZE_SCALE,
        SettingsRepository.MAX_GESTURE_BALL_SIZE_SCALE,
    )
    val gestureBallRadiusPx = with(density) {
        (GESTURE_BALL_BASE_OUTER_RADIUS * normalizedGestureBallSizeScale).toPx()
    }
    val gestureBallMarginPx = with(density) { GESTURE_BALL_MARGIN.toPx() }
    val gestureBallClearancePx = with(density) { FLOATING_DELETE_BUTTON_GESTURE_BALL_CLEARANCE.toPx() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var positioned by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var suppressTapUntilRelease by remember { mutableStateOf(false) }
    val currentOnDeleteClick by rememberUpdatedState(onDeleteClick)

    fun clampX(value: Float): Float {
        val maxX = (containerSize.width.toFloat() - buttonSizePx - marginPx).coerceAtLeast(marginPx)
        return value.coerceIn(marginPx, maxX)
    }

    fun clampY(value: Float): Float {
        val maxY = (containerSize.height.toFloat() - buttonSizePx - marginPx).coerceAtLeast(marginPx)
        return value.coerceIn(marginPx, maxY)
    }

    fun clampCenter(target: Offset): Offset {
        val minX = marginPx + buttonRadiusPx
        val maxX = (containerSize.width.toFloat() - marginPx - buttonRadiusPx).coerceAtLeast(minX)
        val minY = marginPx + buttonRadiusPx
        val maxY = (containerSize.height.toFloat() - marginPx - buttonRadiusPx).coerceAtLeast(minY)
        return Offset(
            x = target.x.coerceIn(minX, maxX),
            y = target.y.coerceIn(minY, maxY),
        )
    }

    if (!positioned && containerSize.width > 0 && containerSize.height > 0) {
        val containerWidth = containerSize.width.toFloat()
        val containerHeight = containerSize.height.toFloat()
        val desiredCenter = clampCenter(
            Offset(
                x = containerWidth - marginPx - buttonRadiusPx,
                y = containerHeight * FLOATING_DELETE_BUTTON_DEFAULT_CENTER_Y_RATIO,
            ),
        )
        var initialCenter = desiredCenter

        if (isGestureBallEnabled) {
            val gestureBallCenter = Offset(
                x = containerWidth - gestureBallRadiusPx - gestureBallMarginPx,
                y = containerHeight * GESTURE_BALL_DEFAULT_CENTER_Y_RATIO,
            )
            val minimumSafeDistance = gestureBallRadiusPx + buttonRadiusPx + gestureBallClearancePx

            if ((initialCenter - gestureBallCenter).getDistanceValue() < minimumSafeDistance) {
                val preferredLowerCenter = clampCenter(
                    Offset(
                        x = initialCenter.x,
                        y = gestureBallCenter.y + minimumSafeDistance,
                    ),
                )
                val fallbackUpperCenter = clampCenter(
                    Offset(
                        x = initialCenter.x,
                        y = gestureBallCenter.y - minimumSafeDistance,
                    ),
                )

                initialCenter = when {
                    (preferredLowerCenter - gestureBallCenter).getDistanceValue() >=
                        minimumSafeDistance -> preferredLowerCenter

                    (fallbackUpperCenter - gestureBallCenter).getDistanceValue() >=
                        minimumSafeDistance -> fallbackUpperCenter

                    abs(preferredLowerCenter.y - desiredCenter.y) <=
                        abs(fallbackUpperCenter.y - desiredCenter.y) -> preferredLowerCenter

                    else -> fallbackUpperCenter
                }
            }
        }

        offsetX = clampX(initialCenter.x - buttonRadiusPx)
        offsetY = clampY(initialCenter.y - buttonRadiusPx)
        positioned = true
    }

    Box(
        modifier = modifier.onSizeChanged { containerSize = it },
    ) {
        Surface(
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(containerSize) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            suppressTapUntilRelease = true
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = clampX(offsetX + dragAmount.x)
                        offsetY = clampY(offsetY + dragAmount.y)
                    }
                }
                .pointerInput(isDragging, currentOnDeleteClick) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isTap = true

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (isTap && !isDragging && !suppressTapUntilRelease) {
                                    currentOnDeleteClick()
                                }
                                suppressTapUntilRelease = false
                                break
                            }

                            if (change.positionChange() != Offset.Zero) {
                                isTap = false
                            }
                        }
                    }
                },
            shape = CircleShape,
            color = Color(0xFFD32F2F),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(id = R.string.floating_delete_button_title),
                    tint = Color.White,
                )
            }
        }
    }
}



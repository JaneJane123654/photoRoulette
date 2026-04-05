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
internal fun PhotoDeck(
    previousCard: MediaCard?,
    visibleCards: List<MediaCard>,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    isSwipeDeleteEnabled: Boolean,
    swipeGestureSensitivity: Float,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    showFullImage: Boolean,
    isTapImageToggleEnabled: Boolean,
    onSwipeAction: (SwipeAction, Long) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val playerPool = remember(context) { DeckPlayerPool(context) }
    var topCardDragProgress by remember { mutableFloatStateOf(0f) }
    var topCardDownCoverProgress by remember { mutableFloatStateOf(0f) }
    var topCardRightCoverProgress by remember { mutableFloatStateOf(0f) }
    var isTopCardForceFullImage by remember { mutableStateOf(false) }
    var isTopCardImageGestureLocked by remember { mutableStateOf(false) }
    val topCard = visibleCards.firstOrNull()
    val previousTopCoverTriggerDirection = when {
        swipeDownAction == SwipeAction.Previous -> SwipeDirection.Down
        swipeUpAction == SwipeAction.Previous -> SwipeDirection.Up
        else -> null
    }
    val previousRightCoverTriggerDirection = when {
        swipeRightAction == SwipeAction.Previous -> SwipeDirection.Right
        swipeLeftAction == SwipeAction.Previous -> SwipeDirection.Left
        else -> null
    }
    val shouldUsePreviousCardTopCover =
        previousTopCoverTriggerDirection != null && previousCard != null
    val shouldUsePreviousCardRightCover =
        previousRightCoverTriggerDirection != null && previousCard != null
    val nextVideoUri = remember(visibleCards) {
        visibleCards
            .drop(1)
            .firstOrNull { card -> card.isVideoLike }
            ?.playbackUri
    }

    DisposableEffect(playerPool) {
        onDispose {
            playerPool.release()
        }
    }

    LaunchedEffect(visibleCards.firstOrNull()?.id) {
        topCardDragProgress = 0f
        topCardDownCoverProgress = 0f
        topCardRightCoverProgress = 0f
        isTopCardForceFullImage = false
        isTopCardImageGestureLocked = false
    }

    LaunchedEffect(
        topCard?.id,
        topCard?.playbackUri,
        topCard?.kind,
        nextVideoUri,
    ) {
        val topVideoUri = topCard?.takeIf { card -> card.isVideoLike }?.playbackUri
        if (topVideoUri != null) {
            playerPool.activate(
                uri = topVideoUri,
                autoPlay = topCard.kind == MediaKind.Video,
            )
        } else {
            playerPool.clearActive()
        }

        playerPool.warmup(nextVideoUri)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val deckWidth = minOf(maxWidth, maxHeight * CARD_ASPECT_RATIO).coerceAtMost(MAX_DECK_WIDTH)
        val deckHeight = deckWidth / CARD_ASPECT_RATIO
        val deckWidthPx = with(density) { deckWidth.toPx() }
        val deckHeightPx = with(density) { deckHeight.toPx() }
        val clampedTopCardDownCoverProgress = topCardDownCoverProgress.coerceIn(0f, 1f)
        val clampedTopCardRightCoverProgress = topCardRightCoverProgress.coerceIn(0f, 1f)
        val previousCoverTranslationY = -deckHeightPx * (1f - clampedTopCardDownCoverProgress)
        val previousCoverTranslationX = -deckWidthPx * (1f - clampedTopCardRightCoverProgress)

        Box(
            modifier = Modifier.size(
                width = deckWidth,
                height = deckHeight,
            ).clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            val topCardShowFullImage = showFullImage || isTopCardForceFullImage

            for (index in visibleCards.indices.reversed()) {
                val card = visibleCards[index]
                val isTopCard = index == 0
                val layerInset = CARD_LAYER_INSET * index.toFloat()
                val horizontalInset = layerInset
                val verticalInset = layerInset * 1.2f
                val cardZIndex = (visibleCards.size - index).toFloat()

                key(card.id) {
                    SwipeableCard(
                        onSwiped = { direction ->
                            val action = when (direction) {
                                SwipeDirection.Left -> swipeLeftAction
                                SwipeDirection.Right -> swipeRightAction
                                SwipeDirection.Up -> swipeUpAction
                                SwipeDirection.Down -> swipeDownAction
                            }
                            onSwipeAction(action, card.id)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = horizontalInset,
                                vertical = verticalInset,
                            )
                            .zIndex(cardZIndex),
                        enabled = isTopCard && !isTopCardImageGestureLocked,
                        gestureSensitivity = swipeGestureSensitivity,
                        canSwipeLeft = isTopCard && canSwipeForAction(
                            action = swipeLeftAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeRight = isTopCard && canSwipeForAction(
                            action = swipeRightAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeUp = isTopCard && canSwipeForAction(
                            action = swipeUpAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        canSwipeDown = isTopCard && canSwipeForAction(
                            action = swipeDownAction,
                            canSwipePrevious = canSwipePrevious,
                            canSwipeNext = canSwipeNext,
                            isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                        ),
                        restingScale = when (index) {
                            0 -> 1f
                            1 -> 0.96f
                            else -> 0.92f
                        },
                        revealProgress = when (index) {
                            0 -> 0f
                            1 -> topCardDragProgress
                            else -> topCardDragProgress * BACK_CARD_REVEAL_MULTIPLIER
                        },
                        onDragProgressChanged = if (isTopCard) {
                            { topCardDragProgress = it }
                        } else {
                            {}
                        },
                        isDownSwipeCoverEnabled =
                            isTopCard &&
                                shouldUsePreviousCardTopCover &&
                                canSwipePrevious,
                        downSwipeCoverTriggerDirection =
                            previousTopCoverTriggerDirection ?: SwipeDirection.Down,
                        onDownSwipeCoverProgressChanged = if (isTopCard) {
                            { topCardDownCoverProgress = it }
                        } else {
                            {}
                        },
                        isRightSwipeCoverEnabled =
                            isTopCard &&
                                shouldUsePreviousCardRightCover &&
                                canSwipePrevious,
                        rightSwipeCoverTriggerDirection =
                            previousRightCoverTriggerDirection ?: SwipeDirection.Right,
                        onRightSwipeCoverProgressChanged = if (isTopCard) {
                            { topCardRightCoverProgress = it }
                        } else {
                            {}
                        },
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        MediaCardContent(
                            card = card,
                            isTopCard = isTopCard,
                            playerPool = playerPool,
                            showFullImage = if (isTopCard) topCardShowFullImage else showFullImage,
                            enableTwoFingerTransform = isTopCard,
                            enableTapToggle = isTapImageToggleEnabled,
                            onGestureLockChanged = if (isTopCard) {
                                { isTopCardImageGestureLocked = it }
                            } else {
                                {}
                            },
                            onTapWhenIdle = if (isTopCard) {
                                { isTopCardForceFullImage = !isTopCardForceFullImage }
                            } else {
                                {}
                            },
                        )
                    }
                }
            }

            val shouldShowDownSwipeCover =
                shouldUsePreviousCardTopCover &&
                    clampedTopCardDownCoverProgress > DOWN_SWIPE_COVER_VISIBILITY_EPSILON
            val shouldShowRightSwipeCover =
                shouldUsePreviousCardRightCover &&
                    clampedTopCardRightCoverProgress > RIGHT_SWIPE_COVER_VISIBILITY_EPSILON

            if (shouldShowDownSwipeCover || shouldShowRightSwipeCover) {
                val coverCard = previousCard
                key("cover-${coverCard.id}") {
                    SwipeableCard(
                        onSwiped = { false },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = if (shouldShowRightSwipeCover) {
                                    previousCoverTranslationX
                                } else {
                                    0f
                                }
                                translationY = if (shouldShowDownSwipeCover) {
                                    previousCoverTranslationY
                                } else {
                                    0f
                                }
                            }
                            .zIndex((visibleCards.size + 2).toFloat()),
                        enabled = false,
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        MediaCardContent(
                            card = coverCard,
                            isTopCard = false,
                            playerPool = playerPool,
                            showFullImage = showFullImage,
                            enableTwoFingerTransform = false,
                            enableTapToggle = false,
                            onGestureLockChanged = {},
                            onTapWhenIdle = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MediaCardContent(
    card: MediaCard,
    isTopCard: Boolean,
    playerPool: DeckPlayerPool,
    showFullImage: Boolean,
    enableTwoFingerTransform: Boolean,
    enableTapToggle: Boolean,
    onGestureLockChanged: (Boolean) -> Unit,
    onTapWhenIdle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (card.kind) {
        MediaKind.Image -> {
            StaticPhotoCardImage(
                card = card,
                showFullImage = showFullImage,
                enableTwoFingerTransform = enableTwoFingerTransform,
                enableTapToggle = enableTapToggle,
                onGestureLockChanged = onGestureLockChanged,
                onTapWhenIdle = onTapWhenIdle,
                modifier = modifier,
            )
        }

        MediaKind.AnimatedImage -> {
            AnimatedPhotoCardImage(
                card = card,
                showFullImage = showFullImage,
                enableTapToggle = enableTapToggle,
                onGestureLockChanged = onGestureLockChanged,
                onTapWhenIdle = onTapWhenIdle,
                modifier = modifier,
            )
        }

        MediaKind.Video,
        MediaKind.LivePhoto,
        -> {
            VideoCardContent(
                card = card,
                isTopCard = isTopCard,
                playerPool = playerPool,
                showFullImage = showFullImage,
                onGestureLockChanged = onGestureLockChanged,
                modifier = modifier,
            )
        }
    }
}



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
internal fun SettingsDialog(
    isSwipeDeleteEnabled: Boolean,
    isDeleteReminderEnabled: Boolean,
    swipeGestureSensitivity: Float,
    showFullImage: Boolean,
    isTapImageToggleEnabled: Boolean,
    showFloatingDeleteButton: Boolean,
    isGestureBallEnabled: Boolean,
    gestureBallSizeScale: Float,
    isGestureBallFeedbackEnabled: Boolean,
    showGestureBallActionHint: Boolean,
    isSilentDeleteEnabled: Boolean,
    silentDeleteDcimLabel: String?,
    silentDeletePicturesLabel: String?,
    hasDcimDirectoryAuthorized: Boolean,
    hasPicturesDirectoryAuthorized: Boolean,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    swipeUpAction: SwipeAction,
    swipeDownAction: SwipeAction,
    defaultBehaviorNoticeMode: DefaultBehaviorNoticeMode,
    selectedLanguageTag: String,
    onDismiss: () -> Unit,
    onSwipeDeleteEnabledChange: (Boolean) -> Unit,
    onDeleteReminderEnabledChange: (Boolean) -> Unit,
    onSwipeGestureSensitivityChange: (Float) -> Unit,
    onShowFullImageChange: (Boolean) -> Unit,
    onTapImageToggleEnabledChange: (Boolean) -> Unit,
    onShowFloatingDeleteButtonChange: (Boolean) -> Unit,
    onGestureBallEnabledChange: (Boolean) -> Unit,
    onGestureBallSizeScaleChange: (Float) -> Unit,
    onGestureBallFeedbackEnabledChange: (Boolean) -> Unit,
    onShowGestureBallActionHintChange: (Boolean) -> Unit,
    onSilentDeleteEnabledChange: (Boolean) -> Unit,
    onConfigureSilentDeleteDcimDirectory: () -> Unit,
    onConfigureSilentDeletePicturesDirectory: () -> Unit,
    onSwipeLeftActionChange: (SwipeAction) -> Unit,
    onSwipeRightActionChange: (SwipeAction) -> Unit,
    onSwipeUpActionChange: (SwipeAction) -> Unit,
    onSwipeDownActionChange: (SwipeAction) -> Unit,
    onDefaultBehaviorNoticeEnabledChange: (Boolean) -> Unit,
    onLanguageTagChange: (String) -> Unit,
    onCheckUpdateClick: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_dialog_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.settings_dialog_done))
                    }
                }

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_guide))
                UsageGuideCard()
                DefaultBehaviorNoticeControls(
                    mode = defaultBehaviorNoticeMode,
                    onCheckedChange = onDefaultBehaviorNoticeEnabledChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_display))
                ImageDisplayControls(
                    showFullImage = showFullImage,
                    onCheckedChange = onShowFullImageChange,
                )
                TapImageToggleControls(
                    isEnabled = isTapImageToggleEnabled,
                    onCheckedChange = onTapImageToggleEnabledChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_swipe))
                SwipeBehaviorControls(
                    swipeLeftAction = swipeLeftAction,
                    swipeRightAction = swipeRightAction,
                    swipeUpAction = swipeUpAction,
                    swipeDownAction = swipeDownAction,
                    swipeGestureSensitivity = swipeGestureSensitivity,
                    onSwipeLeftActionChange = onSwipeLeftActionChange,
                    onSwipeRightActionChange = onSwipeRightActionChange,
                    onSwipeUpActionChange = onSwipeUpActionChange,
                    onSwipeDownActionChange = onSwipeDownActionChange,
                    onSwipeGestureSensitivityChange = onSwipeGestureSensitivityChange,
                )
                GestureBallControls(
                    isGestureBallEnabled = isGestureBallEnabled,
                    sizeScale = gestureBallSizeScale,
                    isFeedbackEnabled = isGestureBallFeedbackEnabled,
                    showActionHint = showGestureBallActionHint,
                    onCheckedChange = onGestureBallEnabledChange,
                    onSizeScaleChange = onGestureBallSizeScaleChange,
                    onFeedbackEnabledChange = onGestureBallFeedbackEnabledChange,
                    onActionHintEnabledChange = onShowGestureBallActionHintChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_delete))
                SwipeDeleteControls(
                    isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                    onCheckedChange = onSwipeDeleteEnabledChange,
                )
                DeleteReminderControls(
                    isEnabled = isDeleteReminderEnabled,
                    onCheckedChange = onDeleteReminderEnabledChange,
                )
                FloatingDeleteButtonControls(
                    isEnabled = showFloatingDeleteButton,
                    onCheckedChange = onShowFloatingDeleteButtonChange,
                )
                SilentDeleteControls(
                    isSilentDeleteEnabled = isSilentDeleteEnabled,
                    dcimLabel = silentDeleteDcimLabel,
                    picturesLabel = silentDeletePicturesLabel,
                    hasDcimDirectoryAuthorized = hasDcimDirectoryAuthorized,
                    hasPicturesDirectoryAuthorized = hasPicturesDirectoryAuthorized,
                    onCheckedChange = onSilentDeleteEnabledChange,
                    onConfigureDcimDirectory = onConfigureSilentDeleteDcimDirectory,
                    onConfigurePicturesDirectory = onConfigureSilentDeletePicturesDirectory,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_language))
                LanguageSettingsControls(
                    selectedLanguageTag = selectedLanguageTag,
                    onLanguageTagChange = onLanguageTagChange,
                )

                SettingsSectionTitle(text = stringResource(id = R.string.settings_section_update))
                UpdateControls(
                    onCheckUpdateClick = onCheckUpdateClick,
                )
            }
        }
    }
}

@Composable
internal fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun UsageGuideCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.usage_guide_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_one),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_two),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_three),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_four),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.usage_guide_line_five),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DefaultBehaviorNoticeControls(
    mode: DefaultBehaviorNoticeMode,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = mode == DefaultBehaviorNoticeMode.Visible
    val descriptionRes = when (mode) {
        DefaultBehaviorNoticeMode.Visible -> R.string.default_behavior_visibility_visible_description
        DefaultBehaviorNoticeMode.AutoHidden -> R.string.default_behavior_visibility_auto_hidden_description
        DefaultBehaviorNoticeMode.UserHidden -> R.string.default_behavior_visibility_user_hidden_description
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.default_behavior_visibility_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}



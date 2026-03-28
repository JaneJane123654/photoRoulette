package com.example.photoroulette.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.example.photoroulette.R
import com.example.photoroulette.data.datastore.SettingsRepository
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photoroulette.ui.components.EmptyGalleryScreen
import com.example.photoroulette.ui.components.PermissionDeniedState
import com.example.photoroulette.ui.components.SwipeableCard
import com.example.photoroulette.utils.IntentHelper
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.viewmodel.MainViewModel
import com.example.photoroulette.viewmodel.states.HomeUiState

@Composable
fun MainScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    showPermissionRationale: Boolean = false,
    onPermissionRationaleDismissed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionMode by viewModel.permissionMode.collectAsStateWithLifecycle()
    val isSwipeDeleteEnabled by viewModel.isSwipeDeleteEnabled.collectAsStateWithLifecycle()

    MainScreenContent(
        state = uiState,
        permissionMode = permissionMode,
        isSwipeDeleteEnabled = isSwipeDeleteEnabled,
        onRequestPermission = onRequestPermission,
        onOpenSettings = onOpenSettings,
        onPermissionRationaleDismissed = onPermissionRationaleDismissed,
        onRefresh = viewModel::refreshMedia,
        onSwipeDeleteEnabledChange = viewModel::setSwipeDeleteEnabled,
        selectedLanguageTag = selectedLanguageTag,
        onLanguageTagChange = onLanguageTagChange,
        onSwipePrevious = viewModel::swipePrevious,
        onSwipeNext = viewModel::swipeNext,
        onDelete = viewModel::swipeDelete,
        showPermissionRationale = showPermissionRationale,
        modifier = modifier,
    )
}

@Composable
private fun MainScreenContent(
    state: HomeUiState,
    permissionMode: PermissionHelper.PermissionMode,
    isSwipeDeleteEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionRationaleDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onSwipeDeleteEnabledChange: (Boolean) -> Unit,
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    onSwipePrevious: (Long) -> Unit,
    onSwipeNext: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    showPermissionRationale: Boolean,
    modifier: Modifier = Modifier,
) {
    if (
        permissionMode == PermissionHelper.PermissionMode.DENIED &&
        showPermissionRationale
    ) {
        PermissionRationaleScreen(
            modifier = modifier,
            onRequestClicked = onRequestPermission,
            onMaybeLaterClicked = onPermissionRationaleDismissed,
        )
        return
    }

    val effectiveState = if (permissionMode == PermissionHelper.PermissionMode.DENIED) {
        HomeUiState.PermissionDenied
    } else {
        state
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MainHeader(
                onRefresh = onRefresh,
                canRefresh = effectiveState != HomeUiState.PermissionDenied,
            )

            AnimatedVisibility(
                visible = permissionMode == PermissionHelper.PermissionMode.GRANTED_PARTIAL,
            ) {
                PartialAccessBanner(onExpandAccessClick = onRequestPermission)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (effectiveState) {
                    HomeUiState.Loading -> LoadingState()
                    HomeUiState.PermissionDenied -> PermissionDeniedState(
                        onOpenSettingsClick = onOpenSettings,
                    )

                    HomeUiState.Empty -> EmptyGalleryScreen()
                    is HomeUiState.Ready -> PhotoDeck(
                        visibleIds = effectiveState.visibleIds,
                        canSwipePrevious = effectiveState.canSwipeToPrevious,
                        canSwipeNext = effectiveState.canSwipeToNext,
                        onSwipePrevious = onSwipePrevious,
                        onSwipeNext = onSwipeNext,
                        onDelete = onDelete,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (effectiveState != HomeUiState.PermissionDenied) {
                SwipeDeleteControls(
                    isSwipeDeleteEnabled = isSwipeDeleteEnabled,
                    onCheckedChange = onSwipeDeleteEnabledChange,
                )
                LanguageSettingsControls(
                    selectedLanguageTag = selectedLanguageTag,
                    onLanguageTagChange = onLanguageTagChange,
                )
            }
        }
    }
}

@Composable
private fun MainHeader(
    onRefresh: () -> Unit,
    canRefresh: Boolean,
    modifier: Modifier = Modifier,
) {
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
                    text = stringResource(id = R.string.home_header_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(id = R.string.home_header_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                enabled = canRefresh,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(id = R.string.refresh_button_content_description),
                )
                Text(
                    text = stringResource(id = R.string.refresh_button_label),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(
    onExpandAccessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = stringResource(id = R.string.partial_access_icon_content_description),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.partial_access_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.partial_access_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            Button(onClick = onExpandAccessClick) {
                Text(text = stringResource(id = R.string.partial_access_expand))
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(id = R.string.loading_deck),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SwipeDeleteControls(
    isSwipeDeleteEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = stringResource(id = R.string.swipe_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isSwipeDeleteEnabled) {
                        stringResource(id = R.string.swipe_delete_enabled_description)
                    } else {
                        stringResource(id = R.string.swipe_delete_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isSwipeDeleteEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun LanguageSettingsControls(
    selectedLanguageTag: String,
    onLanguageTagChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        LanguageUiOption(SettingsRepository.SYSTEM_LANGUAGE_TAG, stringResource(id = R.string.language_auto)),
        LanguageUiOption("ar", stringResource(id = R.string.language_name_arabic)),
        LanguageUiOption("en", stringResource(id = R.string.language_name_english)),
        LanguageUiOption("es", stringResource(id = R.string.language_name_spanish)),
        LanguageUiOption("fr", stringResource(id = R.string.language_name_french)),
        LanguageUiOption("ru", stringResource(id = R.string.language_name_russian)),
        LanguageUiOption("zh", stringResource(id = R.string.language_name_chinese)),
    )

    val effectiveTag = selectedLanguageTag.substringBefore('-').lowercase().ifBlank {
        SettingsRepository.SYSTEM_LANGUAGE_TAG
    }
    val selectedOption = options.firstOrNull { it.tag == effectiveTag } ?: options.first()
    var expanded by remember { mutableStateOf(false) }

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
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.language_option),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = selectedOption.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = stringResource(id = R.string.language_change_action))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.label) },
                            onClick = {
                                expanded = false
                                onLanguageTagChange(option.tag)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoDeck(
    visibleIds: List<Long>,
    canSwipePrevious: Boolean,
    canSwipeNext: Boolean,
    onSwipePrevious: (Long) -> Unit,
    onSwipeNext: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var topCardDragProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visibleIds.firstOrNull()) {
        topCardDragProgress = 0f
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val deckWidth = minOf(maxWidth, maxHeight * CARD_ASPECT_RATIO).coerceAtMost(MAX_DECK_WIDTH)
        val deckHeight = deckWidth / CARD_ASPECT_RATIO

        Box(
            modifier = Modifier.size(
                width = deckWidth,
                height = deckHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            for (index in visibleIds.indices.reversed()) {
                val imageId = visibleIds[index]
                val isTopCard = index == 0
                val layerInset = CARD_LAYER_INSET * index.toFloat()

                key(imageId) {
                    SwipeableCard(
                        onSwipedLeft = { onSwipePrevious(imageId) },
                        onSwipedRight = { onSwipeNext(imageId) },
                        onSwipedUp = { onDelete(imageId) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = layerInset,
                                vertical = layerInset * 1.2f,
                            )
                            .zIndex((visibleIds.size - index).toFloat()),
                        enabled = isTopCard,
                        canSwipeLeft = isTopCard && canSwipePrevious,
                        canSwipeRight = isTopCard && canSwipeNext,
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
                    ) {
                        PhotoCardImage(imageId = imageId)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCardImage(
    imageId: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(context, imageId) {
        ImageRequest.Builder(context)
            .data(IntentHelper.buildImageUri(imageId))
            .crossfade(true)
            .build()
    }

    var visualState by remember(imageId) { mutableStateOf(PhotoVisualState.Loading) }
    val placeholderPainter = ColorPainter(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    )
    val transparentPainter = remember { ColorPainter(Color.Transparent) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            ),
    ) {
        if (visualState != PhotoVisualState.Ready) {
            PhotoFallbackContent(
                isError = visualState == PhotoVisualState.Error,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        AsyncImage(
            model = request,
            contentDescription = stringResource(id = R.string.photo_content_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = placeholderPainter,
            error = transparentPainter,
            fallback = transparentPainter,
            onLoading = { visualState = PhotoVisualState.Loading },
            onSuccess = { visualState = PhotoVisualState.Ready },
            onError = { visualState = PhotoVisualState.Error },
        )
    }
}

@Composable
private fun PhotoFallbackContent(
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        ) {
            Box(
                modifier = Modifier.padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!isError) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = if (isError) {
                stringResource(id = R.string.photo_load_error_title)
            } else {
                stringResource(id = R.string.photo_loading_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (isError) {
                stringResource(id = R.string.photo_load_error_description)
            } else {
                stringResource(id = R.string.photo_loading_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class PhotoVisualState {
    Loading,
    Ready,
    Error,
}

private data class LanguageUiOption(
    val tag: String,
    val label: String,
)

private const val CARD_ASPECT_RATIO = 0.72f
private const val BACK_CARD_REVEAL_MULTIPLIER = 0.72f
private val CARD_LAYER_INSET = 12.dp
private val MAX_DECK_WIDTH = 460.dp

@Preview(showBackground = true, backgroundColor = 0xFFF4F1EA)
@Composable
private fun MainScreenReadyPreview() {
    MaterialTheme {
        MainScreenContent(
            state = HomeUiState.Ready(
                visibleIds = listOf(11L, 12L, 13L),
                canSwipeToPrevious = true,
                canSwipeToNext = true,
            ),
            permissionMode = PermissionHelper.PermissionMode.GRANTED_PARTIAL,
            isSwipeDeleteEnabled = true,
            onRequestPermission = {},
            onOpenSettings = {},
            onPermissionRationaleDismissed = {},
            onRefresh = {},
            onSwipeDeleteEnabledChange = {},
            selectedLanguageTag = SettingsRepository.SYSTEM_LANGUAGE_TAG,
            onLanguageTagChange = {},
            onSwipePrevious = {},
            onSwipeNext = {},
            onDelete = {},
            showPermissionRationale = false,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F1EA)
@Composable
private fun MainScreenPermissionPreview() {
    MaterialTheme {
        MainScreenContent(
            state = HomeUiState.PermissionDenied,
            permissionMode = PermissionHelper.PermissionMode.DENIED,
            isSwipeDeleteEnabled = false,
            onRequestPermission = {},
            onOpenSettings = {},
            onPermissionRationaleDismissed = {},
            onRefresh = {},
            onSwipeDeleteEnabledChange = {},
            selectedLanguageTag = SettingsRepository.SYSTEM_LANGUAGE_TAG,
            onLanguageTagChange = {},
            onSwipePrevious = {},
            onSwipeNext = {},
            onDelete = {},
            showPermissionRationale = true,
        )
    }
}

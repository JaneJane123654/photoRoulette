package com.example.photoroulette.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.photoroulette.R

@Composable
fun PermissionRationaleScreen(
    onRequestClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onMaybeLaterClicked: () -> Unit = {},
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainerLowest,
            MaterialTheme.colorScheme.surface,
        ),
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PhotoPermissionHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                )

                Column(
                    modifier = Modifier.widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.permission_rationale_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )

                    Text(
                        text = stringResource(id = R.string.permission_rationale_description_primary),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(id = R.string.permission_rationale_description_secondary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        PermissionFeatureRow(
                            icon = Icons.Outlined.Shuffle,
                            iconContentDescription = stringResource(id = R.string.permission_feature_all_photos_icon_description),
                            title = stringResource(id = R.string.permission_feature_all_photos_title),
                            body = stringResource(id = R.string.permission_feature_all_photos_body),
                        )

                        PermissionFeatureRow(
                            icon = Icons.Outlined.Collections,
                            iconContentDescription = stringResource(id = R.string.permission_feature_selected_icon_description),
                            title = stringResource(id = R.string.permission_feature_selected_title),
                            body = stringResource(id = R.string.permission_feature_selected_body),
                        )

                        PermissionFeatureRow(
                            icon = Icons.Outlined.Tune,
                            iconContentDescription = stringResource(id = R.string.permission_feature_control_icon_description),
                            title = stringResource(id = R.string.permission_feature_control_title),
                            body = stringResource(id = R.string.permission_feature_control_body),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = onRequestClicked,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(id = R.string.permission_continue))
                    }

                    TextButton(
                        onClick = onMaybeLaterClicked,
                    ) {
                        Text(text = stringResource(id = R.string.permission_maybe_later))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPermissionHero(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        PhotoIllustrationCard(
            icon = Icons.Outlined.Collections,
            iconContentDescription = stringResource(id = R.string.permission_illustration_collection_description),
            accentColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .offset(x = (-82).dp, y = 20.dp)
                .graphicsLayer { rotationZ = -14f },
        )

        PhotoIllustrationCard(
            icon = Icons.Outlined.Shuffle,
            iconContentDescription = stringResource(id = R.string.permission_illustration_shuffle_description),
            accentColor = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .offset(x = 84.dp, y = 26.dp)
                .graphicsLayer { rotationZ = 13f },
        )

        PhotoIllustrationCard(
            icon = Icons.Outlined.PhotoLibrary,
            iconContentDescription = stringResource(id = R.string.permission_illustration_library_description),
            accentColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.graphicsLayer { rotationZ = -2f },
            isPrimary = true,
        )
    }
}

@Composable
private fun PhotoIllustrationCard(
    icon: ImageVector,
    iconContentDescription: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    Surface(
        modifier = modifier.size(
            width = if (isPrimary) 156.dp else 132.dp,
            height = if (isPrimary) 196.dp else 168.dp,
        ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (isPrimary) 8.dp else 4.dp,
        shadowElevation = if (isPrimary) 12.dp else 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = accentColor,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isPrimary) 0.55f else 0.38f,
                )
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                fraction = when (index) {
                                    0 -> 1f
                                    1 -> 0.82f
                                    else -> 0.64f
                                },
                            )
                            .height(12.dp)
                            .background(
                                color = lineColor,
                                shape = RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionFeatureRow(
    icon: ImageVector,
    iconContentDescription: String,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1E8)
@Composable
private fun PermissionRationaleScreenPreview() {
    MaterialTheme {
        PermissionRationaleScreen(
            onRequestClicked = {},
            onMaybeLaterClicked = {},
        )
    }
}

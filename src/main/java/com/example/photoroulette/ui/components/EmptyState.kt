package com.example.photoroulette.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.photoroulette.R

@Composable
fun EmptyGalleryScreen(
    modifier: Modifier = Modifier,
) {
    FallbackStateLayout(
        modifier = modifier,
        icon = Icons.Outlined.Collections,
        title = stringResource(id = R.string.empty_state_title),
        message = stringResource(id = R.string.empty_state_message),
    )
}

@Composable
fun PermissionDeniedState(
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FallbackStateLayout(
        modifier = modifier,
        icon = Icons.Outlined.Settings,
        title = stringResource(id = R.string.permission_denied_title),
        message = stringResource(id = R.string.permission_denied_message),
        action = {
            Button(onClick = onOpenSettingsClick) {
                Text(text = stringResource(id = R.string.open_settings))
            }
        },
    )
}

@Composable
private fun FallbackStateLayout(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                if (action != null) {
                    Spacer(modifier = Modifier.height(28.dp))
                    action()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyGalleryScreenPreview() {
    MaterialTheme {
        EmptyGalleryScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionDeniedStatePreview() {
    MaterialTheme {
        PermissionDeniedState(onOpenSettingsClick = {})
    }
}

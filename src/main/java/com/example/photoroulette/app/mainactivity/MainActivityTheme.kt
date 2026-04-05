package com.example.photoroulette.app

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photoroulette.R
import com.example.photoroulette.model.SilentDeleteScope
import com.example.photoroulette.ui.screens.MainScreen
import com.example.photoroulette.utils.AppLanguageManager
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest


@Composable
internal fun PhotoRouletteTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        DarkPhotoRouletteColorScheme
    } else {
        LightPhotoRouletteColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PhotoRouletteTypography,
        content = content,
    )
}

private val LightPhotoRouletteColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF5A3A28),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFF8F3),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFF1D3BF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2B160B),
    secondary = androidx.compose.ui.graphics.Color(0xFF3F5C5E),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFF3FBFB),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFC7E4E5),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF11282A),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6D4A65),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFF7FB),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF6D7ED),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF2C1127),
    background = androidx.compose.ui.graphics.Color(0xFFF6F0E7),
    onBackground = androidx.compose.ui.graphics.Color(0xFF201A17),
    surface = androidx.compose.ui.graphics.Color(0xFFFCF7F2),
    onSurface = androidx.compose.ui.graphics.Color(0xFF201A17),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8DED5),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF51453D),
)

private val DarkPhotoRouletteColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFE0B59A),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF3A2114),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF5A3A28),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDCC7),
    secondary = androidx.compose.ui.graphics.Color(0xFFA9CBCD),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF0B3436),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF25484A),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFC7E4E5),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD6B9CD),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3D2237),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF563A4E),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFF6D7ED),
    background = androidx.compose.ui.graphics.Color(0xFF12100E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFECE1D9),
    surface = androidx.compose.ui.graphics.Color(0xFF171411),
    onSurface = androidx.compose.ui.graphics.Color(0xFFECE1D9),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF51453D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD3C3B8),
)

private val PhotoRouletteTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)


package com.example.photoroulette.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photoroulette.ui.screens.MainScreen
import com.example.photoroulette.utils.AppLanguageManager
import com.example.photoroulette.utils.PermissionHelper
import com.example.photoroulette.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var hasAttemptedPermissionRequest by mutableStateOf(false)
    private var dismissPermissionRationale by mutableStateOf(false)
    private var shouldShowPermissionRationale by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        viewModel.onPermissionResult(grants)
        shouldShowPermissionRationale = PermissionHelper.shouldShowRequestPermissionRationale(this)
    }

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
        } else {
            viewModel.onSystemDeleteCancelled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasAttemptedPermissionRequest = savedInstanceState?.getBoolean(KEY_PERMISSION_REQUESTED) ?: false
        dismissPermissionRationale = savedInstanceState?.getBoolean(KEY_RATIONALE_DISMISSED) ?: false

        syncPermissionState()

        setContent {
            val permissionMode by viewModel.permissionMode.collectAsStateWithLifecycle()
            val appLanguageTag by viewModel.appLanguageTag.collectAsStateWithLifecycle()
            val showPermissionRationale = shouldShowPermissionRationale &&
                !dismissPermissionRationale &&
                permissionMode == PermissionHelper.PermissionMode.DENIED

            PhotoRouletteTheme {
                val useDarkIcons = !isSystemInDarkTheme()

                LaunchedEffect(useDarkIcons) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = useDarkIcons
                        isAppearanceLightNavigationBars = useDarkIcons
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.deleteRequests.collectLatest { request: IntentSenderRequest ->
                        deleteRequestLauncher.launch(request)
                    }
                }

                LaunchedEffect(appLanguageTag) {
                    AppLanguageManager.applyLanguage(appLanguageTag)
                }

                LaunchedEffect(permissionMode, showPermissionRationale, hasAttemptedPermissionRequest) {
                    if (
                        permissionMode == PermissionHelper.PermissionMode.DENIED &&
                        !showPermissionRationale &&
                        !hasAttemptedPermissionRequest
                    ) {
                        launchInitialPermissionRequest()
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    onRequestPermission = ::launchPermissionRequest,
                    onOpenSettings = ::openAppSettings,
                    selectedLanguageTag = appLanguageTag,
                    onLanguageTagChange = viewModel::setAppLanguageTag,
                    showPermissionRationale = showPermissionRationale,
                    onPermissionRationaleDismissed = {
                        dismissPermissionRationale = true
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncPermissionState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PERMISSION_REQUESTED, hasAttemptedPermissionRequest)
        outState.putBoolean(KEY_RATIONALE_DISMISSED, dismissPermissionRationale)
        super.onSaveInstanceState(outState)
    }

    private fun launchPermissionRequest() {
        launchPermissionRequest(dismissRationale = true)
    }

    private fun launchInitialPermissionRequest() {
        launchPermissionRequest(dismissRationale = false)
    }

    private fun launchPermissionRequest(dismissRationale: Boolean) {
        hasAttemptedPermissionRequest = true
        if (dismissRationale) {
            dismissPermissionRationale = true
        }
        requestPermissionsLauncher.launch(PermissionHelper.getReadPermissions())
    }

    private fun openAppSettings() {
        val settingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(settingsIntent)
    }

    private fun syncPermissionState() {
        val permissionMode = PermissionHelper.checkCurrentPermissionMode(this)
        shouldShowPermissionRationale = PermissionHelper.shouldShowRequestPermissionRationale(this)

        if (permissionMode != PermissionHelper.PermissionMode.DENIED) {
            dismissPermissionRationale = false
        }

        viewModel.onPermissionModeChanged(permissionMode)
    }

    private companion object {
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
        const val KEY_RATIONALE_DISMISSED = "rationale_dismissed"
    }
}

@Composable
private fun PhotoRouletteTheme(
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

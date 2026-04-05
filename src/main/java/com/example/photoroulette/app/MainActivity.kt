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

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var hasAttemptedPermissionRequest by mutableStateOf(false)
    private var dismissPermissionRationale by mutableStateOf(false)
    private var shouldShowPermissionRationale by mutableStateOf(false)
    private var isSilentDeleteGuideVisible by mutableStateOf(false)
    private var pendingSilentDeleteScope: SilentDeleteScope? by mutableStateOf(null)

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

    private val directoryAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val requestScope = pendingSilentDeleteScope
        pendingSilentDeleteScope = null
        val treeUri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || treeUri == null) {
            viewModel.onSilentDeleteDirectoryRequestCancelled(requestScope)
            return@registerForActivityResult
        }

        val grantedFlags = result.data?.flags?.and(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        ) ?: 0

        if (grantedFlags == 0) {
            viewModel.onSilentDeleteDirectoryRequestCancelled(requestScope)
            return@registerForActivityResult
        }

        contentResolver.takePersistableUriPermission(treeUri, grantedFlags)
        val scope = requestScope ?: SilentDeleteScope.Dcim
        viewModel.onSilentDeleteDirectoryGranted(scope, treeUri)
    }

    private val apkInstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onUpdateInstallFlowFinished()
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

                LaunchedEffect(viewModel) {
                    viewModel.silentDeleteDirectoryRequests.collectLatest { scope ->
                        pendingSilentDeleteScope = scope
                        isSilentDeleteGuideVisible = true
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.updateInstallRequests.collectLatest { installUri ->
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(installUri, "application/vnd.android.package-archive")
                            clipData = ClipData.newUri(contentResolver, "update-apk", installUri)
                            putExtra(Intent.EXTRA_RETURN_RESULT, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        runCatching {
                            apkInstallLauncher.launch(installIntent)
                        }.onFailure {
                            viewModel.onUpdateInstallLaunchFailed()
                        }
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

                if (isSilentDeleteGuideVisible) {
                    SilentDeleteGuideDialog(
                        scope = pendingSilentDeleteScope ?: SilentDeleteScope.Dcim,
                        onDismiss = {
                            isSilentDeleteGuideVisible = false
                            val dismissedScope = pendingSilentDeleteScope
                            pendingSilentDeleteScope = null
                            viewModel.onSilentDeleteDirectoryRequestCancelled(dismissedScope)
                        },
                        onContinue = {
                            val targetScope = pendingSilentDeleteScope ?: SilentDeleteScope.Dcim
                            pendingSilentDeleteScope = targetScope
                            isSilentDeleteGuideVisible = false
                            launchDirectoryAccessRequest(targetScope)
                        },
                    )
                }
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

    private fun launchDirectoryAccessRequest(scope: SilentDeleteScope) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )

            val parsedInitialTreeUri = buildInitialUriForScope(scope)

            if (parsedInitialTreeUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, parsedInitialTreeUri)
            }
        }

        directoryAccessLauncher.launch(intent)
    }

    private fun syncPermissionState() {
        val permissionMode = PermissionHelper.checkCurrentPermissionMode(this)
        shouldShowPermissionRationale = PermissionHelper.shouldShowRequestPermissionRationale(this)

        if (permissionMode != PermissionHelper.PermissionMode.DENIED) {
            dismissPermissionRationale = false
        }

        viewModel.onPermissionModeChanged(permissionMode)
    }

    private fun buildInitialUriForScope(scope: SilentDeleteScope): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }

        return when (scope) {
            SilentDeleteScope.Dcim -> {
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADCIM")
            }
            SilentDeleteScope.Pictures -> {
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3APictures")
            }
        }
    }

    private companion object {
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
        const val KEY_RATIONALE_DISMISSED = "rationale_dismissed"
    }
}

@Composable
private fun SilentDeleteGuideDialog(
    scope: SilentDeleteScope,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "silent-delete-guide")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "silent-delete-guide-pulse",
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.scale(pulseScale),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.silent_delete_guide_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = when (scope) {
                            SilentDeleteScope.Dcim -> R.string.silent_delete_guide_description_dcim
                            SilentDeleteScope.Pictures -> R.string.silent_delete_guide_description_pictures
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = androidx.compose.ui.res.stringResource(
                        id = when (scope) {
                            SilentDeleteScope.Dcim -> R.string.silent_delete_guide_hint_dcim
                            SilentDeleteScope.Pictures -> R.string.silent_delete_guide_hint_pictures
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = androidx.compose.ui.res.stringResource(id = R.string.silent_delete_guide_cancel))
                    }
                    Button(onClick = onContinue) {
                        Text(text = androidx.compose.ui.res.stringResource(id = R.string.silent_delete_guide_continue))
                    }
                }
            }
        }
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

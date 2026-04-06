package com.example.photoroulette.app

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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


class MainActivity : AppCompatActivity() {

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
            val normalizedLanguageTag = AppLanguageManager.normalizeLanguageTag(appLanguageTag)
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

                LaunchedEffect(normalizedLanguageTag) {
                    AppLanguageManager.applyLanguage(normalizedLanguageTag)
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
                    selectedLanguageTag = normalizedLanguageTag,
                    onLanguageTagChange = { tag ->
                        val normalizedTag = AppLanguageManager.normalizeLanguageTag(tag)
                        AppLanguageManager.applyLanguage(normalizedTag)
                        viewModel.setAppLanguageTag(normalizedTag)
                    },
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


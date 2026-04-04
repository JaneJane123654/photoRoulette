package com.example.photoroulette.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    enum class PermissionMode {
        GRANTED_ALL,
        GRANTED_PARTIAL,
        DENIED
    }

    fun getReadPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdkInt >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    fun checkCurrentPermissionMode(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): PermissionMode {
        val hasFullMediaPermission = if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            context.hasPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
                context.hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (hasFullMediaPermission) {
            return PermissionMode.GRANTED_ALL
        }

        if (
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        ) {
            return PermissionMode.GRANTED_PARTIAL
        }

        return PermissionMode.DENIED
    }

    fun hasMediaAccess(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = checkCurrentPermissionMode(context, sdkInt) != PermissionMode.DENIED

    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean {
        if (checkCurrentPermissionMode(activity, sdkInt) != PermissionMode.DENIED) {
            return false
        }

        return getReadPermissions(sdkInt).any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    private fun Context.hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

package com.example.photoroulette.utils

import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

object IntentHelper {

    sealed interface DeleteRequestResult {
        data object Deleted : DeleteRequestResult
        data class LaunchRequest(val request: IntentSenderRequest) : DeleteRequestResult
        data class Failed(val throwable: Throwable? = null) : DeleteRequestResult
    }

    fun buildImageUri(
        imageId: Long,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): Uri = ContentUris.withAppendedId(collectionUri, imageId)

    fun createDeleteRequestOrNull(
        contentResolver: ContentResolver,
        imageUri: Uri,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): IntentSenderRequest? = when (val result = prepareDelete(contentResolver, imageUri, sdkInt)) {
        is DeleteRequestResult.LaunchRequest -> result.request
        DeleteRequestResult.Deleted,
        is DeleteRequestResult.Failed,
        -> null
    }

    fun createDeleteRequestOrNull(
        contentResolver: ContentResolver,
        imageId: Long,
        sdkInt: Int = Build.VERSION.SDK_INT,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): IntentSenderRequest? = createDeleteRequestOrNull(
        contentResolver = contentResolver,
        imageUri = buildImageUri(imageId, collectionUri),
        sdkInt = sdkInt,
    )

    fun prepareDelete(
        contentResolver: ContentResolver,
        imageUri: Uri,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): DeleteRequestResult = when {
        sdkInt >= Build.VERSION_CODES.R -> {
            runCatching {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(imageUri))
                DeleteRequestResult.LaunchRequest(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is ActivityNotFoundException,
                    is SecurityException,
                    -> DeleteRequestResult.Failed(throwable)
                    else -> throw throwable
                }
            }
        }

        sdkInt == Build.VERSION_CODES.Q -> {
            try {
                contentResolver.delete(imageUri, null, null)
                DeleteRequestResult.Deleted
            } catch (securityException: SecurityException) {
                val recoverableSecurityException = securityException as? RecoverableSecurityException
                    ?: return DeleteRequestResult.Failed(securityException)

                try {
                    DeleteRequestResult.LaunchRequest(
                        IntentSenderRequest.Builder(
                            recoverableSecurityException.userAction.actionIntent.intentSender,
                        ).build(),
                    )
                } catch (activityNotFoundException: ActivityNotFoundException) {
                    DeleteRequestResult.Failed(activityNotFoundException)
                }
            }
        }

        else -> {
            try {
                contentResolver.delete(imageUri, null, null)
                DeleteRequestResult.Deleted
            } catch (securityException: SecurityException) {
                DeleteRequestResult.Failed(securityException)
            }
        }
    }

    fun prepareDelete(
        contentResolver: ContentResolver,
        imageId: Long,
        sdkInt: Int = Build.VERSION.SDK_INT,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): DeleteRequestResult = prepareDelete(
        contentResolver = contentResolver,
        imageUri = buildImageUri(imageId, collectionUri),
        sdkInt = sdkInt,
    )
}

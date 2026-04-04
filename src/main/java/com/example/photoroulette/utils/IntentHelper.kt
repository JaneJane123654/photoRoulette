package com.example.photoroulette.utils

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.documentfile.provider.DocumentFile

object IntentHelper {

    sealed interface DeleteRequestResult {
        data object Deleted : DeleteRequestResult
        data class LaunchRequest(val request: IntentSenderRequest) : DeleteRequestResult
        data class Failed(val throwable: Throwable? = null) : DeleteRequestResult
    }

    data class SilentDeleteRequest(
        val treeUri: Uri,
        val displayName: String,
        val relativePath: String?,
    )

    data class MediaTarget(
        val id: Long,
        val collectionUri: Uri,
    )

    fun buildImageUri(
        imageId: Long,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): Uri = ContentUris.withAppendedId(collectionUri, imageId)

    fun buildMediaUri(target: MediaTarget): Uri = buildImageUri(
        imageId = target.id,
        collectionUri = target.collectionUri,
    )

    fun buildMediaUri(
        mediaId: Long,
        collectionUri: Uri,
    ): Uri = buildImageUri(
        imageId = mediaId,
        collectionUri = collectionUri,
    )

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

    @SuppressLint("NewApi")
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
        context: Context,
        contentResolver: ContentResolver,
        imageId: Long,
        silentDeleteRequest: SilentDeleteRequest?,
        sdkInt: Int = Build.VERSION.SDK_INT,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): DeleteRequestResult {
        if (silentDeleteRequest != null) {
            when (trySilentDelete(context, silentDeleteRequest)) {
                SilentDeleteOutcome.Deleted -> return DeleteRequestResult.Deleted
                SilentDeleteOutcome.NotAvailable -> Unit
            }
        }

        return prepareDelete(
            contentResolver = contentResolver,
            imageUri = buildImageUri(imageId, collectionUri),
            sdkInt = sdkInt,
        )
    }

    fun prepareDelete(
        context: Context,
        contentResolver: ContentResolver,
        imageUri: Uri,
        silentDeleteRequest: SilentDeleteRequest?,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): DeleteRequestResult {
        if (silentDeleteRequest != null) {
            when (trySilentDelete(context, silentDeleteRequest)) {
                SilentDeleteOutcome.Deleted -> return DeleteRequestResult.Deleted
                SilentDeleteOutcome.NotAvailable -> Unit
            }
        }

        return prepareDelete(
            contentResolver = contentResolver,
            imageUri = imageUri,
            sdkInt = sdkInt,
        )
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

    internal fun buildRelativePathSegments(
        treeDocumentId: String,
        mediaRelativePath: String?,
    ): List<String>? {
        val normalizedMediaPath = normalizeDirectoryPath(mediaRelativePath) ?: return null
        val treePath = normalizeDirectoryPath(treeDocumentId.substringAfter(':', ""))

        if (treePath.isNullOrEmpty()) {
            return normalizedMediaPath.split('/').filter { it.isNotBlank() }
        }

        if (normalizedMediaPath.equals(treePath, ignoreCase = true)) {
            return emptyList()
        }

        val treePrefix = "$treePath/"
        if (!normalizedMediaPath.startsWith(treePrefix, ignoreCase = true)) {
            return null
        }

        return normalizedMediaPath
            .substring(treePrefix.length)
            .split('/')
            .filter { it.isNotBlank() }
    }

    internal fun normalizeDirectoryPath(path: String?): String? {
        val normalized = path
            ?.replace('\\', '/')
            ?.trim()
            ?.trim('/')
            .orEmpty()

        return normalized.ifBlank { null }
    }

    private fun trySilentDelete(
        context: Context,
        request: SilentDeleteRequest,
    ): SilentDeleteOutcome {
        val treeDocumentId = runCatching {
            DocumentsContract.getTreeDocumentId(request.treeUri)
        }.getOrNull() ?: return SilentDeleteOutcome.NotAvailable

        val relativeSegments = buildRelativePathSegments(
            treeDocumentId = treeDocumentId,
            mediaRelativePath = request.relativePath,
        ) ?: return SilentDeleteOutcome.NotAvailable

        val rootDirectory = DocumentFile.fromTreeUri(context, request.treeUri)
            ?: return SilentDeleteOutcome.NotAvailable

        var targetDirectory: DocumentFile? = rootDirectory
        relativeSegments.forEach { segment ->
            targetDirectory = targetDirectory
                ?.findFile(segment)
                ?.takeIf { it.isDirectory }
        }

        val resolvedTargetDirectory = targetDirectory ?: return SilentDeleteOutcome.NotAvailable

        val targetFile = resolvedTargetDirectory.findFile(request.displayName)
            ?.takeIf { it.exists() && it.isFile }
            ?: return SilentDeleteOutcome.NotAvailable

        return if (targetFile.delete()) {
            SilentDeleteOutcome.Deleted
        } else {
            SilentDeleteOutcome.NotAvailable
        }
    }

    private enum class SilentDeleteOutcome {
        Deleted,
        NotAvailable,
    }
}

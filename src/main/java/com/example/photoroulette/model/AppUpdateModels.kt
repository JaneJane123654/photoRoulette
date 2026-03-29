package com.example.photoroulette.model

data class AppReleaseInfo(
    val tagName: String,
    val normalizedVersion: String,
    val title: String?,
    val notes: String?,
    val apkDownloadUrl: String?,
    val releasePageUrl: String?,
)

sealed interface UpdateCheckFeedback {
    data object Idle : UpdateCheckFeedback

    data object Checking : UpdateCheckFeedback

    data object UpToDate : UpdateCheckFeedback

    data class DeferredUntilNewer(
        val deferredVersion: String,
    ) : UpdateCheckFeedback

    data class Failed(
        val reason: String? = null,
    ) : UpdateCheckFeedback
}

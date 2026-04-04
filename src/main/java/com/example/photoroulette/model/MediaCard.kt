package com.example.photoroulette.model

import android.net.Uri

data class MediaCard(
    val id: Long,
    val mimeType: String,
    val kind: MediaKind,
    val previewUri: Uri,
    val playbackUri: Uri? = null,
    val durationMs: Long = 0L,
) {
    val isVideoLike: Boolean
        get() = kind == MediaKind.Video || kind == MediaKind.LivePhoto
}

enum class MediaKind {
    Image,
    AnimatedImage,
    Video,
    LivePhoto,
}

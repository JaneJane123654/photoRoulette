package com.example.photoroulette.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.photoroulette.model.MediaCard
import com.example.photoroulette.model.MediaKind
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val random: Random = Random.Default,
) {
    private val contentResolver = context.applicationContext.contentResolver

    /**
     * Future ContentObserver hooks can watch this URI to refresh the shuffled queue
     * after external gallery changes.
     */
    val observeUri: Uri = MediaStore.Files.getContentUri(EXTERNAL_VOLUME)

    suspend fun getShuffledMediaCards(): List<MediaCard> = withContext(ioDispatcher) {
        val mediaEntries = queryMediaEntries()
        if (mediaEntries.isEmpty()) {
            return@withContext emptyList()
        }

        buildSmartShuffle(mediaEntries)
    }

    suspend fun getSilentDeleteEntry(imageId: Long): SilentDeleteEntry? = withContext(ioDispatcher) {
        contentResolver.query(
            ContentUris.withAppendedId(observeUri, imageId),
            SILENT_DELETE_PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@withContext null
            }

            val displayNameColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (displayNameColumnIndex < 0 || cursor.isNull(displayNameColumnIndex)) {
                return@withContext null
            }

            val relativePathColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            SilentDeleteEntry(
                displayName = cursor.getString(displayNameColumnIndex),
                relativePath = if (relativePathColumnIndex >= 0 && !cursor.isNull(relativePathColumnIndex)) {
                    cursor.getString(relativePathColumnIndex)
                } else {
                    null
                },
            )
        }
    }

    private fun queryMediaEntries(): List<MediaEntry> {
        val entries = mutableListOf<MediaEntry>()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"

        contentResolver.query(
            observeUri,
            PROJECTION,
            SELECTION,
            SELECTION_ARGS,
            sortOrder,
        )?.use { cursor ->
            val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateAddedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mediaTypeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mimeTypeColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val durationColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val displayNameColumnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val dateAddedSeconds = if (cursor.isNull(dateAddedColumnIndex)) {
                    UNKNOWN_DATE_ADDED_SECONDS
                } else {
                    cursor.getLong(dateAddedColumnIndex)
                }

                val id = cursor.getLong(idColumnIndex)
                val mediaType = cursor.getInt(mediaTypeColumnIndex)
                val mimeType = if (mimeTypeColumnIndex >= 0 && !cursor.isNull(mimeTypeColumnIndex)) {
                    cursor.getString(mimeTypeColumnIndex)
                } else {
                    DEFAULT_MIME_TYPE
                }
                val durationMs = if (durationColumnIndex >= 0 && !cursor.isNull(durationColumnIndex)) {
                    cursor.getLong(durationColumnIndex)
                } else {
                    NO_DURATION
                }
                val displayName = if (displayNameColumnIndex >= 0 && !cursor.isNull(displayNameColumnIndex)) {
                    cursor.getString(displayNameColumnIndex)
                } else {
                    EMPTY_DISPLAY_NAME
                }

                val mediaUri = ContentUris.withAppendedId(observeUri, id)
                val mediaKind = resolveMediaKind(
                    mediaType = mediaType,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    displayName = displayName,
                )

                entries += MediaEntry(
                    card = MediaCard(
                        id = id,
                        mimeType = mimeType,
                        kind = mediaKind,
                        previewUri = mediaUri,
                        playbackUri = if (mediaKind == MediaKind.Video || mediaKind == MediaKind.LivePhoto) {
                            mediaUri
                        } else {
                            null
                        },
                        durationMs = durationMs.coerceAtLeast(NO_DURATION),
                    ),
                    dateAddedSeconds = dateAddedSeconds,
                )
            }
        }

        return entries
    }

    private fun buildSmartShuffle(entries: List<MediaEntry>): List<MediaCard> {
        val bucketCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val activeBuckets = entries
            .groupBy { entry ->
                buildMonthBucketKey(
                    dateAddedSeconds = entry.dateAddedSeconds,
                    calendar = bucketCalendar,
                )
            }
            .map { (bucketKey, bucketEntries) ->
                Bucket(
                    key = bucketKey,
                    cards = bucketEntries.map { it.card }.shuffled(random).toMutableList(),
                )
            }
            .shuffled(random)
            .toMutableList()

        val distributedCards = ArrayList<MediaCard>(entries.size)
        var previousBucketKey: Int? = null

        while (activeBuckets.isNotEmpty()) {
            val roundBuckets = activeBuckets.shuffled(random).toMutableList()
            moveDifferentBucketToFront(roundBuckets, previousBucketKey)

            activeBuckets.clear()

            for (bucket in roundBuckets) {
                if (bucket.cards.isEmpty()) {
                    continue
                }

                distributedCards += bucket.cards.removeAt(bucket.cards.lastIndex)
                previousBucketKey = bucket.key

                if (bucket.cards.isNotEmpty()) {
                    activeBuckets += bucket
                }
            }
        }

        return distributedCards
    }

    private fun moveDifferentBucketToFront(
        buckets: MutableList<Bucket>,
        previousBucketKey: Int?,
    ) {
        if (previousBucketKey == null || buckets.size <= 1 || buckets.first().key != previousBucketKey) {
            return
        }

        val replacementIndex = buckets.indexOfFirst { bucket -> bucket.key != previousBucketKey }
        if (replacementIndex <= 0) {
            return
        }

        val firstBucket = buckets.first()
        buckets[0] = buckets[replacementIndex]
        buckets[replacementIndex] = firstBucket
    }

    private fun buildMonthBucketKey(
        dateAddedSeconds: Long,
        calendar: Calendar,
    ): Int {
        if (dateAddedSeconds <= UNKNOWN_DATE_ADDED_SECONDS) {
            return UNKNOWN_BUCKET_KEY
        }

        calendar.timeInMillis = dateAddedSeconds * MILLIS_PER_SECOND
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        return (year * 100) + month
    }

    private fun resolveMediaKind(
        mediaType: Int,
        mimeType: String,
        durationMs: Long,
        displayName: String,
    ): MediaKind {
        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            val normalizedMimeType = mimeType.lowercase()
            val normalizedDisplayName = displayName.lowercase()
            val hasLiveNameHint = normalizedDisplayName.startsWith("mvimg") ||
                normalizedDisplayName.contains("motion") ||
                normalizedDisplayName.contains("live")
            val isLiveStyle = normalizedMimeType.contains("quicktime") ||
                normalizedMimeType.contains("motion") ||
                (hasLiveNameHint && durationMs in 1..LIVE_PHOTO_MAX_DURATION_MS)

            return if (isLiveStyle) {
                MediaKind.LivePhoto
            } else {
                MediaKind.Video
            }
        }

        return if (isAnimatedImageMimeType(mimeType)) {
            MediaKind.AnimatedImage
        } else {
            MediaKind.Image
        }
    }

    private fun isAnimatedImageMimeType(mimeType: String): Boolean {
        val normalizedMimeType = mimeType.lowercase()
        return normalizedMimeType == "image/gif" ||
            normalizedMimeType == "image/webp" ||
            normalizedMimeType == "image/apng"
    }

    private data class MediaEntry(
        val card: MediaCard,
        val dateAddedSeconds: Long,
    )

    data class SilentDeleteEntry(
        val displayName: String,
        val relativePath: String?,
    )

    private data class Bucket(
        val key: Int,
        val cards: MutableList<MediaCard>,
    )

    private companion object {
        const val EXTERNAL_VOLUME = "external"
        val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.DISPLAY_NAME,
        )
        const val SELECTION =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val SELECTION_ARGS = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val SILENT_DELETE_PROJECTION = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )

        const val MILLIS_PER_SECOND = 1_000L
        const val NO_DURATION = 0L
        const val LIVE_PHOTO_MAX_DURATION_MS = 3_500L
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val EMPTY_DISPLAY_NAME = ""
        const val UNKNOWN_DATE_ADDED_SECONDS = 0L
        const val UNKNOWN_BUCKET_KEY = -1
    }
}

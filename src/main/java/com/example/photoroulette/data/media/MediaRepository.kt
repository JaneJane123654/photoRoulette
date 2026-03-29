package com.example.photoroulette.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
    val observeUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    suspend fun getShuffledMediaIds(): List<Long> = withContext(ioDispatcher) {
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
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

        contentResolver.query(
            observeUri,
            PROJECTION,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateAddedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val dateAddedSeconds = if (cursor.isNull(dateAddedColumnIndex)) {
                    UNKNOWN_DATE_ADDED_SECONDS
                } else {
                    cursor.getLong(dateAddedColumnIndex)
                }

                entries += MediaEntry(
                    id = cursor.getLong(idColumnIndex),
                    dateAddedSeconds = dateAddedSeconds,
                )
            }
        }

        return entries
    }

    private fun buildSmartShuffle(entries: List<MediaEntry>): List<Long> {
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
                    ids = bucketEntries.map { it.id }.shuffled(random).toMutableList(),
                )
            }
            .shuffled(random)
            .toMutableList()

        val distributedIds = ArrayList<Long>(entries.size)
        var previousBucketKey: Int? = null

        while (activeBuckets.isNotEmpty()) {
            val roundBuckets = activeBuckets.shuffled(random).toMutableList()
            moveDifferentBucketToFront(roundBuckets, previousBucketKey)

            activeBuckets.clear()

            for (bucket in roundBuckets) {
                if (bucket.ids.isEmpty()) {
                    continue
                }

                distributedIds += bucket.ids.removeAt(bucket.ids.lastIndex)
                previousBucketKey = bucket.key

                if (bucket.ids.isNotEmpty()) {
                    activeBuckets += bucket
                }
            }
        }

        return distributedIds
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

    private data class MediaEntry(
        val id: Long,
        val dateAddedSeconds: Long,
    )

    data class SilentDeleteEntry(
        val displayName: String,
        val relativePath: String?,
    )

    private data class Bucket(
        val key: Int,
        val ids: MutableList<Long>,
    )

    private companion object {
        val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val SILENT_DELETE_PROJECTION = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )

        const val MILLIS_PER_SECOND = 1_000L
        const val UNKNOWN_DATE_ADDED_SECONDS = 0L
        const val UNKNOWN_BUCKET_KEY = -1
    }
}

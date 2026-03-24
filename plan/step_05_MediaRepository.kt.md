# Step 05: Media Repository (MediaRepository.kt)

## Objective
To serve as the sole source of truth for querying local media out of the `MediaStore` API. It must be brutally efficient, querying only the necessary metadata (the ID and timestamp) without ever loading Bitmaps into memory, and it must implement a "Smart Shuffle" algorithm.

## Detailed Requirements and Specifications

1.  **Strict Projection Constraints:**
    When querying `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`, the projection array must be restricted strictly to `MediaStore.Images.Media._ID` and `MediaStore.Images.Media.DATE_ADDED`. Absolutely do not query `DATA`, `SIZE`, or load actual file streams. This ensures the query takes milliseconds even for 50,000 photos.

2.  **Dispatcher and Threading:**
    All MediaStore queries are disk I/O operations and database lookups. The primary function (e.g., `suspend fun getShuffledMediaIds(context: Context): List<Long>`) must be wrapped within a `withContext(Dispatchers.IO)` block to guarantee it never blocks the main UI thread.

3.  **Implementation of Smart Shuffle Algorithm:**
    Do not just call `.shuffled()` on the final list, which leads to the "pseudo-random disaster" (e.g., seeing 5 similar photos from the same day sequentially).
    Instead, implement a distribution bucket algorithm:
    - Group the retrieved IDs by time intervals (e.g., group by month and year based on `DATE_ADDED`).
    - Store the buckets as a list of lists.
    - Iteratively pick one random photo from a bucket, then move to a completely different bucket for the next pick.
    - Continue round-robin or randomized bucket selection until all IDs are exhausted.
    - Return the fully computed and distributed list of `Long` IDs.

4.  **Content Observer Strategy (Optional but Highly Recommended):**
    Ideally, setup a basic mechanism or documentation within the file about receiving external changes (if a photo is deleted via standard gallery). For now, as long as the ID generation is robust, Coil will handle "not found" fallbacks in the UI layer.

## Acceptance Criteria
- Contains zero Bitmap or image decoding logic.
- Only queries `_ID` and `DATE_ADDED`.
- The returned list is shuffled using a time-variant distribution logic rather than pure raw randomness.
- All operations execute cleanly on `Dispatchers.IO` and return an immutable `List<Long>`.
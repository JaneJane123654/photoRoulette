# Behavior Parity Checklist

This checklist is derived from the current native implementation and must be used as the lossless migration baseline.

## 1. Permissions

- Support `DENIED`, `GRANTED_PARTIAL`, and `GRANTED_ALL`.
- Android 14+ partial access must be distinct from full access.
- Android 13 uses image/video read permissions.
- Android 12 and below use external storage read permission.
- If permission is denied and rationale should not show and no request was attempted yet, launch the initial request automatically.
- When permission becomes granted, clear the dismissed rationale state.
- Permission-denied UI must remain reachable after returning from app settings.

## 2. Media Query and Shuffle

- Query only image and video media.
- Exclude zero-size files.
- Exclude `application/octet-stream`.
- Sort source query by `DATE_ADDED DESC, _ID DESC`.
- If no media exists, surface empty state.
- Group media by UTC year-month bucket.
- Shuffle within bucket.
- Shuffle buckets.
- While distributing cards, avoid repeating the same bucket consecutively when an alternative exists.
- If `DATE_ADDED` is null, treat as unknown bucket.

## 3. Media Kind Detection

- `MEDIA_TYPE_VIDEO` can still be a Live Photo style video.
- Detect Live Photo if MIME contains `quicktime` or `motion`.
- Also detect Live Photo if file name hints at motion/live and duration is between `1..3500 ms`.
- Animated image types are `gif`, `webp`, and `apng`.
- Video and Live Photo need playback URI.
- Image and animated image do not need playback URI.

## 4. Deck State

- `Ready` state must contain at least one visible card.
- `Ready` state can expose at most three visible cards.
- `previousCard` is derived from `currentIndex - 1`.
- `canSwipeToPrevious` requires `currentIndex > 0`.
- `canSwipeToNext` requires `currentIndex + 1 < queueIds.size`.
- If cached visible cards exist after restoration, emit ready state without forcing a reload.
- Queue ids must persist across process recreation.

## 5. Swipe and Delete Behavior

- Only the top visible card can react to swipe actions.
- If swipe delete is disabled, delete action falls back to skip/next behavior.
- `Skip` itself returns false from `performSwipeAction`, but delete-disabled flow still advances through `swipeSkip`.
- Previous decrements index and re-emits state.
- Next increments index and re-emits state.
- Delete removes the top card optimistically before system delete result is known.
- Pending delete entry must record original index.
- Failed delete restores card at original index.
- Confirmed delete removes pending state, cache entry, and queued system requests.
- System delete requests must be serialized, not launched in parallel.
- If active delete is cancelled, restore card and dispatch next queued request.
- If a pending delete is already gone from reloaded media set, reconcile it as confirmed.

## 6. Silent Delete

- Silent delete runs only when enabled.
- Silent delete requires both display name and authorized tree coverage.
- If authorized tree URI list is empty, silent delete is unavailable.
- Blank or malformed URI must be ignored safely.
- Tree coverage uses normalized directory paths, not raw strings.
- Blank tree path means root coverage.
- Exact scope replacement removes older URI covering the same exact scope before adding new one.
- Missing silent delete scope is determined in enum order.
- Current scopes are `DCIM` and `Pictures`.
- Entry-inside-tree logic must allow exact folder match and nested child folders.
- If silent delete is unavailable, fall back to normal delete flow.
- If enabling silent delete and a required scope is missing, immediately request directory authorization.
- If authorization is cancelled, pending scope must clear.

## 7. Settings Rules

- Swipe sensitivity must clamp to `0.8..1.35`.
- Gesture ball size must clamp to `0.78..1.38`.
- Card action button default must be initialized once if missing.
- `SYSTEM_LANGUAGE_TAG` must remain supported.
- Only `ar`, `en`, `es`, `fr`, `ru`, `zh` are valid explicit language tags.
- Unsupported tags must fall back to `system`.
- Swipe action defaults are:
  - left = delete
  - right = next
  - up = next
  - down = previous

## 8. Default Behavior Notice Policy

- Monthly max shown count is `5`.
- When month changes, shown count resets to `0`.
- When month changes and mode was `AutoHidden`, mode resets to `Visible`.
- When mode is `Visible` and shown count reaches limit, mode switches to `AutoHidden`.
- If user manually disables notice, mode becomes `UserHidden`.
- If user re-enables notice, shown count resets to `0`.

## 9. Update Check and Install

- Auto-check on launch runs only once per session.
- Manual check always forces feedback state.
- GitHub `404` means no release, not an error.
- Other non-2xx update responses are failures.
- Version compare normalizes `v` prefix and non-numeric suffixes.
- If remote version is not newer, treat as up to date.
- If deferred version is still same/newer than offered version, suppress prompt.
- If a newer version appears than deferred version, clear defer and show prompt.
- APK download must clear old `update-*` apk/download files first.
- Temporary `.download` files must be deleted on failure.
- If APK install launch fails, clear pending package and show failed feedback.
- When install flow finishes, clear in-progress state and pending path.

## 10. Null and Exception Guards That Must Survive Translation

- `cursor.getColumnIndex(...) < 0` branches
- `cursor.isNull(...)` branches
- `Uri.parse(...)` wrapped in safe parse
- `DocumentsContract.getTreeDocumentId(...)` failure path
- `contentResolver.delete(...)` security exception path
- Android Q `RecoverableSecurityException` path
- Android R+ `MediaStore.createDeleteRequest(...)` activity/security exception path
- `result.data?.data == null` directory selection cancellation
- zero granted flags on directory authorization result
- blank persisted URI strings
- missing APK asset in release payload
- failed file rename after download
- failed cleanup should not crash session

## 11. Migration Order Sign-Off

- Foundation complete
- Read-only gallery complete
- Swipe navigation complete
- Delete parity complete
- Settings parity complete
- Silent delete parity complete
- Update parity complete
- Golden and contract tests complete

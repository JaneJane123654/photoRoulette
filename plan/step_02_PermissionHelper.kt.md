# Step 02: Permission Helper Utilities (PermissionHelper.kt)

## Objective

To encapsulate the complex, granular photo permission requests introduced in recent Android versions. This isolates all permission API anomalies (Android 13's `READ_MEDIA_IMAGES` vs. Android 14's `READ_MEDIA_VISUAL_USER_SELECTED`) away from the UI components.

## Detailed Requirements and Specifications

1. **Granular Permission Checks:**
    The application needs to adapt dynamically to the device's OS version.
    - For Android 14 (API 34) and above: You must check and request both `Manifest.permission.READ_MEDIA_IMAGES` and `Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED`. The latter represents the state where a user grants access to only a specific subset of their photo library instead of the entire gallery.
    - For Android 13 (API 33): You must request `Manifest.permission.READ_MEDIA_IMAGES`.
    - For Android 12 and below (API 32 and below): You must fallback to requesting `Manifest.permission.READ_EXTERNAL_STORAGE`.

2. **Partial Access State Evaluation:**
    You must expose a function or state evaluator (e.g., `checkCurrentPermissionMode(context: Context)`) that categorizes the current permission state into one of three strict domains:
    - `GRANTED_ALL`: The user has given full access.
    - `GRANTED_PARTIAL`: The user is on Android 14+ and only granted `READ_MEDIA_VISUAL_USER_SELECTED`. The app can run, but should display a banner noting limited photo availability.
    - `DENIED`: The app cannot function.

3. **Modern ActivityResultContracts Usage:**
    Do not use deprecated Accompanist permission wrappers if possible. Use standard `ActivityResultContracts.RequestMultiplePermissions` within the Compose layer later on, but the helper functions inside `PermissionHelper.kt` should provide the exact array of strings needed for the request based on `Build.VERSION.SDK_INT`.

4. **No Silent Requests:**
    Ensure that the helper provides a boolean check `shouldShowRequestPermissionRationale`. Standard Android guidelines dictate we should show a rationale screen before bothering the user with system dialogs. If this returns true, the UI is blocked from requesting until the rationale is dismissed.

## Acceptance Criteria

- Helper object/class cleanly abstracts SDK version checks (`Build.VERSION.SDK_INT`).
- Explicitly handles Android 14 partial photo access modes, recognizing it as a valid, operable state rather than just outright DENIED.
- Provides utility functions to return arrays of required permission strings so the Compose UI components don't contain hardcoded Manifest logic.

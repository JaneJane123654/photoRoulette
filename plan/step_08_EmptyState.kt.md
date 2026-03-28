# Step 08: Fallback and Empty State Screens (EmptyState.kt)

## Objective

To provide visually appealing and user-friendly fallback screens when the app cannot perform its primary function. This occurs in two main scenarios: either the user's camera roll is genuinely empty, or they have decisively revoked permission to read the media.

## Detailed Requirements and Specifications

1. **Empty Photo Library UI (`EmptyGalleryScreen`):**
    Design a Compose function outlining what the user sees when all photos have been swiped or the gallery is inherently empty.
    - Include a descriptive vector graphic or Lottie animation in the center.
    - Add empathetic typography. E.g., "You're all caught up! Take some new photos and come back!"
    - Utilize Material Design 3 `Surface` or `Scaffold` background colors so the screen seamlessly matches the overall theme instead of jumping to a jarring white or black void.

2. **Permission Denied UI (`PermissionDeniedState`):**
    If the user has fully denied permission and standard rationale checks fail (meaning they have clicked "Don't ask again" or forcefully disabled it in settings), this UI takes over.
    - It must clearly explain *why* the application requires photo access (since that's the sole core mechanic of the app).
    - It must include an actionable `Button` labeled "Open Settings".
    - The button's `onClick` must execute a lambda that triggers an Intent to open the Android Application Details Settings page specific to this app package (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, data = `package:com.your.package`).

3. **Compose Aesthetics and Layouts:**
    Use modern `Column` centering (`Arrangement.Center`, `Alignment.CenterHorizontally`). Apply proper `Modifier.padding` (e.g., `32.dp` from edges) so the text doesn't touch the screen borders. Ensure typography uses `MaterialTheme.typography.headlineMedium` for titles and `bodyMedium` for the rationale text, styled with `MaterialTheme.colorScheme.onSurface`.

## Acceptance Criteria

- `EmptyGalleryScreen` displays successfully without relying on external assets (use standard Material Icons if illustrations aren't available).
- `PermissionDeniedState` correctly passes a callback or Intent to navigate the user securely into OS-level settings.
- The UI strictly adheres to Jetpack Compose Material 3 theming guidelines.

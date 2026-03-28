# Step 10: Elegant Permission Rationale Flow (PermissionRationaleScreen.kt)

## Objective

To follow Android's strictly enforced guidelines on requesting sensitive permissions. In Android 14, accessing a user's photo library causes high user anxiety. A "Rationale Screen" acts as a buffer or landing page explaining exactly *why* the product needs access before the harsh system dialog triggers.

## Detailed Requirements and Specifications

1. **Full Screen Presentation:**
    The Rationale must be a standalone Compose screen (`@Composable fun PermissionRationaleScreen(onRequestClicked: () -> Unit)`). It typically features a large header, an engaging illustration (or aesthetic layout of staggered photo icons), and clear, reassuring copywriting.

2. **Android 14 Partial Access Copywriting:**
    The copy must address Android 14's unique capabilities. It must explain: "You can choose to allow access to all your photos for the best random experience, or just select a few specific albums if you prefer." We must not demand full access uncompromisingly—partial access (`READ_MEDIA_VISUAL_USER_SELECTED`) is perfectly valid and the app will shuffle and show whatever it's given.

3. **Callback Driven Execution:**
    The screen should not house the `ActivityResultContracts` launchers itself. It must be stateless in that regard. When the user taps the primary "Continue" or "Grant Permission" button, it invokes `onRequestClicked()`. The parent standard layer (like `MainActivity` or the wrapping `MainScreen`) handles the actual asynchronous OS pop-up.

4. **Visual Polish, Safe Areas, and A11Y:**
    - Use `WindowInsets.safeDrawing` to ensure the layout avoids device notches or software navigation bars.
    - Implement Semantic content descriptions for any icons to support TalkBack accessibility readers.
    - Add a secondary subtle button (e.g., a `TextButton` saying "Maybe Later") which explicitly declines the flow and moves them into the `EmptyState`.

## Acceptance Criteria

- Entirely stateless Compose UI.
- Contains robust, friendly copywriting specifically demystifying the Android 14 granular photo permission.
- Accommodates Android WindowInsets properly so buttons aren't hidden by system navigation bars.
- Triggers parent callbacks correctly upon user interaction.

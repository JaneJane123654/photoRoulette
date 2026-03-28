# Step 12: Activity and Entry Point Setup (MainActivity.kt)

## Objective

To serve as the root component where the Android System interfaces with our purely Jetpack Compose-based application. `MainActivity` is responsible for registering system environment layout flags, injecting the ViewModel (using factory methods or basic instantiation since we aren't enforcing Hilt/Dagger yet), and handling the actual system Activity Result Contracts.

## Detailed Requirements and Specifications

1. **Immersive Edge-to-Edge Display:**
    In the `onCreate` lifecycle hook, invoke `enableEdgeToEdge()` before calling `setContent`. This is critical for modern design, allowing the photos and the dark aesthetic to bleed under the transparent status bar and the bottom gesture bar. It maximizes the "Random Roulette" immersive screen real estate.

2. **AppTheme Wrapping:**
    The root Composable inside `setContent` must be completely wrapped in your application's defined `PhotoRouletteTheme`. This sets the baseline typography, global Surface colors (likely a dark theme by default, given the media-heavy nature of the app), and system bar contrasting.

3. **ViewModel Initialization with SavedStateHandle:**
    If you are not using a dependency injection framework like Hilt in this boilerplate, you must initialize `MainViewModel` smartly so it receives the `SavedStateHandle`. A generic `viewModels<MainViewModel>()` delegation tied to the activity lifecycle will automatically supply the state registry needed to prevent process-death variable leakage.

4. **ActivityResultLaunchers Registration:**
    - Register a standard `registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`. Upon receiving the result, dispatch the boolean grants down to the `MainViewModel`.
    - Register a launcher for the delete operation `registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult())`. If the user hits "Cancel" on the system popup (returning `Activity.RESULT_CANCELED`), immediately trigger the ViewModel's `rewindDismissedDelete()` function so the dropped image physically springs back onto the screen.

## Acceptance Criteria

- Inherits from `ComponentActivity`.
- Uses Jetpack `enableEdgeToEdge()` for full-screen drawing.
- Captures and delegates the result of the `IntentSender` (the deletion API dialog) directly to the ViewModel to manage successful deletions versus user cancellations.
- Stitches the root `MainScreen` into the navigation/rendering graph cleanly.

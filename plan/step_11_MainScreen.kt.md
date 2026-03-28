# Step 11: Main Interactive Interface (MainScreen.kt)

## Objective

To orchestrate the complex Z-index rendering of the visual photo cards while safely consuming the `MainViewModel`'s state flows. This file stitches together the asynchronous media loading (Coil), the physical swipe models, and the "Undo" visual logic.

## Detailed Requirements and Specifications

1. **Lifecycle-Aware Flow Collection:**
    You must collect the view state strictly utilizing `val state by viewModel.uiState.collectAsStateWithLifecycle()`. This ensures that when the user leaves the app or triggers the system overlay dialog, the Flow detaches, pausing heavy bitmap computation and rendering.

2. **Memory-Safe Z-Index Stack Structure:**
    Build the main stage using a `Box`. Do not use a `LazyColumn` or `LazyRow`.
    Based on the `Ready` state (which contains at most 3 IDs), map over this list and sequentially render `SwipeableCard` instances.
    - Reverse the rendering order or explicitly use `Modifier.zIndex()` to ensure index 0 (the top card) is visually on top of the stack and intercepts touches first.
    - For the cards beneath the top card, they should have pointer input disabled (`pointerInput` ignoring touches) until they become the top layer.

3. **Coil AsyncImage Setup:**
    Inside each `SwipeableCard`, embed an `AsyncImage` utilizing Coil.
    - You must bind `Modifier.fillMaxSize()`.
    - Apply `contentScale = ContentScale.Crop`.
    - Provide a robust `error` placeholder and a subtle `placeholder` (e.g., a shimmer or gray box) for when the disk lookup is happening.
    - This implicitly handles automatic down-sampling required to prevent OOM errors.

4. **Handling Action Lambdas:**
    When the top `SwipeableCard` triggers `onSwipedUp()`, the `MainScreen` must forward the ID to the `MainViewModel`, triggering the intent workflow that will eventually suspend the UI and pop up the Android "Confirm Delete" box.

## Acceptance Criteria

- Employs `collectAsStateWithLifecycle` exclusively for state reads.
- Maintains a rigid `Box` tree containing a maximum of 3 visual DOM nodes for the photo layer regardless of the total library size.
- Correctly integrates Coil `AsyncImage` with `ContentScale.Crop`.
- Connects the gestures correctly to the `ViewModel` intent channels.

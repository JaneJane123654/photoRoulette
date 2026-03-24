# Step 09: Physical Spring Swipe Card Component (SwipeableCard.kt)

## Objective
To engineer the absolute core interaction model of the Photo Roulette app: a Tinder-like physical swipe card. It must feel extremely fluid, avoiding frame drops on 120Hz displays, utilizing physics-based springs instead of linear animations, and strictly bypassing Compose UI tree recomposition through the direct GPU matrix.

## Detailed Requirements and Specifications

1.  **Crucial Rendering Opt-Out via `graphicsLayer`:**
    You must NOT use `Modifier.offset(x, y)` because updating state that triggers the layout phase causes thousands of recompositions during a swipe gesture, completely destroying framerates.
    Instead, update the layout via `Modifier.graphicsLayer { translationX = offsetX.value; translationY = offsetY.value }`. This interacts directly with the render node and is entirely free of composition costs.

2.  **PointerInput and Drag Thresholds:**
    Attach `Modifier.pointerInput(Unit) { detectDragGestures { ... } }`.
    - Track continuous `change.positionChange()`.
    - When `onDragEnd` occurs, measure the final `offsetX` and `offsetY` against screen width/height thresholds.
    - If `offsetX` > `screenWidth * 0.3f`, trigger a "Swipe Right" (Skip).
    - If `offsetX` < `-screenWidth * 0.3f`, trigger a "Swipe Left" (Skip).
    - If `offsetY` < `-screenHeight * 0.3f` (with higher threshold to avoid accidental up-swipes), trigger "Swipe Up" (Delete).

3.  **Physical Spring Animations:**
    If the drag ends but does not clear the threshold thresholds, the card must snap back to center. Use Coroutines tied to the component to launch `offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))`. Do *not* use `tween(durationMillis)`.

4.  **Multi-Dimensional Synergy:**
    Bind properties mathematically to `offsetX` inside the `graphicsLayer`:
    - Rotation: `rotationZ = offsetX.value / 20f`.
    - Background Scale: Create a system where the *next* card in the stack gradually scales from `0.9f` to `1.0f` exactly proportional to the distance the top card is dragged away from the center.

## Acceptance Criteria
- Entire motion system leverages Jetpack Compose `Animatable`.
- `Modifier.graphicsLayer` is exclusively used for visual transformation (translation, scale, rotation).
- Accurately triggers external lambdas `onSwipedLeft`, `onSwipedRight`, and `onSwipedUp`.
- Snaps back gracefully featuring a bouncy spring spec if released early.
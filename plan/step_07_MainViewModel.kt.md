# Step 07: Core View Model (MainViewModel.kt)

## Objective
To act as the brain of the application. The `MainViewModel` orchestrates permission status updates, calls the `MediaRepository` to generate the card queue, slices the visible array for the UI, triggers image preloading, handles user swipe intents, and implements a robust "recover/rewind" mechanism against Android process death.

## Detailed Requirements and Specifications

1.  **Process Death Immunity via SavedStateHandle:**
    The ViewModel's constructor must take `SavedStateHandle`. When a user swipes up, an Intent calls the system delete dialog, which puts our app in the background. Android OS can kill background apps aggressively. You must save the current queue index (`currentIndex`) and the ID of the photo being deleted (`pendingDeleteId`) into the `SavedStateHandle` instantly upon swipe. Upon recreation, read from these keys to seamlessly restore the user's flow.

2.  **Dual Preloading Strategy with Coil:**
    While exposing only 3 IDs to the UI via the `HomeUiState.Ready` StateFlow, the ViewModel must proactively tell Coil to fetch the next 5-10 images. 
    Use Coil's `ImageLoader(context).enqueue(ImageRequest.Builder(context).data(uri).build())` to perform this behind the scenes. This ensures that rapid swiping never greets the user with a white, unloaded box.

3.  **Rewind / Recovery Operations:**
    Implement a `fun onSystemDeleteCancelled()`. If the user hits "Cancel" on the system's deletion dialog, the activity retrieves a `RESULT_CANCELED`. It invokes this ViewModel function. The logic must look at `pendingDeleteId`, insert it rigidly back at the very front of the active `List<Long>`, decrement the `currentIndex`, and emit the new state. This guarantees the UI gets the ID back and triggers a spring "fly-back" animation in Compose.

4.  **Unidirectional StateFlow Management:**
    State must be held in a `MutableStateFlow<HomeUiState>` and exposed immutably as `StateFlow<HomeUiState>` (or via `asStateFlow()`). 

## Acceptance Criteria
- Constructor receives `SavedStateHandle`, `SettingsRepository`, and `MediaRepository`.
- Variables holding the master shuffled list, `currentIndex`, and `pendingDeleteId` are actively synced with `SavedStateHandle`.
- Actively triggers Coil `ImageRequest` preloading.
- Implements explicit `swipeSkip` and `swipeDelete` semantic functions alongside `rewindDismissedDelete`.
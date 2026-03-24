# Step 04: Settings Repository (SettingsRepository.kt)

## Objective
To handle the persistence of user configurations using Jetpack Preferences DataStore. This primarily serves to toggle the "Swipe Up to Delete" functionality, ensuring users don't accidentally bring up system deletion prompts if they just want a safe browsing experience.

## Detailed Requirements and Specifications

1.  **DataStore Initialization using Context Delegation:**
    Create a global extension property on the `Context` class at the top of the file to instantiate the DataStore singleton. E.g., `val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")`. This guarantees that there's only one active instance of DataStore attempting to read/write to the disk, avoiding locking issues.

2.  **Key Definitions:**
    Define the specific keys inside the repository class using `booleanPreferencesKey("enable_swipe_delete")`. Ensure keys are strictly categorized and tightly scoped. You cannot use hardcoded strings nakedly throughout the application; they must be referenced via these strict definitions.

3.  **Exposing State via Flow:**
    Define a public variable, e.g., `val isSwipeDeleteEnabled: Flow<Boolean>`. This Flow maps over the underlying DataStore flow, extracts the boolean value, and provides a safe default (e.g., `false` or `true` depending on your product choice—for safety, defaulting to `false` is generally recommended, requiring the user to explicitly enable the dangerous action). Furthermore, make sure any exceptions (like `IOException` during file read) are caught using `catch { }` operator and emit the empty preferences object to prevent Flow termination.

4.  **Suspend Update Functions:**
    Provide a `suspend fun setSwipeDeleteEnabled(enabled: Boolean)` that utilizes the `dataStore.edit { preferences -> ... }` mechanism to commit changes asynchronously and safely. This function will be triggered by a switch in the app's settings UI or dialog.

## Acceptance Criteria
- Implements `androidx.datastore.preferences.core.Preferences`.
- Uses Kotlin Coroutines `Flow` to reactively stream setting changes to the ViewModel.
- Implements safe, IO-exception-handled read operations using the `catch` block on the stream.
- Exposes a purely async `suspend` function for mutating the required boolean setting.
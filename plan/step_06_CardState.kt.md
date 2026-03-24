# Step 06: UI State Definitions (CardState.kt)

## Objective
To strictly define the MVI/MVVM frontend states. Proper state management prevents implicit states where the UI doesn't know whether it's loading, denied permission, or empty, thus avoiding impossible state representation or UI completely crashing.

## Detailed Requirements and Specifications

1.  **Sealed Interface Construction:**
    Create a `sealed interface HomeUiState`. This forms the root of a strict type hierarchy that the Jetpack Compose `when` statement can exhaustively check. If a new state is ever added, the compiler will force the developer to handle it in the UI.

2.  **Explicit State Variants:**
    You must define the following concrete data objects or data classes within (or inheriting from) the sealed interface:
    - `data object Loading : HomeUiState`: The initial state when the ViewModel is querying the MediaStore and generating the smart shuffled list.
    - `data object PermissionDenied : HomeUiState`: Emitted when the user completely rejects the necessary permissions (triggering the `Rationale` screen in the Compose UI).
    - `data object Empty : HomeUiState`: A valid state representing that the user's gallery has zero photos, or they've swiped through all of them.
    - `data class Ready(val visibleIds: List<Long>) : HomeUiState`: The core functional state. It carries a highly truncated list (e.g., exactly the 3 IDs needed for the top, middle, and bottom cards). 

3.  **Encapsulation of State Data:**
    Ensure that the `Ready` state only holds the IDs actually required by the View. It should *never* hold the full list of 10,000 IDs, as pushing a massive list into a Compose State could cause unnecessary overhead or diffing constraints if not carefully managed. The ViewModel holds the full list; `CardState.Ready` merely holds the window of visible data.

4.  **Immutability:**
    All lists within state data classes must be immutable Kotlin collections (`List<T>`, not `MutableList<T>`). We enforce one-way data flow.

## Acceptance Criteria
- Strictly utilizes `sealed interface` or `sealed class`.
- Contains `Loading`, `PermissionDenied`, `Empty`, and `Ready` variations.
- The `Ready` state is clearly structured to carry only essential pagination or index data (a small list of `Long`).
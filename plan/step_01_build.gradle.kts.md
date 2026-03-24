# Step 01: Build Configuration and Dependencies (build.gradle.kts)

## Objective
To setup the foundational dependencies, build features, and correct platform targets required to support a modern Jetpack Compose application with Android 13/14 specific features, physical animations, data storage, and high-performance image loading.

## Detailed Requirements and Specifications

1.  **Compose Bill of Materials (BOM):**
    You must implement the latest stable `androidx.compose:compose-bom`. This ensures all Compose libraries (Foundation, UI, Material3, Runtime) share compatible versions. Do not hardcode versions for individual Compose libraries unless explicitly overriding the BOM.
    
2.  **Material 3 Design System:**
    Add `androidx.compose.material3:material3`. The app will utilize Material 3 styling for components, typography, and color schemes. Ensure you also include `androidx.compose.material:material-icons-extended` if any specialized icons are required for the swipe actions or empty states.

3.  **Coil for Compose (High-Performance Image Loading):**
    Include `io.coil-kt:coil-compose:2.+`. Coil is critical for our OOM-prevention strategy. It automatically downsamples images based on the exact size of the Composable, mitigating memory spikes when loading massive images taken by high-resolution phone cameras.

4.  **Lifecycle and ViewModel Architecture:**
    Include `androidx.lifecycle:lifecycle-viewmodel-compose` and `androidx.lifecycle:lifecycle-runtime-compose`. The latter is absolutely critical because it provides the `collectAsStateWithLifecycle()` extension. This ensures that when the app is backgrounded (e.g., during the system delete dialog prompt), our Compose UI stops collecting flows, preventing hidden crashes and battery drain.

5.  **Jetpack Preferences DataStore:**
    Implement `androidx.datastore:datastore-preferences`. This will be used instead of the legacy SharedPreferences to store the global toggle state for the "Swipe Up to Delete" functionality.

6.  **Android SDK Versions:**
    Ensure `compileSdk` is set to 34 (Android 14) and `targetSdk` is set to 34. This is a hard requirement because the app must handle the `READ_MEDIA_VISUAL_USER_SELECTED` partial photo access permission introduced in Android 14.

7.  **Compiler and Kotlin Options:**
    Ensure the `kotlinCompilerExtensionVersion` matches the current Compose compiler requirements. Set JVM target to at least `1.8` or `17` depending on the surrounding Gradle setup. Enable the `compose = true` build feature flag.

## Acceptance Criteria
- File is completely valid Kotlin Script (`.kts`).
- All required libraries mentioned above are included.
- Target SDK is explicitly set to 34 to support Android 14 photo picking constraints.
- No legacy Support Library or legacy SharedPreferences dependencies are included.
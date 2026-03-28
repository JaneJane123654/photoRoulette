# 🤖 [System Directive: Pipeline Code Engineer]

## 🚫 Supreme Behavioral Command (Strict Adherence Required)

As the AI responsible for executing code step-by-step, your **ONLY objective** is to:

1. Review the [Single Task Tracking Table] below.
2. Find the **FIRST** item marked as unfinished with `[ ]`.
3. Read the detailed specification locally stored at the corresponding `plan/` file path.
4. **Output ONLY the complete, runnable code for that single file.** Do not explain, do not jump ahead, do not generate multiple files. Output the exact file defined in the tracker.
5. After outputting the code, you **MUST check off the step you just completed by changing `[ ]` to `[x]`**, and then use a markdown code block to reprint this ENTIRE updated 《AI Memory Document》 so the human user can copy it for the next round.

---

## 📦 Global Business Overview

**Project Name**: Photo Roulette
**Business Goal**: A blind-box style local photo browsing application utilizing absolute random presentation. It supports Tinder-like swiping gestures (Swipe Left/Right to skip, Swipe Up to trigger system-level deletion). Core technical challenges involve preventing Out-Of-Memory (OOM) errors during rapid swiping, handling Process Death caused by system permission dialogs, and delivering a buttery-smooth 120Hz physical spring animation.

## ⚙️ Global Development Conventions & Core Technical Rules

- **Programming Language**: Pure Kotlin.
- **UI Framework**: Jetpack Compose. For high-frequency rendering (like gesture translation/rotation/scaling), **you must strictly use `Modifier.graphicsLayer { ... }`**. Never use `Modifier.offset` which triggers heavy recompositions.
- **Image Loading**: Coil Compose. Always use `Modifier.fillMaxSize()` to trigger automatic down-sampling. The ViewModel must asynchronously execute `ImageLoader.enqueue` on the `uiState` IDs to prevent white screens during rapid swiping.
- **Process & Lifecycle**: Essential ViewModel states (`pendingDeleteId`, `currentIndex`) **MUST be saved to `SavedStateHandle`**. Flow collections inside Compose must strictly use `collectAsStateWithLifecycle()`.
- **Media Fetching Algorithm**: Use `ContentResolver` to query `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`. Fetch only `_ID` and `DATE_ADDED`. Do NOT load Bitmaps in the Repository. Apply a "Smart Shuffle" algorithm (bucketing by time/date) to avoid the illusion of pseudo-randomness.

---

## 📋 Single Task Tracking Table

- [x] Step 01: Build & Dependencies (Target: `build.gradle.kts` | Spec: `plan/step_01_build.gradle.kts.md`). Incorporate Compose BOM, Material 3, Coil 2.x, Lifecycle Compose, and DataStore preferences.
- [x] Step 02: Permission Utility (Target: `utils/PermissionHelper.kt` | Spec: `plan/step_02_PermissionHelper.kt.md`). Abstract Android 13/14 specific visual user-selected photo permissions.
- [x] Step 03: System Interaction Utility (Target: `utils/IntentHelper.kt` | Spec: `plan/step_03_IntentHelper.kt.md`). Bridge `MediaStore.createDeleteRequest` and catch Android 10 `RecoverableSecurityException`.
- [x] Step 04: Config Repository (Target: `data/datastore/SettingsRepository.kt` | Spec: `plan/step_04_SettingsRepository.kt.md`). Utilize Preferences DataStore for a "Swipe Up to Delete" toggle switch.
- [x] Step 05: Media Data Source (Target: `data/media/MediaRepository.kt` | Spec: `plan/step_05_MediaRepository.kt.md`). Extract Gallery IDs, perform the distributed Smart Shuffle algorithm.
- [x] Step 06: Architecture States (Target: `viewmodel/states/CardState.kt` | Spec: `plan/step_06_CardState.kt.md`). Define MVI sealed interfaces (`Loading`, `Ready`, `Empty`, `PermissionDenied`).
- [x] Step 07: Core Control Layer (Target: `viewmodel/MainViewModel.kt` | Spec: `plan/step_07_MainViewModel.kt.md`). Implement `SavedStateHandle` rescue mechanisms, Coil preloading, and "Rewind card" state maneuvers.
- [x] Step 08: Fallback UI Components (Target: `ui/components/EmptyState.kt` | Spec: `plan/step_08_EmptyState.kt.md`). Build friendly, accessible Compose screens for empty galleries or blocked permissions.
- [x] Step 09: Physical Interaction UI (Target: `ui/components/SwipeableCard.kt` | Spec: `plan/step_09_SwipeableCard.kt.md`). Manually build `pointerInput` gestures with Compose `Animatable` and physics-based spring regression.
- [x] Step 10: Rationale Block Screen (Target: `ui/screens/PermissionRationaleScreen.kt` | Spec: `plan/step_10_PermissionRationaleScreen.kt.md`). Explain Android 14 photo logic elegantly before requesting OS-level prompts.
- [x] Step 11: Main Stage UI (Target: `ui/screens/MainScreen.kt` | Spec: `plan/step_11_MainScreen.kt.md`). Maintain a memory-safe `Box` utilizing z-index that mounts a maximum of 3 cards simultaneously.
- [x] Step 12: Entry Mechanism (Target: `app/MainActivity.kt` | Spec: `plan/step_12_MainActivity.kt.md`). Inject global theme, Edge-to-Edge logic, and `ActivityResultContracts`.
- [x] Step 13: Project Bootstrap & Build Verification (Targets: `settings.gradle.kts`, `gradle.properties`, `src/main/AndroidManifest.xml`, `gradle/wrapper/*`, and integration fixes in `build.gradle.kts` / Compose sources | Spec: `plan/step_13_project_bootstrap_and_build.md`). Add missing Gradle/plugin management, minimal Android manifest/resources, align the toolchain, and fix integration compile errors until `gradlew assembleDebug` succeeds.

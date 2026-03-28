# Step 13: Project Bootstrap & Build Verification

## Objective

Bridge the gap between the completed feature files and a runnable Android project. The repository must be importable by Gradle/Android Studio, declare the correct plugin repositories and wrapper version, include a minimal manifest/resources set, and successfully assemble a debug APK.

## Detailed Requirements and Specifications

1. **Gradle Project Recognition**
   Add `settings.gradle.kts` with `pluginManagement` and dependency repositories so the Android Gradle Plugin resolves from Google and the project can be opened as a valid Gradle build.

2. **Stable Toolchain Declaration**
   Generate and commit the Gradle wrapper required by the chosen AGP version. Keep the build script compatible with the Kotlin 2.3 DSL (`compilerOptions`) and the locally available Android SDK level.

3. **Android App Scaffolding**
   Add a minimal `src/main/AndroidManifest.xml` that declares:
   - photo read permissions for legacy and modern Android versions
   - the launcher `MainActivity`
   - a valid application theme and label

   Add the smallest required resources such as `strings.xml` and `themes.xml`.

4. **Integration-Level Source Fixes**
   Resolve any compile errors that appear only after the project is assembled as a whole, including Compose API drift and Kotlin inference issues.

## Acceptance Criteria

- `settings.gradle.kts` exists and plugin resolution works.
- `gradlew` / `gradlew.bat` and the wrapper JAR/properties are present.
- `AndroidManifest.xml` and minimal resources exist under `src/main/`.
- `build.gradle.kts` uses current Kotlin compiler DSL conventions.
- Running `gradlew assembleDebug` completes successfully on the configured machine.

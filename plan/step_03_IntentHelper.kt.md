# Step 03: System Delete Intent Helper (IntentHelper.kt)

## Objective
To provide a clean, abstracted way to trigger the Android system's file deletion mechanism. Due to Scoped Storage introduced in Android 10 and formalized heavily in 11+, applications cannot directly delete media belonging to other apps. They must request user consent via a system dialog.

## Detailed Requirements and Specifications

1.  **MediaStore CreateDeleteRequest:**
    For devices running Android 11 (API 30) and above, use `MediaStore.createDeleteRequest(contentResolver, listOf(uri))`. This API generates a `PendingIntent` that brings up the system's "Allow app to delete this photo?" bottom sheet. It requires passing the content resolver and a specific file's URI (built using the MediaStore prefix and the image ID).

2.  **RecoverableSecurityException Fallback (Android 10):**
    If the application runs on Android 10 (API 29), calling `ContentResolver.delete()` on a file you don't own will throw a `RecoverableSecurityException`. The helper class must catch this specific exception. From the caught exception, you extract the `userAction.actionIntent` to prompt the user for permission to delete that specific block of data.

3.  **Encapsulation of IntentSenderRequest:**
    In modern Android development using Jetpack Compose, `startIntentSenderForResult` is handled by `ActivityResultContracts.StartIntentSenderForResult()`. The IntentHelper must provide a method that takes an image `Uri` and returns an `IntentSenderRequest`. This completely offloads the heavy lifting from the UI layer. The UI layer simply takes this request and launches the contract.

4.  **Error Handling and Graceful Degradation:**
    Since this operation involves external processes and backgrounding the main app, ensure all functions handle `ActivityNotFoundException` or generic `SecurityExceptions` without crashing the application. The function should ideally return a wrapped Result or a nullable `IntentSenderRequest` (returning null if the operation is impossible).

## Acceptance Criteria
- Contains `createDeleteRequest` logic using `MediaStore.createDeleteRequest` for Android 11+.
- Contains proper try-catch wrapping for `RecoverableSecurityException` for Android 10 backward compatibility.
- Returns a correctly configured `IntentSenderRequest` that is ready to be consumed by an ActivityResultLauncher in Compose.
- The UI layer won't need to know the underlying Android SDK version being run.
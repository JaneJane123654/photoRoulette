# Flutter Project Blueprint

This folder is the isolated Flutter refactor workspace for `photoRoulette`.

The original Android project under [`/src`](/d:/project/photoRoulette/src) remains untouched. All migration planning, target architecture, and future Flutter implementation work should stay inside `Flutter_Project`.

## Current Status

- Architecture and AI migration rules are ready.
- The local machine does not currently have the Flutter CLI installed, so `flutter create` was not executed yet.
- Use [`scripts/bootstrap_flutter_project.ps1`](/d:/project/photoRoulette/Flutter_Project/scripts/bootstrap_flutter_project.ps1) after Flutter SDK is installed.

## Documents

- Architecture: [`docs/ARCHITECTURE.md`](/d:/project/photoRoulette/Flutter_Project/docs/ARCHITECTURE.md)
- AI migration spec: [`docs/AI_MIGRATION_SPEC.md`](/d:/project/photoRoulette/Flutter_Project/docs/AI_MIGRATION_SPEC.md)
- Behavior parity checklist: [`docs/BEHAVIOR_PARITY_CHECKLIST.md`](/d:/project/photoRoulette/Flutter_Project/docs/BEHAVIOR_PARITY_CHECKLIST.md)

## Migration Principle

1. Build the new Flutter app in physical isolation.
2. Reproduce behavior first, optimize structure second.
3. Keep shared business logic in Dart.
4. Put Android-only abilities behind typed platform bridges.
5. Do not remove or modify the existing native code until Flutter parity is accepted.

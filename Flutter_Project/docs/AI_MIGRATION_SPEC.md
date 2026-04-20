# AI Migration Specification

## 1. Non-Negotiable Rules

1. All new work stays inside `Flutter_Project`.
2. The existing native Android files are read-only reference material.
3. No business rule may be rewritten from memory. Every rule must be traced to source code first.
4. No Flutter widget may contain hidden business logic.
5. All Android-only behavior must sit behind typed platform contracts.
6. Any intentional behavior change must be documented as a product decision, not a silent simplification.

## 2. Target Directory Structure

```text
Flutter_Project/
  docs/
    ARCHITECTURE.md
    AI_MIGRATION_SPEC.md
    BEHAVIOR_PARITY_CHECKLIST.md
  scripts/
    bootstrap_flutter_project.ps1
  lib/
    app/
      bootstrap/
      router/
      theme/
    core/
      error/
      result/
      logging/
      platform/
      utils/
    features/
      permissions/
        data/
        domain/
        presentation/
      media/
        data/
        domain/
        presentation/
      deck/
        presentation/
      delete/
        data/
        domain/
        presentation/
      settings/
        data/
        domain/
        presentation/
      update/
        data/
        domain/
        presentation/
    l10n/
  test/
    unit/
    widget/
    golden/
    contract/
```

## 3. Coding Style for AI Implementers

### 3.1 State rules

- Use one immutable state object per controller.
- Use one controller per feature boundary.
- Use effect streams for one-time UI actions:
  - snackbars
  - dialogs
  - navigation
  - system delete request launch
  - silent delete directory request launch
  - APK install launch

### 3.2 Dependency rules

- Dependencies must be injected through Riverpod providers.
- Repository constructors must receive data sources explicitly.
- Do not call platform plugins directly from widgets.
- Widgets may read providers and dispatch intents only.

### 3.3 Error rules

- No bare `catch (_)`.
- Convert every platform/network/storage failure into typed app errors.
- Preserve special branches:
  - not found
  - denied
  - partial grant
  - cancelled by user
  - malformed URI/path
  - unsupported platform
  - download/install failure

### 3.4 Null-safety rules

Any nullable value seen in native code must be represented in Dart by one of:

- nullable field with explicit guard
- sealed result type
- typed fallback object

Never replace a native null guard with a forced `!`.

## 4. Source Coverage Workflow

For every native source file, AI must produce a Source Coverage Card before implementation.

### Source Coverage Card template

```text
Source file:
Responsibility:
Public entry points:
Mutable state:
Persistence keys:
Permission / SDK branches:
Null guards:
Exception branches:
User-visible side effects:
Platform side effects:
Target Flutter files:
Required tests:
```

### Minimum output per migrated source

- one Source Coverage Card
- one target file mapping
- one parity test list
- one unresolved-risk note if any branch cannot yet be reproduced

## 5. Lossless Migration Protocol

### Stage A. Inventory

AI must inventory:

- every public method
- every persisted key
- every enum and state object
- every `if` branch by SDK version
- every `runCatching`, `try/catch`, `null` guard, and fallback
- every user-facing message path

### Stage B. Rule extraction

For each method, AI must extract:

- input assumptions
- preconditions
- normal output
- failure output
- side effects
- ordering guarantees
- concurrency guarantees

### Stage C. Translation design

Before writing Dart, AI must decide:

- target controller owner
- target repository owner
- platform bridge need or no-bridge need
- state fields
- effect events
- test cases

### Stage D. Implementation

Implementation order per feature:

1. models and result types
2. repository/data source
3. controller/state
4. widgets
5. tests

### Stage E. Parity validation

A migration is not complete until:

- behavior checklist items are checked
- null/error branches have tests
- platform unsupported paths are explicit
- user-visible state matches the old app

## 6. Required Translation Patterns

### 6.1 Activity to Flutter

Pattern:

- lifecycle/bootstrap logic -> app bootstrap/coordinator
- permission launcher -> platform service + effect
- result callback -> controller event handler

### 6.2 ViewModel to Riverpod

Pattern:

- `MutableStateFlow<T>` -> controller `state`
- `SharedFlow<Event>` -> effect stream
- `SavedStateHandle` -> controller session state plus optional Hive restoration if needed
- giant ViewModel -> multiple feature controllers

### 6.3 XML/Compose to widget tree

Pattern:

- extract visual widgets from stateful coordinators
- pure rendering widgets accept plain params
- gesture math stays in presentation helper layer
- no repository access in widgets

### 6.4 Repository to data layer

Pattern:

- Android `ContentResolver` logic -> plugin adapter or Pigeon host bridge
- DataStore logic -> Hive adapter
- `HttpURLConnection` logic -> Dio remote data source

## 7. Rules Specific to This Codebase

### 7.1 Must preserve Android 14 partial permission behavior

The current native code distinguishes:

- full media grant
- partial selected-photos grant
- denied

Flutter code must preserve that three-state behavior and not collapse it into boolean access.

### 7.2 Must preserve optimistic delete semantics

Current behavior:

- top card is dismissed before delete finishes
- failed delete restores the card at original index
- system delete requests are serialized

This ordering is mandatory.

### 7.3 Must preserve silent delete scope logic

Current behavior depends on:

- exact scope match replacement
- authorized tree covering a folder
- normalized path comparison
- blank tree path meaning root coverage

Do not simplify this into naive string contains matching.

### 7.4 Must preserve monthly notice policy

The default behavior notice:

- auto-hides after 5 displays in the same month
- auto-restores next month only if it was auto-hidden
- stays hidden next month if user hid it manually

That distinction is required.

### 7.5 Must preserve update defer policy

The skipped version logic is version-aware:

- if latest version is not newer, clear stale deferred version
- if deferred version is still current or newer than offered, suppress prompt
- if a newer release appears than the deferred version, clear defer and show prompt

## 8. Test Strategy

### Unit tests

- version compare
- shuffle bucketing
- live-photo detection
- settings clamp rules
- path normalization and tree matching
- deferred update logic

### Widget tests

- permission denied state
- empty gallery state
- deck state with previous/current/next visibility
- settings interactions
- update prompt visibility

### Contract tests

- Android host bridge request/response DTOs
- silent delete scope matching
- delete queue serialization

### Golden tests

- permission rationale screen
- main deck shell
- settings panel

## 9. Migration Sequence

### First migration batch

- shared models
- settings storage
- localization
- permission flow
- read-only gallery deck

Reason:

- smallest risk
- highest visible progress
- creates foundation for the rest

### Second migration batch

- swipe actions
- delete protection
- delete reminder
- preload strategy

### Final migration batch

- silent delete
- update install flow
- Android-specific bridges

These are last because they have the strongest platform coupling and the highest regression risk.

## 10. Definition of Done

A feature is done only if:

- behavior parity checklist has no unchecked items for that feature
- tests cover success, null, cancellation, and failure branches
- source-to-target mapping is documented
- no business logic is left inside widgets
- platform-only behavior is isolated

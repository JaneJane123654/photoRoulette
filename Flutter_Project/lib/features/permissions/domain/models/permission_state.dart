import '../../../../core/error/app_error.dart';
import 'permission_access_level.dart';

final class PermissionState {
  const PermissionState({
    required this.accessLevel,
    required this.isRequestInProgress,
    required this.hasRequestedPermissionInSession,
    required this.isRationaleDismissed,
    required this.shouldShowPlatformRationale,
    this.error,
  });

  const PermissionState.initial()
    : accessLevel = PermissionAccessLevel.denied,
      isRequestInProgress = false,
      hasRequestedPermissionInSession = false,
      isRationaleDismissed = false,
      shouldShowPlatformRationale = false,
      error = null;

  final PermissionAccessLevel accessLevel;
  final bool isRequestInProgress;
  final bool hasRequestedPermissionInSession;
  final bool isRationaleDismissed;
  final bool shouldShowPlatformRationale;
  final AppError? error;

  bool get hasMediaAccess => accessLevel.hasMediaAccess;

  bool get shouldShowRationale {
    return accessLevel.isDenied &&
        shouldShowPlatformRationale &&
        !isRationaleDismissed;
  }

  bool get shouldAutoRequestPermission {
    return accessLevel.isDenied &&
        !shouldShowRationale &&
        !hasRequestedPermissionInSession &&
        !isRequestInProgress;
  }

  PermissionState copyWith({
    PermissionAccessLevel? accessLevel,
    bool? isRequestInProgress,
    bool? hasRequestedPermissionInSession,
    bool? isRationaleDismissed,
    bool? shouldShowPlatformRationale,
    AppError? error,
    bool clearError = false,
  }) {
    return PermissionState(
      accessLevel: accessLevel ?? this.accessLevel,
      isRequestInProgress: isRequestInProgress ?? this.isRequestInProgress,
      hasRequestedPermissionInSession:
          hasRequestedPermissionInSession ??
          this.hasRequestedPermissionInSession,
      isRationaleDismissed: isRationaleDismissed ?? this.isRationaleDismissed,
      shouldShowPlatformRationale:
          shouldShowPlatformRationale ?? this.shouldShowPlatformRationale,
      error: clearError ? null : error ?? this.error,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is PermissionState &&
            other.accessLevel == accessLevel &&
            other.isRequestInProgress == isRequestInProgress &&
            other.hasRequestedPermissionInSession ==
                hasRequestedPermissionInSession &&
            other.isRationaleDismissed == isRationaleDismissed &&
            other.shouldShowPlatformRationale == shouldShowPlatformRationale &&
            other.error == error;
  }

  @override
  int get hashCode {
    return Object.hash(
      accessLevel,
      isRequestInProgress,
      hasRequestedPermissionInSession,
      isRationaleDismissed,
      shouldShowPlatformRationale,
      error,
    );
  }
}

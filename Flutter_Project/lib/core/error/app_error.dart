import 'package:flutter/services.dart';

import 'error_codes.dart';

typedef AppErrorMapper = AppError Function(Object error, StackTrace stackTrace);

sealed class AppError implements Exception {
  const AppError({
    required this.code,
    required this.message,
    this.cause,
    this.stackTrace,
  });

  factory AppError.permissionDenied({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return PermissionDeniedAppError(
      message: message ?? AppErrorCode.permissionDenied.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.partialPermissionGranted({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return PartialPermissionGrantedAppError(
      message: message ?? AppErrorCode.partialPermissionGranted.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.notFound({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return NotFoundAppError(
      message: message ?? AppErrorCode.notFound.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.cancelledByUser({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return CancelledByUserAppError(
      message: message ?? AppErrorCode.cancelledByUser.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.malformedUriOrPath({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return MalformedUriOrPathAppError(
      message: message ?? AppErrorCode.malformedUriOrPath.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.storageFailure({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return StorageFailureAppError(
      message: message ?? AppErrorCode.storageFailure.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.networkFailure({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return NetworkFailureAppError(
      message: message ?? AppErrorCode.networkFailure.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.unsupportedPlatform({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return UnsupportedPlatformAppError(
      message: message ?? AppErrorCode.unsupportedPlatform.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.platformContractFailure({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return PlatformContractFailureAppError(
      message: message ?? AppErrorCode.platformContractFailure.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.unknown({
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return UnknownAppError(
      message: message ?? AppErrorCode.unknown.defaultMessage,
      cause: cause,
      stackTrace: stackTrace,
    );
  }

  factory AppError.fromCode(
    AppErrorCode code, {
    String? message,
    Object? cause,
    StackTrace? stackTrace,
  }) {
    return switch (code) {
      AppErrorCode.permissionDenied => AppError.permissionDenied(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.partialPermissionGranted =>
        AppError.partialPermissionGranted(
          message: message,
          cause: cause,
          stackTrace: stackTrace,
        ),
      AppErrorCode.notFound => AppError.notFound(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.cancelledByUser => AppError.cancelledByUser(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.malformedUriOrPath => AppError.malformedUriOrPath(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.storageFailure => AppError.storageFailure(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.networkFailure => AppError.networkFailure(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.unsupportedPlatform => AppError.unsupportedPlatform(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.platformContractFailure => AppError.platformContractFailure(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
      AppErrorCode.unknown => AppError.unknown(
        message: message,
        cause: cause,
        stackTrace: stackTrace,
      ),
    };
  }

  factory AppError.fromException(
    Object error,
    StackTrace stackTrace, {
    AppErrorCode fallbackCode = AppErrorCode.unknown,
    String? message,
  }) {
    if (error is AppError) {
      return error;
    }

    if (error is PlatformException) {
      return AppError.platformContractFailure(
        message: message ?? error.message,
        cause: error,
        stackTrace: stackTrace,
      );
    }

    if (error is UnsupportedError) {
      return AppError.unsupportedPlatform(
        message: message ?? error.message,
        cause: error,
        stackTrace: stackTrace,
      );
    }

    if (error is FormatException) {
      return AppError.malformedUriOrPath(
        message: message ?? error.message,
        cause: error,
        stackTrace: stackTrace,
      );
    }

    return AppError.fromCode(
      fallbackCode,
      message: message,
      cause: error,
      stackTrace: stackTrace,
    );
  }

  final AppErrorCode code;
  final String message;
  final Object? cause;
  final StackTrace? stackTrace;

  @override
  String toString() {
    final causeText = cause == null ? '' : ' Cause: $cause';
    return '${code.value}: $message$causeText';
  }
}

final class PermissionDeniedAppError extends AppError {
  const PermissionDeniedAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.permissionDenied);
}

final class PartialPermissionGrantedAppError extends AppError {
  const PartialPermissionGrantedAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.partialPermissionGranted);
}

final class NotFoundAppError extends AppError {
  const NotFoundAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.notFound);
}

final class CancelledByUserAppError extends AppError {
  const CancelledByUserAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.cancelledByUser);
}

final class MalformedUriOrPathAppError extends AppError {
  const MalformedUriOrPathAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.malformedUriOrPath);
}

final class StorageFailureAppError extends AppError {
  const StorageFailureAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.storageFailure);
}

final class NetworkFailureAppError extends AppError {
  const NetworkFailureAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.networkFailure);
}

final class UnsupportedPlatformAppError extends AppError {
  const UnsupportedPlatformAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.unsupportedPlatform);
}

final class PlatformContractFailureAppError extends AppError {
  const PlatformContractFailureAppError({
    required super.message,
    super.cause,
    super.stackTrace,
  }) : super(code: AppErrorCode.platformContractFailure);
}

final class UnknownAppError extends AppError {
  const UnknownAppError({required super.message, super.cause, super.stackTrace})
    : super(code: AppErrorCode.unknown);
}

AppError mapExceptionToAppError(
  Object error,
  StackTrace stackTrace, {
  AppErrorCode fallbackCode = AppErrorCode.unknown,
  String? message,
}) {
  return AppError.fromException(
    error,
    stackTrace,
    fallbackCode: fallbackCode,
    message: message,
  );
}

AppError mapStorageException(
  Object error,
  StackTrace stackTrace, {
  String? message,
}) {
  return AppError.storageFailure(
    message: message,
    cause: error,
    stackTrace: stackTrace,
  );
}

AppError mapNetworkException(
  Object error,
  StackTrace stackTrace, {
  String? message,
}) {
  return AppError.networkFailure(
    message: message,
    cause: error,
    stackTrace: stackTrace,
  );
}

AppError mapPlatformException(
  Object error,
  StackTrace stackTrace, {
  String? message,
}) {
  return AppError.fromException(
    error,
    stackTrace,
    fallbackCode: AppErrorCode.platformContractFailure,
    message: message,
  );
}

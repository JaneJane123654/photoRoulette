import 'dart:async';

import '../error/app_error.dart';

sealed class AppResult<T> {
  const AppResult();

  const factory AppResult.success(T value) = AppSuccess<T>;

  const factory AppResult.failure(AppError error) = AppFailure<T>;

  static Future<AppResult<T>> guard<T>(
    FutureOr<T> Function() action, {
    AppErrorMapper? errorMapper,
  }) async {
    try {
      return AppResult<T>.success(await action());
    } catch (error, stackTrace) {
      final mappedError =
          errorMapper?.call(error, stackTrace) ??
          AppError.fromException(error, stackTrace);
      return AppResult<T>.failure(mappedError);
    }
  }

  bool get isSuccess => this is AppSuccess<T>;

  bool get isFailure => this is AppFailure<T>;

  R when<R>({
    required R Function(T value) success,
    required R Function(AppError error) failure,
  }) {
    return switch (this) {
      AppSuccess<T>(:final value) => success(value),
      AppFailure<T>(:final error) => failure(error),
    };
  }

  T? get valueOrNull {
    return switch (this) {
      AppSuccess<T>(:final value) => value,
      AppFailure<T>() => null,
    };
  }

  AppError? get errorOrNull {
    return switch (this) {
      AppSuccess<T>() => null,
      AppFailure<T>(:final error) => error,
    };
  }

  T getOrElse(T Function(AppError error) recover) {
    return switch (this) {
      AppSuccess<T>(:final value) => value,
      AppFailure<T>(:final error) => recover(error),
    };
  }

  T getOrThrow() {
    return switch (this) {
      AppSuccess<T>(:final value) => value,
      AppFailure<T>(:final error) => throw error,
    };
  }

  AppResult<R> map<R>(R Function(T value) transform) {
    return switch (this) {
      AppSuccess<T>(:final value) => AppResult<R>.success(transform(value)),
      AppFailure<T>(:final error) => AppResult<R>.failure(error),
    };
  }
}

final class AppSuccess<T> extends AppResult<T> {
  const AppSuccess(this.value);

  final T value;
}

final class AppFailure<T> extends AppResult<T> {
  const AppFailure(this.error);

  final AppError error;
}

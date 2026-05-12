import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';

enum AppLogLevel {
  debug('DEBUG', 500),
  info('INFO', 800),
  warning('WARN', 900),
  error('ERROR', 1000);

  const AppLogLevel(this.label, this.developerLevel);

  final String label;
  final int developerLevel;
}

class AppLogger {
  const AppLogger(this.name);

  factory AppLogger.forType(Type type) => AppLogger(type.toString());

  final String name;

  void debug(String message, {Object? error, StackTrace? stackTrace}) {
    _write(AppLogLevel.debug, message, error: error, stackTrace: stackTrace);
  }

  void info(String message, {Object? error, StackTrace? stackTrace}) {
    _write(AppLogLevel.info, message, error: error, stackTrace: stackTrace);
  }

  void warning(String message, {Object? error, StackTrace? stackTrace}) {
    _write(AppLogLevel.warning, message, error: error, stackTrace: stackTrace);
  }

  void error(String message, {Object? error, StackTrace? stackTrace}) {
    _write(AppLogLevel.error, message, error: error, stackTrace: stackTrace);
  }

  void _write(
    AppLogLevel level,
    String message, {
    Object? error,
    StackTrace? stackTrace,
  }) {
    developer.log(
      message,
      name: name,
      level: level.developerLevel,
      error: error,
      stackTrace: stackTrace,
    );

    if (kDebugMode) {
      debugPrint('[$name] ${level.label}: $message');
    }
  }
}

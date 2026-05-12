import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive_flutter/hive_flutter.dart';

final Provider<AppBootstrapResult> appBootstrapResultProvider =
    Provider<AppBootstrapResult>((Ref ref) {
      throw StateError(
        'App bootstrap result is unavailable. Call bootstrapApp before runApp.',
      );
    });

Future<AppBootstrapResult> bootstrapApp() async {
  try {
    await Hive.initFlutter();

    return const AppBootstrapResult(hiveInitialized: true);
  } catch (error, stackTrace) {
    Error.throwWithStackTrace(AppBootstrapException(error), stackTrace);
  }
}

class AppBootstrapResult {
  const AppBootstrapResult({required this.hiveInitialized});

  final bool hiveInitialized;
}

class AppBootstrapException implements Exception {
  const AppBootstrapException(this.cause);

  final Object cause;

  @override
  String toString() => 'App bootstrap failed: $cause';
}

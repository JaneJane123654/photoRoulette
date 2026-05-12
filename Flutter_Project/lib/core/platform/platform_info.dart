import 'package:flutter/foundation.dart';

import '../error/app_error.dart';
import '../result/app_result.dart';

enum AppPlatform { android, ios, web, macos, windows, linux, fuchsia, unknown }

final class PlatformInfo {
  const PlatformInfo({required this.targetPlatform, required this.isWeb});

  factory PlatformInfo.current() {
    return PlatformInfo(targetPlatform: defaultTargetPlatform, isWeb: kIsWeb);
  }

  final TargetPlatform targetPlatform;
  final bool isWeb;

  AppPlatform get platform {
    if (isWeb) {
      return AppPlatform.web;
    }

    return switch (targetPlatform) {
      TargetPlatform.android => AppPlatform.android,
      TargetPlatform.iOS => AppPlatform.ios,
      TargetPlatform.macOS => AppPlatform.macos,
      TargetPlatform.windows => AppPlatform.windows,
      TargetPlatform.linux => AppPlatform.linux,
      TargetPlatform.fuchsia => AppPlatform.fuchsia,
    };
  }

  bool get isAndroid => platform == AppPlatform.android;

  bool get isIos => platform == AppPlatform.ios;

  bool get isMobile => isAndroid || isIos;

  AppResult<void> requireAndroid(String capabilityName) {
    if (isAndroid) {
      return const AppResult<void>.success(null);
    }

    return AppResult<void>.failure(unsupportedPlatformError(capabilityName));
  }

  AppError unsupportedPlatformError(String capabilityName) {
    return AppError.unsupportedPlatform(
      message:
          '$capabilityName is supported only on Android. '
          'Current platform: ${platform.name}.',
    );
  }
}

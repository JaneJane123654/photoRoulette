import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/core/core.dart';

void main() {
  group('AppResult', () {
    test('guard returns success for completed work', () async {
      final result = await AppResult.guard(() => 42);

      expect(result.valueOrNull, 42);
      expect(result.errorOrNull, isNull);
    });

    test('guard maps thrown exceptions to typed failures', () async {
      final result = await AppResult.guard<int>(
        () => throw const FormatException('bad uri'),
      );

      expect(result.valueOrNull, isNull);
      expect(result.errorOrNull?.code, AppErrorCode.malformedUriOrPath);
    });
  });

  group('EffectChannel', () {
    test(
      'emits one-shot effects to active listeners and closes explicitly',
      () async {
        final channel = EffectChannel<int>();
        final received = expectLater(channel.stream, emitsInOrder(<int>[1, 2]));

        expect(channel.tryEmit(1), isTrue);
        channel.emit(2);

        await received;
        await channel.dispose();

        expect(channel.tryEmit(3), isFalse);
      },
    );
  });

  group('PlatformInfo', () {
    test(
      'creates explicit unsupported-platform failure for Android-only work',
      () {
        const platformInfo = PlatformInfo(
          targetPlatform: TargetPlatform.iOS,
          isWeb: false,
        );

        final result = platformInfo.requireAndroid('Silent delete');

        expect(result.errorOrNull?.code, AppErrorCode.unsupportedPlatform);
      },
    );
  });

  group('AppClock', () {
    test('matches the native yyyy-MM month key format', () {
      final clock = FixedAppClock(DateTime(2026, 4, 26, 9, 30));

      expect(clock.currentMonthKey(), '2026-04');
    });
  });
}

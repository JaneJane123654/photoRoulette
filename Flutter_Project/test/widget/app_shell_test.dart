import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/app/bootstrap/app_bootstrap.dart';
import 'package:photo_roulette_flutter/app/bootstrap/app_root.dart';
import 'package:photo_roulette_flutter/app/localization/app_locales.dart';
import 'package:photo_roulette_flutter/core/core.dart';
import 'package:photo_roulette_flutter/features/permissions/permissions.dart';

void main() {
  testWidgets('app shell routes through the permission gate', (
    WidgetTester tester,
  ) async {
    final _FakePermissionService permissionService = _FakePermissionService();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          appBootstrapResultProvider.overrideWithValue(
            const AppBootstrapResult(hiveInitialized: true),
          ),
          permissionServiceProvider.overrideWithValue(permissionService),
        ],
        child: const PhotoRouletteAppRoot(),
      ),
    );

    await tester.pump();
    expect(find.text('Building your next random deck...'), findsOneWidget);

    permissionService.completeInitialRead(PermissionAccessLevel.denied);
    await tester.pumpAndSettle();

    expect(find.text('Photo access is turned off'), findsOneWidget);
    expect(find.text('Open Settings'), findsOneWidget);
    expect(permissionService.requestCallCount, 1);
  });

  test('supported explicit language tags mirror the migration contract', () {
    expect(AppLocales.explicitLanguageTags, const <String>[
      'ar',
      'en',
      'es',
      'fr',
      'ru',
      'zh',
    ]);
    expect(AppLocales.normalizeLanguageTag('en-US'), 'en');
    expect(AppLocales.normalizeLanguageTag('system'), 'system');
    expect(AppLocales.normalizeLanguageTag('de'), 'system');
  });
}

final class _FakePermissionService implements PermissionService {
  final Completer<PermissionAccessLevel> _initialReadCompleter =
      Completer<PermissionAccessLevel>();

  PermissionAccessLevel currentAccessLevel = PermissionAccessLevel.denied;
  bool _hasConsumedInitialRead = false;
  int requestCallCount = 0;

  void completeInitialRead(PermissionAccessLevel accessLevel) {
    currentAccessLevel = accessLevel;
    _initialReadCompleter.complete(accessLevel);
  }

  @override
  Future<AppResult<PermissionAccessLevel>> readCurrentAccessLevel() async {
    if (!_hasConsumedInitialRead) {
      _hasConsumedInitialRead = true;
      return AppResult<PermissionAccessLevel>.success(
        await _initialReadCompleter.future,
      );
    }

    return AppResult<PermissionAccessLevel>.success(currentAccessLevel);
  }

  @override
  Future<AppResult<PermissionAccessLevel>> requestMediaAccess() async {
    requestCallCount += 1;
    return AppResult<PermissionAccessLevel>.success(currentAccessLevel);
  }

  @override
  Future<AppResult<bool>> shouldShowRequestPermissionRationale() async {
    return const AppResult<bool>.success(false);
  }

  @override
  Future<AppResult<bool>> openAppSettings() async {
    return const AppResult<bool>.success(true);
  }
}

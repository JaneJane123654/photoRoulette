import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/core/core.dart';
import 'package:photo_roulette_flutter/features/settings/settings.dart';

void main() {
  SettingsRepositoryImpl createRepository([
    SettingsValueMap seedValues = const <String, Object?>{},
  ]) {
    return SettingsRepositoryImpl(
      localDataSource: InMemorySettingsLocalDataSource(seedValues),
    );
  }

  group('SettingsRepositoryImpl defaults', () {
    test('loads a complete settings snapshot from empty storage', () async {
      final SettingsRepository repository = createRepository();

      final AppSettings settings = await repository.loadSettings();

      expect(settings.swipeSensitivity, 1);
      expect(settings.gestureBallSize, 1);
      expect(settings.isSwipeDeleteEnabled, isTrue);
      expect(settings.isDeleteReminderEnabled, isTrue);
      expect(settings.showFullImage, isFalse);
      expect(settings.showCardActionsButton, isTrue);
      expect(settings.isTapImageToggleEnabled, isTrue);
      expect(settings.showFloatingDeleteButton, isFalse);
      expect(settings.isGestureBallEnabled, isFalse);
      expect(settings.isGestureBallFeedbackEnabled, isTrue);
      expect(settings.showGestureBallActionHint, isTrue);
      expect(settings.isSilentDeleteEnabled, isFalse);
      expect(settings.selectedLanguageTag, AppLanguageTag.system);
      expect(settings.swipeLeftAction, SwipeAction.delete);
      expect(settings.swipeRightAction, SwipeAction.next);
      expect(settings.swipeUpAction, SwipeAction.next);
      expect(settings.swipeDownAction, SwipeAction.previous);
      expect(
        settings.defaultBehaviorNoticeMode,
        DefaultBehaviorNoticeMode.visible,
      );
      expect(settings.defaultBehaviorNoticeShownMonth, isEmpty);
      expect(settings.defaultBehaviorNoticeShownCount, 0);
      expect(settings.deferredUpdateVersion, isNull);
      expect(settings.cardActionButtonDefaultsInitialized, isFalse);
      expect(settings.silentDeleteTreeUris, isEmpty);
    });

    test('falls back from invalid storage values without throwing', () async {
      final SettingsRepository repository = createRepository(<String, Object?>{
        PersistenceKeys.swipeGestureSensitivity: 9.5,
        PersistenceKeys.gestureBallSizeScale: 0.1,
        PersistenceKeys.appLanguageTag: 'de-DE',
        PersistenceKeys.swipeLeftAction: 'archive',
        PersistenceKeys.defaultBehaviorNoticeShownCount: -3,
      });

      final AppSettings settings = await repository.loadSettings();

      expect(settings.swipeSensitivity, AppSettings.maxSwipeSensitivity);
      expect(settings.gestureBallSize, AppSettings.minGestureBallSize);
      expect(settings.selectedLanguageTag, AppLanguageTag.system);
      expect(settings.swipeLeftAction, SwipeAction.delete);
      expect(settings.defaultBehaviorNoticeShownCount, 0);
    });
  });

  group('SettingsRepositoryImpl updates', () {
    test(
      'clamps gesture settings and normalizes language at write time',
      () async {
        final SettingsRepository repository = createRepository();

        await repository.setSwipeGestureSensitivity(99);
        await repository.setGestureBallSizeScale(-4);
        await repository.setAppLanguageTag('EN-US');

        AppSettings settings = await repository.loadSettings();

        expect(settings.swipeSensitivity, AppSettings.maxSwipeSensitivity);
        expect(settings.gestureBallSize, AppSettings.minGestureBallSize);
        expect(settings.selectedLanguageTag, AppLanguageTag.english);

        await repository.setAppLanguageTag('pt-BR');
        settings = await repository.loadSettings();

        expect(settings.selectedLanguageTag, AppLanguageTag.system);
      },
    );

    test('initializes card action button default only when missing', () async {
      final SettingsRepository repository = createRepository();

      await repository.ensureCardActionButtonDefaultsInitialized();

      AppSettings settings = await repository.loadSettings();

      expect(settings.showCardActionsButton, isTrue);
      expect(settings.cardActionButtonDefaultsInitialized, isTrue);

      await repository.setShowCardActionsButton(false);
      await repository.ensureCardActionButtonDefaultsInitialized();

      settings = await repository.loadSettings();

      expect(settings.showCardActionsButton, isFalse);
      expect(settings.cardActionButtonDefaultsInitialized, isTrue);
    });

    test('stores swipe actions by direction using storage values', () async {
      final SettingsRepository repository = createRepository();

      await repository.setSwipeAction(SwipeDirection.left, SwipeAction.skip);
      await repository.setSwipeDownAction(SwipeAction.delete);

      final AppSettings settings = await repository.loadSettings();

      expect(settings.swipeLeftAction, SwipeAction.skip);
      expect(settings.swipeDownAction, SwipeAction.delete);
    });

    test(
      'normalizes silent delete tree uris and preserves legacy fallback',
      () async {
        final SettingsRepository repository = createRepository(
          <String, Object?>{
            PersistenceKeys.silentDeleteTreeUri: 'content://legacy/tree/DCIM',
          },
        );

        AppSettings settings = await repository.loadSettings();

        expect(settings.silentDeleteTreeUris, <String>[
          'content://legacy/tree/DCIM',
        ]);

        await repository.setSilentDeleteTreeUris(<String>[
          'content://b/tree/Pictures',
          '',
          '   ',
          'content://a/tree/DCIM',
          'content://a/tree/DCIM',
        ]);

        settings = await repository.loadSettings();

        expect(settings.silentDeleteTreeUris, <String>[
          'content://a/tree/DCIM',
          'content://b/tree/Pictures',
        ]);

        await repository.setSilentDeleteTreeUri(null);
        settings = await repository.loadSettings();

        expect(settings.silentDeleteTreeUris, isEmpty);
      },
    );

    test(
      'trims blank deferred update versions and stores nonblank values',
      () async {
        final SettingsRepository repository = createRepository();

        await repository.setSkippedUpdateVersion('  2.1.0  ');

        AppSettings settings = await repository.loadSettings();

        expect(settings.deferredUpdateVersion, '2.1.0');

        await repository.setDeferredUpdateVersion('   ');
        settings = await repository.loadSettings();

        expect(settings.deferredUpdateVersion, isNull);
      },
    );
  });

  group('default behavior notice policy', () {
    test('shows at most the monthly limit and then auto-hides', () async {
      final SettingsRepository repository = createRepository();

      for (int index = 0; index < 5; index += 1) {
        expect(
          await repository.prepareDefaultBehaviorNoticeForSession(
            currentMonthKey: '2026-04',
          ),
          isTrue,
        );
      }

      expect(
        await repository.prepareDefaultBehaviorNoticeForSession(
          currentMonthKey: '2026-04',
        ),
        isFalse,
      );

      final AppSettings settings = await repository.loadSettings();

      expect(
        settings.defaultBehaviorNoticeMode,
        DefaultBehaviorNoticeMode.autoHidden,
      );
      expect(settings.defaultBehaviorNoticeShownMonth, '2026-04');
      expect(settings.defaultBehaviorNoticeShownCount, 5);
    });

    test(
      'restores auto-hidden notice next month but not user-hidden notice',
      () async {
        final SettingsRepository autoHiddenRepository =
            createRepository(<String, Object?>{
              PersistenceKeys.defaultBehaviorNoticeMode: 'auto_hidden',
              PersistenceKeys.defaultBehaviorNoticeShownMonth: '2026-04',
              PersistenceKeys.defaultBehaviorNoticeShownCount: 5,
            });

        expect(
          await autoHiddenRepository.prepareDefaultBehaviorNoticeForSession(
            currentMonthKey: '2026-05',
          ),
          isTrue,
        );

        AppSettings settings = await autoHiddenRepository.loadSettings();

        expect(
          settings.defaultBehaviorNoticeMode,
          DefaultBehaviorNoticeMode.visible,
        );
        expect(settings.defaultBehaviorNoticeShownMonth, '2026-05');
        expect(settings.defaultBehaviorNoticeShownCount, 1);

        final SettingsRepository userHiddenRepository =
            createRepository(<String, Object?>{
              PersistenceKeys.defaultBehaviorNoticeMode: 'user_hidden',
              PersistenceKeys.defaultBehaviorNoticeShownMonth: '2026-04',
              PersistenceKeys.defaultBehaviorNoticeShownCount: 5,
            });

        expect(
          await userHiddenRepository.prepareDefaultBehaviorNoticeForSession(
            currentMonthKey: '2026-05',
          ),
          isFalse,
        );

        settings = await userHiddenRepository.loadSettings();

        expect(
          settings.defaultBehaviorNoticeMode,
          DefaultBehaviorNoticeMode.userHidden,
        );
        expect(settings.defaultBehaviorNoticeShownMonth, '2026-05');
        expect(settings.defaultBehaviorNoticeShownCount, 0);
      },
    );

    test('manual enable resets shown count', () async {
      final SettingsRepository repository = createRepository(<String, Object?>{
        PersistenceKeys.defaultBehaviorNoticeMode: 'user_hidden',
        PersistenceKeys.defaultBehaviorNoticeShownCount: 4,
      });

      await repository.setDefaultBehaviorNoticeEnabled(true);

      final AppSettings settings = await repository.loadSettings();

      expect(
        settings.defaultBehaviorNoticeMode,
        DefaultBehaviorNoticeMode.visible,
      );
      expect(settings.defaultBehaviorNoticeShownCount, 0);
    });

    test('rejects a non-positive monthly display limit', () async {
      final SettingsRepository repository = createRepository();

      expect(
        () => repository.prepareDefaultBehaviorNoticeForSession(
          currentMonthKey: '2026-04',
          monthlyDisplayLimit: 0,
        ),
        throwsArgumentError,
      );
    });
  });
}

final class InMemorySettingsLocalDataSource implements SettingsLocalDataSource {
  InMemorySettingsLocalDataSource(SettingsValueMap seedValues)
    : _values = <String, Object?>{...seedValues};

  SettingsValueMap _values;
  final StreamController<SettingsValueMap> _controller =
      StreamController<SettingsValueMap>.broadcast();

  @override
  Future<SettingsValueMap> readAll() async {
    return Map<String, Object?>.unmodifiable(_values);
  }

  @override
  Future<void> replaceAll(SettingsValueMap values) async {
    _values = _normalizedCopy(values);
    _emit();
  }

  @override
  Future<void> updateValues(SettingsValueMutation mutation) async {
    final SettingsValueMap updatedValues = <String, Object?>{..._values};

    mutation(updatedValues);

    _values = _normalizedCopy(updatedValues);
    _emit();
  }

  @override
  Stream<SettingsValueMap> watchValues() async* {
    yield await readAll();
    yield* _controller.stream;
  }

  SettingsValueMap _normalizedCopy(SettingsValueMap values) {
    final SettingsValueMap normalizedValues = <String, Object?>{};
    for (final MapEntry<String, Object?> entry in values.entries) {
      final Object? value = entry.value;
      if (value != null) {
        normalizedValues[entry.key] = value;
      }
    }

    return normalizedValues;
  }

  void _emit() {
    _controller.add(Map<String, Object?>.unmodifiable(_values));
  }
}

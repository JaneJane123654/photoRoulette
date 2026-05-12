import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/persistence_keys.dart';
import '../../../core/error/app_error.dart';
import '../domain/models/app_language_tag.dart';
import '../domain/models/app_settings.dart';
import '../domain/models/default_notice_mode.dart';
import '../domain/models/swipe_action.dart';
import '../domain/models/swipe_direction.dart';
import '../domain/settings_repository.dart';
import 'settings_local_data_source.dart';

final Provider<SettingsLocalDataSource> settingsLocalDataSourceProvider =
    Provider<SettingsLocalDataSource>((Ref ref) {
      return HiveSettingsLocalDataSource();
    });

final Provider<SettingsRepository> settingsRepositoryProvider =
    Provider<SettingsRepository>((Ref ref) {
      return SettingsRepositoryImpl(
        localDataSource: ref.watch(settingsLocalDataSourceProvider),
      );
    });

final class SettingsRepositoryImpl implements SettingsRepository {
  const SettingsRepositoryImpl({required this.localDataSource});

  final SettingsLocalDataSource localDataSource;

  @override
  Future<AppSettings> loadSettings() {
    return _guardStorage(() async {
      final SettingsValueMap values = await localDataSource.readAll();
      return _settingsFromStorage(values);
    });
  }

  @override
  Stream<AppSettings> watchSettings() async* {
    try {
      await for (final SettingsValueMap values
          in localDataSource.watchValues()) {
        yield _settingsFromStorage(values);
      }
    } catch (error, stackTrace) {
      Error.throwWithStackTrace(
        mapStorageException(error, stackTrace),
        stackTrace,
      );
    }
  }

  @override
  Future<void> saveSettings(AppSettings settings) {
    return _guardStorage(() async {
      await localDataSource.replaceAll(_settingsToStorageMap(settings));
    });
  }

  @override
  Future<void> setSwipeDeleteEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableSwipeDelete, enabled);
  }

  @override
  Future<void> setDeleteReminderEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableDeleteReminder, enabled);
  }

  @override
  Future<void> setSwipeGestureSensitivity(double sensitivity) {
    return _setValue(
      PersistenceKeys.swipeGestureSensitivity,
      AppSettings.clampSwipeSensitivity(sensitivity),
    );
  }

  @override
  Future<void> setShowFullImage(bool enabled) {
    return _setValue(PersistenceKeys.showFullImage, enabled);
  }

  @override
  Future<void> setShowCardActionsButton(bool enabled) {
    return _updateValues((SettingsValueMap values) {
      values[PersistenceKeys.showCardActionsButton] = enabled;
      values[PersistenceKeys.cardActionButtonDefaultsInitialized] = true;
    });
  }

  @override
  Future<void> ensureCardActionButtonDefaultsInitialized() {
    return _updateValues((SettingsValueMap values) {
      if (values[PersistenceKeys.showCardActionsButton] == null) {
        values[PersistenceKeys.showCardActionsButton] =
            AppSettings.defaultCardActionsButtonVisible;
      }
      values[PersistenceKeys.cardActionButtonDefaultsInitialized] = true;
    });
  }

  @override
  Future<void> setTapImageToggleEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableTapImageToggle, enabled);
  }

  @override
  Future<void> setShowFloatingDeleteButton(bool enabled) {
    return _setValue(PersistenceKeys.showFloatingDeleteButton, enabled);
  }

  @override
  Future<void> setGestureBallEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableGestureBall, enabled);
  }

  @override
  Future<void> setGestureBallSizeScale(double scale) {
    return _setValue(
      PersistenceKeys.gestureBallSizeScale,
      AppSettings.clampGestureBallSize(scale),
    );
  }

  @override
  Future<void> setGestureBallSize(double scale) {
    return setGestureBallSizeScale(scale);
  }

  @override
  Future<void> setGestureBallFeedbackEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableGestureBallFeedback, enabled);
  }

  @override
  Future<void> setShowGestureBallActionHint(bool enabled) {
    return _setValue(PersistenceKeys.showGestureBallActionHint, enabled);
  }

  @override
  Future<void> setSilentDeleteEnabled(bool enabled) {
    return _setValue(PersistenceKeys.enableSilentDelete, enabled);
  }

  @override
  Future<void> setSilentDeleteTreeUris(Iterable<String> uris) {
    return _updateValues((SettingsValueMap values) {
      final Set<String> normalizedUris = <String>{};
      for (final String uri in uris) {
        if (uri.trim().isNotEmpty) {
          normalizedUris.add(uri);
        }
      }

      if (normalizedUris.isEmpty) {
        values.remove(PersistenceKeys.silentDeleteTreeUris);
        values.remove(PersistenceKeys.silentDeleteTreeUri);
      } else {
        final List<String> uriList = normalizedUris.toList(growable: false);
        values[PersistenceKeys.silentDeleteTreeUris] = uriList;

        if (uriList.length == 1) {
          values[PersistenceKeys.silentDeleteTreeUri] = uriList.first;
        } else {
          values.remove(PersistenceKeys.silentDeleteTreeUri);
        }
      }
    });
  }

  @override
  Future<void> setSilentDeleteTreeUri(String? uri) {
    return setSilentDeleteTreeUris(
      uri == null ? const <String>[] : <String>[uri],
    );
  }

  @override
  Future<void> setAppLanguageTag(String tag) {
    return _setValue(
      PersistenceKeys.appLanguageTag,
      AppLanguageTag.normalizeLanguageTag(tag),
    );
  }

  @override
  Future<void> setSelectedLanguageTag(AppLanguageTag tag) {
    return _setValue(PersistenceKeys.appLanguageTag, tag.storageValue);
  }

  @override
  Future<void> setSwipeAction(SwipeDirection direction, SwipeAction action) {
    return switch (direction) {
      SwipeDirection.left => setSwipeLeftAction(action),
      SwipeDirection.right => setSwipeRightAction(action),
      SwipeDirection.up => setSwipeUpAction(action),
      SwipeDirection.down => setSwipeDownAction(action),
    };
  }

  @override
  Future<void> setSwipeLeftAction(SwipeAction action) {
    return _setValue(PersistenceKeys.swipeLeftAction, action.storageValue);
  }

  @override
  Future<void> setSwipeRightAction(SwipeAction action) {
    return _setValue(PersistenceKeys.swipeRightAction, action.storageValue);
  }

  @override
  Future<void> setSwipeUpAction(SwipeAction action) {
    return _setValue(PersistenceKeys.swipeUpAction, action.storageValue);
  }

  @override
  Future<void> setSwipeDownAction(SwipeAction action) {
    return _setValue(PersistenceKeys.swipeDownAction, action.storageValue);
  }

  @override
  Future<void> setDefaultBehaviorNoticeEnabled(bool enabled) {
    return _updateValues((SettingsValueMap values) {
      final DefaultBehaviorNoticeMode mode = enabled
          ? DefaultBehaviorNoticeMode.visible
          : DefaultBehaviorNoticeMode.userHidden;

      values[PersistenceKeys.defaultBehaviorNoticeMode] = mode.storageValue;

      if (enabled) {
        values[PersistenceKeys.defaultBehaviorNoticeShownCount] = 0;
      }
    });
  }

  @override
  Future<bool> prepareDefaultBehaviorNoticeForSession({
    required String currentMonthKey,
    int monthlyDisplayLimit = AppSettings.defaultBehaviorNoticeMonthlyMaxShown,
  }) async {
    if (monthlyDisplayLimit <= 0) {
      throw ArgumentError.value(
        monthlyDisplayLimit,
        'monthlyDisplayLimit',
        'must be greater than zero.',
      );
    }

    bool shouldShowNotice = false;

    await _updateValues((SettingsValueMap values) {
      DefaultBehaviorNoticeMode mode =
          DefaultBehaviorNoticeMode.fromStorageValue(
            _readString(values, PersistenceKeys.defaultBehaviorNoticeMode),
          ) ??
          DefaultBehaviorNoticeMode.visible;

      String shownMonth =
          _readString(
            values,
            PersistenceKeys.defaultBehaviorNoticeShownMonth,
          ) ??
          currentMonthKey;

      int shownCount = _readInt(
        values,
        PersistenceKeys.defaultBehaviorNoticeShownCount,
        0,
      );
      if (shownCount < 0) {
        shownCount = 0;
      }

      if (shownMonth != currentMonthKey) {
        shownMonth = currentMonthKey;
        shownCount = 0;

        if (mode == DefaultBehaviorNoticeMode.autoHidden) {
          mode = DefaultBehaviorNoticeMode.visible;
        }
      }

      if (mode == DefaultBehaviorNoticeMode.visible) {
        if (shownCount >= monthlyDisplayLimit) {
          mode = DefaultBehaviorNoticeMode.autoHidden;
          shouldShowNotice = false;
        } else {
          shownCount += 1;
          shouldShowNotice = true;
        }
      }

      values[PersistenceKeys.defaultBehaviorNoticeMode] = mode.storageValue;
      values[PersistenceKeys.defaultBehaviorNoticeShownMonth] = shownMonth;
      values[PersistenceKeys.defaultBehaviorNoticeShownCount] = shownCount;
    });

    return shouldShowNotice;
  }

  @override
  Future<void> setSkippedUpdateVersion(String? versionName) {
    return _updateValues((SettingsValueMap values) {
      final String normalizedVersion = versionName?.trim() ?? '';
      if (normalizedVersion.isEmpty) {
        values.remove(PersistenceKeys.deferredUpdateVersion);
      } else {
        values[PersistenceKeys.deferredUpdateVersion] = normalizedVersion;
      }
    });
  }

  @override
  Future<void> setDeferredUpdateVersion(String? versionName) {
    return setSkippedUpdateVersion(versionName);
  }

  Future<void> _setValue(String key, Object value) {
    return _updateValues((SettingsValueMap values) {
      values[key] = value;
    });
  }

  Future<void> _updateValues(SettingsValueMutation mutation) {
    return _guardStorage(() async {
      await localDataSource.updateValues(mutation);
    });
  }

  Future<T> _guardStorage<T>(Future<T> Function() action) async {
    try {
      return await action();
    } catch (error, stackTrace) {
      Error.throwWithStackTrace(
        mapStorageException(error, stackTrace),
        stackTrace,
      );
    }
  }

  AppSettings _settingsFromStorage(SettingsValueMap values) {
    final bool hasShowCardActionsButton =
        values[PersistenceKeys.showCardActionsButton] != null;

    return AppSettings(
      swipeSensitivity: _readDouble(
        values,
        PersistenceKeys.swipeGestureSensitivity,
        AppSettings.defaultSwipeSensitivity,
        min: AppSettings.minSwipeSensitivity,
        max: AppSettings.maxSwipeSensitivity,
      ),
      gestureBallSize: _readDouble(
        values,
        PersistenceKeys.gestureBallSizeScale,
        AppSettings.defaultGestureBallSize,
        min: AppSettings.minGestureBallSize,
        max: AppSettings.maxGestureBallSize,
      ),
      isSwipeDeleteEnabled: _readBool(
        values,
        PersistenceKeys.enableSwipeDelete,
        true,
      ),
      isDeleteReminderEnabled: _readBool(
        values,
        PersistenceKeys.enableDeleteReminder,
        AppSettings.defaultDeleteReminderEnabled,
      ),
      showFullImage: _readBool(values, PersistenceKeys.showFullImage, false),
      showCardActionsButton: _readBool(
        values,
        PersistenceKeys.showCardActionsButton,
        AppSettings.defaultCardActionsButtonVisible,
      ),
      isTapImageToggleEnabled: _readBool(
        values,
        PersistenceKeys.enableTapImageToggle,
        AppSettings.defaultTapImageToggleEnabled,
      ),
      showFloatingDeleteButton: _readBool(
        values,
        PersistenceKeys.showFloatingDeleteButton,
        false,
      ),
      isGestureBallEnabled: _readBool(
        values,
        PersistenceKeys.enableGestureBall,
        false,
      ),
      isGestureBallFeedbackEnabled: _readBool(
        values,
        PersistenceKeys.enableGestureBallFeedback,
        AppSettings.defaultGestureBallFeedbackEnabled,
      ),
      showGestureBallActionHint: _readBool(
        values,
        PersistenceKeys.showGestureBallActionHint,
        AppSettings.defaultGestureBallActionHintEnabled,
      ),
      isSilentDeleteEnabled: _readBool(
        values,
        PersistenceKeys.enableSilentDelete,
        false,
      ),
      selectedLanguageTag: AppLanguageTag.fromStorageValue(
        _readString(values, PersistenceKeys.appLanguageTag),
      ),
      swipeLeftAction: _readSwipeAction(
        values,
        PersistenceKeys.swipeLeftAction,
        AppSettings.defaultLeftAction,
      ),
      swipeRightAction: _readSwipeAction(
        values,
        PersistenceKeys.swipeRightAction,
        AppSettings.defaultRightAction,
      ),
      swipeUpAction: _readSwipeAction(
        values,
        PersistenceKeys.swipeUpAction,
        AppSettings.defaultUpAction,
      ),
      swipeDownAction: _readSwipeAction(
        values,
        PersistenceKeys.swipeDownAction,
        AppSettings.defaultDownAction,
      ),
      defaultBehaviorNoticeMode:
          DefaultBehaviorNoticeMode.fromStorageValue(
            _readString(values, PersistenceKeys.defaultBehaviorNoticeMode),
          ) ??
          DefaultBehaviorNoticeMode.visible,
      defaultBehaviorNoticeShownMonth:
          _readString(
            values,
            PersistenceKeys.defaultBehaviorNoticeShownMonth,
          ) ??
          '',
      defaultBehaviorNoticeShownCount: _readInt(
        values,
        PersistenceKeys.defaultBehaviorNoticeShownCount,
        0,
      ).clamp(0, 1 << 31),
      deferredUpdateVersion: _readNonBlankString(
        values,
        PersistenceKeys.deferredUpdateVersion,
      ),
      cardActionButtonDefaultsInitialized: _readBool(
        values,
        PersistenceKeys.cardActionButtonDefaultsInitialized,
        hasShowCardActionsButton,
      ),
      silentDeleteTreeUris: _readSilentDeleteTreeUris(values),
    );
  }

  SettingsValueMap _settingsToStorageMap(AppSettings settings) {
    final AppSettings normalizedSettings = settings.normalized();
    final SettingsValueMap values = <String, Object?>{
      PersistenceKeys.enableSwipeDelete:
          normalizedSettings.isSwipeDeleteEnabled,
      PersistenceKeys.enableDeleteReminder:
          normalizedSettings.isDeleteReminderEnabled,
      PersistenceKeys.enableSilentDelete:
          normalizedSettings.isSilentDeleteEnabled,
      PersistenceKeys.enableTapImageToggle:
          normalizedSettings.isTapImageToggleEnabled,
      PersistenceKeys.showCardActionsButton:
          normalizedSettings.showCardActionsButton,
      PersistenceKeys.showFullImage: normalizedSettings.showFullImage,
      PersistenceKeys.showFloatingDeleteButton:
          normalizedSettings.showFloatingDeleteButton,
      PersistenceKeys.enableGestureBall:
          normalizedSettings.isGestureBallEnabled,
      PersistenceKeys.enableGestureBallFeedback:
          normalizedSettings.isGestureBallFeedbackEnabled,
      PersistenceKeys.showGestureBallActionHint:
          normalizedSettings.showGestureBallActionHint,
      PersistenceKeys.swipeGestureSensitivity:
          normalizedSettings.swipeSensitivity,
      PersistenceKeys.gestureBallSizeScale: normalizedSettings.gestureBallSize,
      PersistenceKeys.appLanguageTag:
          normalizedSettings.selectedLanguageTag.storageValue,
      PersistenceKeys.swipeLeftAction:
          normalizedSettings.swipeLeftAction.storageValue,
      PersistenceKeys.swipeRightAction:
          normalizedSettings.swipeRightAction.storageValue,
      PersistenceKeys.swipeUpAction:
          normalizedSettings.swipeUpAction.storageValue,
      PersistenceKeys.swipeDownAction:
          normalizedSettings.swipeDownAction.storageValue,
      PersistenceKeys.defaultBehaviorNoticeMode:
          normalizedSettings.defaultBehaviorNoticeMode.storageValue,
      PersistenceKeys.defaultBehaviorNoticeShownMonth:
          normalizedSettings.defaultBehaviorNoticeShownMonth,
      PersistenceKeys.defaultBehaviorNoticeShownCount:
          normalizedSettings.defaultBehaviorNoticeShownCount,
      PersistenceKeys.cardActionButtonDefaultsInitialized:
          normalizedSettings.cardActionButtonDefaultsInitialized,
    };

    final String? deferredUpdateVersion =
        normalizedSettings.deferredUpdateVersion;
    if (deferredUpdateVersion == null) {
      values.remove(PersistenceKeys.deferredUpdateVersion);
    } else {
      values[PersistenceKeys.deferredUpdateVersion] = deferredUpdateVersion;
    }

    final List<String> treeUris = normalizedSettings.silentDeleteTreeUris;
    if (treeUris.isEmpty) {
      values.remove(PersistenceKeys.silentDeleteTreeUris);
      values.remove(PersistenceKeys.silentDeleteTreeUri);
    } else {
      values[PersistenceKeys.silentDeleteTreeUris] = treeUris;
      if (treeUris.length == 1) {
        values[PersistenceKeys.silentDeleteTreeUri] = treeUris.first;
      } else {
        values.remove(PersistenceKeys.silentDeleteTreeUri);
      }
    }

    return values;
  }

  bool _readBool(SettingsValueMap values, String key, bool fallback) {
    final Object? value = values[key];
    return value is bool ? value : fallback;
  }

  double _readDouble(
    SettingsValueMap values,
    String key,
    double fallback, {
    required double min,
    required double max,
  }) {
    final Object? value = values[key];
    final double numericValue = value is num ? value.toDouble() : fallback;
    return numericValue.clamp(min, max).toDouble();
  }

  int _readInt(SettingsValueMap values, String key, int fallback) {
    final Object? value = values[key];
    return value is int ? value : fallback;
  }

  String? _readString(SettingsValueMap values, String key) {
    final Object? value = values[key];
    return value is String ? value : null;
  }

  String? _readNonBlankString(SettingsValueMap values, String key) {
    final String? value = _readString(values, key);
    if (value == null || value.trim().isEmpty) {
      return null;
    }

    return value;
  }

  SwipeAction _readSwipeAction(
    SettingsValueMap values,
    String key,
    SwipeAction fallback,
  ) {
    return SwipeAction.fromStorageValue(_readString(values, key)) ?? fallback;
  }

  List<String> _readSilentDeleteTreeUris(SettingsValueMap values) {
    final Set<String> persistedUris = <String>{};

    for (final String uri in _readStringCollection(
      values[PersistenceKeys.silentDeleteTreeUris],
    )) {
      if (uri.trim().isNotEmpty) {
        persistedUris.add(uri);
      }
    }

    if (persistedUris.isNotEmpty) {
      final List<String> sortedUris = persistedUris.toList();
      sortedUris.sort();
      return List<String>.unmodifiable(sortedUris);
    }

    final String? legacyUri = _readString(
      values,
      PersistenceKeys.silentDeleteTreeUri,
    );
    if (legacyUri == null || legacyUri.trim().isEmpty) {
      return const <String>[];
    }

    return <String>[legacyUri];
  }

  Iterable<String> _readStringCollection(Object? value) sync* {
    if (value is Iterable<Object?>) {
      for (final Object? item in value) {
        if (item is String) {
          yield item;
        }
      }
    }
  }
}

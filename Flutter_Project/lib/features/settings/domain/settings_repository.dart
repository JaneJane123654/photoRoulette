import 'models/app_language_tag.dart';
import 'models/app_settings.dart';
import 'models/swipe_action.dart';
import 'models/swipe_direction.dart';

abstract interface class SettingsRepository {
  Future<AppSettings> loadSettings();

  Stream<AppSettings> watchSettings();

  Future<void> saveSettings(AppSettings settings);

  Future<void> setSwipeDeleteEnabled(bool enabled);

  Future<void> setDeleteReminderEnabled(bool enabled);

  Future<void> setSwipeGestureSensitivity(double sensitivity);

  Future<void> setShowFullImage(bool enabled);

  Future<void> setShowCardActionsButton(bool enabled);

  Future<void> ensureCardActionButtonDefaultsInitialized();

  Future<void> setTapImageToggleEnabled(bool enabled);

  Future<void> setShowFloatingDeleteButton(bool enabled);

  Future<void> setGestureBallEnabled(bool enabled);

  Future<void> setGestureBallSizeScale(double scale);

  Future<void> setGestureBallSize(double scale);

  Future<void> setGestureBallFeedbackEnabled(bool enabled);

  Future<void> setShowGestureBallActionHint(bool enabled);

  Future<void> setSilentDeleteEnabled(bool enabled);

  Future<void> setSilentDeleteTreeUris(Iterable<String> uris);

  Future<void> setSilentDeleteTreeUri(String? uri);

  Future<void> setAppLanguageTag(String tag);

  Future<void> setSelectedLanguageTag(AppLanguageTag tag);

  Future<void> setSwipeAction(SwipeDirection direction, SwipeAction action);

  Future<void> setSwipeLeftAction(SwipeAction action);

  Future<void> setSwipeRightAction(SwipeAction action);

  Future<void> setSwipeUpAction(SwipeAction action);

  Future<void> setSwipeDownAction(SwipeAction action);

  Future<void> setDefaultBehaviorNoticeEnabled(bool enabled);

  Future<bool> prepareDefaultBehaviorNoticeForSession({
    required String currentMonthKey,
    int monthlyDisplayLimit = AppSettings.defaultBehaviorNoticeMonthlyMaxShown,
  });

  Future<void> setSkippedUpdateVersion(String? versionName);

  Future<void> setDeferredUpdateVersion(String? versionName);
}

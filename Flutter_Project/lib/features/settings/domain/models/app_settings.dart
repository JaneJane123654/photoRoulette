import 'package:collection/collection.dart';

import 'app_language_tag.dart';
import 'default_notice_mode.dart';
import 'swipe_action.dart';
import 'swipe_direction.dart';

const Object _unset = Object();

final class AppSettings {
  const AppSettings({
    required this.swipeSensitivity,
    required this.gestureBallSize,
    required this.isSwipeDeleteEnabled,
    required this.isDeleteReminderEnabled,
    required this.showFullImage,
    required this.showCardActionsButton,
    required this.isTapImageToggleEnabled,
    required this.showFloatingDeleteButton,
    required this.isGestureBallEnabled,
    required this.isGestureBallFeedbackEnabled,
    required this.showGestureBallActionHint,
    required this.isSilentDeleteEnabled,
    required this.selectedLanguageTag,
    required this.swipeLeftAction,
    required this.swipeRightAction,
    required this.swipeUpAction,
    required this.swipeDownAction,
    required this.defaultBehaviorNoticeMode,
    required this.defaultBehaviorNoticeShownMonth,
    required this.defaultBehaviorNoticeShownCount,
    required this.deferredUpdateVersion,
    required this.cardActionButtonDefaultsInitialized,
    required this.silentDeleteTreeUris,
  });

  static const double minSwipeSensitivity = 0.8;
  static const double maxSwipeSensitivity = 1.35;
  static const double defaultSwipeSensitivity = 1;

  static const double minGestureBallSize = 0.78;
  static const double maxGestureBallSize = 1.38;
  static const double defaultGestureBallSize = 1;

  static const bool defaultDeleteReminderEnabled = true;
  static const bool defaultTapImageToggleEnabled = true;
  static const bool defaultCardActionsButtonVisible = true;
  static const bool defaultGestureBallFeedbackEnabled = true;
  static const bool defaultGestureBallActionHintEnabled = true;
  static const int defaultBehaviorNoticeMonthlyMaxShown = 5;

  static const SwipeAction defaultLeftAction = SwipeAction.delete;
  static const SwipeAction defaultRightAction = SwipeAction.next;
  static const SwipeAction defaultUpAction = SwipeAction.next;
  static const SwipeAction defaultDownAction = SwipeAction.previous;

  static const AppSettings defaults = AppSettings(
    swipeSensitivity: defaultSwipeSensitivity,
    gestureBallSize: defaultGestureBallSize,
    isSwipeDeleteEnabled: true,
    isDeleteReminderEnabled: defaultDeleteReminderEnabled,
    showFullImage: false,
    showCardActionsButton: defaultCardActionsButtonVisible,
    isTapImageToggleEnabled: defaultTapImageToggleEnabled,
    showFloatingDeleteButton: false,
    isGestureBallEnabled: false,
    isGestureBallFeedbackEnabled: defaultGestureBallFeedbackEnabled,
    showGestureBallActionHint: defaultGestureBallActionHintEnabled,
    isSilentDeleteEnabled: false,
    selectedLanguageTag: AppLanguageTag.system,
    swipeLeftAction: defaultLeftAction,
    swipeRightAction: defaultRightAction,
    swipeUpAction: defaultUpAction,
    swipeDownAction: defaultDownAction,
    defaultBehaviorNoticeMode: DefaultBehaviorNoticeMode.visible,
    defaultBehaviorNoticeShownMonth: '',
    defaultBehaviorNoticeShownCount: 0,
    deferredUpdateVersion: null,
    cardActionButtonDefaultsInitialized: false,
    silentDeleteTreeUris: <String>[],
  );

  final double swipeSensitivity;
  final double gestureBallSize;
  final bool isSwipeDeleteEnabled;
  final bool isDeleteReminderEnabled;
  final bool showFullImage;
  final bool showCardActionsButton;
  final bool isTapImageToggleEnabled;
  final bool showFloatingDeleteButton;
  final bool isGestureBallEnabled;
  final bool isGestureBallFeedbackEnabled;
  final bool showGestureBallActionHint;
  final bool isSilentDeleteEnabled;
  final AppLanguageTag selectedLanguageTag;
  final SwipeAction swipeLeftAction;
  final SwipeAction swipeRightAction;
  final SwipeAction swipeUpAction;
  final SwipeAction swipeDownAction;
  final DefaultBehaviorNoticeMode defaultBehaviorNoticeMode;
  final String defaultBehaviorNoticeShownMonth;
  final int defaultBehaviorNoticeShownCount;
  final String? deferredUpdateVersion;
  final bool cardActionButtonDefaultsInitialized;
  final List<String> silentDeleteTreeUris;

  static double clampSwipeSensitivity(double value) {
    return value.clamp(minSwipeSensitivity, maxSwipeSensitivity).toDouble();
  }

  static double clampGestureBallSize(double value) {
    return value.clamp(minGestureBallSize, maxGestureBallSize).toDouble();
  }

  SwipeAction actionForDirection(SwipeDirection direction) {
    return switch (direction) {
      SwipeDirection.left => swipeLeftAction,
      SwipeDirection.right => swipeRightAction,
      SwipeDirection.up => swipeUpAction,
      SwipeDirection.down => swipeDownAction,
    };
  }

  AppSettings withSwipeAction(SwipeDirection direction, SwipeAction action) {
    return switch (direction) {
      SwipeDirection.left => copyWith(swipeLeftAction: action),
      SwipeDirection.right => copyWith(swipeRightAction: action),
      SwipeDirection.up => copyWith(swipeUpAction: action),
      SwipeDirection.down => copyWith(swipeDownAction: action),
    };
  }

  AppSettings normalized() {
    return copyWith(
      swipeSensitivity: clampSwipeSensitivity(swipeSensitivity),
      gestureBallSize: clampGestureBallSize(gestureBallSize),
      defaultBehaviorNoticeShownCount: defaultBehaviorNoticeShownCount < 0
          ? 0
          : defaultBehaviorNoticeShownCount,
      deferredUpdateVersion: _normalizeDeferredVersion(deferredUpdateVersion),
      silentDeleteTreeUris: _normalizeUriList(silentDeleteTreeUris),
    );
  }

  AppSettings copyWith({
    double? swipeSensitivity,
    double? gestureBallSize,
    bool? isSwipeDeleteEnabled,
    bool? isDeleteReminderEnabled,
    bool? showFullImage,
    bool? showCardActionsButton,
    bool? isTapImageToggleEnabled,
    bool? showFloatingDeleteButton,
    bool? isGestureBallEnabled,
    bool? isGestureBallFeedbackEnabled,
    bool? showGestureBallActionHint,
    bool? isSilentDeleteEnabled,
    AppLanguageTag? selectedLanguageTag,
    SwipeAction? swipeLeftAction,
    SwipeAction? swipeRightAction,
    SwipeAction? swipeUpAction,
    SwipeAction? swipeDownAction,
    DefaultBehaviorNoticeMode? defaultBehaviorNoticeMode,
    String? defaultBehaviorNoticeShownMonth,
    int? defaultBehaviorNoticeShownCount,
    Object? deferredUpdateVersion = _unset,
    bool? cardActionButtonDefaultsInitialized,
    List<String>? silentDeleteTreeUris,
  }) {
    return AppSettings(
      swipeSensitivity: swipeSensitivity ?? this.swipeSensitivity,
      gestureBallSize: gestureBallSize ?? this.gestureBallSize,
      isSwipeDeleteEnabled: isSwipeDeleteEnabled ?? this.isSwipeDeleteEnabled,
      isDeleteReminderEnabled:
          isDeleteReminderEnabled ?? this.isDeleteReminderEnabled,
      showFullImage: showFullImage ?? this.showFullImage,
      showCardActionsButton:
          showCardActionsButton ?? this.showCardActionsButton,
      isTapImageToggleEnabled:
          isTapImageToggleEnabled ?? this.isTapImageToggleEnabled,
      showFloatingDeleteButton:
          showFloatingDeleteButton ?? this.showFloatingDeleteButton,
      isGestureBallEnabled: isGestureBallEnabled ?? this.isGestureBallEnabled,
      isGestureBallFeedbackEnabled:
          isGestureBallFeedbackEnabled ?? this.isGestureBallFeedbackEnabled,
      showGestureBallActionHint:
          showGestureBallActionHint ?? this.showGestureBallActionHint,
      isSilentDeleteEnabled:
          isSilentDeleteEnabled ?? this.isSilentDeleteEnabled,
      selectedLanguageTag: selectedLanguageTag ?? this.selectedLanguageTag,
      swipeLeftAction: swipeLeftAction ?? this.swipeLeftAction,
      swipeRightAction: swipeRightAction ?? this.swipeRightAction,
      swipeUpAction: swipeUpAction ?? this.swipeUpAction,
      swipeDownAction: swipeDownAction ?? this.swipeDownAction,
      defaultBehaviorNoticeMode:
          defaultBehaviorNoticeMode ?? this.defaultBehaviorNoticeMode,
      defaultBehaviorNoticeShownMonth:
          defaultBehaviorNoticeShownMonth ??
          this.defaultBehaviorNoticeShownMonth,
      defaultBehaviorNoticeShownCount:
          defaultBehaviorNoticeShownCount ??
          this.defaultBehaviorNoticeShownCount,
      deferredUpdateVersion: identical(deferredUpdateVersion, _unset)
          ? this.deferredUpdateVersion
          : deferredUpdateVersion as String?,
      cardActionButtonDefaultsInitialized:
          cardActionButtonDefaultsInitialized ??
          this.cardActionButtonDefaultsInitialized,
      silentDeleteTreeUris: silentDeleteTreeUris ?? this.silentDeleteTreeUris,
    );
  }

  static String? _normalizeDeferredVersion(String? version) {
    final String normalizedVersion = version?.trim() ?? '';
    if (normalizedVersion.isEmpty) {
      return null;
    }

    return normalizedVersion;
  }

  static List<String> _normalizeUriList(Iterable<String> uris) {
    final Set<String> normalizedUris = <String>{};
    for (final String uri in uris) {
      if (uri.trim().isNotEmpty) {
        normalizedUris.add(uri);
      }
    }

    return List<String>.unmodifiable(normalizedUris);
  }

  @override
  bool operator ==(Object other) {
    const ListEquality<String> listEquality = ListEquality<String>();
    return identical(this, other) ||
        other is AppSettings &&
            other.swipeSensitivity == swipeSensitivity &&
            other.gestureBallSize == gestureBallSize &&
            other.isSwipeDeleteEnabled == isSwipeDeleteEnabled &&
            other.isDeleteReminderEnabled == isDeleteReminderEnabled &&
            other.showFullImage == showFullImage &&
            other.showCardActionsButton == showCardActionsButton &&
            other.isTapImageToggleEnabled == isTapImageToggleEnabled &&
            other.showFloatingDeleteButton == showFloatingDeleteButton &&
            other.isGestureBallEnabled == isGestureBallEnabled &&
            other.isGestureBallFeedbackEnabled ==
                isGestureBallFeedbackEnabled &&
            other.showGestureBallActionHint == showGestureBallActionHint &&
            other.isSilentDeleteEnabled == isSilentDeleteEnabled &&
            other.selectedLanguageTag == selectedLanguageTag &&
            other.swipeLeftAction == swipeLeftAction &&
            other.swipeRightAction == swipeRightAction &&
            other.swipeUpAction == swipeUpAction &&
            other.swipeDownAction == swipeDownAction &&
            other.defaultBehaviorNoticeMode == defaultBehaviorNoticeMode &&
            other.defaultBehaviorNoticeShownMonth ==
                defaultBehaviorNoticeShownMonth &&
            other.defaultBehaviorNoticeShownCount ==
                defaultBehaviorNoticeShownCount &&
            other.deferredUpdateVersion == deferredUpdateVersion &&
            other.cardActionButtonDefaultsInitialized ==
                cardActionButtonDefaultsInitialized &&
            listEquality.equals(
              other.silentDeleteTreeUris,
              silentDeleteTreeUris,
            );
  }

  @override
  int get hashCode {
    return Object.hashAll(<Object?>[
      swipeSensitivity,
      gestureBallSize,
      isSwipeDeleteEnabled,
      isDeleteReminderEnabled,
      showFullImage,
      showCardActionsButton,
      isTapImageToggleEnabled,
      showFloatingDeleteButton,
      isGestureBallEnabled,
      isGestureBallFeedbackEnabled,
      showGestureBallActionHint,
      isSilentDeleteEnabled,
      selectedLanguageTag,
      swipeLeftAction,
      swipeRightAction,
      swipeUpAction,
      swipeDownAction,
      defaultBehaviorNoticeMode,
      defaultBehaviorNoticeShownMonth,
      defaultBehaviorNoticeShownCount,
      deferredUpdateVersion,
      cardActionButtonDefaultsInitialized,
      Object.hashAll(silentDeleteTreeUris),
    ]);
  }
}

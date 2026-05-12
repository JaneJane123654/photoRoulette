// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Photo Roulette';

  @override
  String get startupReadyMessage => 'Bootstrap shell ready';

  @override
  String get homeHeaderTitle => 'Photo Roulette';

  @override
  String get homeHeaderDescription =>
      'Randomly browse your gallery with one-handed gestures. Open Settings below to customize behavior.';

  @override
  String get loading => 'Building your next random deck...';

  @override
  String get emptyStateTitle => 'You\'re all caught up!';

  @override
  String get emptyStateMessage =>
      'There are no photos left to browse right now. Take a few new shots and come back for another round.';

  @override
  String get permissionRequestAction => 'Continue';

  @override
  String get permissionMaybeLaterAction => 'Maybe Later';

  @override
  String get permissionOpenSettingsAction => 'Open Settings';

  @override
  String get permissionDeniedTitle => 'Photo access is turned off';

  @override
  String get permissionDeniedMessage =>
      'Photo Roulette needs access to your photo library so it can build the shuffle deck. Open system settings to allow photo access for this app.';

  @override
  String get permissionSettingsOpenFailedMessage => 'Unable to open settings.';

  @override
  String get permissionRationaleTitle => 'Shuffle your gallery on your terms';

  @override
  String get permissionRationaleDescriptionPrimary =>
      'Photo Roulette needs photo access to build a random deck from your library. On Android 14, you can allow all photos for the broadest shuffle, or share only selected photos and albums if that feels better.';

  @override
  String get permissionRationaleDescriptionSecondary =>
      'Whatever you choose is valid. The app only shuffles and shows the photos Android lets it see, and nothing leaves your device.';

  @override
  String get permissionFeatureAllPhotosIconDescription =>
      'Shuffle all photos option';

  @override
  String get permissionFeatureAllPhotosTitle =>
      'Best random mix with full access';

  @override
  String get permissionFeatureAllPhotosBody =>
      'Giving access to all photos unlocks the widest range of surprises across your whole library.';

  @override
  String get permissionFeatureSelectedIconDescription =>
      'Selected photos and albums option';

  @override
  String get permissionFeatureSelectedTitle => 'Selected photos still work';

  @override
  String get permissionFeatureSelectedBody =>
      'Prefer a smaller set? Android 14 can limit access to only the photos or albums you choose, and Photo Roulette will stay inside that boundary.';

  @override
  String get permissionFeatureControlIconDescription =>
      'Permission control reminder';

  @override
  String get permissionFeatureControlTitle => 'You stay in control';

  @override
  String get permissionFeatureControlBody =>
      'You can revisit the permission choice later in system settings if you want to broaden or narrow what the app can shuffle.';

  @override
  String get permissionIllustrationCollectionDescription =>
      'A photo collection card in the permission illustration';

  @override
  String get permissionIllustrationShuffleDescription =>
      'A shuffle card in the permission illustration';

  @override
  String get permissionIllustrationLibraryDescription =>
      'A highlighted photo access card in the permission illustration';

  @override
  String get partialAccessTitle => 'Browsing a selected subset';

  @override
  String get partialAccessDescription =>
      'Only the photos Android currently shares are in this deck. Expand access if you want a wider shuffle.';

  @override
  String get partialAccessExpand => 'Expand';

  @override
  String get galleryPlaceholderPartialAccess => 'Selected photo access granted';

  @override
  String get galleryPlaceholderFullAccess => 'Full photo access granted';

  @override
  String get galleryPlaceholderMessage =>
      'Permission gate passed. The real gallery deck will replace this temporary shell in the deck migration step.';

  @override
  String get refreshButtonContentDescription => 'Reshuffle your photo deck';

  @override
  String get refreshButtonLabel => 'Reshuffle';

  @override
  String get partialAccessIconContentDescription =>
      'Partial photo access enabled';

  @override
  String get swipeActionSkip => 'Skip / no action';

  @override
  String get swipeActionPrevious => 'Previous photo';

  @override
  String get swipeActionNext => 'Next photo';

  @override
  String get photoContentDescription => 'Photo from your library';

  @override
  String get photoLoadErrorTitle => 'This photo couldn\'t be loaded.';

  @override
  String get photoLoadingTitle => 'Loading photo...';

  @override
  String get photoLoadErrorDescription =>
      'You can keep swiping and the deck will move on to the next image.';

  @override
  String get photoLoadingDescription =>
      'Coil is fetching a down-sampled version to keep swiping smooth and memory-safe.';

  @override
  String get settingsLabel => 'Settings';

  @override
  String get retryAction => 'Retry';

  @override
  String get cancelAction => 'Cancel';

  @override
  String get confirmAction => 'Confirm';

  @override
  String get updateAvailableTitle => 'New version available';

  @override
  String get deleteFailedMessage => 'Unable to delete photo. Please try again.';
}

import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_fr.dart';
import 'app_localizations_ru.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'generated/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('ru'),
    Locale('zh'),
  ];

  /// Application title from the native app_name string.
  ///
  /// In en, this message translates to:
  /// **'Photo Roulette'**
  String get appTitle;

  /// Temporary startup placeholder shown before feature pages are migrated.
  ///
  /// In en, this message translates to:
  /// **'Bootstrap shell ready'**
  String get startupReadyMessage;

  /// Main header title from home_header_title.
  ///
  /// In en, this message translates to:
  /// **'Photo Roulette'**
  String get homeHeaderTitle;

  /// Main header description from home_header_description.
  ///
  /// In en, this message translates to:
  /// **'Randomly browse your gallery with one-handed gestures. Open Settings below to customize behavior.'**
  String get homeHeaderDescription;

  /// Shared loading message seeded from loading_deck.
  ///
  /// In en, this message translates to:
  /// **'Building your next random deck...'**
  String get loading;

  /// Generic empty gallery/deck title.
  ///
  /// In en, this message translates to:
  /// **'You\'re all caught up!'**
  String get emptyStateTitle;

  /// Generic empty gallery/deck message.
  ///
  /// In en, this message translates to:
  /// **'There are no photos left to browse right now. Take a few new shots and come back for another round.'**
  String get emptyStateMessage;

  /// Primary action that starts the photo permission request.
  ///
  /// In en, this message translates to:
  /// **'Continue'**
  String get permissionRequestAction;

  /// Secondary action that dismisses the permission rationale.
  ///
  /// In en, this message translates to:
  /// **'Maybe Later'**
  String get permissionMaybeLaterAction;

  /// Action that opens system settings for permission recovery.
  ///
  /// In en, this message translates to:
  /// **'Open Settings'**
  String get permissionOpenSettingsAction;

  /// Permission denied state title.
  ///
  /// In en, this message translates to:
  /// **'Photo access is turned off'**
  String get permissionDeniedTitle;

  /// Permission denied state body.
  ///
  /// In en, this message translates to:
  /// **'Photo Roulette needs access to your photo library so it can build the shuffle deck. Open system settings to allow photo access for this app.'**
  String get permissionDeniedMessage;

  /// Shown when system app settings cannot be opened.
  ///
  /// In en, this message translates to:
  /// **'Unable to open settings.'**
  String get permissionSettingsOpenFailedMessage;

  /// Permission rationale title from permission_rationale_title.
  ///
  /// In en, this message translates to:
  /// **'Shuffle your gallery on your terms'**
  String get permissionRationaleTitle;

  /// Primary permission rationale copy.
  ///
  /// In en, this message translates to:
  /// **'Photo Roulette needs photo access to build a random deck from your library. On Android 14, you can allow all photos for the broadest shuffle, or share only selected photos and albums if that feels better.'**
  String get permissionRationaleDescriptionPrimary;

  /// Secondary permission rationale copy.
  ///
  /// In en, this message translates to:
  /// **'Whatever you choose is valid. The app only shuffles and shows the photos Android lets it see, and nothing leaves your device.'**
  String get permissionRationaleDescriptionSecondary;

  /// No description provided for @permissionFeatureAllPhotosIconDescription.
  ///
  /// In en, this message translates to:
  /// **'Shuffle all photos option'**
  String get permissionFeatureAllPhotosIconDescription;

  /// No description provided for @permissionFeatureAllPhotosTitle.
  ///
  /// In en, this message translates to:
  /// **'Best random mix with full access'**
  String get permissionFeatureAllPhotosTitle;

  /// No description provided for @permissionFeatureAllPhotosBody.
  ///
  /// In en, this message translates to:
  /// **'Giving access to all photos unlocks the widest range of surprises across your whole library.'**
  String get permissionFeatureAllPhotosBody;

  /// No description provided for @permissionFeatureSelectedIconDescription.
  ///
  /// In en, this message translates to:
  /// **'Selected photos and albums option'**
  String get permissionFeatureSelectedIconDescription;

  /// No description provided for @permissionFeatureSelectedTitle.
  ///
  /// In en, this message translates to:
  /// **'Selected photos still work'**
  String get permissionFeatureSelectedTitle;

  /// No description provided for @permissionFeatureSelectedBody.
  ///
  /// In en, this message translates to:
  /// **'Prefer a smaller set? Android 14 can limit access to only the photos or albums you choose, and Photo Roulette will stay inside that boundary.'**
  String get permissionFeatureSelectedBody;

  /// No description provided for @permissionFeatureControlIconDescription.
  ///
  /// In en, this message translates to:
  /// **'Permission control reminder'**
  String get permissionFeatureControlIconDescription;

  /// No description provided for @permissionFeatureControlTitle.
  ///
  /// In en, this message translates to:
  /// **'You stay in control'**
  String get permissionFeatureControlTitle;

  /// No description provided for @permissionFeatureControlBody.
  ///
  /// In en, this message translates to:
  /// **'You can revisit the permission choice later in system settings if you want to broaden or narrow what the app can shuffle.'**
  String get permissionFeatureControlBody;

  /// No description provided for @permissionIllustrationCollectionDescription.
  ///
  /// In en, this message translates to:
  /// **'A photo collection card in the permission illustration'**
  String get permissionIllustrationCollectionDescription;

  /// No description provided for @permissionIllustrationShuffleDescription.
  ///
  /// In en, this message translates to:
  /// **'A shuffle card in the permission illustration'**
  String get permissionIllustrationShuffleDescription;

  /// No description provided for @permissionIllustrationLibraryDescription.
  ///
  /// In en, this message translates to:
  /// **'A highlighted photo access card in the permission illustration'**
  String get permissionIllustrationLibraryDescription;

  /// No description provided for @partialAccessTitle.
  ///
  /// In en, this message translates to:
  /// **'Browsing a selected subset'**
  String get partialAccessTitle;

  /// No description provided for @partialAccessDescription.
  ///
  /// In en, this message translates to:
  /// **'Only the photos Android currently shares are in this deck. Expand access if you want a wider shuffle.'**
  String get partialAccessDescription;

  /// No description provided for @partialAccessExpand.
  ///
  /// In en, this message translates to:
  /// **'Expand'**
  String get partialAccessExpand;

  /// No description provided for @galleryPlaceholderPartialAccess.
  ///
  /// In en, this message translates to:
  /// **'Selected photo access granted'**
  String get galleryPlaceholderPartialAccess;

  /// No description provided for @galleryPlaceholderFullAccess.
  ///
  /// In en, this message translates to:
  /// **'Full photo access granted'**
  String get galleryPlaceholderFullAccess;

  /// No description provided for @galleryPlaceholderMessage.
  ///
  /// In en, this message translates to:
  /// **'Permission gate passed. The real gallery deck will replace this temporary shell in the deck migration step.'**
  String get galleryPlaceholderMessage;

  /// No description provided for @refreshButtonContentDescription.
  ///
  /// In en, this message translates to:
  /// **'Reshuffle your photo deck'**
  String get refreshButtonContentDescription;

  /// No description provided for @refreshButtonLabel.
  ///
  /// In en, this message translates to:
  /// **'Reshuffle'**
  String get refreshButtonLabel;

  /// No description provided for @partialAccessIconContentDescription.
  ///
  /// In en, this message translates to:
  /// **'Partial photo access enabled'**
  String get partialAccessIconContentDescription;

  /// No description provided for @swipeActionSkip.
  ///
  /// In en, this message translates to:
  /// **'Skip / no action'**
  String get swipeActionSkip;

  /// No description provided for @swipeActionPrevious.
  ///
  /// In en, this message translates to:
  /// **'Previous photo'**
  String get swipeActionPrevious;

  /// No description provided for @swipeActionNext.
  ///
  /// In en, this message translates to:
  /// **'Next photo'**
  String get swipeActionNext;

  /// No description provided for @photoContentDescription.
  ///
  /// In en, this message translates to:
  /// **'Photo from your library'**
  String get photoContentDescription;

  /// No description provided for @photoLoadErrorTitle.
  ///
  /// In en, this message translates to:
  /// **'This photo couldn\'t be loaded.'**
  String get photoLoadErrorTitle;

  /// No description provided for @photoLoadingTitle.
  ///
  /// In en, this message translates to:
  /// **'Loading photo...'**
  String get photoLoadingTitle;

  /// No description provided for @photoLoadErrorDescription.
  ///
  /// In en, this message translates to:
  /// **'You can keep swiping and the deck will move on to the next image.'**
  String get photoLoadErrorDescription;

  /// No description provided for @photoLoadingDescription.
  ///
  /// In en, this message translates to:
  /// **'Coil is fetching a down-sampled version to keep swiping smooth and memory-safe.'**
  String get photoLoadingDescription;

  /// Shared settings label.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settingsLabel;

  /// Shared retry action label.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retryAction;

  /// Shared cancel action label.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancelAction;

  /// Shared confirm action label.
  ///
  /// In en, this message translates to:
  /// **'Confirm'**
  String get confirmAction;

  /// Update available dialog title from update_dialog_title.
  ///
  /// In en, this message translates to:
  /// **'New version available'**
  String get updateAvailableTitle;

  /// Shared delete failure message for later delete flows.
  ///
  /// In en, this message translates to:
  /// **'Unable to delete photo. Please try again.'**
  String get deleteFailedMessage;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) => <String>[
    'ar',
    'en',
    'es',
    'fr',
    'ru',
    'zh',
  ].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'fr':
      return AppLocalizationsFr();
    case 'ru':
      return AppLocalizationsRu();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}

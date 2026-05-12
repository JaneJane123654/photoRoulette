// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appTitle => 'Photo Roulette';

  @override
  String get startupReadyMessage => 'Structure de démarrage prête';

  @override
  String get homeHeaderTitle => 'Photo Roulette';

  @override
  String get homeHeaderDescription =>
      'Parcourez votre galerie aléatoirement avec des gestes à une main. Ouvrez les paramètres ci-dessous pour personnaliser le comportement.';

  @override
  String get loading => 'Préparation de votre prochaine pile aléatoire...';

  @override
  String get emptyStateTitle => 'Vous avez tout parcouru !';

  @override
  String get emptyStateMessage =>
      'Il n\'y a plus de photos à parcourir pour le moment. Prenez-en quelques nouvelles et revenez pour une autre session.';

  @override
  String get permissionRequestAction => 'Continuer';

  @override
  String get permissionMaybeLaterAction => 'Plus tard';

  @override
  String get permissionOpenSettingsAction => 'Ouvrir les paramètres';

  @override
  String get permissionDeniedTitle => 'L\'accès aux photos est désactivé';

  @override
  String get permissionDeniedMessage =>
      'Photo Roulette a besoin d\'accéder à votre photothèque pour créer la pile aléatoire. Ouvrez les paramètres système pour autoriser l\'accès aux photos pour cette app.';

  @override
  String get permissionSettingsOpenFailedMessage =>
      'Impossible d\'ouvrir les paramètres.';

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
      'Option de mélange de toutes les photos';

  @override
  String get permissionFeatureAllPhotosTitle =>
      'Meilleur mélange aléatoire avec accès complet';

  @override
  String get permissionFeatureAllPhotosBody =>
      'Autoriser l\'accès à toutes les photos offre la plus grande variété de surprises dans toute votre photothèque.';

  @override
  String get permissionFeatureSelectedIconDescription =>
      'Option de photos et albums sélectionnés';

  @override
  String get permissionFeatureSelectedTitle =>
      'Les photos sélectionnées fonctionnent aussi';

  @override
  String get permissionFeatureSelectedBody =>
      'Vous préférez un ensemble plus petit ? Android 14 peut limiter l\'accès aux photos ou albums que vous choisissez, et Photo Roulette restera dans cette limite.';

  @override
  String get permissionFeatureControlIconDescription =>
      'Rappel de contrôle des permissions';

  @override
  String get permissionFeatureControlTitle => 'Vous gardez le contrôle';

  @override
  String get permissionFeatureControlBody =>
      'Vous pouvez revoir ce choix plus tard dans les paramètres système si vous voulez élargir ou réduire ce que l\'app peut mélanger.';

  @override
  String get permissionIllustrationCollectionDescription =>
      'Une carte de collection de photos dans l\'illustration de permission';

  @override
  String get permissionIllustrationShuffleDescription =>
      'Une carte de mélange dans l\'illustration de permission';

  @override
  String get permissionIllustrationLibraryDescription =>
      'Une carte mise en avant pour l\'accès aux photos dans l\'illustration de permission';

  @override
  String get partialAccessTitle =>
      'Navigation dans un sous-ensemble sélectionné';

  @override
  String get partialAccessDescription =>
      'Cette pile contient seulement les photos actuellement partagées par Android. Étendez l\'accès si vous voulez un mélange plus large.';

  @override
  String get partialAccessExpand => 'Étendre';

  @override
  String get galleryPlaceholderPartialAccess =>
      'Accès aux photos sélectionnées accordé';

  @override
  String get galleryPlaceholderFullAccess => 'Accès complet aux photos accordé';

  @override
  String get galleryPlaceholderMessage =>
      'Le portail de permission est franchi. La vraie pile de galerie remplacera cet écran temporaire lors de la migration de la pile.';

  @override
  String get refreshButtonContentDescription =>
      'Remélanger votre pile de photos';

  @override
  String get refreshButtonLabel => 'Remélanger';

  @override
  String get partialAccessIconContentDescription =>
      'Accès partiel aux photos activé';

  @override
  String get swipeActionSkip => 'Ignorer / aucune action';

  @override
  String get swipeActionPrevious => 'Photo précédente';

  @override
  String get swipeActionNext => 'Photo suivante';

  @override
  String get photoContentDescription => 'Photo from your library';

  @override
  String get photoLoadErrorTitle => 'This photo could not be loaded.';

  @override
  String get photoLoadingTitle => 'Loading photo...';

  @override
  String get photoLoadErrorDescription =>
      'You can keep swiping and the deck will move on to the next image.';

  @override
  String get photoLoadingDescription =>
      'Coil is fetching a down-sampled version to keep swiping smooth and memory-safe.';

  @override
  String get settingsLabel => 'Paramètres';

  @override
  String get retryAction => 'Réessayer';

  @override
  String get cancelAction => 'Annuler';

  @override
  String get confirmAction => 'Confirmer';

  @override
  String get updateAvailableTitle => 'Nouvelle version disponible';

  @override
  String get deleteFailedMessage =>
      'Impossible de supprimer la photo. Réessayez.';
}

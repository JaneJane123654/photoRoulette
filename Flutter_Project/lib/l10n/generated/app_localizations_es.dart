// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get appTitle => 'Ruleta de Fotos';

  @override
  String get startupReadyMessage => 'Estructura de inicio lista';

  @override
  String get homeHeaderTitle => 'Ruleta de Fotos';

  @override
  String get homeHeaderDescription =>
      'Desliza a la izquierda o derecha para omitir. Desliza hacia arriba para eliminar cuando ese modo esté activado.';

  @override
  String get loading => 'Preparando tu próximo mazo aleatorio...';

  @override
  String get emptyStateTitle => '¡Ya terminaste!';

  @override
  String get emptyStateMessage =>
      'No quedan fotos para explorar por ahora. Toma algunas nuevas y vuelve para otra ronda.';

  @override
  String get permissionRequestAction => 'Continuar';

  @override
  String get permissionMaybeLaterAction => 'Quizás después';

  @override
  String get permissionOpenSettingsAction => 'Abrir ajustes';

  @override
  String get permissionDeniedTitle => 'El acceso a fotos está desactivado';

  @override
  String get permissionDeniedMessage =>
      'Ruleta de Fotos necesita acceso a tu biblioteca para crear el mazo aleatorio. Abre ajustes del sistema para permitir el acceso a fotos de esta app.';

  @override
  String get permissionSettingsOpenFailedMessage => 'No se pudo abrir ajustes.';

  @override
  String get permissionRationaleTitle => 'Mezcla tu galería a tu manera';

  @override
  String get permissionRationaleDescriptionPrimary =>
      'Ruleta de Fotos necesita acceso a las fotos para crear un mazo aleatorio de tu biblioteca. En Android 14, puedes permitir todas las fotos para una mezcla más amplia o compartir solo fotos y álbumes seleccionados.';

  @override
  String get permissionRationaleDescriptionSecondary =>
      'Cualquier opción es válida. La app solo mezcla y muestra las fotos que Android le permite ver, y nada sale de tu dispositivo.';

  @override
  String get permissionFeatureAllPhotosIconDescription =>
      'Opción para mezclar todas las fotos';

  @override
  String get permissionFeatureAllPhotosTitle =>
      'La mejor mezcla aleatoria con acceso completo';

  @override
  String get permissionFeatureAllPhotosBody =>
      'Dar acceso a todas las fotos desbloquea la variedad más amplia de sorpresas en toda tu biblioteca.';

  @override
  String get permissionFeatureSelectedIconDescription =>
      'Opción de fotos y álbumes seleccionados';

  @override
  String get permissionFeatureSelectedTitle =>
      'Las fotos seleccionadas también funcionan';

  @override
  String get permissionFeatureSelectedBody =>
      '¿Prefieres un conjunto más pequeño? Android 14 puede limitar el acceso solo a las fotos o álbumes que elijas, y Ruleta de Fotos respetará ese límite.';

  @override
  String get permissionFeatureControlIconDescription =>
      'Recordatorio de control de permisos';

  @override
  String get permissionFeatureControlTitle => 'Tú tienes el control';

  @override
  String get permissionFeatureControlBody =>
      'Puedes revisar la elección del permiso más tarde en los ajustes del sistema si quieres ampliar o reducir lo que la app puede mezclar.';

  @override
  String get permissionIllustrationCollectionDescription =>
      'Una tarjeta de colección de fotos en la ilustración del permiso';

  @override
  String get permissionIllustrationShuffleDescription =>
      'Una tarjeta de mezcla en la ilustración del permiso';

  @override
  String get permissionIllustrationLibraryDescription =>
      'Una tarjeta destacada de acceso a fotos en la ilustración del permiso';

  @override
  String get partialAccessTitle => 'Navegando un subconjunto seleccionado';

  @override
  String get partialAccessDescription =>
      'Solo las fotos que Android comparte actualmente están en este mazo. Amplía el acceso si quieres una mezcla más amplia.';

  @override
  String get partialAccessExpand => 'Ampliar';

  @override
  String get galleryPlaceholderPartialAccess =>
      'Acceso a fotos seleccionadas concedido';

  @override
  String get galleryPlaceholderFullAccess =>
      'Acceso completo a fotos concedido';

  @override
  String get galleryPlaceholderMessage =>
      'La puerta de permisos ya se superó. El mazo real de la galería reemplazará esta pantalla temporal en la migración del mazo.';

  @override
  String get refreshButtonContentDescription =>
      'Mezclar de nuevo tu mazo de fotos';

  @override
  String get refreshButtonLabel => 'Mezclar de nuevo';

  @override
  String get partialAccessIconContentDescription =>
      'Acceso parcial a fotos activado';

  @override
  String get swipeActionSkip => 'Omitir / sin acción';

  @override
  String get swipeActionPrevious => 'Foto anterior';

  @override
  String get swipeActionNext => 'Foto siguiente';

  @override
  String get photoContentDescription => 'Foto de tu biblioteca';

  @override
  String get photoLoadErrorTitle => 'No se pudo cargar esta foto.';

  @override
  String get photoLoadingTitle => 'Cargando foto...';

  @override
  String get photoLoadErrorDescription =>
      'Puedes seguir deslizando y el mazo pasará a la siguiente imagen.';

  @override
  String get photoLoadingDescription =>
      'Coil está cargando una versión reducida para mantener el deslizamiento fluido y seguro en memoria.';

  @override
  String get settingsLabel => 'Ajustes';

  @override
  String get retryAction => 'Reintentar';

  @override
  String get cancelAction => 'Cancelar';

  @override
  String get confirmAction => 'Confirmar';

  @override
  String get updateAvailableTitle => 'Nueva versión disponible';

  @override
  String get deleteFailedMessage =>
      'No se pudo eliminar la foto. Inténtalo de nuevo.';
}

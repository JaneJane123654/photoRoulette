// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'Фото Рулетка';

  @override
  String get startupReadyMessage => 'Стартовая оболочка готова';

  @override
  String get homeHeaderTitle => 'Фото Рулетка';

  @override
  String get homeHeaderDescription =>
      'Проведите влево или вправо, чтобы пропустить. Проведите вверх, чтобы удалить, если этот режим включён.';

  @override
  String get loading => 'Готовим вашу следующую случайную колоду...';

  @override
  String get emptyStateTitle => 'Вы всё просмотрели!';

  @override
  String get emptyStateMessage =>
      'Сейчас больше нет фотографий для просмотра. Сделайте новые снимки и возвращайтесь за следующим раундом.';

  @override
  String get permissionRequestAction => 'Продолжить';

  @override
  String get permissionMaybeLaterAction => 'Позже';

  @override
  String get permissionOpenSettingsAction => 'Открыть настройки';

  @override
  String get permissionDeniedTitle => 'Доступ к фото отключён';

  @override
  String get permissionDeniedMessage =>
      'Фото Рулетка нужен доступ к вашей библиотеке, чтобы собрать случайную колоду. Откройте системные настройки и разрешите доступ к фото для этого приложения.';

  @override
  String get permissionSettingsOpenFailedMessage =>
      'Не удалось открыть настройки.';

  @override
  String get permissionRationaleTitle => 'Перемешивайте галерею по-своему';

  @override
  String get permissionRationaleDescriptionPrimary =>
      'Фото Рулетка нужен доступ к фотографиям, чтобы собрать случайную колоду из вашей библиотеки. На Android 14 можно разрешить все фото для максимального перемешивания или открыть только выбранные фото и альбомы.';

  @override
  String get permissionRationaleDescriptionSecondary =>
      'Любой выбор правильный. Приложение перемешивает и показывает только те фото, которые Android разрешает видеть, и ничего не покидает устройство.';

  @override
  String get permissionFeatureAllPhotosIconDescription =>
      'Вариант перемешивания всех фото';

  @override
  String get permissionFeatureAllPhotosTitle =>
      'Лучший случайный микс с полным доступом';

  @override
  String get permissionFeatureAllPhotosBody =>
      'Доступ ко всем фото открывает самый широкий набор неожиданных снимков из всей библиотеки.';

  @override
  String get permissionFeatureSelectedIconDescription =>
      'Вариант выбранных фото и альбомов';

  @override
  String get permissionFeatureSelectedTitle => 'Выбранные фото тоже подходят';

  @override
  String get permissionFeatureSelectedBody =>
      'Нужен меньший набор? Android 14 может ограничить доступ только выбранными фото или альбомами, и Фото Рулетка останется в этих границах.';

  @override
  String get permissionFeatureControlIconDescription =>
      'Напоминание об управлении разрешением';

  @override
  String get permissionFeatureControlTitle => 'Контроль за вами';

  @override
  String get permissionFeatureControlBody =>
      'Позже можно вернуться к выбору разрешений в системных настройках, чтобы расширить или сузить то, что приложение может перемешивать.';

  @override
  String get permissionIllustrationCollectionDescription =>
      'Карточка коллекции фото в иллюстрации разрешения';

  @override
  String get permissionIllustrationShuffleDescription =>
      'Карточка перемешивания в иллюстрации разрешения';

  @override
  String get permissionIllustrationLibraryDescription =>
      'Выделенная карточка доступа к фото в иллюстрации разрешения';

  @override
  String get partialAccessTitle => 'Просмотр выбранной части';

  @override
  String get partialAccessDescription =>
      'В этой колоде только фото, которыми сейчас делится Android. Расширьте доступ для более широкого перемешивания.';

  @override
  String get partialAccessExpand => 'Расширить';

  @override
  String get galleryPlaceholderPartialAccess =>
      'Доступ к выбранным фото разрешён';

  @override
  String get galleryPlaceholderFullAccess => 'Полный доступ к фото разрешён';

  @override
  String get galleryPlaceholderMessage =>
      'Экран разрешений пройден. Настоящая колода галереи заменит эту временную оболочку на этапе миграции колоды.';

  @override
  String get refreshButtonContentDescription =>
      'Перемешать колоду фотографий заново';

  @override
  String get refreshButtonLabel => 'Перемешать';

  @override
  String get partialAccessIconContentDescription =>
      'Включён частичный доступ к фото';

  @override
  String get swipeActionSkip => 'Пропуск / без действия';

  @override
  String get swipeActionPrevious => 'Предыдущее фото';

  @override
  String get swipeActionNext => 'Следующее фото';

  @override
  String get photoContentDescription => 'Фото из вашей библиотеки';

  @override
  String get photoLoadErrorTitle => 'Не удалось загрузить это фото.';

  @override
  String get photoLoadingTitle => 'Загрузка фото...';

  @override
  String get photoLoadErrorDescription =>
      'Можно продолжать смахивать, и колода перейдёт к следующему изображению.';

  @override
  String get photoLoadingDescription =>
      'Coil загружает уменьшенную версию, чтобы смахивание оставалось плавным и экономным по памяти.';

  @override
  String get settingsLabel => 'Настройки';

  @override
  String get retryAction => 'Повторить';

  @override
  String get cancelAction => 'Отмена';

  @override
  String get confirmAction => 'Подтвердить';

  @override
  String get updateAvailableTitle => 'Доступна новая версия';

  @override
  String get deleteFailedMessage =>
      'Не удалось удалить фото. Повторите попытку.';
}

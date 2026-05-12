// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get appTitle => 'روليت الصور';

  @override
  String get startupReadyMessage => 'هيكل بدء التشغيل جاهز';

  @override
  String get homeHeaderTitle => 'روليت الصور';

  @override
  String get homeHeaderDescription =>
      'اسحب لليسار أو اليمين للتخطي. اسحب للأعلى للحذف عند تفعيل هذا الوضع.';

  @override
  String get loading => 'جارٍ إعداد مجموعة عشوائية جديدة...';

  @override
  String get emptyStateTitle => 'انتهيت من التصفح!';

  @override
  String get emptyStateMessage =>
      'لا توجد صور أخرى للتصفح حالياً. التقط بعض الصور الجديدة ثم عد لجولة أخرى.';

  @override
  String get permissionRequestAction => 'متابعة';

  @override
  String get permissionMaybeLaterAction => 'لاحقاً';

  @override
  String get permissionOpenSettingsAction => 'فتح الإعدادات';

  @override
  String get permissionDeniedTitle => 'تم إيقاف الوصول إلى الصور';

  @override
  String get permissionDeniedMessage =>
      'يحتاج روليت الصور إلى الوصول إلى مكتبة الصور لبناء مجموعة الخلط. افتح إعدادات النظام للسماح للتطبيق بالوصول إلى الصور.';

  @override
  String get permissionSettingsOpenFailedMessage => 'تعذر فتح الإعدادات.';

  @override
  String get permissionRationaleTitle => 'اخلط معرضك بالطريقة التي تريدها';

  @override
  String get permissionRationaleDescriptionPrimary =>
      'يحتاج روليت الصور إلى إذن الوصول للصور لبناء مجموعة عشوائية من مكتبتك. في Android 14 يمكنك السماح بكل الصور للحصول على أوسع خلط، أو مشاركة صور وألبومات محددة فقط.';

  @override
  String get permissionRationaleDescriptionSecondary =>
      'أي اختيار تقوم به صحيح. التطبيق يخلط ويعرض فقط الصور التي يسمح Android برؤيتها، ولا يغادر أي شيء جهازك.';

  @override
  String get permissionFeatureAllPhotosIconDescription => 'خيار خلط كل الصور';

  @override
  String get permissionFeatureAllPhotosTitle =>
      'أفضل خلط عشوائي مع الوصول الكامل';

  @override
  String get permissionFeatureAllPhotosBody =>
      'منح الوصول إلى كل الصور يفتح أوسع نطاق من المفاجآت في مكتبتك كلها.';

  @override
  String get permissionFeatureSelectedIconDescription =>
      'خيار الصور والألبومات المحددة';

  @override
  String get permissionFeatureSelectedTitle => 'الصور المحددة تعمل أيضاً';

  @override
  String get permissionFeatureSelectedBody =>
      'تفضل مجموعة أصغر؟ يمكن لـ Android 14 تقييد الوصول إلى الصور أو الألبومات التي تختارها فقط، وسيبقى روليت الصور داخل هذا الحد.';

  @override
  String get permissionFeatureControlIconDescription =>
      'تذكير بالتحكم في الإذن';

  @override
  String get permissionFeatureControlTitle => 'أنت المتحكم';

  @override
  String get permissionFeatureControlBody =>
      'يمكنك الرجوع إلى اختيار الإذن لاحقاً من إعدادات النظام إذا أردت توسيع أو تضييق ما يمكن للتطبيق خلطه.';

  @override
  String get permissionIllustrationCollectionDescription =>
      'بطاقة مجموعة صور في توضيح الإذن';

  @override
  String get permissionIllustrationShuffleDescription =>
      'بطاقة خلط في توضيح الإذن';

  @override
  String get permissionIllustrationLibraryDescription =>
      'بطاقة وصول صور مميزة في توضيح الإذن';

  @override
  String get partialAccessTitle => 'تصفح مجموعة محددة';

  @override
  String get partialAccessDescription =>
      'تحتوي هذه المجموعة فقط على الصور التي يشاركها Android حالياً. وسّع الوصول إذا أردت خلطاً أوسع.';

  @override
  String get partialAccessExpand => 'توسيع';

  @override
  String get galleryPlaceholderPartialAccess => 'تم منح وصول للصور المحددة';

  @override
  String get galleryPlaceholderFullAccess => 'تم منح وصول كامل للصور';

  @override
  String get galleryPlaceholderMessage =>
      'تم اجتياز بوابة الإذن. سيحل سطح معرض الصور الحقيقي محل هذه الواجهة المؤقتة في خطوة ترحيل سطح البطاقات.';

  @override
  String get refreshButtonContentDescription => 'إعادة خلط مجموعة الصور';

  @override
  String get refreshButtonLabel => 'إعادة خلط';

  @override
  String get partialAccessIconContentDescription => 'تم تفعيل وصول جزئي للصور';

  @override
  String get swipeActionSkip => 'تخطي / بدون إجراء';

  @override
  String get swipeActionPrevious => 'الصورة السابقة';

  @override
  String get swipeActionNext => 'الصورة التالية';

  @override
  String get photoContentDescription => 'صورة من مكتبتك';

  @override
  String get photoLoadErrorTitle => 'تعذر تحميل هذه الصورة.';

  @override
  String get photoLoadingTitle => 'جارٍ تحميل الصورة...';

  @override
  String get photoLoadErrorDescription =>
      'يمكنك متابعة السحب، وستنتقل المجموعة إلى الصورة التالية.';

  @override
  String get photoLoadingDescription =>
      'يقوم Coil بجلب نسخة منخفضة الدقة للحفاظ على سلاسة السحب وأمان الذاكرة.';

  @override
  String get settingsLabel => 'الإعدادات';

  @override
  String get retryAction => 'إعادة المحاولة';

  @override
  String get cancelAction => 'إلغاء';

  @override
  String get confirmAction => 'تأكيد';

  @override
  String get updateAvailableTitle => 'إصدار جديد متاح';

  @override
  String get deleteFailedMessage => 'تعذر حذف الصورة. حاول مرة أخرى.';
}

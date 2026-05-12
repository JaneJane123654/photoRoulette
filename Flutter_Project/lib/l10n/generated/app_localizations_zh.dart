// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appTitle => '照片轮盘';

  @override
  String get startupReadyMessage => '启动壳已就绪';

  @override
  String get homeHeaderTitle => '照片轮盘';

  @override
  String get homeHeaderDescription => '支持单手手势随机浏览。点击下方“设置”即可自定义各项行为。';

  @override
  String get loading => '正在构建新的随机卡组...';

  @override
  String get emptyStateTitle => '已经浏览完啦！';

  @override
  String get emptyStateMessage => '当前没有可浏览的照片。拍几张新照片后再回来继续随机浏览吧。';

  @override
  String get permissionRequestAction => '继续';

  @override
  String get permissionMaybeLaterAction => '稍后再说';

  @override
  String get permissionOpenSettingsAction => '打开设置';

  @override
  String get permissionDeniedTitle => '照片访问权限已关闭';

  @override
  String get permissionDeniedMessage =>
      '照片轮盘需要访问你的图库才能构建随机卡组。请在系统设置中为此应用开启照片权限。';

  @override
  String get permissionSettingsOpenFailedMessage => '无法打开设置。';

  @override
  String get permissionRationaleTitle => '按你的方式随机浏览图库';

  @override
  String get permissionRationaleDescriptionPrimary =>
      '照片轮盘需要访问照片权限，才能从你的图库中生成随机卡组。在 Android 14 上，你可以授权全部照片以获得最广泛的随机效果，也可以仅共享选中的照片或相册。';

  @override
  String get permissionRationaleDescriptionSecondary =>
      '无论你如何选择都可以。应用只会随机展示 Android 允许访问的照片，且不会上传任何内容到设备外。';

  @override
  String get permissionFeatureAllPhotosIconDescription => '随机全部照片选项';

  @override
  String get permissionFeatureAllPhotosTitle => '完整访问可获得最佳随机范围';

  @override
  String get permissionFeatureAllPhotosBody => '授权访问全部照片后，可以在整个图库中获得最丰富的随机惊喜。';

  @override
  String get permissionFeatureSelectedIconDescription => '选中照片和相册选项';

  @override
  String get permissionFeatureSelectedTitle => '只选部分照片也可以使用';

  @override
  String get permissionFeatureSelectedBody =>
      '如果你更想使用较小范围，Android 14 可以只共享你选择的照片或相册，照片轮盘会严格停留在这个边界内。';

  @override
  String get permissionFeatureControlIconDescription => '权限控制提醒';

  @override
  String get permissionFeatureControlTitle => '控制权始终在你手里';

  @override
  String get permissionFeatureControlBody =>
      '之后你也可以在系统设置中重新调整权限，扩大或缩小应用可随机浏览的内容。';

  @override
  String get permissionIllustrationCollectionDescription => '权限插图中的照片集合卡片';

  @override
  String get permissionIllustrationShuffleDescription => '权限插图中的随机卡片';

  @override
  String get permissionIllustrationLibraryDescription => '权限插图中高亮的照片访问卡片';

  @override
  String get partialAccessTitle => '当前仅浏览已选范围';

  @override
  String get partialAccessDescription =>
      '当前卡组只包含 Android 已共享给应用的照片。若想获得更广泛的随机结果，请扩展访问权限。';

  @override
  String get partialAccessExpand => '扩展';

  @override
  String get galleryPlaceholderPartialAccess => '已获得所选照片访问权限';

  @override
  String get galleryPlaceholderFullAccess => '已获得完整照片访问权限';

  @override
  String get galleryPlaceholderMessage => '权限入口已通过。真正的图库卡组会在卡组迁移步骤中替换这个临时界面。';

  @override
  String get refreshButtonContentDescription => '重新打乱照片卡组';

  @override
  String get refreshButtonLabel => '重新打乱';

  @override
  String get partialAccessIconContentDescription => '已启用部分照片访问权限';

  @override
  String get swipeActionSkip => '跳过 / 不执行';

  @override
  String get swipeActionPrevious => '上一张图片';

  @override
  String get swipeActionNext => '下一张图片';

  @override
  String get photoContentDescription => '来自你的图库的照片';

  @override
  String get photoLoadErrorTitle => '这张照片无法加载。';

  @override
  String get photoLoadingTitle => '正在加载照片...';

  @override
  String get photoLoadErrorDescription => '你可以继续滑动，卡组会自动切换到下一张图片。';

  @override
  String get photoLoadingDescription => 'Coil 正在加载下采样版本，以保证滑动流畅并控制内存占用。';

  @override
  String get settingsLabel => '设置';

  @override
  String get retryAction => '重试';

  @override
  String get cancelAction => '取消';

  @override
  String get confirmAction => '确认';

  @override
  String get updateAvailableTitle => '发现新版本';

  @override
  String get deleteFailedMessage => '删除照片失败，请重试。';
}

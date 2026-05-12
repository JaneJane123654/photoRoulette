import '../../../core/result/app_result.dart';
import 'models/media_asset.dart';

abstract interface class GalleryRepository {
  Future<AppResult<List<MediaAsset>>> loadMediaAssets();
}

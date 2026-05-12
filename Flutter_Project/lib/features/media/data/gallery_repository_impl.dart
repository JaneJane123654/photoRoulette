import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_error.dart';
import '../../../core/logging/app_logger.dart';
import '../../../core/result/app_result.dart';
import '../domain/gallery_repository.dart';
import '../domain/models/media_asset.dart';
import 'gallery_media_data_source.dart';
import 'media_classifier.dart';
import 'raw_gallery_asset.dart';

final Provider<GalleryMediaDataSource> galleryMediaDataSourceProvider =
    Provider<GalleryMediaDataSource>((Ref ref) {
      return PhotoManagerGalleryDataSource();
    });

final Provider<MediaClassifier> mediaClassifierProvider =
    Provider<MediaClassifier>((Ref ref) {
      return const MediaClassifier();
    });

final Provider<GalleryRepository> galleryRepositoryProvider =
    Provider<GalleryRepository>((Ref ref) {
      return GalleryRepositoryImpl(
        dataSource: ref.watch(galleryMediaDataSourceProvider),
        classifier: ref.watch(mediaClassifierProvider),
      );
    });

final class GalleryRepositoryImpl implements GalleryRepository {
  const GalleryRepositoryImpl({
    required GalleryMediaDataSource dataSource,
    required MediaClassifier classifier,
    AppLogger logger = const AppLogger('GalleryRepository'),
  }) : _dataSource = dataSource,
       _classifier = classifier,
       _logger = logger;

  final GalleryMediaDataSource _dataSource;
  final MediaClassifier _classifier;
  final AppLogger _logger;

  @override
  Future<AppResult<List<MediaAsset>>> loadMediaAssets() {
    return AppResult.guard<List<MediaAsset>>(() async {
      final List<RawGalleryAsset> rawAssets = await _dataSource
          .queryMediaAssets();
      if (rawAssets.isEmpty) {
        _logger.debug('Gallery query returned no media rows.');
        return const <MediaAsset>[];
      }

      final List<MediaAsset> normalizedAssets = <MediaAsset>[];
      for (final RawGalleryAsset rawAsset in rawAssets) {
        final MediaAsset? asset = _classifier.normalize(rawAsset);
        if (asset != null) {
          normalizedAssets.add(asset);
        }
      }

      if (normalizedAssets.isEmpty) {
        _logger.debug('Gallery query had no valid media after filtering.');
        return const <MediaAsset>[];
      }

      return _sortByNativeSourceOrder(normalizedAssets);
    }, errorMapper: _mapGalleryFailure);
  }

  List<MediaAsset> _sortByNativeSourceOrder(List<MediaAsset> assets) {
    final List<_IndexedMediaAsset> indexedAssets = <_IndexedMediaAsset>[
      for (var index = 0; index < assets.length; index += 1)
        _IndexedMediaAsset(index: index, asset: assets[index]),
    ];

    indexedAssets.sort((_IndexedMediaAsset left, _IndexedMediaAsset right) {
      final int leftDateAddedSeconds = left.asset.dateAddedSeconds ?? 0;
      final int rightDateAddedSeconds = right.asset.dateAddedSeconds ?? 0;
      final int dateComparison = rightDateAddedSeconds.compareTo(
        leftDateAddedSeconds,
      );
      if (dateComparison != 0) {
        return dateComparison;
      }

      final int? leftNumericId = left.asset.numericId;
      final int? rightNumericId = right.asset.numericId;
      if (leftNumericId != null && rightNumericId != null) {
        final int idComparison = rightNumericId.compareTo(leftNumericId);
        if (idComparison != 0) {
          return idComparison;
        }
      } else {
        final int idComparison = right.asset.id.compareTo(left.asset.id);
        if (idComparison != 0) {
          return idComparison;
        }
      }

      return left.index.compareTo(right.index);
    });

    return List<MediaAsset>.unmodifiable(
      indexedAssets.map((_IndexedMediaAsset item) => item.asset),
    );
  }

  AppError _mapGalleryFailure(Object error, StackTrace stackTrace) {
    return mapPlatformException(
      error,
      stackTrace,
      message: 'Gallery media query failed.',
    );
  }
}

final class _IndexedMediaAsset {
  const _IndexedMediaAsset({required this.index, required this.asset});

  final int index;
  final MediaAsset asset;
}

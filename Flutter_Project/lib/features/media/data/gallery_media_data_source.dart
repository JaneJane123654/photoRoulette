import 'package:photo_manager/photo_manager.dart' as photo_manager;

import '../../../core/logging/app_logger.dart';
import '../../../core/platform/platform_info.dart';
import 'raw_gallery_asset.dart';

abstract interface class GalleryMediaDataSource {
  Future<List<RawGalleryAsset>> queryMediaAssets();
}

final class PhotoManagerGalleryDataSource implements GalleryMediaDataSource {
  PhotoManagerGalleryDataSource({
    PlatformInfo? platformInfo,
    AppLogger logger = const AppLogger('PhotoManagerGalleryDataSource'),
  }) : _platformInfo = platformInfo ?? PlatformInfo.current(),
       _logger = logger;

  static const int _pageSize = 512;
  static const int _millisecondsPerSecond = 1000;
  static const int _mediaTypeImage = 1;
  static const int _mediaTypeVideo = 3;
  static const String _androidFilesContentUriPrefix =
      'content://media/external/file';
  static const String _androidDateAddedColumn = 'date_added';
  static const String _androidIdColumn = '_id';
  static const String _androidSizeColumn = '_size';
  static const String _androidMimeTypeColumn = 'mime_type';
  static const String _androidMediaTypeColumn = 'media_type';
  static const String _defaultMimeType = 'application/octet-stream';
  static const String _androidParityWhere =
      '(($_androidMediaTypeColumn=$_mediaTypeImage OR '
      '$_androidMediaTypeColumn=$_mediaTypeVideo) AND '
      '$_androidSizeColumn>0 AND '
      "($_androidMimeTypeColumn LIKE 'image/%' OR "
      "$_androidMimeTypeColumn LIKE 'video/%') AND "
      "$_androidMimeTypeColumn!='$_defaultMimeType')";

  final PlatformInfo _platformInfo;
  final AppLogger _logger;

  @override
  Future<List<RawGalleryAsset>> queryMediaAssets() async {
    final photo_manager.PMFilter filter = _buildFilter();
    final int assetCount = await photo_manager.PhotoManager.getAssetCount(
      filterOption: filter,
      type: photo_manager.RequestType.common,
    );
    if (assetCount <= 0) {
      return const <RawGalleryAsset>[];
    }

    final List<RawGalleryAsset> assets = <RawGalleryAsset>[];
    var page = 0;
    while (assets.length < assetCount) {
      final List<photo_manager.AssetEntity> pageEntities =
          await photo_manager.PhotoManager.getAssetListPaged(
            page: page,
            pageCount: _pageSize,
            filterOption: filter,
            type: photo_manager.RequestType.common,
          );
      if (pageEntities.isEmpty) {
        break;
      }

      for (final photo_manager.AssetEntity entity in pageEntities) {
        assets.add(await _mapEntity(entity));
      }

      if (pageEntities.length < _pageSize) {
        break;
      }
      page += 1;
    }

    return List<RawGalleryAsset>.unmodifiable(assets);
  }

  photo_manager.PMFilter _buildFilter() {
    if (_platformInfo.isAndroid) {
      final photo_manager.CustomFilter filter = photo_manager.CustomFilter.sql(
        where: _androidParityWhere,
        orderBy: const <photo_manager.OrderByItem>[
          photo_manager.OrderByItem.desc(_androidDateAddedColumn),
          photo_manager.OrderByItem.desc(_androidIdColumn),
        ],
      )..needTitle = true;

      return filter;
    }

    const photo_manager.SizeConstraint ignoredSizeConstraint =
        photo_manager.SizeConstraint(ignoreSize: true);
    const photo_manager.DurationConstraint unboundedDurationConstraint =
        photo_manager.DurationConstraint(
          max: Duration(days: 36500),
          allowNullable: true,
        );

    return photo_manager.FilterOptionGroup(
      imageOption: const photo_manager.FilterOption(
        needTitle: true,
        sizeConstraint: ignoredSizeConstraint,
      ),
      videoOption: const photo_manager.FilterOption(
        needTitle: true,
        sizeConstraint: ignoredSizeConstraint,
        durationConstraint: unboundedDurationConstraint,
      ),
      orders: const <photo_manager.OrderOption>[
        photo_manager.OrderOption(
          type: photo_manager.OrderOptionType.createDate,
        ),
      ],
    );
  }

  Future<RawGalleryAsset> _mapEntity(photo_manager.AssetEntity entity) async {
    final RawGalleryMediaType mediaType = _mapMediaType(entity.type);
    final Uri? contentUri = _buildAndroidContentUri(entity.id);
    final String? displayName = await _readDisplayName(entity);
    final String? mimeType = await _readMimeType(entity);
    final Uri? playbackUri = mediaType == RawGalleryMediaType.video
        ? await _readPlaybackUri(entity, fallbackUri: contentUri)
        : null;

    return RawGalleryAsset(
      id: entity.id,
      numericId: int.tryParse(entity.id),
      mediaType: mediaType,
      displayName: displayName,
      mimeType: mimeType,
      durationMs: _durationMsFor(entity, mediaType),
      width: entity.width,
      height: entity.height,
      dateAddedSeconds: entity.createDateSecond,
      relativePath: entity.relativePath,
      previewUri: contentUri,
      playbackUri: playbackUri,
    );
  }

  RawGalleryMediaType _mapMediaType(photo_manager.AssetType assetType) {
    return switch (assetType) {
      photo_manager.AssetType.image => RawGalleryMediaType.image,
      photo_manager.AssetType.video => RawGalleryMediaType.video,
      photo_manager.AssetType.audio => RawGalleryMediaType.audio,
      photo_manager.AssetType.other => RawGalleryMediaType.other,
    };
  }

  int _durationMsFor(
    photo_manager.AssetEntity entity,
    RawGalleryMediaType mediaType,
  ) {
    if (mediaType != RawGalleryMediaType.video) {
      return 0;
    }

    return entity.duration * _millisecondsPerSecond;
  }

  Uri? _buildAndroidContentUri(String assetId) {
    if (!_platformInfo.isAndroid) {
      return null;
    }

    final int? numericId = int.tryParse(assetId);
    if (numericId == null) {
      return null;
    }

    return Uri.parse('$_androidFilesContentUriPrefix/$numericId');
  }

  Future<String?> _readDisplayName(photo_manager.AssetEntity entity) async {
    final String? title = entity.title;
    if (title != null) {
      return title;
    }

    try {
      return await entity.titleAsync;
    } catch (error, stackTrace) {
      _logger.warning(
        'Falling back to an empty display name for media id=${entity.id}.',
        error: error,
        stackTrace: stackTrace,
      );
      return null;
    }
  }

  Future<String?> _readMimeType(photo_manager.AssetEntity entity) async {
    final String? mimeType = entity.mimeType;
    if (mimeType != null) {
      return mimeType;
    }

    try {
      return await entity.mimeTypeAsync;
    } catch (error, stackTrace) {
      _logger.warning(
        'Falling back to the default MIME type for media id=${entity.id}.',
        error: error,
        stackTrace: stackTrace,
      );
      return null;
    }
  }

  Future<Uri?> _readPlaybackUri(
    photo_manager.AssetEntity entity, {
    required Uri? fallbackUri,
  }) async {
    if (fallbackUri != null) {
      return fallbackUri;
    }

    try {
      final String? mediaUrl = await entity.getMediaUrl();
      if (mediaUrl == null || mediaUrl.trim().isEmpty) {
        return null;
      }

      return Uri.parse(mediaUrl);
    } on FormatException catch (error, stackTrace) {
      _logger.warning(
        'Ignoring malformed playback URI for media id=${entity.id}.',
        error: error,
        stackTrace: stackTrace,
      );
      return null;
    } catch (error, stackTrace) {
      _logger.warning(
        'Unable to read playback URI for media id=${entity.id}.',
        error: error,
        stackTrace: stackTrace,
      );
      return null;
    }
  }
}

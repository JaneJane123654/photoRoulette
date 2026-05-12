import '../../../core/logging/app_logger.dart';
import '../domain/models/media_asset.dart';
import '../domain/models/media_kind.dart';
import 'raw_gallery_asset.dart';

final class MediaClassifier {
  const MediaClassifier({AppLogger logger = const AppLogger('MediaClassifier')})
    : _logger = logger;

  static const int noDurationMs = 0;
  static const int livePhotoMaxDurationMs = 3500;
  static const String defaultMimeType = 'application/octet-stream';
  static const String emptyDisplayName = '';

  final AppLogger _logger;

  MediaAsset? normalize(RawGalleryAsset rawAsset) {
    if (!_isSupportedMediaType(rawAsset.mediaType)) {
      _logger.debug(
        'Ignoring unsupported gallery media type for id=${rawAsset.id}: '
        '${rawAsset.mediaType.name}.',
      );
      return null;
    }

    final int? fileSizeBytes = rawAsset.fileSizeBytes;
    if (fileSizeBytes != null && fileSizeBytes <= 0) {
      _logger.debug('Ignoring zero-size gallery media id=${rawAsset.id}.');
      return null;
    }

    final String mimeType = _normalizeMimeType(rawAsset.mimeType);
    final String normalizedMimeType = mimeType.toLowerCase();
    if (normalizedMimeType == defaultMimeType) {
      _logger.debug(
        'Ignoring gallery media id=${rawAsset.id} with default MIME type.',
      );
      return null;
    }

    if (!_mimeMatchesMediaType(normalizedMimeType, rawAsset.mediaType)) {
      _logger.debug(
        'Ignoring gallery media id=${rawAsset.id} with MIME type $mimeType.',
      );
      return null;
    }

    final int rawDurationMs = rawAsset.durationMs ?? noDurationMs;
    final int durationMs = rawDurationMs < noDurationMs
        ? noDurationMs
        : rawDurationMs;
    final String displayName =
        rawAsset.displayName ?? MediaClassifier.emptyDisplayName;
    final MediaKind mediaKind = resolveMediaKind(
      mediaType: rawAsset.mediaType,
      mimeType: mimeType,
      durationMs: durationMs,
      displayName: displayName,
    );

    return MediaAsset(
      id: rawAsset.id,
      numericId: rawAsset.numericId ?? int.tryParse(rawAsset.id),
      displayName: displayName,
      mimeType: mimeType,
      durationMs: durationMs,
      width: rawAsset.width ?? 0,
      height: rawAsset.height ?? 0,
      dateAddedSeconds: rawAsset.dateAddedSeconds,
      relativePath: rawAsset.relativePath,
      kind: mediaKind,
      requiresPlaybackUri: mediaKind.requiresPlaybackUri,
      thumbnailSource: MediaThumbnailSource(
        assetId: rawAsset.id,
        contentUri: rawAsset.previewUri,
      ),
      previewUri: rawAsset.previewUri,
      playbackUri: mediaKind.requiresPlaybackUri ? rawAsset.playbackUri : null,
    );
  }

  MediaKind resolveMediaKind({
    required RawGalleryMediaType mediaType,
    required String mimeType,
    required int durationMs,
    required String displayName,
  }) {
    if (mediaType == RawGalleryMediaType.video) {
      final String normalizedMimeType = mimeType.toLowerCase();
      final String normalizedDisplayName = displayName.toLowerCase();
      final bool hasLiveNameHint =
          normalizedDisplayName.startsWith('mvimg') ||
          normalizedDisplayName.contains('motion') ||
          normalizedDisplayName.contains('live');
      final bool isLiveStyle =
          normalizedMimeType.contains('quicktime') ||
          normalizedMimeType.contains('motion') ||
          (hasLiveNameHint &&
              durationMs >= 1 &&
              durationMs <= livePhotoMaxDurationMs);

      return isLiveStyle ? MediaKind.livePhotoVideo : MediaKind.video;
    }

    return isAnimatedImageMimeType(mimeType)
        ? MediaKind.animatedImage
        : MediaKind.image;
  }

  bool isAnimatedImageMimeType(String mimeType) {
    final String normalizedMimeType = mimeType.toLowerCase();
    return normalizedMimeType == 'image/gif' ||
        normalizedMimeType == 'image/webp' ||
        normalizedMimeType == 'image/apng';
  }

  bool _isSupportedMediaType(RawGalleryMediaType mediaType) {
    return mediaType == RawGalleryMediaType.image ||
        mediaType == RawGalleryMediaType.video;
  }

  bool _mimeMatchesMediaType(
    String normalizedMimeType,
    RawGalleryMediaType mediaType,
  ) {
    return switch (mediaType) {
      RawGalleryMediaType.image => normalizedMimeType.startsWith('image/'),
      RawGalleryMediaType.video => normalizedMimeType.startsWith('video/'),
      RawGalleryMediaType.audio || RawGalleryMediaType.other => false,
    };
  }

  String _normalizeMimeType(String? mimeType) {
    final String? value = mimeType?.trim();
    if (value == null || value.isEmpty) {
      return defaultMimeType;
    }

    return value;
  }
}

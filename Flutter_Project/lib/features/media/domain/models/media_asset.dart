import 'media_bucket.dart';
import 'media_kind.dart';

final class MediaThumbnailSource {
  const MediaThumbnailSource({required this.assetId, this.contentUri});

  final String assetId;
  final Uri? contentUri;
}

final class MediaAsset {
  const MediaAsset({
    required this.id,
    required this.displayName,
    required this.mimeType,
    required this.durationMs,
    required this.width,
    required this.height,
    required this.dateAddedSeconds,
    required this.relativePath,
    required this.kind,
    required this.requiresPlaybackUri,
    required this.thumbnailSource,
    this.numericId,
    this.previewUri,
    this.playbackUri,
  });

  final String id;
  final int? numericId;
  final String displayName;
  final String mimeType;
  final int durationMs;
  final int width;
  final int height;
  final int? dateAddedSeconds;
  final String? relativePath;
  final MediaKind kind;
  final bool requiresPlaybackUri;
  final MediaThumbnailSource thumbnailSource;
  final Uri? previewUri;
  final Uri? playbackUri;

  bool get isVideoLike => kind.isVideoLike;

  DateTime? get addedAtUtc {
    final int? seconds = dateAddedSeconds;
    if (seconds == null || seconds <= 0) {
      return null;
    }

    return DateTime.fromMillisecondsSinceEpoch(
      seconds * MediaBucketKey.millisecondsPerSecond,
      isUtc: true,
    );
  }

  MediaBucketKey get bucketKey {
    return MediaBucketKey.fromDateAddedSeconds(dateAddedSeconds);
  }
}

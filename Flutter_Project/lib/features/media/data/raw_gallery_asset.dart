enum RawGalleryMediaType { image, video, audio, other }

final class RawGalleryAsset {
  const RawGalleryAsset({
    required this.id,
    required this.mediaType,
    this.numericId,
    this.displayName,
    this.mimeType,
    this.durationMs,
    this.width,
    this.height,
    this.dateAddedSeconds,
    this.relativePath,
    this.fileSizeBytes,
    this.previewUri,
    this.playbackUri,
  });

  final String id;
  final int? numericId;
  final RawGalleryMediaType mediaType;
  final String? displayName;
  final String? mimeType;
  final int? durationMs;
  final int? width;
  final int? height;
  final int? dateAddedSeconds;
  final String? relativePath;
  final int? fileSizeBytes;
  final Uri? previewUri;
  final Uri? playbackUri;
}

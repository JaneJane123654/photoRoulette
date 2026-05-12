import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/features/media/media.dart';

void main() {
  const MediaClassifier classifier = MediaClassifier();

  RawGalleryAsset rawAsset({
    String id = '1',
    RawGalleryMediaType mediaType = RawGalleryMediaType.image,
    String? displayName = 'IMG_0001.jpg',
    String? mimeType = 'image/jpeg',
    int? durationMs = 0,
    int? fileSizeBytes = 42,
    Uri? previewUri,
    Uri? playbackUri,
  }) {
    return RawGalleryAsset(
      id: id,
      numericId: int.tryParse(id),
      mediaType: mediaType,
      displayName: displayName,
      mimeType: mimeType,
      durationMs: durationMs,
      width: 1200,
      height: 800,
      dateAddedSeconds: 1777161600,
      relativePath: 'DCIM/Camera/',
      fileSizeBytes: fileSizeBytes,
      previewUri: previewUri ?? Uri.parse('content://media/external/file/$id'),
      playbackUri: playbackUri,
    );
  }

  group('MediaClassifier filters', () {
    test('excludes unsupported media types', () {
      final MediaAsset? asset = classifier.normalize(
        rawAsset(mediaType: RawGalleryMediaType.audio, mimeType: 'audio/mpeg'),
      );

      expect(asset, isNull);
    });

    test('excludes zero-size files', () {
      final MediaAsset? asset = classifier.normalize(
        rawAsset(fileSizeBytes: 0),
      );

      expect(asset, isNull);
    });

    test('excludes missing or default MIME types', () {
      expect(classifier.normalize(rawAsset(mimeType: null)), isNull);
      expect(
        classifier.normalize(rawAsset(mimeType: 'application/octet-stream')),
        isNull,
      );
    });

    test('excludes MIME types outside the raw media family', () {
      final MediaAsset? asset = classifier.normalize(
        rawAsset(mediaType: RawGalleryMediaType.image, mimeType: 'video/mp4'),
      );

      expect(asset, isNull);
    });
  });

  group('MediaClassifier kind detection', () {
    test('classifies still and animated images', () {
      expect(
        classifier.normalize(rawAsset(mimeType: 'image/jpeg'))?.kind,
        MediaKind.image,
      );
      expect(
        classifier.normalize(rawAsset(mimeType: 'image/gif'))?.kind,
        MediaKind.animatedImage,
      );
      expect(
        classifier.normalize(rawAsset(mimeType: 'IMAGE/WEBP'))?.kind,
        MediaKind.animatedImage,
      );
      expect(
        classifier.normalize(rawAsset(mimeType: 'image/apng'))?.kind,
        MediaKind.animatedImage,
      );
    });

    test('classifies quicktime and motion MIME videos as live-photo style', () {
      final MediaAsset? quickTimeAsset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          mimeType: 'video/quicktime',
          durationMs: 8000,
          playbackUri: Uri.parse('content://media/external/file/1'),
        ),
      );
      final MediaAsset? motionAsset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          mimeType: 'video/motion-photo',
          durationMs: 8000,
          playbackUri: Uri.parse('content://media/external/file/2'),
        ),
      );

      expect(quickTimeAsset?.kind, MediaKind.livePhotoVideo);
      expect(motionAsset?.kind, MediaKind.livePhotoVideo);
      expect(quickTimeAsset?.requiresPlaybackUri, isTrue);
    });

    test('uses motion/live display name hints only inside 1..3500 ms', () {
      MediaAsset? asset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          displayName: 'MVIMG_20260426.mp4',
          mimeType: 'video/mp4',
          durationMs: 1,
          playbackUri: Uri.parse('content://media/external/file/1'),
        ),
      );
      expect(asset?.kind, MediaKind.livePhotoVideo);

      asset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          displayName: 'motion_clip.mp4',
          mimeType: 'video/mp4',
          durationMs: 3500,
          playbackUri: Uri.parse('content://media/external/file/2'),
        ),
      );
      expect(asset?.kind, MediaKind.livePhotoVideo);

      asset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          displayName: 'live_clip.mp4',
          mimeType: 'video/mp4',
          durationMs: 3501,
          playbackUri: Uri.parse('content://media/external/file/3'),
        ),
      );
      expect(asset?.kind, MediaKind.video);

      asset = classifier.normalize(
        rawAsset(
          mediaType: RawGalleryMediaType.video,
          displayName: 'live_clip.mp4',
          mimeType: 'video/mp4',
          durationMs: 0,
          playbackUri: Uri.parse('content://media/external/file/4'),
        ),
      );
      expect(asset?.kind, MediaKind.video);
    });

    test('does not require playback URIs for still or animated images', () {
      final MediaAsset? asset = classifier.normalize(
        rawAsset(mimeType: 'image/gif'),
      );

      expect(asset?.requiresPlaybackUri, isFalse);
      expect(asset?.playbackUri, isNull);
    });
  });
}

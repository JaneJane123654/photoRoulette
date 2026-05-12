import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/core/core.dart';
import 'package:photo_roulette_flutter/features/media/media.dart';

void main() {
  GalleryRepositoryImpl createRepository(List<RawGalleryAsset> assets) {
    return GalleryRepositoryImpl(
      dataSource: FakeGalleryMediaDataSource(assets),
      classifier: const MediaClassifier(),
    );
  }

  RawGalleryAsset rawAsset({
    required String id,
    required int? dateAddedSeconds,
    RawGalleryMediaType mediaType = RawGalleryMediaType.image,
    String mimeType = 'image/jpeg',
    int fileSizeBytes = 64,
  }) {
    return RawGalleryAsset(
      id: id,
      numericId: int.tryParse(id),
      mediaType: mediaType,
      displayName: 'asset_$id',
      mimeType: mimeType,
      durationMs: mediaType == RawGalleryMediaType.video ? 9000 : 0,
      width: 1024,
      height: 768,
      dateAddedSeconds: dateAddedSeconds,
      relativePath: 'Pictures/',
      fileSizeBytes: fileSizeBytes,
      previewUri: Uri.parse('content://media/external/file/$id'),
      playbackUri: mediaType == RawGalleryMediaType.video
          ? Uri.parse('content://media/external/file/$id')
          : null,
    );
  }

  group('GalleryRepositoryImpl', () {
    test('returns an empty success when the gallery has no rows', () async {
      final GalleryRepository repository = createRepository(
        const <RawGalleryAsset>[],
      );

      final AppResult<List<MediaAsset>> result = await repository
          .loadMediaAssets();

      expect(result, isA<AppSuccess<List<MediaAsset>>>());
      expect(result.valueOrNull, isEmpty);
    });

    test(
      'filters invalid rows and sorts by DATE_ADDED DESC, _ID DESC',
      () async {
        final GalleryRepository repository = createRepository(<RawGalleryAsset>[
          rawAsset(id: '2', dateAddedSeconds: 100),
          rawAsset(
            id: '5',
            dateAddedSeconds: 300,
            mediaType: RawGalleryMediaType.video,
            mimeType: 'video/mp4',
          ),
          rawAsset(id: '3', dateAddedSeconds: 300),
          rawAsset(id: '9', dateAddedSeconds: 900, fileSizeBytes: 0),
          rawAsset(
            id: '8',
            dateAddedSeconds: 800,
            mimeType: 'application/octet-stream',
          ),
          rawAsset(
            id: '7',
            dateAddedSeconds: 700,
            mediaType: RawGalleryMediaType.audio,
            mimeType: 'audio/mpeg',
          ),
        ]);

        final AppResult<List<MediaAsset>> result = await repository
            .loadMediaAssets();

        final List<MediaAsset> assets = result.valueOrNull!;

        expect(assets.map((MediaAsset asset) => asset.id), <String>[
          '5',
          '3',
          '2',
        ]);
        expect(assets.first.kind, MediaKind.video);
        expect(assets.first.requiresPlaybackUri, isTrue);
      },
    );

    test('assigns unknown bucket when DATE_ADDED is missing or zero', () async {
      final GalleryRepository repository = createRepository(<RawGalleryAsset>[
        rawAsset(id: '1', dateAddedSeconds: null),
        rawAsset(id: '2', dateAddedSeconds: 0),
      ]);

      final AppResult<List<MediaAsset>> result = await repository
          .loadMediaAssets();

      final List<MediaAsset> assets = result.valueOrNull!;

      expect(assets, hasLength(2));
      expect(
        assets.every((MediaAsset asset) => asset.bucketKey.isUnknown),
        isTrue,
      );
    });

    test('maps data-source failures into AppResult failure', () async {
      final GalleryRepository repository = GalleryRepositoryImpl(
        dataSource: ThrowingGalleryMediaDataSource(),
        classifier: const MediaClassifier(),
      );

      final AppResult<List<MediaAsset>> result = await repository
          .loadMediaAssets();

      expect(result, isA<AppFailure<List<MediaAsset>>>());
      expect(result.errorOrNull?.code, AppErrorCode.platformContractFailure);
    });
  });
}

final class FakeGalleryMediaDataSource implements GalleryMediaDataSource {
  const FakeGalleryMediaDataSource(this.assets);

  final List<RawGalleryAsset> assets;

  @override
  Future<List<RawGalleryAsset>> queryMediaAssets() async {
    return assets;
  }
}

final class ThrowingGalleryMediaDataSource implements GalleryMediaDataSource {
  @override
  Future<List<RawGalleryAsset>> queryMediaAssets() async {
    throw StateError('query failed');
  }
}

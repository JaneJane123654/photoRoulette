import 'dart:async';
import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/core/core.dart';
import 'package:photo_roulette_flutter/features/deck/deck.dart';
import 'package:photo_roulette_flutter/features/media/media.dart';

void main() {
  ProviderContainer createContainer({
    required GalleryRepository galleryRepository,
    required DeckSessionRepository sessionRepository,
    MediaShuffleService? shuffleService,
  }) {
    final ProviderContainer container = ProviderContainer(
      overrides: [
        galleryRepositoryProvider.overrideWithValue(galleryRepository),
        deckSessionRepositoryProvider.overrideWithValue(sessionRepository),
        mediaShuffleServiceProvider.overrideWithValue(
          shuffleService ?? MediaShuffleService(random: math.Random(1)),
        ),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  group('MediaShuffleService', () {
    test('distributes shuffled month buckets without adjacent repeats', () {
      final MediaShuffleService service = MediaShuffleService(
        random: math.Random(4),
      );
      final List<MediaAsset> assets = <MediaAsset>[
        mediaAsset(id: 'jan-1', dateAddedSeconds: utcSeconds(2026, 1, 10)),
        mediaAsset(id: 'jan-2', dateAddedSeconds: utcSeconds(2026, 1, 11)),
        mediaAsset(id: 'feb-1', dateAddedSeconds: utcSeconds(2026, 2, 10)),
        mediaAsset(id: 'feb-2', dateAddedSeconds: utcSeconds(2026, 2, 11)),
        mediaAsset(id: 'mar-1', dateAddedSeconds: utcSeconds(2026, 3, 10)),
        mediaAsset(id: 'mar-2', dateAddedSeconds: utcSeconds(2026, 3, 11)),
      ];

      final List<MediaAsset> shuffled = service.buildSmartShuffle(assets);

      expect(
        shuffled.map((MediaAsset asset) => asset.id),
        unorderedEquals(assets.map((MediaAsset asset) => asset.id)),
      );
      for (var index = 1; index < shuffled.length; index += 1) {
        expect(shuffled[index].bucketKey, isNot(shuffled[index - 1].bucketKey));
      }
    });

    test('keeps missing DATE_ADDED assets in the explicit unknown bucket', () {
      final List<MediaAsset> shuffled =
          MediaShuffleService(random: math.Random(2)).buildSmartShuffle(
            <MediaAsset>[mediaAsset(id: 'unknown-1', dateAddedSeconds: null)],
          );

      expect(shuffled.single.bucketKey, MediaBucketKey.unknown);
    });
  });

  group('GalleryDeckState', () {
    test('ready state requires one to three visible cards', () {
      expect(
        () => GalleryDeckState.ready(
          previousCard: null,
          visibleCards: const <DeckCard>[],
          currentIndex: 0,
          totalQueueLength: 0,
          canSwipeToPrevious: false,
          canSwipeToNext: false,
        ),
        throwsAssertionError,
      );

      expect(
        () => GalleryDeckState.ready(
          previousCard: null,
          visibleCards: <DeckCard>[
            DeckCard.fromAsset(mediaAsset(id: '1')),
            DeckCard.fromAsset(mediaAsset(id: '2')),
            DeckCard.fromAsset(mediaAsset(id: '3')),
            DeckCard.fromAsset(mediaAsset(id: '4')),
          ],
          currentIndex: 0,
          totalQueueLength: 4,
          canSwipeToPrevious: false,
          canSwipeToNext: true,
        ),
        throwsAssertionError,
      );
    });
  });

  group('GalleryDeckController', () {
    test(
      'restores cached visible cards before source reload completes',
      () async {
        final ControlledGalleryRepository galleryRepository =
            ControlledGalleryRepository();
        final InMemoryDeckSessionRepository sessionRepository =
            InMemoryDeckSessionRepository(
              DeckSessionSnapshot(
                queueIds: const <String>['1', '2', '3', '4'],
                currentIndex: 1,
                visibleCards: <DeckCard>[
                  DeckCard.fromAsset(mediaAsset(id: '2')),
                  DeckCard.fromAsset(mediaAsset(id: '3')),
                  DeckCard.fromAsset(mediaAsset(id: '4')),
                ],
              ),
            );
        final ProviderContainer container = createContainer(
          galleryRepository: galleryRepository,
          sessionRepository: sessionRepository,
        );
        final GalleryDeckController controller = container.read(
          galleryDeckControllerProvider.notifier,
        );

        final Future<AppResult<void>> loadFuture = controller.initialLoad();
        await Future<void>.delayed(Duration.zero);

        final GalleryDeckState restoredState = container.read(
          galleryDeckControllerProvider,
        );
        expect(restoredState, isA<GalleryDeckReady>());
        final GalleryDeckReady readyState = restoredState as GalleryDeckReady;
        expect(
          readyState.visibleCards.map((DeckCard card) => card.id),
          <String>['2', '3', '4'],
        );
        expect(readyState.currentIndex, 1);
        expect(readyState.totalQueueLength, 4);
        expect(readyState.canSwipeToPrevious, isTrue);
        expect(readyState.canSwipeToNext, isTrue);

        galleryRepository.completeWith(<MediaAsset>[
          mediaAsset(id: '1'),
          mediaAsset(id: '2'),
          mediaAsset(id: '3'),
          mediaAsset(id: '4'),
        ]);
        await loadFuture;

        expect(
          container.read(galleryDeckControllerProvider),
          isA<GalleryDeckReady>(),
        );
      },
    );

    test(
      'moves only from the current top card and updates session cache',
      () async {
        final ControlledGalleryRepository galleryRepository =
            ControlledGalleryRepository();
        final InMemoryDeckSessionRepository sessionRepository =
            InMemoryDeckSessionRepository(
              DeckSessionSnapshot(
                queueIds: const <String>['1', '2', '3'],
                currentIndex: 0,
                visibleCards: <DeckCard>[
                  DeckCard.fromAsset(mediaAsset(id: '1')),
                  DeckCard.fromAsset(mediaAsset(id: '2')),
                  DeckCard.fromAsset(mediaAsset(id: '3')),
                ],
              ),
            );
        final ProviderContainer container = createContainer(
          galleryRepository: galleryRepository,
          sessionRepository: sessionRepository,
        );
        final GalleryDeckController controller = container.read(
          galleryDeckControllerProvider.notifier,
        );

        final Future<AppResult<void>> loadFuture = controller.initialLoad();
        await Future<void>.delayed(Duration.zero);

        expect(await controller.moveToNext(expectedTopCardId: '2'), isFalse);
        expect(await controller.moveToNext(expectedTopCardId: '1'), isTrue);

        GalleryDeckState state = container.read(galleryDeckControllerProvider);
        expect(state, isA<GalleryDeckReady>());
        GalleryDeckReady readyState = state as GalleryDeckReady;
        expect(readyState.previousCard?.id, '1');
        expect(readyState.currentCard.id, '2');
        expect(readyState.currentIndex, 1);
        expect(readyState.canSwipeToPrevious, isTrue);
        expect(readyState.canSwipeToNext, isTrue);
        expect(sessionRepository.savedSession?.currentIndex, 1);
        expect(
          sessionRepository.savedSession?.visibleCards.map(
            (DeckCard card) => card.id,
          ),
          <String>['2', '3'],
        );

        expect(await controller.moveToPrevious(expectedTopCardId: '2'), isTrue);
        final GalleryDeckState previousState = container.read(
          galleryDeckControllerProvider,
        );
        readyState = previousState as GalleryDeckReady;
        expect(readyState.currentCard.id, '1');
        expect(readyState.canSwipeToPrevious, isFalse);
        expect(readyState.canSwipeToNext, isTrue);

        galleryRepository.completeWith(<MediaAsset>[
          mediaAsset(id: '1'),
          mediaAsset(id: '2'),
          mediaAsset(id: '3'),
        ]);
        await loadFuture;
      },
    );

    test('reloads empty galleries into the empty state', () async {
      final ImmediateGalleryRepository galleryRepository =
          ImmediateGalleryRepository(const <MediaAsset>[]);
      final InMemoryDeckSessionRepository sessionRepository =
          InMemoryDeckSessionRepository();
      final ProviderContainer container = createContainer(
        galleryRepository: galleryRepository,
        sessionRepository: sessionRepository,
      );
      final GalleryDeckController controller = container.read(
        galleryDeckControllerProvider.notifier,
      );

      await controller.reloadFromSource();

      expect(
        container.read(galleryDeckControllerProvider),
        isA<GalleryDeckEmpty>(),
      );
      expect(sessionRepository.savedSession?.queueIds, isEmpty);
      expect(sessionRepository.savedSession?.currentIndex, 0);
    });
  });
}

int utcSeconds(int year, int month, int day) {
  return DateTime.utc(year, month, day).millisecondsSinceEpoch ~/
      MediaBucketKey.millisecondsPerSecond;
}

MediaAsset mediaAsset({
  required String id,
  int? dateAddedSeconds = 1777161600,
}) {
  return MediaAsset(
    id: id,
    numericId: int.tryParse(id),
    displayName: 'asset_$id',
    mimeType: 'image/jpeg',
    durationMs: 0,
    width: 1024,
    height: 768,
    dateAddedSeconds: dateAddedSeconds,
    relativePath: 'Pictures/',
    kind: MediaKind.image,
    requiresPlaybackUri: false,
    thumbnailSource: MediaThumbnailSource(
      assetId: id,
      contentUri: Uri.parse('content://media/external/file/$id'),
    ),
    previewUri: Uri.parse('content://media/external/file/$id'),
  );
}

final class ControlledGalleryRepository implements GalleryRepository {
  final Completer<AppResult<List<MediaAsset>>> _completer =
      Completer<AppResult<List<MediaAsset>>>();

  @override
  Future<AppResult<List<MediaAsset>>> loadMediaAssets() {
    return _completer.future;
  }

  void completeWith(List<MediaAsset> assets) {
    if (!_completer.isCompleted) {
      _completer.complete(AppResult<List<MediaAsset>>.success(assets));
    }
  }
}

final class ImmediateGalleryRepository implements GalleryRepository {
  const ImmediateGalleryRepository(this.assets);

  final List<MediaAsset> assets;

  @override
  Future<AppResult<List<MediaAsset>>> loadMediaAssets() async {
    return AppResult<List<MediaAsset>>.success(assets);
  }
}

final class InMemoryDeckSessionRepository implements DeckSessionRepository {
  InMemoryDeckSessionRepository([this.session]);

  DeckSessionSnapshot? session;
  DeckSessionSnapshot? savedSession;

  @override
  Future<AppResult<DeckSessionSnapshot?>> loadSession() async {
    return AppResult<DeckSessionSnapshot?>.success(session);
  }

  @override
  Future<AppResult<void>> saveSession(DeckSessionSnapshot session) async {
    savedSession = session;
    this.session = session;
    return const AppResult<void>.success(null);
  }

  @override
  Future<AppResult<void>> clearSession() async {
    session = null;
    savedSession = null;
    return const AppResult<void>.success(null);
  }
}

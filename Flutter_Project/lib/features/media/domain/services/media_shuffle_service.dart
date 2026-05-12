import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/media_asset.dart';
import '../models/media_bucket.dart';

final Provider<MediaShuffleService> mediaShuffleServiceProvider =
    Provider<MediaShuffleService>((Ref ref) {
      return MediaShuffleService();
    });

final class MediaShuffleService {
  MediaShuffleService({math.Random? random})
    : _random = random ?? math.Random();

  final math.Random _random;

  List<MediaAsset> buildSmartShuffle(List<MediaAsset> entries) {
    if (entries.isEmpty) {
      return const <MediaAsset>[];
    }

    final Map<MediaBucketKey, List<MediaAsset>> groupedEntries =
        <MediaBucketKey, List<MediaAsset>>{};
    for (final MediaAsset entry in entries) {
      groupedEntries
          .putIfAbsent(entry.bucketKey, () => <MediaAsset>[])
          .add(entry);
    }

    final List<_ShuffleBucket> activeBuckets = _shuffled<_ShuffleBucket>(
      groupedEntries.entries.map((
        MapEntry<MediaBucketKey, List<MediaAsset>> entry,
      ) {
        return _ShuffleBucket(
          key: entry.key,
          cards: _shuffled<MediaAsset>(entry.value),
        );
      }),
    ).toList(growable: true);

    final List<MediaAsset> distributedCards = <MediaAsset>[];
    MediaBucketKey? previousBucketKey;

    while (activeBuckets.isNotEmpty) {
      final List<_ShuffleBucket> roundBuckets = _shuffled<_ShuffleBucket>(
        activeBuckets,
      ).toList(growable: true);
      _moveDifferentBucketToFront(roundBuckets, previousBucketKey);

      activeBuckets.clear();

      for (final _ShuffleBucket bucket in roundBuckets) {
        if (bucket.cards.isEmpty) {
          continue;
        }

        distributedCards.add(bucket.cards.removeLast());
        previousBucketKey = bucket.key;

        if (bucket.cards.isNotEmpty) {
          activeBuckets.add(bucket);
        }
      }
    }

    return List<MediaAsset>.unmodifiable(distributedCards);
  }

  List<T> _shuffled<T>(Iterable<T> values) {
    final List<T> shuffledValues = values.toList(growable: true);
    shuffledValues.shuffle(_random);
    return shuffledValues;
  }

  void _moveDifferentBucketToFront(
    List<_ShuffleBucket> buckets,
    MediaBucketKey? previousBucketKey,
  ) {
    if (previousBucketKey == null ||
        buckets.length <= 1 ||
        buckets.first.key != previousBucketKey) {
      return;
    }

    final int replacementIndex = buckets.indexWhere(
      (_ShuffleBucket bucket) => bucket.key != previousBucketKey,
    );
    if (replacementIndex <= 0) {
      return;
    }

    final _ShuffleBucket firstBucket = buckets.first;
    buckets[0] = buckets[replacementIndex];
    buckets[replacementIndex] = firstBucket;
  }
}

final class _ShuffleBucket {
  const _ShuffleBucket({required this.key, required this.cards});

  final MediaBucketKey key;
  final List<MediaAsset> cards;
}

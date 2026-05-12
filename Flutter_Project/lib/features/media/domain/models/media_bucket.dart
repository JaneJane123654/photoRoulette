final class MediaBucketKey implements Comparable<MediaBucketKey> {
  const MediaBucketKey._(this.value);

  factory MediaBucketKey.fromDateAddedSeconds(int? dateAddedSeconds) {
    if (dateAddedSeconds == null || dateAddedSeconds <= 0) {
      return MediaBucketKey.unknown;
    }

    final DateTime addedAtUtc = DateTime.fromMillisecondsSinceEpoch(
      dateAddedSeconds * millisecondsPerSecond,
      isUtc: true,
    );

    return MediaBucketKey._((addedAtUtc.year * 100) + addedAtUtc.month);
  }

  static const MediaBucketKey unknown = MediaBucketKey._(-1);
  static const int millisecondsPerSecond = 1000;

  final int value;

  bool get isUnknown => value == unknown.value;

  int? get year {
    if (isUnknown) {
      return null;
    }

    return value ~/ 100;
  }

  int? get month {
    if (isUnknown) {
      return null;
    }

    return value % 100;
  }

  @override
  int compareTo(MediaBucketKey other) {
    return value.compareTo(other.value);
  }

  @override
  bool operator ==(Object other) {
    return other is MediaBucketKey && other.value == value;
  }

  @override
  int get hashCode => value.hashCode;

  @override
  String toString() {
    if (isUnknown) {
      return 'MediaBucketKey.unknown';
    }

    return 'MediaBucketKey($value)';
  }
}

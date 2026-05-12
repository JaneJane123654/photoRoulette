import '../../../media/domain/models/media_asset.dart';
import '../../../media/domain/models/media_kind.dart';

final class DeckCard {
  const DeckCard({required this.asset});

  factory DeckCard.fromAsset(MediaAsset asset) {
    return DeckCard(asset: asset);
  }

  final MediaAsset asset;

  static const String _idKey = 'id';
  static const String _numericIdKey = 'numeric_id';
  static const String _displayNameKey = 'display_name';
  static const String _mimeTypeKey = 'mime_type';
  static const String _durationMsKey = 'duration_ms';
  static const String _widthKey = 'width';
  static const String _heightKey = 'height';
  static const String _dateAddedSecondsKey = 'date_added_seconds';
  static const String _relativePathKey = 'relative_path';
  static const String _kindKey = 'kind';
  static const String _requiresPlaybackUriKey = 'requires_playback_uri';
  static const String _thumbnailAssetIdKey = 'thumbnail_asset_id';
  static const String _thumbnailContentUriKey = 'thumbnail_content_uri';
  static const String _previewUriKey = 'preview_uri';
  static const String _playbackUriKey = 'playback_uri';
  static const String _defaultMimeType = 'application/octet-stream';
  static const String _emptyDisplayName = '';

  String get id => asset.id;

  int? get numericId => asset.numericId;

  String get displayName => asset.displayName;

  String get mimeType => asset.mimeType;

  int get durationMs => asset.durationMs;

  int get width => asset.width;

  int get height => asset.height;

  int? get dateAddedSeconds => asset.dateAddedSeconds;

  String? get relativePath => asset.relativePath;

  MediaKind get kind => asset.kind;

  bool get requiresPlaybackUri => asset.requiresPlaybackUri;

  bool get isVideoLike => asset.isVideoLike;

  Uri? get previewUri => asset.previewUri;

  Uri? get playbackUri => asset.playbackUri;

  Map<String, Object?> toStorageMap() {
    return <String, Object?>{
      _idKey: asset.id,
      _numericIdKey: asset.numericId,
      _displayNameKey: asset.displayName,
      _mimeTypeKey: asset.mimeType,
      _durationMsKey: asset.durationMs,
      _widthKey: asset.width,
      _heightKey: asset.height,
      _dateAddedSecondsKey: asset.dateAddedSeconds,
      _relativePathKey: asset.relativePath,
      _kindKey: asset.kind.storageValue,
      _requiresPlaybackUriKey: asset.requiresPlaybackUri,
      _thumbnailAssetIdKey: asset.thumbnailSource.assetId,
      _thumbnailContentUriKey: asset.thumbnailSource.contentUri?.toString(),
      _previewUriKey: asset.previewUri?.toString(),
      _playbackUriKey: asset.playbackUri?.toString(),
    };
  }

  static DeckCard? tryFromStorageValue(Object? value) {
    if (value is! Map<dynamic, dynamic>) {
      return null;
    }

    final Map<String, Object?> values = <String, Object?>{};
    value.forEach((dynamic key, dynamic mapValue) {
      if (key is String) {
        values[key] = mapValue;
      }
    });

    final String? id = _readNonBlankString(values[_idKey]);
    if (id == null) {
      return null;
    }

    final MediaKind? kind = MediaKind.fromStorageValue(
      _readNonBlankString(values[_kindKey]),
    );
    if (kind == null) {
      return null;
    }

    final String thumbnailAssetId =
        _readNonBlankString(values[_thumbnailAssetIdKey]) ?? id;

    return DeckCard(
      asset: MediaAsset(
        id: id,
        numericId: _readInt(values[_numericIdKey]) ?? int.tryParse(id),
        displayName: _readString(values[_displayNameKey]) ?? _emptyDisplayName,
        mimeType: _readNonBlankString(values[_mimeTypeKey]) ?? _defaultMimeType,
        durationMs: _readNonNegativeInt(values[_durationMsKey]),
        width: _readNonNegativeInt(values[_widthKey]),
        height: _readNonNegativeInt(values[_heightKey]),
        dateAddedSeconds: _readInt(values[_dateAddedSecondsKey]),
        relativePath: _readString(values[_relativePathKey]),
        kind: kind,
        requiresPlaybackUri:
            _readBool(values[_requiresPlaybackUriKey]) ??
            kind.requiresPlaybackUri,
        thumbnailSource: MediaThumbnailSource(
          assetId: thumbnailAssetId,
          contentUri: _readUri(values[_thumbnailContentUriKey]),
        ),
        previewUri: _readUri(values[_previewUriKey]),
        playbackUri: _readUri(values[_playbackUriKey]),
      ),
    );
  }

  static String? _readString(Object? value) {
    return value is String ? value : null;
  }

  static String? _readNonBlankString(Object? value) {
    final String? text = _readString(value);
    if (text == null || text.trim().isEmpty) {
      return null;
    }

    return text;
  }

  static int? _readInt(Object? value) {
    if (value is int) {
      return value;
    }

    if (value is num) {
      return value.toInt();
    }

    return null;
  }

  static int _readNonNegativeInt(Object? value) {
    final int? intValue = _readInt(value);
    if (intValue == null || intValue < 0) {
      return 0;
    }

    return intValue;
  }

  static bool? _readBool(Object? value) {
    return value is bool ? value : null;
  }

  static Uri? _readUri(Object? value) {
    final String? uriString = _readNonBlankString(value);
    if (uriString == null) {
      return null;
    }

    try {
      return Uri.parse(uriString);
    } on FormatException {
      return null;
    }
  }
}

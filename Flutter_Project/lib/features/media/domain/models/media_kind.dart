enum MediaKind {
  image('image'),
  animatedImage('animated_image'),
  video('video'),
  livePhotoVideo('live_photo_video');

  const MediaKind(this.storageValue);

  final String storageValue;

  static MediaKind? fromStorageValue(String? value) {
    if (value == null) {
      return null;
    }

    for (final MediaKind kind in MediaKind.values) {
      if (kind.storageValue == value) {
        return kind;
      }
    }

    return null;
  }

  bool get isVideoLike {
    return this == MediaKind.video || this == MediaKind.livePhotoVideo;
  }

  bool get requiresPlaybackUri => isVideoLike;
}

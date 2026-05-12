import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:photo_manager/photo_manager.dart' as photo_manager;
import 'package:video_player/video_player.dart';

import '../../../../core/logging/app_logger.dart';
import '../../../../l10n/generated/app_localizations.dart';
import '../../../media/domain/models/media_kind.dart';
import '../../domain/models/deck_card.dart';

class MediaPreviewSurface extends StatelessWidget {
  const MediaPreviewSurface({
    required this.card,
    required this.isTopCard,
    this.showFullImage = false,
    this.enableTapToggle = true,
    super.key,
  });

  final DeckCard card;
  final bool isTopCard;
  final bool showFullImage;
  final bool enableTapToggle;

  @override
  Widget build(BuildContext context) {
    return switch (card.kind) {
      MediaKind.image => _ImagePreviewSurface(
        card: card,
        isTopCard: isTopCard,
        showFullImage: showFullImage,
        enableTapToggle: enableTapToggle,
      ),
      MediaKind.animatedImage => _ImagePreviewSurface(
        card: card,
        isTopCard: isTopCard,
        showFullImage: showFullImage,
        enableTapToggle: enableTapToggle,
      ),
      MediaKind.video || MediaKind.livePhotoVideo => _VideoPreviewSurface(
        card: card,
        isTopCard: isTopCard,
        showFullImage: showFullImage,
      ),
    };
  }
}

enum _PhotoVisualState { loading, ready, error }

class _ImagePreviewSurface extends StatefulWidget {
  const _ImagePreviewSurface({
    required this.card,
    required this.isTopCard,
    required this.showFullImage,
    required this.enableTapToggle,
  });

  final DeckCard card;
  final bool isTopCard;
  final bool showFullImage;
  final bool enableTapToggle;

  @override
  State<_ImagePreviewSurface> createState() => _ImagePreviewSurfaceState();
}

class _ImagePreviewSurfaceState extends State<_ImagePreviewSurface> {
  final TransformationController _transformationController =
      TransformationController();

  late Future<Uint8List?> _thumbnailFuture;
  var _forceFullImage = false;

  bool get _effectiveShowFullImage => widget.showFullImage || _forceFullImage;

  @override
  void initState() {
    super.initState();
    _thumbnailFuture = _loadThumbnail(
      widget.card,
      sizePx: widget.card.kind == MediaKind.animatedImage
          ? _animatedFallbackSizePx
          : _thumbnailSizePx,
    );
  }

  @override
  void didUpdateWidget(covariant _ImagePreviewSurface oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.card.id != widget.card.id) {
      _thumbnailFuture = _loadThumbnail(
        widget.card,
        sizePx: widget.card.kind == MediaKind.animatedImage
            ? _animatedFallbackSizePx
            : _thumbnailSizePx,
      );
      _forceFullImage = false;
      _resetTransform();
    }
  }

  @override
  void dispose() {
    _transformationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final Widget image = FutureBuilder<Uint8List?>(
      future: _thumbnailFuture,
      builder: (BuildContext context, AsyncSnapshot<Uint8List?> snapshot) {
        if (snapshot.hasError ||
            snapshot.connectionState == ConnectionState.done &&
                snapshot.data == null) {
          return const _PhotoFallbackContent(isError: true);
        }

        final Uint8List? bytes = snapshot.data;
        if (bytes == null) {
          return const _PhotoFallbackContent(isError: false);
        }

        return Image.memory(
          bytes,
          width: double.infinity,
          height: double.infinity,
          fit: _effectiveShowFullImage ? BoxFit.contain : BoxFit.cover,
          gaplessPlayback: true,
          filterQuality: FilterQuality.low,
          errorBuilder: (_, _, _) => const _PhotoFallbackContent(isError: true),
        );
      },
    );

    return _MediaBackdrop(
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: widget.isTopCard ? _handleTap : null,
        child: widget.isTopCard
            ? InteractiveViewer(
                transformationController: _transformationController,
                minScale: _minPhotoGestureScale,
                maxScale: _maxPhotoGestureScale,
                boundaryMargin: EdgeInsets.zero,
                clipBehavior: Clip.hardEdge,
                child: SizedBox.expand(child: image),
              )
            : image,
      ),
    );
  }

  void _handleTap() {
    if (!_isTransformAtRest()) {
      _resetTransform();
      return;
    }

    if (!widget.enableTapToggle) {
      return;
    }

    setState(() {
      _forceFullImage = !_forceFullImage;
    });
  }

  bool _isTransformAtRest() {
    final Matrix4 matrix = _transformationController.value;
    final double scale = matrix.getMaxScaleOnAxis();
    final Vector3Translation translation = Vector3Translation.fromMatrix(
      matrix,
    );

    return (scale - _minPhotoGestureScale).abs() <=
            _photoGestureLockScaleEpsilon &&
        translation.x.abs() <= _photoGestureOffsetLockEpsilon &&
        translation.y.abs() <= _photoGestureOffsetLockEpsilon;
  }

  void _resetTransform() {
    _transformationController.value = Matrix4.identity();
  }
}

class _VideoPreviewSurface extends StatefulWidget {
  const _VideoPreviewSurface({
    required this.card,
    required this.isTopCard,
    required this.showFullImage,
  });

  final DeckCard card;
  final bool isTopCard;
  final bool showFullImage;

  @override
  State<_VideoPreviewSurface> createState() => _VideoPreviewSurfaceState();
}

class _VideoPreviewSurfaceState extends State<_VideoPreviewSurface> {
  static const AppLogger _logger = AppLogger('MediaPreviewSurface');

  late Future<Uint8List?> _coverFuture;
  VideoPlayerController? _controller;
  String? _controllerSource;
  _PhotoVisualState _visualState = _PhotoVisualState.loading;
  bool _isCoverVisible = true;
  bool _isLiveMotionActive = false;

  bool get _isLivePhoto => widget.card.kind == MediaKind.livePhotoVideo;

  @override
  void initState() {
    super.initState();
    _coverFuture = _loadThumbnail(
      widget.card,
      sizePx: _thumbnailSizePx,
      frame: _videoCoverPrimaryFrameMs,
    );
    _configurePlayback();
  }

  @override
  void didUpdateWidget(covariant _VideoPreviewSurface oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.card.id != widget.card.id) {
      _coverFuture = _loadThumbnail(
        widget.card,
        sizePx: _thumbnailSizePx,
        frame: _videoCoverPrimaryFrameMs,
      );
      _isCoverVisible = true;
      _isLiveMotionActive = false;
      _visualState = _PhotoVisualState.loading;
    }

    if (oldWidget.card.playbackUri != widget.card.playbackUri ||
        oldWidget.card.kind != widget.card.kind ||
        oldWidget.isTopCard != widget.isTopCard) {
      _configurePlayback();
    }
  }

  @override
  void dispose() {
    unawaited(_disposeController());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final VideoPlayerController? controller = _controller;
    final Uri? playbackUri = widget.card.playbackUri;
    final bool hasPlayableTopCard =
        widget.isTopCard && playbackUri != null && controller != null;
    final bool shouldShowPlayer =
        hasPlayableTopCard &&
        controller.value.isInitialized &&
        (!_isLivePhoto || _isLiveMotionActive);
    final bool shouldShowCover =
        !shouldShowPlayer || _isCoverVisible || !widget.isTopCard;

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: hasPlayableTopCard && !_isLivePhoto ? _toggleVideoPlayback : null,
      onLongPressStart: hasPlayableTopCard && _isLivePhoto
          ? (_) => _startLiveMotion()
          : null,
      onLongPressEnd: hasPlayableTopCard && _isLivePhoto
          ? (_) => _stopLiveMotion()
          : null,
      child: _MediaBackdrop(
        child: Stack(
          fit: StackFit.expand,
          children: <Widget>[
            if (shouldShowPlayer)
              _FittedVideoPlayer(
                controller: controller,
                fit: widget.showFullImage ? BoxFit.contain : BoxFit.cover,
              ),
            if (shouldShowCover)
              AnimatedOpacity(
                opacity: shouldShowPlayer && !_isCoverVisible
                    ? _videoCoverHiddenOpacity
                    : 1,
                duration: const Duration(
                  milliseconds: _videoCoverFadeDurationMs,
                ),
                child: _ThumbnailImage(
                  future: _coverFuture,
                  fit: widget.showFullImage ? BoxFit.contain : BoxFit.cover,
                ),
              ),
            if (_visualState == _PhotoVisualState.error && widget.isTopCard)
              const Center(child: _PhotoFallbackContent(isError: true)),
          ],
        ),
      ),
    );
  }

  void _configurePlayback() {
    final Uri? playbackUri = widget.card.playbackUri;
    if (!widget.isTopCard || playbackUri == null) {
      unawaited(_disposeController());
      return;
    }

    final String source = playbackUri.toString();
    if (_controllerSource == source && _controller != null) {
      return;
    }

    unawaited(_disposeController());
    final VideoPlayerController controller = _buildVideoController(playbackUri);
    _controller = controller;
    _controllerSource = source;
    _visualState = _PhotoVisualState.loading;
    _isCoverVisible = true;
    _isLiveMotionActive = false;
    unawaited(_initializeController(controller, source));
  }

  Future<void> _initializeController(
    VideoPlayerController controller,
    String source,
  ) async {
    try {
      await controller.setLooping(true);
      await controller.setVolume(0);
      await controller.initialize();
      if (!mounted ||
          _controller != controller ||
          _controllerSource != source) {
        return;
      }

      if (_isLivePhoto) {
        await controller.pause();
        await controller.seekTo(Duration.zero);
        if (!mounted ||
            _controller != controller ||
            _controllerSource != source) {
          return;
        }
        setState(() {
          _visualState = _PhotoVisualState.ready;
          _isCoverVisible = true;
          _isLiveMotionActive = false;
        });
      } else {
        await controller.play();
        if (!mounted ||
            _controller != controller ||
            _controllerSource != source) {
          return;
        }
        setState(() {
          _visualState = _PhotoVisualState.ready;
          _isCoverVisible = false;
        });
      }
    } catch (error, stackTrace) {
      _logger.warning(
        'Video preview initialization failed for media id=${widget.card.id}.',
        error: error,
        stackTrace: stackTrace,
      );
      if (!mounted ||
          _controller != controller ||
          _controllerSource != source) {
        return;
      }
      setState(() {
        _visualState = _PhotoVisualState.error;
        _isCoverVisible = true;
        _isLiveMotionActive = false;
      });
    }
  }

  Future<void> _disposeController() async {
    final VideoPlayerController? controller = _controller;
    _controller = null;
    _controllerSource = null;
    if (controller == null) {
      return;
    }

    try {
      await controller.pause();
      await controller.dispose();
    } catch (error, stackTrace) {
      _logger.warning(
        'Video preview disposal failed for media id=${widget.card.id}.',
        error: error,
        stackTrace: stackTrace,
      );
    }
  }

  void _toggleVideoPlayback() {
    final VideoPlayerController? controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }

    if (controller.value.isPlaying) {
      unawaited(controller.pause());
    } else {
      unawaited(controller.play());
      setState(() {
        _isCoverVisible = false;
      });
    }
  }

  void _startLiveMotion() {
    final VideoPlayerController? controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }

    setState(() {
      _isLiveMotionActive = true;
      _isCoverVisible = false;
      _visualState = _PhotoVisualState.ready;
    });
    unawaited(controller.play());
  }

  void _stopLiveMotion() {
    final VideoPlayerController? controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return;
    }

    setState(() {
      _isLiveMotionActive = false;
      _isCoverVisible = true;
    });
    unawaited(controller.pause());
    unawaited(controller.seekTo(Duration.zero));
  }
}

class _ThumbnailImage extends StatelessWidget {
  const _ThumbnailImage({required this.future, required this.fit});

  final Future<Uint8List?> future;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Uint8List?>(
      future: future,
      builder: (BuildContext context, AsyncSnapshot<Uint8List?> snapshot) {
        if (snapshot.hasError ||
            snapshot.connectionState == ConnectionState.done &&
                snapshot.data == null) {
          return const _PhotoFallbackContent(isError: true);
        }

        final Uint8List? bytes = snapshot.data;
        if (bytes == null) {
          return const _PhotoFallbackContent(isError: false);
        }

        return Image.memory(
          bytes,
          width: double.infinity,
          height: double.infinity,
          fit: fit,
          gaplessPlayback: true,
          filterQuality: FilterQuality.low,
          errorBuilder: (_, _, _) => const _PhotoFallbackContent(isError: true),
        );
      },
    );
  }
}

class _FittedVideoPlayer extends StatelessWidget {
  const _FittedVideoPlayer({required this.controller, required this.fit});

  final VideoPlayerController controller;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    final Size videoSize = controller.value.size;
    if (videoSize.width <= 0 || videoSize.height <= 0) {
      return const SizedBox.shrink();
    }

    return ClipRect(
      child: FittedBox(
        fit: fit,
        child: SizedBox(
          width: videoSize.width,
          height: videoSize.height,
          child: VideoPlayer(controller),
        ),
      ),
    );
  }
}

class _MediaBackdrop extends StatelessWidget {
  const _MediaBackdrop({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: <Color>[
            colorScheme.surfaceContainerHighest,
            colorScheme.surfaceContainer,
          ],
        ),
      ),
      child: child,
    );
  }
}

class _PhotoFallbackContent extends StatelessWidget {
  const _PhotoFallbackContent({required this.isError});

  final bool isError;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            DecoratedBox(
              decoration: BoxDecoration(
                color: colorScheme.surface.withValues(alpha: 0.44),
                shape: BoxShape.circle,
              ),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: isError
                    ? Icon(
                        Icons.broken_image_outlined,
                        color: colorScheme.onSurfaceVariant,
                      )
                    : const SizedBox.square(
                        dimension: 24,
                        child: CircularProgressIndicator(strokeWidth: 2.5),
                      ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              isError ? l10n.photoLoadErrorTitle : l10n.photoLoadingTitle,
              style: textTheme.titleMedium?.copyWith(
                color: colorScheme.onSurface,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(
              isError
                  ? l10n.photoLoadErrorDescription
                  : l10n.photoLoadingDescription,
              style: textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

final class Vector3Translation {
  const Vector3Translation({required this.x, required this.y});

  factory Vector3Translation.fromMatrix(Matrix4 matrix) {
    final Float64List storage = matrix.storage;
    return Vector3Translation(x: storage[12], y: storage[13]);
  }

  final double x;
  final double y;
}

Future<Uint8List?> _loadThumbnail(
  DeckCard card, {
  required int sizePx,
  int frame = 0,
}) async {
  try {
    final photo_manager.AssetEntity entity = _assetEntityFor(card);
    return entity.thumbnailDataWithOption(
      photo_manager.ThumbnailOption(
        size: photo_manager.ThumbnailSize.square(sizePx),
        quality: _thumbnailQuality,
        frame: frame,
      ),
    );
  } catch (error, stackTrace) {
    const AppLogger('MediaPreviewSurface').warning(
      'Thumbnail load failed for media id=${card.id}.',
      error: error,
      stackTrace: stackTrace,
    );
    Error.throwWithStackTrace(error, stackTrace);
  }
}

photo_manager.AssetEntity _assetEntityFor(DeckCard card) {
  return photo_manager.AssetEntity(
    id: card.asset.thumbnailSource.assetId,
    typeInt: card.isVideoLike
        ? photo_manager.AssetType.video.index
        : photo_manager.AssetType.image.index,
    width: card.width,
    height: card.height,
    duration: (card.durationMs / Duration.millisecondsPerSecond).round(),
    title: card.displayName.isEmpty ? null : card.displayName,
    createDateSecond: card.dateAddedSeconds,
    relativePath: card.relativePath,
    mimeType: card.mimeType,
  );
}

VideoPlayerController _buildVideoController(Uri uri) {
  if (!kIsWeb &&
      defaultTargetPlatform == TargetPlatform.android &&
      uri.scheme == _contentScheme) {
    return VideoPlayerController.contentUri(uri);
  }

  if (!kIsWeb && uri.scheme == _fileScheme) {
    return VideoPlayerController.file(File.fromUri(uri));
  }

  if (uri.scheme == _httpScheme || uri.scheme == _httpsScheme) {
    return VideoPlayerController.networkUrl(uri);
  }

  if (!kIsWeb && uri.scheme.isEmpty) {
    return VideoPlayerController.file(File(uri.toString()));
  }

  return VideoPlayerController.networkUrl(uri);
}

const int _thumbnailSizePx = 1080;
const int _animatedFallbackSizePx = 1080;
const int _thumbnailQuality = 90;
const int _videoCoverPrimaryFrameMs = 80;
const int _videoCoverFadeDurationMs = 220;
const double _videoCoverHiddenOpacity = 0;
const double _minPhotoGestureScale = 1;
const double _maxPhotoGestureScale = 4;
const double _photoGestureLockScaleEpsilon = 0.02;
const double _photoGestureOffsetLockEpsilon = 0.5;
const String _contentScheme = 'content';
const String _fileScheme = 'file';
const String _httpScheme = 'http';
const String _httpsScheme = 'https';

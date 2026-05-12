import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/logging/app_logger.dart';
import '../../../core/result/app_result.dart';
import '../../media/domain/gallery_repository.dart';
import '../../media/domain/models/media_asset.dart';
import '../../media/domain/services/media_shuffle_service.dart';
import '../../media/data/gallery_repository_impl.dart';
import '../data/deck_session_repository.dart';
import '../data/hive_deck_session_repository.dart';
import '../domain/models/deck_card.dart';
import 'gallery_deck_state.dart';

final NotifierProvider<GalleryDeckController, GalleryDeckState>
galleryDeckControllerProvider =
    NotifierProvider<GalleryDeckController, GalleryDeckState>(
      GalleryDeckController.new,
    );

final class GalleryDeckController extends Notifier<GalleryDeckState> {
  GalleryDeckController({
    GalleryRepository? galleryRepository,
    MediaShuffleService? shuffleService,
    DeckSessionRepository? sessionRepository,
    AppLogger logger = const AppLogger('GalleryDeckController'),
  }) : _injectedGalleryRepository = galleryRepository,
       _injectedShuffleService = shuffleService,
       _injectedSessionRepository = sessionRepository,
       _logger = logger;

  final GalleryRepository? _injectedGalleryRepository;
  final MediaShuffleService? _injectedShuffleService;
  final DeckSessionRepository? _injectedSessionRepository;
  final AppLogger _logger;

  late GalleryRepository _galleryRepository;
  late MediaShuffleService _shuffleService;
  late DeckSessionRepository _sessionRepository;

  List<String> _queueIds = <String>[];
  final Map<String, DeckCard> _mediaCardCache = <String, DeckCard>{};
  var _currentIndex = 0;
  var _loadGeneration = 0;

  List<String> get queueIds => List<String>.unmodifiable(_queueIds);

  int get currentIndex => _currentIndex;

  @override
  GalleryDeckState build() {
    _galleryRepository =
        _injectedGalleryRepository ?? ref.watch(galleryRepositoryProvider);
    _shuffleService =
        _injectedShuffleService ?? ref.watch(mediaShuffleServiceProvider);
    _sessionRepository =
        _injectedSessionRepository ?? ref.watch(deckSessionRepositoryProvider);

    return const GalleryDeckState.initial();
  }

  Future<AppResult<void>> initialLoad() async {
    final int generation = _beginLoad();
    final bool restoredReady = await _restoreSession(generation);
    if (!_isCurrentLoad(generation)) {
      return const AppResult<void>.success(null);
    }

    return _refreshMedia(
      generation: generation,
      showLoading: !restoredReady,
      preserveReadyOnFailure: restoredReady,
    );
  }

  Future<AppResult<void>> reloadFromSource() {
    final int generation = _beginLoad();
    return _refreshMedia(
      generation: generation,
      showLoading: true,
      preserveReadyOnFailure: false,
    );
  }

  Future<bool> moveToNext({String? expectedTopCardId}) async {
    if (!_isTopVisibleId(expectedTopCardId) || !_canSwipeToNext()) {
      return false;
    }

    _setCurrentIndex(_currentIndex + 1);
    _emitQueueState();
    await refreshSessionCache();
    return true;
  }

  Future<bool> moveToPrevious({String? expectedTopCardId}) async {
    if (!_isTopVisibleId(expectedTopCardId) || !_canSwipeToPrevious()) {
      return false;
    }

    _setCurrentIndex(_currentIndex - 1);
    _emitQueueState();
    await refreshSessionCache();
    return true;
  }

  Future<bool> moveSkip({String? expectedTopCardId}) {
    return moveToNext(expectedTopCardId: expectedTopCardId);
  }

  Future<AppResult<void>> markSessionCacheDirty() {
    return refreshSessionCache();
  }

  Future<AppResult<void>> refreshSessionCache() {
    return _sessionRepository.saveSession(
      DeckSessionSnapshot(
        queueIds: List<String>.unmodifiable(_queueIds),
        currentIndex: _currentIndex,
        visibleCards: _visibleCardsForCurrentWindow(),
      ),
    );
  }

  bool isTopVisibleId(String cardId) {
    return _idAt(_currentIndex) == cardId;
  }

  int _beginLoad() {
    _loadGeneration += 1;
    return _loadGeneration;
  }

  bool _isCurrentLoad(int generation) {
    return generation == _loadGeneration;
  }

  Future<bool> _restoreSession(int generation) async {
    final AppResult<DeckSessionSnapshot?> sessionResult =
        await _sessionRepository.loadSession();
    if (!_isCurrentLoad(generation)) {
      return false;
    }

    switch (sessionResult) {
      case AppSuccess<DeckSessionSnapshot?>(:final value):
        final DeckSessionSnapshot? session = value;
        if (session == null || !session.hasQueue) {
          return false;
        }

        _queueIds = session.queueIds.toList(growable: true);
        _setCurrentIndex(session.currentIndex);
        _updateMediaCardCache(session.visibleCards);

        if (_queueIds.isNotEmpty &&
            _currentIndex < _queueIds.length &&
            _hasCachedCardsForVisibleWindow()) {
          _emitQueueState();
          return state is GalleryDeckReady;
        }

        if (_queueIds.isNotEmpty) {
          state = const GalleryDeckState.loading();
        }
        return false;
      case AppFailure<DeckSessionSnapshot?>(:final error):
        _logger.warning(
          'Falling back to a fresh deck load after session restore failed.',
          error: error,
          stackTrace: error.stackTrace,
        );
        return false;
    }
  }

  Future<AppResult<void>> _refreshMedia({
    required int generation,
    required bool showLoading,
    required bool preserveReadyOnFailure,
  }) async {
    if (showLoading && state is! GalleryDeckReady) {
      state = const GalleryDeckState.loading();
    }

    final AppResult<List<MediaAsset>> mediaResult = await _galleryRepository
        .loadMediaAssets();
    if (!_isCurrentLoad(generation)) {
      return const AppResult<void>.success(null);
    }

    switch (mediaResult) {
      case AppSuccess<List<MediaAsset>>(:final value):
        final List<MediaAsset> shuffledCards = _shuffleService
            .buildSmartShuffle(value);
        final List<String> shuffledIds = <String>[
          for (final MediaAsset card in shuffledCards) card.id,
        ];

        _queueIds = shuffledIds.toList(growable: true);
        _updateMediaCardCache(<DeckCard>[
          for (final MediaAsset asset in shuffledCards)
            DeckCard.fromAsset(asset),
        ]);
        _setCurrentIndex(_coerceCurrentIndexIntoQueueBounds(_currentIndex));
        _emitQueueState();
        await refreshSessionCache();
        return const AppResult<void>.success(null);
      case AppFailure<List<MediaAsset>>(:final error):
        _logger.warning(
          'Gallery deck reload failed.',
          error: error,
          stackTrace: error.stackTrace,
        );

        if (!(preserveReadyOnFailure && state is GalleryDeckReady)) {
          state = GalleryDeckState.error(error);
        }

        return AppResult<void>.failure(error);
    }
  }

  void _emitQueueState() {
    final List<DeckCard> visibleCards = _visibleCardsForCurrentWindow();

    state = visibleCards.isEmpty
        ? const GalleryDeckState.empty()
        : GalleryDeckState.ready(
            previousCard: _previousCard(),
            visibleCards: visibleCards,
            currentIndex: _currentIndex,
            totalQueueLength: _queueIds.length,
            canSwipeToPrevious: _canSwipeToPrevious(),
            canSwipeToNext: _canSwipeToNext(),
          );
  }

  void _updateMediaCardCache(List<DeckCard> cards) {
    _mediaCardCache.clear();
    for (final DeckCard card in cards) {
      _mediaCardCache[card.id] = card;
    }
  }

  bool _hasCachedCardsForVisibleWindow() {
    final List<String> visibleIds = _visibleIdsForCurrentWindow();

    return visibleIds.isNotEmpty &&
        visibleIds.every(_mediaCardCache.containsKey);
  }

  List<String> _visibleIdsForCurrentWindow() {
    if (_currentIndex >= _queueIds.length) {
      return const <String>[];
    }

    final int endExclusive = _coerceIndexIntoQueueRange(
      _currentIndex + GalleryDeckReady.maxVisibleCardCount,
    );
    return List<String>.unmodifiable(
      _queueIds.sublist(_currentIndex, endExclusive),
    );
  }

  List<DeckCard> _visibleCardsForCurrentWindow() {
    return List<DeckCard>.unmodifiable(
      _visibleIdsForCurrentWindow()
          .map((String id) => _mediaCardCache[id])
          .whereType<DeckCard>(),
    );
  }

  DeckCard? _previousCard() {
    final int previousIndex = _currentIndex - 1;
    if (previousIndex < 0 || previousIndex >= _queueIds.length) {
      return null;
    }

    return _mediaCardCache[_queueIds[previousIndex]];
  }

  String? _idAt(int index) {
    if (index < 0 || index >= _queueIds.length) {
      return null;
    }

    return _queueIds[index];
  }

  bool _isTopVisibleId(String? expectedTopCardId) {
    if (expectedTopCardId == null) {
      return true;
    }

    return isTopVisibleId(expectedTopCardId);
  }

  bool _canSwipeToPrevious() {
    return _currentIndex > 0;
  }

  bool _canSwipeToNext() {
    return _currentIndex + 1 < _queueIds.length;
  }

  void _setCurrentIndex(int value) {
    _currentIndex = value < 0 ? 0 : value;
  }

  int _coerceCurrentIndexIntoQueueBounds(int value) {
    if (value < 0) {
      return 0;
    }

    if (value > _queueIds.length) {
      return _queueIds.length;
    }

    return value;
  }

  int _coerceIndexIntoQueueRange(int value) {
    if (value < 0) {
      return 0;
    }

    if (value > _queueIds.length) {
      return _queueIds.length;
    }

    return value;
  }
}

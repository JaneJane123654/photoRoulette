import '../../../core/error/app_error.dart';
import '../domain/models/deck_card.dart';

sealed class GalleryDeckState {
  const GalleryDeckState();

  const factory GalleryDeckState.initial() = GalleryDeckInitial;

  const factory GalleryDeckState.loading() = GalleryDeckLoading;

  const factory GalleryDeckState.empty() = GalleryDeckEmpty;

  const factory GalleryDeckState.ready({
    required DeckCard? previousCard,
    required List<DeckCard> visibleCards,
    required int currentIndex,
    required int totalQueueLength,
    required bool canSwipeToPrevious,
    required bool canSwipeToNext,
  }) = GalleryDeckReady;

  const factory GalleryDeckState.error(AppError error) = GalleryDeckError;
}

final class GalleryDeckInitial extends GalleryDeckState {
  const GalleryDeckInitial();
}

final class GalleryDeckLoading extends GalleryDeckState {
  const GalleryDeckLoading();
}

final class GalleryDeckEmpty extends GalleryDeckState {
  const GalleryDeckEmpty();
}

final class GalleryDeckReady extends GalleryDeckState {
  const GalleryDeckReady({
    required this.previousCard,
    required this.visibleCards,
    required this.currentIndex,
    required this.totalQueueLength,
    required this.canSwipeToPrevious,
    required this.canSwipeToNext,
  }) : assert(
         visibleCards.length > 0,
         'Ready must contain at least one visible media ID.',
       ),
       assert(
         visibleCards.length <= maxVisibleCardCount,
         'Ready can expose at most $maxVisibleCardCount visible media IDs.',
       );

  static const int maxVisibleCardCount = 3;

  final DeckCard? previousCard;
  final List<DeckCard> visibleCards;
  final int currentIndex;
  final int totalQueueLength;
  final bool canSwipeToPrevious;
  final bool canSwipeToNext;

  DeckCard get currentCard => visibleCards.first;
}

final class GalleryDeckError extends GalleryDeckState {
  const GalleryDeckError(this.error);

  final AppError error;
}

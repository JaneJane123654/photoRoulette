import '../../../core/result/app_result.dart';
import '../domain/models/deck_card.dart';

abstract interface class DeckSessionRepository {
  Future<AppResult<DeckSessionSnapshot?>> loadSession();

  Future<AppResult<void>> saveSession(DeckSessionSnapshot session);

  Future<AppResult<void>> clearSession();
}

final class DeckSessionSnapshot {
  const DeckSessionSnapshot({
    required this.queueIds,
    required this.currentIndex,
    required this.visibleCards,
  });

  final List<String> queueIds;
  final int currentIndex;
  final List<DeckCard> visibleCards;

  bool get hasQueue => queueIds.isNotEmpty;

  bool get hasVisibleCardCache => visibleCards.isNotEmpty;
}

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:hive/hive.dart';

import '../../../core/constants/hive_box_names.dart';
import '../../../core/constants/persistence_keys.dart';
import '../../../core/error/app_error.dart';
import '../../../core/result/app_result.dart';
import '../domain/models/deck_card.dart';
import 'deck_session_repository.dart';

final Provider<DeckSessionRepository> deckSessionRepositoryProvider =
    Provider<DeckSessionRepository>((Ref ref) {
      return HiveDeckSessionRepository();
    });

final class HiveDeckSessionRepository implements DeckSessionRepository {
  HiveDeckSessionRepository({this.boxName = HiveBoxNames.session});

  final String boxName;
  Box<dynamic>? _box;

  Future<Box<dynamic>> _openBox() async {
    final Box<dynamic>? cachedBox = _box;
    if (cachedBox != null && cachedBox.isOpen) {
      return cachedBox;
    }

    if (Hive.isBoxOpen(boxName)) {
      _box = Hive.box<dynamic>(boxName);
    } else {
      _box = await Hive.openBox<dynamic>(boxName);
    }

    return _box!;
  }

  @override
  Future<AppResult<DeckSessionSnapshot?>> loadSession() {
    return AppResult.guard<DeckSessionSnapshot?>(() async {
      final Box<dynamic> box = await _openBox();
      final List<String> queueIds = _readStringList(
        box.get(PersistenceKeys.deckQueueIds),
      );
      if (queueIds.isEmpty) {
        return null;
      }

      return DeckSessionSnapshot(
        queueIds: queueIds,
        currentIndex: _readNonNegativeInt(
          box.get(PersistenceKeys.deckCurrentIndex),
        ),
        visibleCards: _readVisibleCards(
          box.get(PersistenceKeys.deckVisibleCards),
        ),
      );
    }, errorMapper: _mapStorageFailure);
  }

  @override
  Future<AppResult<void>> saveSession(DeckSessionSnapshot session) {
    return AppResult.guard<void>(() async {
      final Box<dynamic> box = await _openBox();
      await box.putAll(<String, Object?>{
        PersistenceKeys.deckQueueIds: session.queueIds,
        PersistenceKeys.deckCurrentIndex: session.currentIndex,
        PersistenceKeys.deckVisibleCards: <Map<String, Object?>>[
          for (final DeckCard card in session.visibleCards) card.toStorageMap(),
        ],
      });
    }, errorMapper: _mapStorageFailure);
  }

  @override
  Future<AppResult<void>> clearSession() {
    return AppResult.guard<void>(() async {
      final Box<dynamic> box = await _openBox();
      await box.deleteAll(<String>[
        PersistenceKeys.deckQueueIds,
        PersistenceKeys.deckCurrentIndex,
        PersistenceKeys.deckVisibleCards,
      ]);
    }, errorMapper: _mapStorageFailure);
  }

  List<String> _readStringList(Object? value) {
    if (value is! Iterable<dynamic>) {
      return const <String>[];
    }

    final List<String> values = <String>[];
    for (final dynamic item in value) {
      if (item is String && item.trim().isNotEmpty) {
        values.add(item);
      }
    }

    return List<String>.unmodifiable(values);
  }

  List<DeckCard> _readVisibleCards(Object? value) {
    if (value is! Iterable<dynamic>) {
      return const <DeckCard>[];
    }

    final List<DeckCard> cards = <DeckCard>[];
    for (final dynamic item in value) {
      final DeckCard? card = DeckCard.tryFromStorageValue(item as Object?);
      if (card != null) {
        cards.add(card);
      }
    }

    return List<DeckCard>.unmodifiable(cards);
  }

  int _readNonNegativeInt(Object? value) {
    final int intValue = value is int
        ? value
        : value is num
        ? value.toInt()
        : 0;
    return intValue < 0 ? 0 : intValue;
  }

  AppError _mapStorageFailure(Object error, StackTrace stackTrace) {
    return mapStorageException(
      error,
      stackTrace,
      message: 'Deck session storage failed.',
    );
  }
}

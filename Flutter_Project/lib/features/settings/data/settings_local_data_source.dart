import 'package:hive/hive.dart';

import '../../../core/constants/hive_box_names.dart';

typedef SettingsValueMap = Map<String, Object?>;
typedef SettingsValueMutation = void Function(SettingsValueMap values);

abstract interface class SettingsLocalDataSource {
  Future<SettingsValueMap> readAll();

  Future<void> replaceAll(SettingsValueMap values);

  Future<void> updateValues(SettingsValueMutation mutation);

  Stream<SettingsValueMap> watchValues();
}

final class HiveSettingsLocalDataSource implements SettingsLocalDataSource {
  HiveSettingsLocalDataSource({this.boxName = HiveBoxNames.settings});

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
  Future<SettingsValueMap> readAll() async {
    final Box<dynamic> box = await _openBox();
    return _snapshot(box);
  }

  @override
  Future<void> replaceAll(SettingsValueMap values) async {
    final Box<dynamic> box = await _openBox();
    await _syncBox(box, values);
  }

  @override
  Future<void> updateValues(SettingsValueMutation mutation) async {
    final Box<dynamic> box = await _openBox();
    final SettingsValueMap values = <String, Object?>{..._snapshot(box)};

    mutation(values);

    await _syncBox(box, values);
  }

  @override
  Stream<SettingsValueMap> watchValues() async* {
    final Box<dynamic> box = await _openBox();
    yield _snapshot(box);

    await for (final BoxEvent _ in box.watch()) {
      yield _snapshot(box);
    }
  }

  SettingsValueMap _snapshot(Box<dynamic> box) {
    final SettingsValueMap values = <String, Object?>{};
    for (final dynamic rawKey in box.keys) {
      if (rawKey is String) {
        values[rawKey] = box.get(rawKey);
      }
    }

    return Map<String, Object?>.unmodifiable(values);
  }

  Future<void> _syncBox(Box<dynamic> box, SettingsValueMap values) async {
    final Set<String> incomingKeys = values.keys.toSet();
    final List<String> keysToDelete = <String>[];

    for (final dynamic rawKey in box.keys) {
      if (rawKey is String &&
          (!incomingKeys.contains(rawKey) || values[rawKey] == null)) {
        keysToDelete.add(rawKey);
      }
    }

    for (final MapEntry<String, Object?> entry in values.entries) {
      if (entry.value == null) {
        keysToDelete.add(entry.key);
      }
    }

    if (keysToDelete.isNotEmpty) {
      await box.deleteAll(keysToDelete.toSet());
    }

    final Map<String, Object> valuesToWrite = <String, Object>{};
    for (final MapEntry<String, Object?> entry in values.entries) {
      final Object? value = entry.value;
      if (value != null) {
        valuesToWrite[entry.key] = _normalizeHiveValue(value);
      }
    }

    if (valuesToWrite.isNotEmpty) {
      await box.putAll(valuesToWrite);
    }
  }

  Object _normalizeHiveValue(Object value) {
    if (value is Set<String>) {
      return value.toList(growable: false);
    }

    return value;
  }
}

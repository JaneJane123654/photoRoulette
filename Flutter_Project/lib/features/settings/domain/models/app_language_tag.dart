final class AppLanguageTag {
  const AppLanguageTag._(this.storageValue);

  static const AppLanguageTag system = AppLanguageTag._('system');
  static const AppLanguageTag arabic = AppLanguageTag._('ar');
  static const AppLanguageTag english = AppLanguageTag._('en');
  static const AppLanguageTag spanish = AppLanguageTag._('es');
  static const AppLanguageTag french = AppLanguageTag._('fr');
  static const AppLanguageTag russian = AppLanguageTag._('ru');
  static const AppLanguageTag chinese = AppLanguageTag._('zh');

  static const String systemStorageValue = 'system';

  static const List<String> explicitStorageValues = <String>[
    'ar',
    'en',
    'es',
    'fr',
    'ru',
    'zh',
  ];

  static const Map<String, AppLanguageTag> _valuesByStorageValue =
      <String, AppLanguageTag>{
        'system': system,
        'ar': arabic,
        'en': english,
        'es': spanish,
        'fr': french,
        'ru': russian,
        'zh': chinese,
      };

  final String storageValue;

  static AppLanguageTag fromStorageValue(String? value) {
    return _valuesByStorageValue[normalizeLanguageTag(value)] ?? system;
  }

  static String normalizeLanguageTag(String? tag) {
    if (tag == null) {
      return systemStorageValue;
    }

    if (tag == systemStorageValue) {
      return tag;
    }

    final int separatorIndex = tag.indexOf('-');
    final String primarySubtag = separatorIndex == -1
        ? tag
        : tag.substring(0, separatorIndex);
    final String normalized = primarySubtag.toLowerCase();

    if (explicitStorageValues.contains(normalized)) {
      return normalized;
    }

    return systemStorageValue;
  }

  bool get isSystem => storageValue == systemStorageValue;

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is AppLanguageTag && other.storageValue == storageValue;
  }

  @override
  int get hashCode => storageValue.hashCode;

  @override
  String toString() => storageValue;
}

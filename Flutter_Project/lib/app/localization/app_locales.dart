import 'package:flutter/widgets.dart';

abstract final class AppLocales {
  static const String systemLanguageTag = 'system';

  static const List<String> explicitLanguageTags = <String>[
    'ar',
    'en',
    'es',
    'fr',
    'ru',
    'zh',
  ];

  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('ru'),
    Locale('zh'),
  ];

  static const Locale fallbackLocale = Locale('en');

  static String normalizeLanguageTag(String tag) {
    if (tag == systemLanguageTag) {
      return tag;
    }

    final String normalized = tag.split('-').first.toLowerCase();
    return explicitLanguageTags.contains(normalized)
        ? normalized
        : systemLanguageTag;
  }

  static Locale? localeFromLanguageTag(String tag) {
    final String normalizedTag = normalizeLanguageTag(tag);
    if (normalizedTag == systemLanguageTag) {
      return null;
    }

    return Locale(normalizedTag);
  }

  static Locale resolveSystemLocale(
    Iterable<Locale>? preferredLocales,
    Iterable<Locale> supportedLocales,
  ) {
    for (final Locale preferredLocale
        in preferredLocales ?? const Iterable<Locale>.empty()) {
      final String normalizedTag = normalizeLanguageTag(
        preferredLocale.toLanguageTag(),
      );
      if (normalizedTag == systemLanguageTag) {
        continue;
      }

      for (final Locale supportedLocale in supportedLocales) {
        if (supportedLocale.languageCode == normalizedTag) {
          return supportedLocale;
        }
      }
    }

    return fallbackLocale;
  }
}

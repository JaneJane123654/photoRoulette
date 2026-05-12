import 'package:flutter/material.dart';

abstract final class AppTextStyles {
  static const String serifFontFamily = 'serif';
  static const String sansSerifFontFamily = 'sans';

  static const TextStyle headlineLarge = TextStyle(
    fontFamily: serifFontFamily,
    fontWeight: FontWeight.bold,
    fontSize: 34,
    height: 40 / 34,
  );

  static const TextStyle headlineMedium = TextStyle(
    fontFamily: serifFontFamily,
    fontWeight: FontWeight.w600,
    fontSize: 28,
    height: 34 / 28,
  );

  static const TextStyle headlineSmall = TextStyle(
    fontFamily: serifFontFamily,
    fontWeight: FontWeight.w600,
    fontSize: 24,
    height: 30 / 24,
  );

  static const TextStyle titleMedium = TextStyle(
    fontFamily: sansSerifFontFamily,
    fontWeight: FontWeight.w600,
    fontSize: 16,
    height: 22 / 16,
  );

  static const TextStyle bodyLarge = TextStyle(
    fontFamily: sansSerifFontFamily,
    fontWeight: FontWeight.normal,
    fontSize: 16,
    height: 24 / 16,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontFamily: sansSerifFontFamily,
    fontWeight: FontWeight.normal,
    fontSize: 14,
    height: 20 / 14,
  );

  static const TextTheme textTheme = TextTheme(
    headlineLarge: headlineLarge,
    headlineMedium: headlineMedium,
    headlineSmall: headlineSmall,
    titleMedium: titleMedium,
    bodyLarge: bodyLarge,
    bodyMedium: bodyMedium,
  );
}

import 'package:flutter/material.dart';

abstract final class AppColors {
  static const Color lightPrimary = Color(0xFF5A3A28);
  static const Color lightOnPrimary = Color(0xFFFFF8F3);
  static const Color lightPrimaryContainer = Color(0xFFF1D3BF);
  static const Color lightOnPrimaryContainer = Color(0xFF2B160B);
  static const Color lightSecondary = Color(0xFF3F5C5E);
  static const Color lightOnSecondary = Color(0xFFF3FBFB);
  static const Color lightSecondaryContainer = Color(0xFFC7E4E5);
  static const Color lightOnSecondaryContainer = Color(0xFF11282A);
  static const Color lightTertiary = Color(0xFF6D4A65);
  static const Color lightOnTertiary = Color(0xFFFFF7FB);
  static const Color lightTertiaryContainer = Color(0xFFF6D7ED);
  static const Color lightOnTertiaryContainer = Color(0xFF2C1127);
  static const Color lightBackground = Color(0xFFF6F0E7);
  static const Color lightOnBackground = Color(0xFF201A17);
  static const Color lightSurface = Color(0xFFFCF7F2);
  static const Color lightOnSurface = Color(0xFF201A17);
  static const Color lightSurfaceVariant = Color(0xFFE8DED5);
  static const Color lightOnSurfaceVariant = Color(0xFF51453D);

  static const Color darkPrimary = Color(0xFFE0B59A);
  static const Color darkOnPrimary = Color(0xFF3A2114);
  static const Color darkPrimaryContainer = Color(0xFF5A3A28);
  static const Color darkOnPrimaryContainer = Color(0xFFFFDCC7);
  static const Color darkSecondary = Color(0xFFA9CBCD);
  static const Color darkOnSecondary = Color(0xFF0B3436);
  static const Color darkSecondaryContainer = Color(0xFF25484A);
  static const Color darkOnSecondaryContainer = Color(0xFFC7E4E5);
  static const Color darkTertiary = Color(0xFFD6B9CD);
  static const Color darkOnTertiary = Color(0xFF3D2237);
  static const Color darkTertiaryContainer = Color(0xFF563A4E);
  static const Color darkOnTertiaryContainer = Color(0xFFF6D7ED);
  static const Color darkBackground = Color(0xFF12100E);
  static const Color darkOnBackground = Color(0xFFECE1D9);
  static const Color darkSurface = Color(0xFF171411);
  static const Color darkOnSurface = Color(0xFFECE1D9);
  static const Color darkSurfaceVariant = Color(0xFF51453D);
  static const Color darkOnSurfaceVariant = Color(0xFFD3C3B8);

  static const ColorScheme lightScheme = ColorScheme.light(
    primary: lightPrimary,
    onPrimary: lightOnPrimary,
    primaryContainer: lightPrimaryContainer,
    onPrimaryContainer: lightOnPrimaryContainer,
    secondary: lightSecondary,
    onSecondary: lightOnSecondary,
    secondaryContainer: lightSecondaryContainer,
    onSecondaryContainer: lightOnSecondaryContainer,
    tertiary: lightTertiary,
    onTertiary: lightOnTertiary,
    tertiaryContainer: lightTertiaryContainer,
    onTertiaryContainer: lightOnTertiaryContainer,
    surface: lightSurface,
    onSurface: lightOnSurface,
    surfaceContainerHighest: lightSurfaceVariant,
    onSurfaceVariant: lightOnSurfaceVariant,
  );

  static const ColorScheme darkScheme = ColorScheme.dark(
    primary: darkPrimary,
    onPrimary: darkOnPrimary,
    primaryContainer: darkPrimaryContainer,
    onPrimaryContainer: darkOnPrimaryContainer,
    secondary: darkSecondary,
    onSecondary: darkOnSecondary,
    secondaryContainer: darkSecondaryContainer,
    onSecondaryContainer: darkOnSecondaryContainer,
    tertiary: darkTertiary,
    onTertiary: darkOnTertiary,
    tertiaryContainer: darkTertiaryContainer,
    onTertiaryContainer: darkOnTertiaryContainer,
    surface: darkSurface,
    onSurface: darkOnSurface,
    surfaceContainerHighest: darkSurfaceVariant,
    onSurfaceVariant: darkOnSurfaceVariant,
  );
}

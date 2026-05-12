import 'package:flutter/material.dart';

import '../../l10n/generated/app_localizations.dart';
import '../localization/app_locales.dart';
import '../router/app_router.dart';
import '../theme/app_theme.dart';

class PhotoRouletteAppRoot extends StatelessWidget {
  const PhotoRouletteAppRoot({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      onGenerateTitle: (BuildContext context) =>
          AppLocalizations.of(context).appTitle,
      debugShowCheckedModeBanner: false,
      routerConfig: appRouter,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocales.supportedLocales,
      localeListResolutionCallback: AppLocales.resolveSystemLocale,
    );
  }
}

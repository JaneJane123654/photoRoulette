import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/bootstrap/app_bootstrap.dart';
import 'app/bootstrap/app_root.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final AppBootstrapResult bootstrapResult = await bootstrapApp();

  runApp(
    ProviderScope(
      overrides: [
        appBootstrapResultProvider.overrideWithValue(bootstrapResult),
      ],
      child: const PhotoRouletteAppRoot(),
    ),
  );
}

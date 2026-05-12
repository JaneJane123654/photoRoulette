import 'package:go_router/go_router.dart';

import '../../features/permissions/presentation/permission_gate_page.dart';

abstract final class AppRouteNames {
  static const String permissionGate = 'permissionGate';
}

abstract final class AppRoutePaths {
  static const String permissionGate = '/';
}

GoRouter createAppRouter() {
  return GoRouter(
    initialLocation: AppRoutePaths.permissionGate,
    routes: <RouteBase>[
      GoRoute(
        path: AppRoutePaths.permissionGate,
        name: AppRouteNames.permissionGate,
        builder: (_, _) => const PermissionGatePage(),
      ),
    ],
  );
}

final GoRouter appRouter = createAppRouter();

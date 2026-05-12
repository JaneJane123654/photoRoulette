import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:photo_roulette_flutter/core/core.dart';
import 'package:photo_roulette_flutter/features/permissions/permissions.dart';

void main() {
  ProviderContainer createContainer(FakePermissionService service) {
    final ProviderContainer container = ProviderContainer(
      overrides: [permissionServiceProvider.overrideWithValue(service)],
    );
    addTearDown(container.dispose);
    return container;
  }

  group('PermissionController', () {
    test(
      'initial refresh preserves partial access as a distinct state',
      () async {
        final FakePermissionService service = FakePermissionService(
          currentAccessLevel: PermissionAccessLevel.grantedPartial,
        );
        final ProviderContainer container = createContainer(service);
        final PermissionController controller = container.read(
          permissionControllerProvider.notifier,
        );

        controller.markRationaleDismissed();
        await controller.initialRefresh();

        final PermissionState state = container.read(
          permissionControllerProvider,
        );

        expect(state.accessLevel, PermissionAccessLevel.grantedPartial);
        expect(state.isRationaleDismissed, isFalse);
        expect(state.shouldShowPlatformRationale, isFalse);
        expect(state.hasMediaAccess, isTrue);
      },
    );

    test(
      'manual request records the attempt and dismisses rationale',
      () async {
        final FakePermissionService service = FakePermissionService(
          currentAccessLevel: PermissionAccessLevel.denied,
          requestAccessLevel: PermissionAccessLevel.denied,
          shouldShowRationale: true,
        );
        final ProviderContainer container = createContainer(service);
        final PermissionController controller = container.read(
          permissionControllerProvider.notifier,
        );

        await controller.requestPermissionNow();

        final PermissionState state = container.read(
          permissionControllerProvider,
        );

        expect(state.accessLevel, PermissionAccessLevel.denied);
        expect(state.hasRequestedPermissionInSession, isTrue);
        expect(state.isRationaleDismissed, isTrue);
        expect(state.shouldShowPlatformRationale, isTrue);
        expect(state.shouldShowRationale, isFalse);
        expect(state.isRequestInProgress, isFalse);
      },
    );

    test('auto request can leave rationale visible state untouched', () async {
      final FakePermissionService service = FakePermissionService(
        requestAccessLevel: PermissionAccessLevel.grantedAll,
      );
      final ProviderContainer container = createContainer(service);
      final PermissionController controller = container.read(
        permissionControllerProvider.notifier,
      );

      expect(
        container
            .read(permissionControllerProvider)
            .shouldAutoRequestPermission,
        isTrue,
      );

      await controller.requestPermissionNow(dismissRationale: false);

      final PermissionState state = container.read(
        permissionControllerProvider,
      );

      expect(state.accessLevel, PermissionAccessLevel.grantedAll);
      expect(state.hasRequestedPermissionInSession, isTrue);
      expect(state.isRationaleDismissed, isFalse);
      expect(state.shouldAutoRequestPermission, isFalse);
    });

    test(
      'app settings navigation emits an effect and keeps denied UI reachable',
      () async {
        final FakePermissionService service = FakePermissionService();
        final ProviderContainer container = createContainer(service);
        final PermissionController controller = container.read(
          permissionControllerProvider.notifier,
        );
        final Stream<PermissionEffect> effects = container.read(
          permissionEffectsProvider,
        );
        final Future<PermissionEffect> nextEffect = effects.first;

        await controller.requestAppSettingsNavigation();
        await controller.refreshAfterReturningFromSettings();

        final PermissionEffect effect = await nextEffect;
        final PermissionState state = container.read(
          permissionControllerProvider,
        );

        expect(effect, isA<OpenAppSettingsRequested>());
        expect(service.openSettingsCallCount, 1);
        expect(state.accessLevel, PermissionAccessLevel.denied);
        expect(state.shouldAutoRequestPermission, isTrue);
      },
    );
  });
}

final class FakePermissionService implements PermissionService {
  FakePermissionService({
    this.currentAccessLevel = PermissionAccessLevel.denied,
    this.requestAccessLevel = PermissionAccessLevel.denied,
    this.shouldShowRationale = false,
    this.openSettingsResult = true,
  });

  PermissionAccessLevel currentAccessLevel;
  PermissionAccessLevel requestAccessLevel;
  bool shouldShowRationale;
  bool openSettingsResult;
  int openSettingsCallCount = 0;

  @override
  Future<AppResult<PermissionAccessLevel>> readCurrentAccessLevel() async {
    return AppResult<PermissionAccessLevel>.success(currentAccessLevel);
  }

  @override
  Future<AppResult<PermissionAccessLevel>> requestMediaAccess() async {
    currentAccessLevel = requestAccessLevel;
    return AppResult<PermissionAccessLevel>.success(requestAccessLevel);
  }

  @override
  Future<AppResult<bool>> shouldShowRequestPermissionRationale() async {
    return AppResult<bool>.success(shouldShowRationale);
  }

  @override
  Future<AppResult<bool>> openAppSettings() async {
    openSettingsCallCount += 1;
    if (openSettingsResult) {
      return const AppResult<bool>.success(true);
    }

    return AppResult<bool>.failure(
      AppError.platformContractFailure(
        message: 'App settings page could not be opened.',
      ),
    );
  }
}

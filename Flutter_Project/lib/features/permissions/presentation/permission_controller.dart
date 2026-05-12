import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/core.dart';
import '../data/permission_service_impl.dart';
import '../domain/models/permission_access_level.dart';
import '../domain/models/permission_state.dart';
import '../domain/permission_service.dart';
import 'permission_effect.dart';

final NotifierProvider<PermissionController, PermissionState>
permissionControllerProvider =
    NotifierProvider<PermissionController, PermissionState>(
      PermissionController.new,
    );

final Provider<Stream<PermissionEffect>> permissionEffectsProvider =
    Provider<Stream<PermissionEffect>>((Ref ref) {
      final PermissionController controller = ref.watch(
        permissionControllerProvider.notifier,
      );
      return controller.effects;
    });

final StreamProvider<PermissionEffect> permissionEffectEventsProvider =
    StreamProvider<PermissionEffect>((Ref ref) {
      return ref.watch(permissionEffectsProvider);
    });

final class PermissionController extends Notifier<PermissionState> {
  PermissionController({
    PermissionService? service,
    EffectChannel<PermissionEffect>? effectChannel,
  }) : _injectedService = service,
       _effects = effectChannel ?? EffectChannel<PermissionEffect>();

  final PermissionService? _injectedService;
  final EffectChannel<PermissionEffect> _effects;
  late PermissionService _service;

  Stream<PermissionEffect> get effects => _effects.stream;

  @override
  PermissionState build() {
    _service = _injectedService ?? ref.watch(permissionServiceProvider);
    ref.onDispose(() {
      unawaited(_effects.dispose());
    });
    return const PermissionState.initial();
  }

  Future<AppResult<PermissionAccessLevel>> initialRefresh() {
    return refresh();
  }

  Future<AppResult<PermissionAccessLevel>> refresh() async {
    final AppResult<PermissionAccessLevel> accessResult = await _service
        .readCurrentAccessLevel();
    return _completeAccessUpdate(accessResult);
  }

  Future<AppResult<PermissionAccessLevel>> refreshAfterReturningFromSettings() {
    return refresh();
  }

  Future<AppResult<PermissionAccessLevel>> requestPermissionNow({
    bool dismissRationale = true,
  }) async {
    if (state.isRequestInProgress) {
      return AppResult<PermissionAccessLevel>.success(state.accessLevel);
    }

    state = state.copyWith(
      isRequestInProgress: true,
      hasRequestedPermissionInSession: true,
      isRationaleDismissed: dismissRationale
          ? true
          : state.isRationaleDismissed,
      clearError: true,
    );

    final AppResult<PermissionAccessLevel> accessResult = await _service
        .requestMediaAccess();
    return _completeAccessUpdate(accessResult);
  }

  void markRationaleDismissed() {
    state = state.copyWith(isRationaleDismissed: true);
  }

  Future<AppResult<bool>> requestAppSettingsNavigation() async {
    _effects.emit(const PermissionEffect.openAppSettingsRequested());

    final AppResult<bool> result = await _service.openAppSettings();

    switch (result) {
      case AppSuccess<bool>():
        state = state.copyWith(clearError: true);
      case AppFailure<bool>(:final error):
        state = state.copyWith(error: error);
        _effects.emit(PermissionEffect.openAppSettingsFailed(error));
    }

    return result;
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }

  Future<AppResult<PermissionAccessLevel>> _completeAccessUpdate(
    AppResult<PermissionAccessLevel> accessResult,
  ) async {
    switch (accessResult) {
      case AppSuccess<PermissionAccessLevel>(:final value):
        final AppResult<bool> rationaleResult = await _readRationaleForAccess(
          value,
        );

        switch (rationaleResult) {
          case AppSuccess<bool>(value: final shouldShowRationale):
            _applyAccessLevel(
              accessLevel: value,
              shouldShowPlatformRationale: shouldShowRationale,
            );
            return AppResult<PermissionAccessLevel>.success(value);
          case AppFailure<bool>(:final error):
            _applyAccessLevel(
              accessLevel: value,
              shouldShowPlatformRationale: false,
              error: error,
            );
            return AppResult<PermissionAccessLevel>.failure(error);
        }
      case AppFailure<PermissionAccessLevel>(:final error):
        state = state.copyWith(isRequestInProgress: false, error: error);
        return accessResult;
    }
  }

  Future<AppResult<bool>> _readRationaleForAccess(
    PermissionAccessLevel accessLevel,
  ) {
    if (accessLevel.hasMediaAccess) {
      return Future<AppResult<bool>>.value(
        const AppResult<bool>.success(false),
      );
    }

    return _service.shouldShowRequestPermissionRationale();
  }

  void _applyAccessLevel({
    required PermissionAccessLevel accessLevel,
    required bool shouldShowPlatformRationale,
    AppError? error,
  }) {
    state = state.copyWith(
      accessLevel: accessLevel,
      isRequestInProgress: false,
      isRationaleDismissed: accessLevel.hasMediaAccess
          ? false
          : state.isRationaleDismissed,
      shouldShowPlatformRationale: accessLevel.hasMediaAccess
          ? false
          : shouldShowPlatformRationale,
      error: error,
      clearError: error == null,
    );
  }
}

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart'
    as permission_handler;
import 'package:photo_manager/photo_manager.dart' as photo_manager;

import '../../../core/core.dart';
import '../domain/models/permission_access_level.dart';
import '../domain/permission_service.dart';

final Provider<PermissionService> permissionServiceProvider =
    Provider<PermissionService>((Ref ref) {
      return PermissionServiceImpl();
    });

abstract interface class PhotoManagerPermissionGateway {
  Future<photo_manager.PermissionState> getPermissionState(
    photo_manager.PermissionRequestOption requestOption,
  );

  Future<photo_manager.PermissionState> requestPermission(
    photo_manager.PermissionRequestOption requestOption,
  );
}

abstract interface class PermissionRationaleGateway {
  Future<bool> shouldShowRequestPermissionRationale();
}

abstract interface class AppSettingsGateway {
  Future<bool> openAppSettings();
}

final class PhotoManagerPermissionGatewayImpl
    implements PhotoManagerPermissionGateway {
  const PhotoManagerPermissionGatewayImpl();

  @override
  Future<photo_manager.PermissionState> getPermissionState(
    photo_manager.PermissionRequestOption requestOption,
  ) {
    return photo_manager.PhotoManager.getPermissionState(
      requestOption: requestOption,
    );
  }

  @override
  Future<photo_manager.PermissionState> requestPermission(
    photo_manager.PermissionRequestOption requestOption,
  ) {
    return photo_manager.PhotoManager.requestPermissionExtend(
      requestOption: requestOption,
    );
  }
}

final class PermissionHandlerRationaleGateway
    implements PermissionRationaleGateway {
  const PermissionHandlerRationaleGateway();

  @override
  Future<bool> shouldShowRequestPermissionRationale() async {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return false;
    }

    final List<bool> rationaleResults = await Future.wait(<Future<bool>>[
      permission_handler.Permission.photos.shouldShowRequestRationale,
      permission_handler.Permission.videos.shouldShowRequestRationale,
      permission_handler.Permission.storage.shouldShowRequestRationale,
    ]);

    return rationaleResults.any((bool shouldShow) => shouldShow);
  }
}

final class PermissionHandlerAppSettingsGateway implements AppSettingsGateway {
  const PermissionHandlerAppSettingsGateway();

  @override
  Future<bool> openAppSettings() {
    return permission_handler.openAppSettings();
  }
}

final class PermissionServiceImpl implements PermissionService {
  PermissionServiceImpl({
    PhotoManagerPermissionGateway photoManagerGateway =
        const PhotoManagerPermissionGatewayImpl(),
    PermissionRationaleGateway rationaleGateway =
        const PermissionHandlerRationaleGateway(),
    AppSettingsGateway appSettingsGateway =
        const PermissionHandlerAppSettingsGateway(),
    PlatformInfo? platformInfo,
    AppLogger logger = const AppLogger('PermissionService'),
  }) : _photoManagerGateway = photoManagerGateway,
       _rationaleGateway = rationaleGateway,
       _appSettingsGateway = appSettingsGateway,
       _platformInfo = platformInfo ?? PlatformInfo.current(),
       _logger = logger;

  static const photo_manager.PermissionRequestOption _requestOption =
      photo_manager.PermissionRequestOption(
        androidPermission: photo_manager.AndroidPermission(
          type: photo_manager.RequestType.common,
          mediaLocation: false,
        ),
        iosAccessLevel: photo_manager.IosAccessLevel.readWrite,
      );

  final PhotoManagerPermissionGateway _photoManagerGateway;
  final PermissionRationaleGateway _rationaleGateway;
  final AppSettingsGateway _appSettingsGateway;
  final PlatformInfo _platformInfo;
  final AppLogger _logger;

  @override
  Future<AppResult<PermissionAccessLevel>> readCurrentAccessLevel() async {
    if (!_platformInfo.isMobile) {
      return AppResult<PermissionAccessLevel>.failure(
        _platformInfo.unsupportedPlatformError('Media permission access'),
      );
    }

    return AppResult.guard<PermissionAccessLevel>(() async {
      final photo_manager.PermissionState pluginState =
          await _photoManagerGateway.getPermissionState(_requestOption);
      final PermissionAccessLevel accessLevel = _mapPermissionState(
        pluginState,
      );
      _logger.debug(
        'Read media permission access: ${accessLevel.name} '
        'from ${pluginState.name}.',
      );
      return accessLevel;
    }, errorMapper: _mapPermissionFailure);
  }

  @override
  Future<AppResult<PermissionAccessLevel>> requestMediaAccess() async {
    if (!_platformInfo.isMobile) {
      return AppResult<PermissionAccessLevel>.failure(
        _platformInfo.unsupportedPlatformError('Media permission request'),
      );
    }

    return AppResult.guard<PermissionAccessLevel>(() async {
      final photo_manager.PermissionState pluginState =
          await _photoManagerGateway.requestPermission(_requestOption);
      final PermissionAccessLevel accessLevel = _mapPermissionState(
        pluginState,
      );
      _logger.debug(
        'Media permission request completed: ${accessLevel.name} '
        'from ${pluginState.name}.',
      );
      return accessLevel;
    }, errorMapper: _mapPermissionFailure);
  }

  @override
  Future<AppResult<bool>> shouldShowRequestPermissionRationale() async {
    if (!_platformInfo.isAndroid) {
      return const AppResult<bool>.success(false);
    }

    return AppResult.guard<bool>(() async {
      final bool shouldShow = await _rationaleGateway
          .shouldShowRequestPermissionRationale();
      _logger.debug('Read media permission rationale flag: $shouldShow.');
      return shouldShow;
    }, errorMapper: _mapPermissionFailure);
  }

  @override
  Future<AppResult<bool>> openAppSettings() async {
    if (!_platformInfo.isMobile) {
      return AppResult<bool>.failure(
        _platformInfo.unsupportedPlatformError('App settings navigation'),
      );
    }

    final AppResult<bool> openResult = await AppResult.guard<bool>(() async {
      final bool opened = await _appSettingsGateway.openAppSettings();
      _logger.debug('App settings navigation requested: opened=$opened.');
      return opened;
    }, errorMapper: _mapPermissionFailure);

    return switch (openResult) {
      AppSuccess<bool>(:final value) when value => openResult,
      AppSuccess<bool>() => AppResult<bool>.failure(
        AppError.platformContractFailure(
          message: 'App settings page could not be opened.',
        ),
      ),
      AppFailure<bool>() => openResult,
    };
  }

  PermissionAccessLevel _mapPermissionState(
    photo_manager.PermissionState pluginState,
  ) {
    return switch (pluginState) {
      photo_manager.PermissionState.authorized =>
        PermissionAccessLevel.grantedAll,
      photo_manager.PermissionState.limited =>
        PermissionAccessLevel.grantedPartial,
      photo_manager.PermissionState.notDetermined ||
      photo_manager.PermissionState.restricted ||
      photo_manager.PermissionState.denied => PermissionAccessLevel.denied,
    };
  }

  AppError _mapPermissionFailure(Object error, StackTrace stackTrace) {
    return mapPlatformException(
      error,
      stackTrace,
      message: 'Media permission platform operation failed.',
    );
  }
}

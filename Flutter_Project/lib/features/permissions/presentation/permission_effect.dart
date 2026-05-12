import '../../../core/error/app_error.dart';

sealed class PermissionEffect {
  const PermissionEffect();

  const factory PermissionEffect.openAppSettingsRequested() =
      OpenAppSettingsRequested;

  const factory PermissionEffect.openAppSettingsFailed(AppError error) =
      OpenAppSettingsFailed;
}

final class OpenAppSettingsRequested extends PermissionEffect {
  const OpenAppSettingsRequested();
}

final class OpenAppSettingsFailed extends PermissionEffect {
  const OpenAppSettingsFailed(this.error);

  final AppError error;
}

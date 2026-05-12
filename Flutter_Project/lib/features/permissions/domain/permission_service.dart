import '../../../core/result/app_result.dart';
import 'models/permission_access_level.dart';

abstract interface class PermissionService {
  Future<AppResult<PermissionAccessLevel>> readCurrentAccessLevel();

  Future<AppResult<PermissionAccessLevel>> requestMediaAccess();

  Future<AppResult<bool>> shouldShowRequestPermissionRationale();

  Future<AppResult<bool>> openAppSettings();
}

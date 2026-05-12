enum PermissionAccessLevel {
  denied,
  grantedPartial,
  grantedAll;

  bool get hasMediaAccess => this != PermissionAccessLevel.denied;

  bool get isDenied => this == PermissionAccessLevel.denied;

  bool get isGrantedPartial => this == PermissionAccessLevel.grantedPartial;

  bool get isGrantedAll => this == PermissionAccessLevel.grantedAll;
}

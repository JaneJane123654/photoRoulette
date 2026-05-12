enum AppErrorCode {
  permissionDenied('permission_denied', 'Permission denied.'),
  partialPermissionGranted(
    'partial_permission_granted',
    'Only partial media permission was granted.',
  ),
  notFound('not_found', 'Requested item was not found.'),
  cancelledByUser('cancelled_by_user', 'Operation was cancelled by the user.'),
  malformedUriOrPath('malformed_uri_or_path', 'Malformed URI or path.'),
  storageFailure('storage_failure', 'Storage operation failed.'),
  networkFailure('network_failure', 'Network operation failed.'),
  unsupportedPlatform(
    'unsupported_platform',
    'This operation is unsupported on the current platform.',
  ),
  platformContractFailure(
    'platform_contract_failure',
    'Platform contract failed.',
  ),
  unknown('unknown', 'Unknown failure.');

  const AppErrorCode(this.value, this.defaultMessage);

  final String value;
  final String defaultMessage;
}

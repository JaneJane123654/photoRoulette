enum SilentDeleteScope {
  dcim('DCIM'),
  pictures('Pictures');

  const SilentDeleteScope(this.directoryName);

  final String directoryName;
}

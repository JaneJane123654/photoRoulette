enum DefaultBehaviorNoticeMode {
  visible('visible'),
  autoHidden('auto_hidden'),
  userHidden('user_hidden');

  const DefaultBehaviorNoticeMode(this.storageValue);

  final String storageValue;

  static DefaultBehaviorNoticeMode? fromStorageValue(String? value) {
    for (final DefaultBehaviorNoticeMode mode
        in DefaultBehaviorNoticeMode.values) {
      if (mode.storageValue == value) {
        return mode;
      }
    }

    return null;
  }
}

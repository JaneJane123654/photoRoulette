enum SwipeAction {
  skip('skip'),
  delete('delete'),
  previous('previous'),
  next('next');

  const SwipeAction(this.storageValue);

  final String storageValue;

  static SwipeAction? fromStorageValue(String? value) {
    for (final SwipeAction action in SwipeAction.values) {
      if (action.storageValue == value) {
        return action;
      }
    }

    return null;
  }
}

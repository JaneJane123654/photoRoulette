import 'dart:async';

/// A small one-shot event pipe for controller effects such as dialogs,
/// snackbars, navigation, delete launch requests, and install launch requests.
final class EffectChannel<T> {
  EffectChannel({bool sync = false})
    : _controller = StreamController<T>.broadcast(sync: sync);

  final StreamController<T> _controller;

  Stream<T> get stream => _controller.stream;

  bool get isClosed => _controller.isClosed;

  void emit(T effect) {
    if (_controller.isClosed) {
      return;
    }

    _controller.add(effect);
  }

  bool tryEmit(T effect) {
    if (_controller.isClosed) {
      return false;
    }

    try {
      _controller.add(effect);
      return true;
    } on StateError {
      return false;
    }
  }

  Future<void> dispose() {
    return _controller.close();
  }
}

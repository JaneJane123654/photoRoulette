abstract interface class AppClock {
  DateTime now();

  DateTime nowUtc();
}

extension AppClockMonthKey on AppClock {
  String currentMonthKey() {
    final current = now();
    final year = current.year.toString().padLeft(4, '0');
    final month = current.month.toString().padLeft(2, '0');
    return '$year-$month';
  }
}

final class SystemAppClock implements AppClock {
  const SystemAppClock();

  @override
  DateTime now() => DateTime.now();

  @override
  DateTime nowUtc() => DateTime.now().toUtc();
}

final class FixedAppClock implements AppClock {
  const FixedAppClock(this.fixedNow);

  final DateTime fixedNow;

  @override
  DateTime now() => fixedNow;

  @override
  DateTime nowUtc() => fixedNow.toUtc();
}

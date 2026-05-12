import 'package:flutter/material.dart';

import '../../../app/theme/app_spacing.dart';

class PermissionDeniedView extends StatelessWidget {
  const PermissionDeniedView({
    required this.title,
    required this.message,
    required this.requestActionLabel,
    required this.settingsActionLabel,
    required this.onRequestPermission,
    required this.onOpenSettings,
    this.errorMessage,
    this.isBusy = false,
    super.key,
  });

  final String title;
  final String message;
  final String requestActionLabel;
  final String settingsActionLabel;
  final VoidCallback onRequestPermission;
  final VoidCallback onOpenSettings;
  final String? errorMessage;
  final bool isBusy;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.xl,
              vertical: AppSpacing.lg,
            ),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Icon(
                    Icons.settings_outlined,
                    size: 96,
                    color: colorScheme.primary,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  Semantics(
                    header: true,
                    child: Text(
                      title,
                      style: textTheme.headlineMedium?.copyWith(
                        color: colorScheme.onSurface,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  Text(
                    message,
                    style: textTheme.bodyMedium?.copyWith(
                      color: colorScheme.onSurface,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  if (errorMessage != null) ...<Widget>[
                    const SizedBox(height: AppSpacing.md),
                    Text(
                      errorMessage!,
                      style: textTheme.bodySmall?.copyWith(
                        color: colorScheme.error,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                  const SizedBox(height: 28),
                  FilledButton(
                    onPressed: isBusy ? null : onRequestPermission,
                    child: Text(requestActionLabel),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  OutlinedButton(
                    onPressed: isBusy ? null : onOpenSettings,
                    child: Text(settingsActionLabel),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

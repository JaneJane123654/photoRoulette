import 'package:flutter/material.dart';

import '../../../app/theme/app_spacing.dart';
import '../../permissions/domain/models/permission_access_level.dart';

class GalleryShellPlaceholderPage extends StatelessWidget {
  const GalleryShellPlaceholderPage({
    required this.accessLevel,
    required this.title,
    required this.description,
    required this.accessLabel,
    required this.placeholderMessage,
    required this.partialAccessTitle,
    required this.partialAccessDescription,
    required this.partialAccessExpandLabel,
    required this.onExpandAccess,
    super.key,
  });

  final PermissionAccessLevel accessLevel;
  final String title;
  final String description;
  final String accessLabel;
  final String placeholderMessage;
  final String partialAccessTitle;
  final String partialAccessDescription;
  final String partialAccessExpandLabel;
  final VoidCallback onExpandAccess;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              _GalleryHeader(
                title: title,
                description: description,
                accessLabel: accessLabel,
              ),
              if (accessLevel.isGrantedPartial) ...<Widget>[
                const SizedBox(height: AppSpacing.md),
                _PartialAccessBanner(
                  title: partialAccessTitle,
                  description: partialAccessDescription,
                  actionLabel: partialAccessExpandLabel,
                  onExpandAccess: onExpandAccess,
                ),
              ],
              Expanded(
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 420),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Icon(
                          Icons.photo_library_outlined,
                          size: 80,
                          color: colorScheme.primary,
                        ),
                        const SizedBox(height: AppSpacing.md),
                        Text(
                          placeholderMessage,
                          style: textTheme.bodyLarge?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GalleryHeader extends StatelessWidget {
  const _GalleryHeader({
    required this.title,
    required this.description,
    required this.accessLabel,
  });

  final String title;
  final String description;
  final String accessLabel;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: 18,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Expanded(
                  child: Semantics(
                    header: true,
                    child: Text(
                      title,
                      style: textTheme.headlineSmall?.copyWith(
                        color: colorScheme.onSurface,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Icon(Icons.check_circle_outline, color: colorScheme.primary),
              ],
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              description,
              style: textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              accessLabel,
              style: textTheme.labelLarge?.copyWith(color: colorScheme.primary),
            ),
          ],
        ),
      ),
    );
  }
}

class _PartialAccessBanner extends StatelessWidget {
  const _PartialAccessBanner({
    required this.title,
    required this.description,
    required this.actionLabel,
    required this.onExpandAccess,
  });

  final String title;
  final String description;
  final String actionLabel;
  final VoidCallback onExpandAccess;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(AppRadii.xl),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: 18,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: <Widget>[
            DecoratedBox(
              decoration: BoxDecoration(
                color: colorScheme.surface.withValues(alpha: 0.32),
                shape: BoxShape.circle,
              ),
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Icon(
                  Icons.security_outlined,
                  color: colorScheme.onSecondaryContainer,
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    title,
                    style: textTheme.titleMedium?.copyWith(
                      color: colorScheme.onSecondaryContainer,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    description,
                    style: textTheme.bodyMedium?.copyWith(
                      color: colorScheme.onSecondaryContainer.withValues(
                        alpha: 0.85,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            FilledButton(onPressed: onExpandAccess, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}

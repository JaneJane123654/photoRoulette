import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/theme/app_spacing.dart';
import '../../../l10n/generated/app_localizations.dart';
import '../../permissions/domain/models/permission_access_level.dart';
import '../../settings/domain/models/app_settings.dart';
import 'gallery_deck_controller.dart';
import 'gallery_deck_state.dart';
import 'widgets/deck_card_stack.dart';
import 'widgets/deck_empty_view.dart';
import 'widgets/deck_error_view.dart';
import 'widgets/deck_loading_view.dart';

class GalleryDeckPage extends ConsumerStatefulWidget {
  const GalleryDeckPage({
    required this.accessLevel,
    required this.onExpandAccess,
    super.key,
  });

  final PermissionAccessLevel accessLevel;
  final VoidCallback onExpandAccess;

  @override
  ConsumerState<GalleryDeckPage> createState() => _GalleryDeckPageState();
}

class _GalleryDeckPageState extends ConsumerState<GalleryDeckPage> {
  @override
  void initState() {
    super.initState();
    unawaited(ref.read(galleryDeckControllerProvider.notifier).initialLoad());
  }

  @override
  Widget build(BuildContext context) {
    final GalleryDeckState state = ref.watch(galleryDeckControllerProvider);
    final AppLocalizations l10n = AppLocalizations.of(context);

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
                title: l10n.homeHeaderTitle,
                description: l10n.homeHeaderDescription,
                canRefresh:
                    state is! GalleryDeckInitial &&
                    state is! GalleryDeckLoading,
                onRefresh: _refreshDeck,
              ),
              if (widget.accessLevel.isGrantedPartial) ...<Widget>[
                const SizedBox(height: AppSpacing.md),
                _PartialAccessBanner(onExpandAccess: widget.onExpandAccess),
              ],
              const SizedBox(height: AppSpacing.md),
              Expanded(child: _buildDeckBody(state)),
              if (state is GalleryDeckReady) ...<Widget>[
                const SizedBox(height: AppSpacing.md),
                _DeckNavigationControls(
                  canGoPrevious: state.canSwipeToPrevious,
                  canGoNext: state.canSwipeToNext,
                  onPrevious: () => _movePrevious(state),
                  onNext: () => _moveNext(state),
                  onSkip: () => _moveSkip(state),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDeckBody(GalleryDeckState state) {
    return switch (state) {
      GalleryDeckInitial() || GalleryDeckLoading() => const DeckLoadingView(),
      GalleryDeckEmpty() => const DeckEmptyView(),
      GalleryDeckError(:final error) => DeckErrorView(
        error: error,
        onRetry: _refreshDeck,
      ),
      GalleryDeckReady(:final visibleCards) => DeckCardStack(
        visibleCards: visibleCards,
        showFullImage: AppSettings.defaults.showFullImage,
        enableTapToggle: AppSettings.defaults.isTapImageToggleEnabled,
      ),
    };
  }

  void _refreshDeck() {
    unawaited(
      ref.read(galleryDeckControllerProvider.notifier).reloadFromSource(),
    );
  }

  void _movePrevious(GalleryDeckReady state) {
    unawaited(
      ref
          .read(galleryDeckControllerProvider.notifier)
          .moveToPrevious(expectedTopCardId: state.currentCard.id),
    );
  }

  void _moveNext(GalleryDeckReady state) {
    unawaited(
      ref
          .read(galleryDeckControllerProvider.notifier)
          .moveToNext(expectedTopCardId: state.currentCard.id),
    );
  }

  void _moveSkip(GalleryDeckReady state) {
    unawaited(
      ref
          .read(galleryDeckControllerProvider.notifier)
          .moveSkip(expectedTopCardId: state.currentCard.id),
    );
  }
}

class _GalleryHeader extends StatelessWidget {
  const _GalleryHeader({
    required this.title,
    required this.description,
    required this.canRefresh,
    required this.onRefresh,
  });

  final String title;
  final String description;
  final bool canRefresh;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Row(
          children: <Widget>[
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Semantics(
                    header: true,
                    child: Text(
                      title,
                      style: textTheme.headlineSmall?.copyWith(
                        color: colorScheme.onSurface,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    description,
                    style: textTheme.bodyMedium?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            FilledButton.icon(
              onPressed: canRefresh ? onRefresh : null,
              icon: Icon(
                Icons.refresh_outlined,
                semanticLabel: l10n.refreshButtonContentDescription,
              ),
              label: Text(l10n.refreshButtonLabel),
            ),
          ],
        ),
      ),
    );
  }
}

class _PartialAccessBanner extends StatelessWidget {
  const _PartialAccessBanner({required this.onExpandAccess});

  final VoidCallback onExpandAccess;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
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
                  semanticLabel: l10n.partialAccessIconContentDescription,
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
                    l10n.partialAccessTitle,
                    style: textTheme.titleMedium?.copyWith(
                      color: colorScheme.onSecondaryContainer,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    l10n.partialAccessDescription,
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
            FilledButton(
              onPressed: onExpandAccess,
              child: Text(l10n.partialAccessExpand),
            ),
          ],
        ),
      ),
    );
  }
}

class _DeckNavigationControls extends StatelessWidget {
  const _DeckNavigationControls({
    required this.canGoPrevious,
    required this.canGoNext,
    required this.onPrevious,
    required this.onNext,
    required this.onSkip,
  });

  final bool canGoPrevious;
  final bool canGoNext;
  final VoidCallback onPrevious;
  final VoidCallback onNext;
  final VoidCallback onSkip;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);

    return Row(
      children: <Widget>[
        Expanded(
          child: OutlinedButton.icon(
            onPressed: canGoPrevious ? onPrevious : null,
            icon: const Icon(Icons.chevron_left_outlined),
            label: _ButtonLabel(l10n.swipeActionPrevious),
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: TextButton.icon(
            onPressed: canGoNext ? onSkip : null,
            icon: const Icon(Icons.skip_next_outlined),
            label: _ButtonLabel(l10n.swipeActionSkip),
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: FilledButton.icon(
            onPressed: canGoNext ? onNext : null,
            icon: const Icon(Icons.chevron_right_outlined),
            label: _ButtonLabel(l10n.swipeActionNext),
          ),
        ),
      ],
    );
  }
}

class _ButtonLabel extends StatelessWidget {
  const _ButtonLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return FittedBox(fit: BoxFit.scaleDown, child: Text(text, maxLines: 1));
  }
}

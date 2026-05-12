import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app/theme/app_spacing.dart';
import '../../../core/error/app_error.dart';
import '../../../l10n/generated/app_localizations.dart';
import '../../deck/presentation/gallery_deck_page.dart';
import '../domain/models/permission_state.dart';
import 'permission_controller.dart';
import 'permission_denied_view.dart';
import 'permission_effect.dart';
import 'permission_loading_view.dart';

class PermissionGatePage extends ConsumerStatefulWidget {
  const PermissionGatePage({super.key});

  @override
  ConsumerState<PermissionGatePage> createState() => _PermissionGatePageState();
}

class _PermissionGatePageState extends ConsumerState<PermissionGatePage> {
  late final AppLifecycleListener _lifecycleListener;

  bool _hasCompletedInitialRefresh = false;
  bool _autoRequestScheduled = false;

  @override
  void initState() {
    super.initState();
    _lifecycleListener = AppLifecycleListener(
      onResume: _refreshAfterReturningFromSettings,
    );
    unawaited(_runInitialRefresh());
  }

  @override
  void dispose() {
    _lifecycleListener.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final PermissionState state = ref.watch(permissionControllerProvider);

    ref.listen<PermissionState>(
      permissionControllerProvider,
      (_, PermissionState next) => _maybeTriggerAutoRequest(next),
    );
    ref.listen<AsyncValue<PermissionEffect>>(permissionEffectEventsProvider, (
      _,
      AsyncValue<PermissionEffect> next,
    ) {
      next.whenData(_handlePermissionEffect);
    });

    if (!_hasCompletedInitialRefresh || state.isRequestInProgress) {
      return PermissionLoadingView(message: l10n.loading);
    }

    if (state.accessLevel.isGrantedPartial || state.accessLevel.isGrantedAll) {
      return GalleryDeckPage(
        accessLevel: state.accessLevel,
        onExpandAccess: _requestPermissionFromUserAction,
      );
    }

    if (state.shouldShowRationale) {
      return _PermissionRationaleView(
        errorMessage: state.error?.message,
        onRequestClicked: _requestPermissionFromUserAction,
        onMaybeLaterClicked: _dismissRationale,
      );
    }

    return PermissionDeniedView(
      title: l10n.permissionDeniedTitle,
      message: l10n.permissionDeniedMessage,
      requestActionLabel: l10n.permissionRequestAction,
      settingsActionLabel: l10n.permissionOpenSettingsAction,
      errorMessage: state.error?.message,
      isBusy: state.isRequestInProgress,
      onRequestPermission: _requestPermissionFromUserAction,
      onOpenSettings: _openAppSettings,
    );
  }

  Future<void> _runInitialRefresh() async {
    await ref.read(permissionControllerProvider.notifier).initialRefresh();
    if (!mounted) {
      return;
    }

    setState(() {
      _hasCompletedInitialRefresh = true;
    });
    _maybeTriggerAutoRequest(ref.read(permissionControllerProvider));
  }

  Future<void> _refreshAfterReturningFromSettings() async {
    if (!_hasCompletedInitialRefresh) {
      return;
    }

    await ref
        .read(permissionControllerProvider.notifier)
        .refreshAfterReturningFromSettings();
    if (!mounted) {
      return;
    }

    _maybeTriggerAutoRequest(ref.read(permissionControllerProvider));
  }

  void _maybeTriggerAutoRequest(PermissionState state) {
    if (!_hasCompletedInitialRefresh ||
        _autoRequestScheduled ||
        !state.shouldAutoRequestPermission) {
      return;
    }

    _autoRequestScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }

      unawaited(_requestPermissionAutomatically());
    });
  }

  Future<void> _requestPermissionAutomatically() async {
    await ref
        .read(permissionControllerProvider.notifier)
        .requestPermissionNow(dismissRationale: false);
    if (!mounted) {
      return;
    }

    setState(() {
      _autoRequestScheduled = false;
    });
  }

  void _requestPermissionFromUserAction() {
    unawaited(
      ref
          .read(permissionControllerProvider.notifier)
          .requestPermissionNow(dismissRationale: true),
    );
  }

  void _dismissRationale() {
    ref.read(permissionControllerProvider.notifier).markRationaleDismissed();
  }

  void _openAppSettings() {
    unawaited(
      ref
          .read(permissionControllerProvider.notifier)
          .requestAppSettingsNavigation(),
    );
  }

  void _handlePermissionEffect(PermissionEffect effect) {
    switch (effect) {
      case OpenAppSettingsRequested():
        break;
      case OpenAppSettingsFailed(:final AppError error):
        final AppLocalizations l10n = AppLocalizations.of(context);
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(
            SnackBar(
              content: Text(
                '${l10n.permissionSettingsOpenFailedMessage} ${error.message}',
              ),
            ),
          );
    }
  }
}

class _PermissionRationaleView extends StatelessWidget {
  const _PermissionRationaleView({
    required this.onRequestClicked,
    required this.onMaybeLaterClicked,
    this.errorMessage,
  });

  final VoidCallback onRequestClicked;
  final VoidCallback onMaybeLaterClicked;
  final String? errorMessage;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              colorScheme.surface,
              colorScheme.surfaceContainerLowest,
              colorScheme.surface,
            ],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: 20,
              ),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 520),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    const _PhotoPermissionHero(),
                    const SizedBox(height: AppSpacing.lg),
                    Semantics(
                      header: true,
                      child: Text(
                        l10n.permissionRationaleTitle,
                        style: textTheme.headlineMedium?.copyWith(
                          color: colorScheme.onSurface,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      l10n.permissionRationaleDescriptionPrimary,
                      style: textTheme.bodyLarge?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      l10n.permissionRationaleDescriptionSecondary,
                      style: textTheme.bodyMedium?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    if (errorMessage != null) ...<Widget>[
                      const SizedBox(height: AppSpacing.sm),
                      Text(
                        errorMessage!,
                        style: textTheme.bodySmall?.copyWith(
                          color: colorScheme.error,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                    const SizedBox(height: AppSpacing.lg),
                    _PermissionFeatureCard(
                      children: <Widget>[
                        _PermissionFeatureRow(
                          icon: Icons.shuffle_outlined,
                          iconContentDescription:
                              l10n.permissionFeatureAllPhotosIconDescription,
                          title: l10n.permissionFeatureAllPhotosTitle,
                          body: l10n.permissionFeatureAllPhotosBody,
                        ),
                        _PermissionFeatureRow(
                          icon: Icons.collections_outlined,
                          iconContentDescription:
                              l10n.permissionFeatureSelectedIconDescription,
                          title: l10n.permissionFeatureSelectedTitle,
                          body: l10n.permissionFeatureSelectedBody,
                        ),
                        _PermissionFeatureRow(
                          icon: Icons.tune_outlined,
                          iconContentDescription:
                              l10n.permissionFeatureControlIconDescription,
                          title: l10n.permissionFeatureControlTitle,
                          body: l10n.permissionFeatureControlBody,
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: onRequestClicked,
                        child: Text(l10n.permissionRequestAction),
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    TextButton(
                      onPressed: onMaybeLaterClicked,
                      child: Text(l10n.permissionMaybeLaterAction),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PhotoPermissionHero extends StatelessWidget {
  const _PhotoPermissionHero();

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return SizedBox(
      height: 240,
      child: Stack(
        alignment: Alignment.center,
        children: <Widget>[
          Transform.translate(
            offset: const Offset(-82, 20),
            child: Transform.rotate(
              angle: -14 * math.pi / 180,
              child: _PhotoIllustrationCard(
                icon: Icons.collections_outlined,
                iconContentDescription:
                    l10n.permissionIllustrationCollectionDescription,
                accentColor: colorScheme.secondaryContainer,
              ),
            ),
          ),
          Transform.translate(
            offset: const Offset(84, 26),
            child: Transform.rotate(
              angle: 13 * math.pi / 180,
              child: _PhotoIllustrationCard(
                icon: Icons.shuffle_outlined,
                iconContentDescription:
                    l10n.permissionIllustrationShuffleDescription,
                accentColor: colorScheme.tertiaryContainer,
              ),
            ),
          ),
          Transform.rotate(
            angle: -2 * math.pi / 180,
            child: _PhotoIllustrationCard(
              icon: Icons.photo_library_outlined,
              iconContentDescription:
                  l10n.permissionIllustrationLibraryDescription,
              accentColor: colorScheme.primaryContainer,
              isPrimary: true,
            ),
          ),
        ],
      ),
    );
  }
}

class _PhotoIllustrationCard extends StatelessWidget {
  const _PhotoIllustrationCard({
    required this.icon,
    required this.iconContentDescription,
    required this.accentColor,
    this.isPrimary = false,
  });

  final IconData icon;
  final String iconContentDescription;
  final Color accentColor;
  final bool isPrimary;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final double width = isPrimary ? 156 : 132;
    final double height = isPrimary ? 196 : 168;

    return Material(
      color: colorScheme.surfaceContainer,
      elevation: isPrimary ? 12 : 6,
      shadowColor: colorScheme.shadow.withValues(alpha: 0.18),
      borderRadius: BorderRadius.circular(28),
      child: SizedBox(
        width: width,
        height: height,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              DecoratedBox(
                decoration: BoxDecoration(
                  color: accentColor,
                  shape: BoxShape.circle,
                ),
                child: SizedBox.square(
                  dimension: 48,
                  child: Icon(
                    icon,
                    semanticLabel: iconContentDescription,
                    color: colorScheme.onSurface,
                  ),
                ),
              ),
              Column(
                children: <Widget>[
                  _IllustrationLine(fraction: 1, isPrimary: isPrimary),
                  const SizedBox(height: AppSpacing.xs),
                  _IllustrationLine(fraction: 0.82, isPrimary: isPrimary),
                  const SizedBox(height: AppSpacing.xs),
                  _IllustrationLine(fraction: 0.64, isPrimary: isPrimary),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _IllustrationLine extends StatelessWidget {
  const _IllustrationLine({required this.fraction, required this.isPrimary});

  final double fraction;
  final bool isPrimary;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final double alpha = isPrimary ? 0.55 : 0.38;

    return FractionallySizedBox(
      widthFactor: fraction,
      alignment: AlignmentDirectional.centerStart,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: colorScheme.onSurfaceVariant.withValues(alpha: alpha),
          borderRadius: BorderRadius.circular(AppRadii.full),
        ),
        child: const SizedBox(height: 12),
      ),
    );
  }
}

class _PermissionFeatureCard extends StatelessWidget {
  const _PermissionFeatureCard({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Column(
          children: <Widget>[
            for (
              int index = 0;
              index < children.length;
              index += 1
            ) ...<Widget>[
              children[index],
              if (index != children.length - 1)
                const SizedBox(height: AppSpacing.md),
            ],
          ],
        ),
      ),
    );
  }
}

class _PermissionFeatureRow extends StatelessWidget {
  const _PermissionFeatureRow({
    required this.icon,
    required this.iconContentDescription,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String iconContentDescription;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        DecoratedBox(
          decoration: BoxDecoration(
            color: colorScheme.primaryContainer,
            shape: BoxShape.circle,
          ),
          child: SizedBox.square(
            dimension: 42,
            child: Icon(
              icon,
              semanticLabel: iconContentDescription,
              color: colorScheme.onPrimaryContainer,
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
                  color: colorScheme.onSurface,
                ),
              ),
              const SizedBox(height: AppSpacing.xxs),
              Text(
                body,
                style: textTheme.bodyMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

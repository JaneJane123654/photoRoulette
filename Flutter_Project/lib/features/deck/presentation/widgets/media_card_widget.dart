import 'package:flutter/material.dart';

import '../../../../l10n/generated/app_localizations.dart';
import '../../domain/models/deck_card.dart';
import 'media_preview_surface.dart';

class MediaCardWidget extends StatelessWidget {
  const MediaCardWidget({
    required this.card,
    required this.isTopCard,
    this.showFullImage = false,
    this.enableTapToggle = true,
    super.key,
  });

  final DeckCard card;
  final bool isTopCard;
  final bool showFullImage;
  final bool enableTapToggle;

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Semantics(
      image: true,
      label: l10n.photoContentDescription,
      child: Material(
        color: colorScheme.surface,
        elevation: isTopCard ? 10 : 4,
        shadowColor: colorScheme.shadow.withValues(
          alpha: isTopCard ? 0.18 : 0.10,
        ),
        borderRadius: BorderRadius.circular(_cardCornerRadius),
        clipBehavior: Clip.antiAlias,
        child: MediaPreviewSurface(
          card: card,
          isTopCard: isTopCard,
          showFullImage: showFullImage,
          enableTapToggle: enableTapToggle,
        ),
      ),
    );
  }
}

const double _cardCornerRadius = 28;

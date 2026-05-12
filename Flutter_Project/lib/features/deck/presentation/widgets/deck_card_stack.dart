import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../domain/models/deck_card.dart';
import 'media_card_widget.dart';

class DeckCardStack extends StatelessWidget {
  const DeckCardStack({
    required this.visibleCards,
    this.showFullImage = false,
    this.enableTapToggle = true,
    super.key,
  });

  final List<DeckCard> visibleCards;
  final bool showFullImage;
  final bool enableTapToggle;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final Size deckSize = _resolveDeckSize(constraints);

        return Center(
          child: SizedBox(
            width: deckSize.width,
            height: deckSize.height,
            child: ClipRect(
              child: Stack(
                alignment: Alignment.center,
                children: <Widget>[
                  for (
                    int index = visibleCards.length - 1;
                    index >= 0;
                    index -= 1
                  )
                    _LayeredDeckCard(
                      key: ValueKey<String>(visibleCards[index].id),
                      card: visibleCards[index],
                      index: index,
                      isTopCard: index == 0,
                      showFullImage: showFullImage,
                      enableTapToggle: enableTapToggle,
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Size _resolveDeckSize(BoxConstraints constraints) {
    final double maxWidth = constraints.maxWidth.isFinite
        ? constraints.maxWidth
        : _maxDeckWidth;
    final double maxHeight = constraints.maxHeight.isFinite
        ? constraints.maxHeight
        : _maxDeckWidth / _cardAspectRatio;
    final double width = math
        .min(maxWidth, maxHeight * _cardAspectRatio)
        .clamp(0, _maxDeckWidth)
        .toDouble();
    final double height = width / _cardAspectRatio;

    return Size(width, height);
  }
}

class _LayeredDeckCard extends StatelessWidget {
  const _LayeredDeckCard({
    required super.key,
    required this.card,
    required this.index,
    required this.isTopCard,
    required this.showFullImage,
    required this.enableTapToggle,
  });

  final DeckCard card;
  final int index;
  final bool isTopCard;
  final bool showFullImage;
  final bool enableTapToggle;

  @override
  Widget build(BuildContext context) {
    final double layerInset = _cardLayerInset * index;
    final double scale = switch (index) {
      0 => 1,
      1 => 0.96,
      _ => 0.92,
    };

    return Positioned.fill(
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: layerInset,
          vertical: layerInset * 1.2,
        ),
        child: Transform.scale(
          scale: scale,
          child: IgnorePointer(
            ignoring: !isTopCard,
            child: MediaCardWidget(
              card: card,
              isTopCard: isTopCard,
              showFullImage: showFullImage,
              enableTapToggle: enableTapToggle,
            ),
          ),
        ),
      ),
    );
  }
}

const double _cardAspectRatio = 0.72;
const double _cardLayerInset = 12;
const double _maxDeckWidth = 460;

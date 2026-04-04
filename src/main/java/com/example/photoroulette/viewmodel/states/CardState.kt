package com.example.photoroulette.viewmodel.states

import com.example.photoroulette.model.MediaCard

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data object PermissionDenied : HomeUiState

    data object Empty : HomeUiState

    data class Ready(
        val previousCard: MediaCard?,
        val visibleCards: List<MediaCard>,
        val canSwipeToPrevious: Boolean,
        val canSwipeToNext: Boolean,
    ) : HomeUiState {
        init {
            require(visibleCards.isNotEmpty()) {
                "Ready must contain at least one visible media ID."
            }
            require(visibleCards.size <= MAX_VISIBLE_CARD_COUNT) {
                "Ready can expose at most $MAX_VISIBLE_CARD_COUNT visible media IDs."
            }
        }
    }

    companion object {
        const val MAX_VISIBLE_CARD_COUNT = 3
    }
}

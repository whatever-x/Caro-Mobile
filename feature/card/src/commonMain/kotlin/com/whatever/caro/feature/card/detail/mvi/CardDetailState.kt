package com.whatever.caro.feature.card.detail.mvi

import androidx.compose.runtime.Immutable
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CardDetailState(
    val cards: ImmutableList<DeckCard> = persistentListOf(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isFlipped: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
) : UiState {
    val currentCard: DeckCard?
        get() = cards.getOrNull(currentIndex)
}

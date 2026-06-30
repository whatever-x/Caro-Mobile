package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.deck.detail.model.CardItem
import kotlinx.collections.immutable.ImmutableList

data class DeckDetailState(
    val deck: Deck =
        Deck(
            id = 1L,
            title = "Preview Title",
            description = "Preview Description for Deck Detail",
            cardTotalCount = CardItem.fakeList().size,
            todayLearningCount = CardItem.fakeList().size,
            todayCompleteCount = 0,
            state = DeckState.NOT_STARTED,
        ),
    val deckCardList: ImmutableList<CardItem> = CardItem.fakeList(),
    val isSortBottomSheetVisible: Boolean = false,
    val isDeckEditBottomSheetVisible: Boolean = false,
    val selectedSortOption: DeckDetailSortOption = DeckDetailSortOption.CREATED,
) : UiState {
    val isEmptyDeckCard: Boolean
        get() = deckCardList.isEmpty()
}

enum class DeckDetailSortOption {
    CREATED,
    LAST_REVIEWED,
    FREQUENCY,
}

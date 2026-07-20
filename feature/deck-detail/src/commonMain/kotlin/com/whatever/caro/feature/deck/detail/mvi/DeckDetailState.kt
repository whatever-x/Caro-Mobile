package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.deck.detail.model.CardItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeckDetailState(
    val deck: Deck,
    val deckCardList: ImmutableList<CardItem> = persistentListOf(),
    val isCardListLoading: Boolean = false,
    val isSortBottomSheetVisible: Boolean = false,
    val isDeckEditBottomSheetVisible: Boolean = false,
    val selectedSortOption: DeckDetailSortOption = DeckDetailSortOption.CREATED,
) : UiState {
    val isEmptyDeckCard: Boolean
        get() = deckCardList.isEmpty() && isCardListLoading.not()
}

enum class DeckDetailSortOption {
    CREATED,
    LAST_REVIEWED,
    FREQUENCY,
}

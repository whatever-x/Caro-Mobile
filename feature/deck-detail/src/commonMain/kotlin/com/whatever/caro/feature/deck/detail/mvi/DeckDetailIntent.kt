package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface DeckDetailIntent : UiIntent {
    data object ClickBack : DeckDetailIntent

    data object ClickAddCard : DeckDetailIntent

    data object ClickAllStudy : DeckDetailIntent

    data object ClickDailyStudy : DeckDetailIntent

    data object ClickSortCardList : DeckDetailIntent

    data object DismissSortBottomSheet : DeckDetailIntent

    data class ClickSortOption(
        val sortOption: DeckDetailSortOption,
    ) : DeckDetailIntent

    data object ClickEditCardList : DeckDetailIntent

    data object ClickEditDeck : DeckDetailIntent

    data object DismissDeckEditBottomSheet : DeckDetailIntent

    data object ClickDeckEditBottomSheetEdit : DeckDetailIntent

    data object ClickDeckEditBottomSheetDelete : DeckDetailIntent

    data class ClickCard(
        val cardId: Long,
    ) : DeckDetailIntent
}

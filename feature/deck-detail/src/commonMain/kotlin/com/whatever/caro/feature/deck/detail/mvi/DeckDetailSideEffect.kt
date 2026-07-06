package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface DeckDetailSideEffect : UiSideEffect {
    data object NavigateBack : DeckDetailSideEffect

    data class NavigateToCreateCard(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToAllStudy(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToDailyStudy(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToEditCardList(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToEditDeck(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToCardDetail(
        val cardId: Long,
    ) : DeckDetailSideEffect
}

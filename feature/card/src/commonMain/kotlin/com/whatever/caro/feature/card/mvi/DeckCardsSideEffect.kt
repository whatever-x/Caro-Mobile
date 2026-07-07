package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface DeckCardsSideEffect : UiSideEffect {
    data object NavigateBack : DeckCardsSideEffect

    data class NavigateToCreateCard(
        val deckId: Long,
    ) : DeckCardsSideEffect

    data class NavigateToEditCard(
        val cardId: Long,
        val front: String,
        val back: String,
    ) : DeckCardsSideEffect

    data object ShowLoadError : DeckCardsSideEffect
}

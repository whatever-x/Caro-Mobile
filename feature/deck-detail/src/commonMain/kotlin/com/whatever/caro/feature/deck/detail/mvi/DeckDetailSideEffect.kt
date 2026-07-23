package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.model.learning.LearningMode
import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface DeckDetailSideEffect : UiSideEffect {
    data object NavigateBack : DeckDetailSideEffect

    data class NavigateToCreateCard(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToLearning(
        val deckId: Long,
        val mode: LearningMode,
    ) : DeckDetailSideEffect

    data class NavigateToEditCardList(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToEditDeck(
        val deckId: Long,
    ) : DeckDetailSideEffect

    data class NavigateToEditCard(
        val cardId: Long,
        val front: String,
        val back: String,
    ) : DeckDetailSideEffect

    data object ShowCardLoadError : DeckDetailSideEffect
}

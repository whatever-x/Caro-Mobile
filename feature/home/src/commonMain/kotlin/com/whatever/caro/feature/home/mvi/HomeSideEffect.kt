package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface HomeSideEffect : UiSideEffect {
    data object NavigateToSetting : HomeSideEffect

    data object NavigateToCreateDeck : HomeSideEffect

    data class NavigateToDailyLearning(
        val deckId: Long,
    ) : HomeSideEffect

    data class NavigateToDeckDetail(
        val deck: Deck,
    ) : HomeSideEffect
}

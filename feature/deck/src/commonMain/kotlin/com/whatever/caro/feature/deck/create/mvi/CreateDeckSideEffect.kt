package com.whatever.caro.feature.deck.create.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CreateDeckSideEffect : UiSideEffect {
    data object NavigateBack : CreateDeckSideEffect

    data class Created(
        val deck: Deck,
    ) : CreateDeckSideEffect

    data object ShowError : CreateDeckSideEffect
}

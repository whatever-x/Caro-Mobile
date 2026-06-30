package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface DeckDetailSideEffect : UiSideEffect {
    data object NavigateBack : DeckDetailSideEffect
}

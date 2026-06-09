package com.whatever.caro.feature.deck.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CreateDeckSideEffect : UiSideEffect {
    data object NavigateBack : CreateDeckSideEffect
}

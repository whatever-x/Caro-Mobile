package com.whatever.caro.feature.deck.create.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CreateDeckSideEffect : UiSideEffect {
    data object NavigateBack : CreateDeckSideEffect

    data object ShowError : CreateDeckSideEffect
}

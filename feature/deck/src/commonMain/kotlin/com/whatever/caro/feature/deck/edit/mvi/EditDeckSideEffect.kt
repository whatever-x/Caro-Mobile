package com.whatever.caro.feature.deck.edit.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface EditDeckSideEffect : UiSideEffect {
    data object NavigateBack : EditDeckSideEffect

    data object ShowError : EditDeckSideEffect
}

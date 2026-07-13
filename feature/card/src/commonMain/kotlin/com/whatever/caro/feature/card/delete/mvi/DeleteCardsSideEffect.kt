package com.whatever.caro.feature.card.delete.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface DeleteCardsSideEffect : UiSideEffect {
    data object NavigateBack : DeleteCardsSideEffect

    data object ShowLoadError : DeleteCardsSideEffect

    data object ShowDeleteError : DeleteCardsSideEffect
}

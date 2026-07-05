package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CreateCardSideEffect : UiSideEffect {
    data object NavigateBack : CreateCardSideEffect

    data object ShowSaveError : CreateCardSideEffect
}

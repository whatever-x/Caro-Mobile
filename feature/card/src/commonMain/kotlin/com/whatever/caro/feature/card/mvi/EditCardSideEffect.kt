package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface EditCardSideEffect : UiSideEffect {
    data object NavigateBack : EditCardSideEffect

    data object ShowSaveError : EditCardSideEffect
}

package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface HomeSideEffect : UiSideEffect {
    data object NavigateToSetting : HomeSideEffect

    data object NavigateToProfile : HomeSideEffect

    data object NavigateToCreateDeck : HomeSideEffect
}

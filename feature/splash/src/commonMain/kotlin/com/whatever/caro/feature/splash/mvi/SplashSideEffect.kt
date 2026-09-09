package com.whatever.caro.feature.splash.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface SplashSideEffect : UiSideEffect {
    data object NavigateLogin : SplashSideEffect

    data object NavigateCreateProfile : SplashSideEffect

    data object NavigateHome : SplashSideEffect
}

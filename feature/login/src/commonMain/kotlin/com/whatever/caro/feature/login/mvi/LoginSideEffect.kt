package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface LoginSideEffect : UiSideEffect {

    data object NavigateHome : LoginSideEffect

}
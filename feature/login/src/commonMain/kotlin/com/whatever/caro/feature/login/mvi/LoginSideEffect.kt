package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.feature.login.model.LoginError

sealed interface LoginSideEffect : UiSideEffect {
    data object NavigateHome : LoginSideEffect
    data class ShowErrorToast(val error: LoginError) : LoginSideEffect
}

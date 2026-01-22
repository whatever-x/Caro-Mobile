package com.whatever.caro.feature.login

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginSideEffect
import com.whatever.caro.feature.login.mvi.LoginState
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(
    initialState = LoginState()
) {

    override suspend fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.ClickLogin -> postSideEffect(LoginSideEffect.NavigateHome)
        }
    }

}
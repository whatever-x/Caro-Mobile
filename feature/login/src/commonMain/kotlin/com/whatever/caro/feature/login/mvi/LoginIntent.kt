package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface LoginIntent : UiIntent {
    data object ClickLogin : LoginIntent
}

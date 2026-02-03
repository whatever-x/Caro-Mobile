package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class LoginState(
    val test: String = "Login Screen",
): UiState
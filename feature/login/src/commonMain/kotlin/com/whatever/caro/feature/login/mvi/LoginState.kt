package com.whatever.caro.feature.login.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class LoginState(
    val isLoading: Boolean = false,
) : UiState

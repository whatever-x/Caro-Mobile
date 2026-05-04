package com.whatever.caro.feature.splash.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class SplashState(
    val isLoading: Boolean = true,
) : UiState

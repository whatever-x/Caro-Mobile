package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class HomeState(
    val test: String = "HomeScreen",
    val name: String = "",
): UiState

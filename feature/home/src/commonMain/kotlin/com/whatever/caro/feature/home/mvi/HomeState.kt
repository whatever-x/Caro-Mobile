package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class HomeState(
    val screenName: String = "",
    val name: String = "",
    val sampleString: String = "",
) : UiState

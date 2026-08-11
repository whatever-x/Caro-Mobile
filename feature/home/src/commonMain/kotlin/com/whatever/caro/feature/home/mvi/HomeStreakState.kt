package com.whatever.caro.feature.home.mvi

sealed interface HomeStreakState {
    data object Loading : HomeStreakState

    data object NotStarted : HomeStreakState

    data class Active(
        val days: Int,
    ) : HomeStreakState

    data object Broken : HomeStreakState
}

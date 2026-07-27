package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class HomeState(
    val nickname: String = "",
    val streakState: HomeStreakState = HomeStreakState.Loading,
    val decks: ImmutableList<Deck> = persistentListOf(),
    val isLoading: Boolean = false,
    val hasLoadError: Boolean = false,
) : UiState {
    val isDeckEmpty: Boolean
        get() = decks.isEmpty() && isLoading.not() && hasLoadError.not()
}

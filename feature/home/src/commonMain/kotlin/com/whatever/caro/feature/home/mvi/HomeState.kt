package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiState

data class HomeState(
    val nickname: String = "",
    val additionalDescription: String = "",
    val learningDays: Int = 0,
    val decks: List<Deck> = emptyList(),
) : UiState

package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiState

data class DeckDetailState(
    val screenName: String = "",
    val deckId: Long = 0L,
) : UiState

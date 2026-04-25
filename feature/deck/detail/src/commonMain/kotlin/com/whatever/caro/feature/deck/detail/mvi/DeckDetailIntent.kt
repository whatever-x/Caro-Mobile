package com.whatever.caro.feature.deck.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface DeckDetailIntent : UiIntent {
    data object ClickBack : DeckDetailIntent
}

package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface HomeIntent : UiIntent {
    data object Initialize : HomeIntent

    data object ClickRetry : HomeIntent

    data object ClickSettingButton : HomeIntent

    data object ClickCreateDeckButton : HomeIntent

    data class ClickDeckButton(
        val deck: Deck,
    ) : HomeIntent

    data object ClickCreateDeck : HomeIntent

    data class ClickStartLearning(
        val deckId: Long,
    ) : HomeIntent
}

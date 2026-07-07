package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface DeckCardsIntent : UiIntent {
    data object ClickBack : DeckCardsIntent

    data object ClickAddCard : DeckCardsIntent

    data class ClickEditCard(
        val cardId: Long,
    ) : DeckCardsIntent

    data object RefreshCards : DeckCardsIntent

    data object ClickRetry : DeckCardsIntent
}

package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.model.card.Card
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeckCardsState(
    val deckId: Long,
    val deckTitle: String,
    val cards: ImmutableList<Card> = persistentListOf(),
    val isLoading: Boolean = false,
    val hasLoadFailed: Boolean = false,
) : UiState {
    val isEmpty: Boolean
        get() = cards.isEmpty() && isLoading.not() && hasLoadFailed.not()
}

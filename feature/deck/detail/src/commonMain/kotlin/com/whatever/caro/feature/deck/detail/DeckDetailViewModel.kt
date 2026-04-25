package com.whatever.caro.feature.deck.detail

import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState

class DeckDetailViewModel(
    private val navKey: DeckDetailEntry,
) : BaseViewModel<DeckDetailState, DeckDetailIntent, DeckDetailSideEffect>(
        initialState = DeckDetailState(),
    ) {
    override suspend fun handleIntent(intent: DeckDetailIntent) {
        when (intent) {
            DeckDetailIntent.ClickBack -> postSideEffect(DeckDetailSideEffect.NavigateBack)
        }
    }

    fun init() {
        reduce {
            copy(
                screenName = "DeckDetailScreen",
                deckId = navKey.payload.deckId,
            )
        }
    }
}

package com.whatever.caro.feature.card

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.mvi.DeckCardsIntent
import com.whatever.caro.feature.card.mvi.DeckCardsSideEffect
import com.whatever.caro.feature.card.mvi.DeckCardsState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException

class DeckCardsViewModel(
    private val cardRepository: CardRepository,
    private val deckId: Long,
    deckTitle: String,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<DeckCardsState, DeckCardsIntent, DeckCardsSideEffect>(
        initialState =
            DeckCardsState(
                deckId = deckId,
                deckTitle = deckTitle,
            ),
        exceptionFilter = exceptionFilter,
    ) {
    init {
        loadCards()
    }

    override suspend fun handleIntent(intent: DeckCardsIntent) {
        when (intent) {
            DeckCardsIntent.ClickBack -> postSideEffect(DeckCardsSideEffect.NavigateBack)
            DeckCardsIntent.ClickAddCard -> postSideEffect(DeckCardsSideEffect.NavigateToCreateCard(deckId))
            is DeckCardsIntent.ClickEditCard -> handleEditCard(intent.cardId)
            DeckCardsIntent.RefreshCards -> loadCards()
            DeckCardsIntent.ClickRetry -> loadCards()
        }
    }

    private fun loadCards() {
        if (currentState.isLoading) return
        reduce {
            copy(
                isLoading = true,
                hasLoadFailed = false,
            )
        }

        launch {
            runCatching {
                cardRepository.getCardsByDeck(deckId = deckId)
            }.onSuccess { cards ->
                reduce {
                    copy(
                        cards = cards.toPersistentList(),
                        isLoading = false,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                reduce {
                    copy(
                        isLoading = false,
                        hasLoadFailed = true,
                    )
                }
                postSideEffect(DeckCardsSideEffect.ShowLoadError)
            }
        }
    }

    private fun handleEditCard(cardId: Long) {
        val card = currentState.cards.firstOrNull { it.id == cardId } ?: return
        postSideEffect(
            DeckCardsSideEffect.NavigateToEditCard(
                cardId = card.id,
                front = card.content.front,
                back = card.content.back,
            ),
        )
    }
}

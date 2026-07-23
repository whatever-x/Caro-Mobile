package com.whatever.caro.feature.card.detail

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.detail.mvi.CardDetailIntent
import com.whatever.caro.feature.card.detail.mvi.CardDetailSideEffect
import com.whatever.caro.feature.card.detail.mvi.CardDetailState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException

class CardDetailViewModel(
    private val deckId: Long,
    private val initialCardId: Long,
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<CardDetailState, CardDetailIntent, CardDetailSideEffect>(
        initialState = CardDetailState(),
        exceptionFilter = exceptionFilter,
    ) {
    init {
        loadCards(preferredCardId = initialCardId)
    }

    override suspend fun handleIntent(intent: CardDetailIntent) {
        when (intent) {
            CardDetailIntent.ClickBack -> {
                postSideEffect(CardDetailSideEffect.NavigateBack)
            }

            CardDetailIntent.ClickEdit -> {
                val card = currentState.currentCard ?: return
                postSideEffect(
                    CardDetailSideEffect.NavigateToEdit(
                        cardId = card.id,
                        front = card.content.front,
                        back = card.content.back,
                    ),
                )
            }

            CardDetailIntent.ClickDelete -> {
                if (!currentState.isLoading && !currentState.isDeleting && currentState.currentCard != null) {
                    reduce { copy(isDeleteDialogVisible = true) }
                }
            }

            CardDetailIntent.DismissDeleteDialog -> {
                if (!currentState.isDeleting) {
                    reduce { copy(isDeleteDialogVisible = false) }
                }
            }

            CardDetailIntent.ConfirmDelete -> {
                deleteCurrentCard()
            }

            CardDetailIntent.FlipCard -> {
                if (currentState.currentCard != null) {
                    reduce { copy(isFlipped = !isFlipped) }
                }
            }

            is CardDetailIntent.ChangeCard -> {
                changeCard(index = intent.index)
            }

            CardDetailIntent.RefreshCards -> {
                if (!currentState.isDeleting && !currentState.isDeleteDialogVisible) {
                    loadCards(preferredCardId = currentState.currentCard?.id ?: initialCardId)
                }
            }
        }
    }

    private fun changeCard(index: Int) {
        if (index !in currentState.cards.indices || index == currentState.currentIndex) return
        reduce {
            copy(
                currentIndex = index,
                isFlipped = false,
            )
        }
    }

    private fun loadCards(preferredCardId: Long) {
        if (currentState.isLoading) return
        reduce { copy(isLoading = true) }

        launch {
            runCatching {
                deckRepository.getDeckCards(deckId = deckId)
            }.onSuccess { cards ->
                if (cards.isEmpty()) {
                    reduce { copy(isLoading = false) }
                    postSideEffect(CardDetailSideEffect.NavigateBack)
                    return@onSuccess
                }

                val preferredIndex = cards.indexOfFirst { it.id == preferredCardId }
                val fallbackIndex = currentState.currentIndex.coerceAtMost(cards.lastIndex)
                val newIndex = preferredIndex.takeIf { it >= 0 } ?: fallbackIndex
                val currentCardId = currentState.currentCard?.id
                reduce {
                    copy(
                        cards = cards.toPersistentList(),
                        currentIndex = newIndex,
                        isLoading = false,
                        isFlipped = isFlipped && cards[newIndex].id == currentCardId,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                val hasLoadedCards = currentState.cards.isNotEmpty()
                reduce { copy(isLoading = false) }
                postSideEffect(
                    if (hasLoadedCards) {
                        CardDetailSideEffect.ShowRefreshError
                    } else {
                        CardDetailSideEffect.ShowLoadError
                    },
                )
            }
        }
    }

    private fun deleteCurrentCard() {
        if (currentState.isLoading || currentState.isDeleting) return
        val card = currentState.currentCard ?: return
        val deletedIndex = currentState.currentIndex
        reduce { copy(isDeleting = true) }

        launch {
            runCatching {
                cardRepository.deleteCards(cardIds = listOf(card.id))
            }.onSuccess {
                val remainingCards = currentState.cards.filterNot { it.id == card.id }
                if (remainingCards.isEmpty()) {
                    reduce {
                        copy(
                            cards = persistentListOf(),
                            currentIndex = 0,
                            isDeleting = false,
                            isDeleteDialogVisible = false,
                            isFlipped = false,
                        )
                    }
                    postSideEffect(CardDetailSideEffect.NavigateBack)
                } else {
                    reduce {
                        copy(
                            cards = remainingCards.toPersistentList(),
                            currentIndex = deletedIndex.coerceAtMost(remainingCards.lastIndex),
                            isDeleting = false,
                            isDeleteDialogVisible = false,
                            isFlipped = false,
                        )
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                reduce {
                    copy(
                        isDeleting = false,
                        isDeleteDialogVisible = false,
                    )
                }
                postSideEffect(CardDetailSideEffect.ShowDeleteError)
            }
        }
    }
}

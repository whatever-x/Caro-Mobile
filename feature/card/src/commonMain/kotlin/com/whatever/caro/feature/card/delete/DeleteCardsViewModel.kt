package com.whatever.caro.feature.card.delete

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsIntent
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsSideEffect
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

class DeleteCardsViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val deckId: Long,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<DeleteCardsState, DeleteCardsIntent, DeleteCardsSideEffect>(
        initialState = DeleteCardsState(),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: DeleteCardsIntent) {
        when (intent) {
            DeleteCardsIntent.Initialize -> initialize()

            DeleteCardsIntent.ClickBack,
            DeleteCardsIntent.ClickCancel,
            -> postSideEffect(DeleteCardsSideEffect.NavigateBack)

            is DeleteCardsIntent.ClickCard -> toggleCard(intent.cardId)

            DeleteCardsIntent.ClickSelectAll -> toggleAllCards()

            DeleteCardsIntent.ClickDeleteSelected -> showDeleteConfirmDialog()

            DeleteCardsIntent.ClickDeleteCancel -> reduce { copy(isDeleteConfirmDialogVisible = false) }

            DeleteCardsIntent.ClickDeleteConfirm -> deleteSelectedCards()
        }
    }

    private fun initialize() {
        if (currentState.isLoading || currentState.cards.isNotEmpty()) return
        reduce { copy(isLoading = true) }

        launch {
            suspendRunCatching {
                deckRepository.getDeckCards(deckId = deckId)
            }.onSuccess { cards ->
                reduce {
                    copy(
                        cards = cards.map { card -> DeleteCardItem(card = card) }.toPersistentList(),
                        isLoading = false,
                    )
                }
            }.onFailure {
                reduce { copy(isLoading = false) }
                postSideEffect(DeleteCardsSideEffect.ShowLoadError)
            }
        }
    }

    private fun toggleCard(cardId: Long) {
        if (currentState.isDeleting) return
        val selectedIds =
            currentState.selectedCardIds
                .toMutableSet()
                .apply {
                    if (cardId in this) {
                        remove(cardId)
                    } else {
                        add(cardId)
                    }
                }.toPersistentSet()

        reduce {
            copy(
                selectedCardIds = selectedIds,
                cards =
                    cards
                        .map { item ->
                            item.copy(isSelected = item.card.id in selectedIds)
                        }.toPersistentList(),
            )
        }
    }

    private fun toggleAllCards() {
        if (currentState.isDeleting || currentState.cards.isEmpty()) return
        val selectedIds =
            if (currentState.isAllCardsSelected) {
                emptySet<Long>().toPersistentSet()
            } else {
                currentState.cards.map { item -> item.card.id }.toPersistentSet()
            }

        reduce {
            copy(
                selectedCardIds = selectedIds,
                cards =
                    cards
                        .map { item -> item.copy(isSelected = item.card.id in selectedIds) }
                        .toPersistentList(),
            )
        }
    }

    private fun showDeleteConfirmDialog() {
        if (currentState.isDeleteEnabled.not()) return
        reduce { copy(isDeleteConfirmDialogVisible = true) }
    }

    private fun deleteSelectedCards() {
        if (currentState.isDeleteEnabled.not()) return
        val selectedIds = currentState.selectedCardIds.toList()
        reduce {
            copy(
                isDeleting = true,
                isDeleteConfirmDialogVisible = false,
            )
        }

        launch {
            suspendRunCatching {
                cardRepository.deleteCards(cardIds = selectedIds)
            }.onSuccess {
                postSideEffect(DeleteCardsSideEffect.NavigateBack)
            }.onFailure {
                reduce { copy(isDeleting = false) }
                postSideEffect(DeleteCardsSideEffect.ShowDeleteError)
            }
        }
    }
}

package com.whatever.caro.feature.card

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.mvi.CreateCardIntent
import com.whatever.caro.feature.card.mvi.CreateCardSideEffect
import com.whatever.caro.feature.card.mvi.CreateCardState
import com.whatever.caro.feature.card.mvi.StagedCard
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException

class CreateCardViewModel(
    private val cardRepository: CardRepository,
    private val deckId: Long,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<CreateCardState, CreateCardIntent, CreateCardSideEffect>(
        initialState = CreateCardState(),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: CreateCardIntent) {
        when (intent) {
            is CreateCardIntent.UpdateFront -> handleUpdateFront(intent.front)
            is CreateCardIntent.UpdateBack -> handleUpdateBack(intent.back)
            is CreateCardIntent.ClickSwap -> handleSwap()
            is CreateCardIntent.ClickAddCard -> handleAddCard()
            is CreateCardIntent.ClickRemoveCard -> handleRemoveCard(intent.id)
            is CreateCardIntent.ClickSave -> handleSave()
            is CreateCardIntent.ClickBack -> postSideEffect(CreateCardSideEffect.NavigateBack)
        }
    }

    private fun handleUpdateFront(front: String) {
        reduce { copy(front = front.take(CardInputLimits.FIELD_MAX)) }
    }

    private fun handleUpdateBack(back: String) {
        reduce { copy(back = back.take(CardInputLimits.FIELD_MAX)) }
    }

    private fun handleSwap() {
        reduce { copy(front = back, back = front) }
    }

    private fun handleAddCard() {
        if (currentState.isAddEnabled.not()) return
        reduce {
            copy(
                addedCards =
                    addedCards.toPersistentList().add(
                        StagedCard(id = nextCardId, content = CardContent(front = front, back = back)),
                    ),
                nextCardId = nextCardId + 1,
                front = "",
                back = "",
            )
        }
    }

    private fun handleRemoveCard(id: Long) {
        reduce { copy(addedCards = addedCards.filterNot { it.id == id }.toPersistentList()) }
    }

    private fun handleSave() {
        if (currentState.isSaveEnabled.not()) return
        val cards = currentState.addedCards.map { it.content }
        reduce { copy(isSaving = true) }

        launch {
            runCatching {
                cardRepository.createCards(
                    deckId = deckId,
                    cards = cards,
                )
            }.onSuccess {
                postSideEffect(CreateCardSideEffect.NavigateBack)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                reduce { copy(isSaving = false) }
                postSideEffect(CreateCardSideEffect.ShowSaveError)
            }
        }
    }
}

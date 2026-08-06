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
            is CreateCardIntent.ClickBack -> handleBack()
            is CreateCardIntent.ConfirmDiscard -> handleConfirmDiscard()
            is CreateCardIntent.DismissDiscardDialog -> reduce { copy(isDiscardDialogVisible = false) }
            is CreateCardIntent.DismissMaxCardsDialog -> reduce { copy(isMaxCardsDialogVisible = false) }
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
            val added =
                addedCards.toPersistentList().add(
                    StagedCard(id = nextCardId, content = CardContent(front = front, back = back)),
                )
            copy(
                addedCards = added,
                nextCardId = nextCardId + 1,
                front = "",
                back = "",
                // 한도에 도달한 그 순간에만 안내한다(이후 추가 버튼은 계속 비활성).
                isMaxCardsDialogVisible = added.size >= CardInputLimits.MAX_CARDS,
            )
        }
    }

    private fun handleBack() {
        if (currentState.hasUnsavedInput) {
            reduce { copy(isDiscardDialogVisible = true) }
        } else {
            postSideEffect(CreateCardSideEffect.NavigateBack)
        }
    }

    // 다이얼로그가 떠 있는 동안만 1회 소비한다. 연타하면 NavigateBack 이 쌓여 두 화면 뒤로 간다.
    private fun handleConfirmDiscard() {
        if (currentState.isDiscardDialogVisible.not()) return
        reduce { copy(isDiscardDialogVisible = false) }
        postSideEffect(CreateCardSideEffect.NavigateBack)
    }

    private fun handleRemoveCard(id: Long) {
        reduce { copy(addedCards = addedCards.filterNot { it.id == id }.toPersistentList()) }
    }

    private fun handleSave() {
        if (currentState.isSaveEnabled.not()) return
        val cards = currentState.addedCards.map { it.content }
        reduce { copy(isSaving = true, isMaxCardsDialogVisible = false) }

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

package com.whatever.caro.feature.card

import com.whatever.caro.core.data.repository.card.CardRepository
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.card.mvi.EditCardIntent
import com.whatever.caro.feature.card.mvi.EditCardSideEffect
import com.whatever.caro.feature.card.mvi.EditCardState
import kotlinx.coroutines.CancellationException

class EditCardViewModel(
    private val cardRepository: CardRepository,
    private val cardId: Long,
    front: String,
    back: String,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<EditCardState, EditCardIntent, EditCardSideEffect>(
        initialState =
            EditCardState(
                front = front.take(CardInputLimits.FIELD_MAX),
                back = back.take(CardInputLimits.FIELD_MAX),
            ),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: EditCardIntent) {
        when (intent) {
            is EditCardIntent.UpdateFront -> handleUpdateFront(intent.front)
            is EditCardIntent.UpdateBack -> handleUpdateBack(intent.back)
            EditCardIntent.ClickSwap -> handleSwap()
            EditCardIntent.ClickSave -> handleSave()
            EditCardIntent.ClickBack -> postSideEffect(EditCardSideEffect.NavigateBack)
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

    private fun handleSave() {
        if (currentState.isSaveEnabled.not()) return
        val content = CardContent(front = currentState.front, back = currentState.back)
        reduce { copy(isSaving = true) }

        launch {
            runCatching {
                cardRepository.updateCard(
                    cardId = cardId,
                    content = content,
                )
            }.onSuccess {
                postSideEffect(EditCardSideEffect.NavigateBack)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                reduce { copy(isSaving = false) }
                postSideEffect(EditCardSideEffect.ShowSaveError)
            }
        }
    }
}

package com.whatever.caro.feature.deck.edit

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.edit.mvi.EditDeckIntent
import com.whatever.caro.feature.deck.edit.mvi.EditDeckSideEffect
import com.whatever.caro.feature.deck.edit.mvi.EditDeckState
import io.github.aakira.napier.Napier

class EditDeckViewModel(
    private val deckRepository: DeckRepository,
    private val deckId: Long,
    exceptionFilter: ExceptionFilter,
    deckName: String,
    deckDescription: String,
) : BaseViewModel<EditDeckState, EditDeckIntent, EditDeckSideEffect>(
        initialState =
            EditDeckState(
                name = deckName,
                description = deckDescription,
            ),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: EditDeckIntent) {
        when (intent) {
            is EditDeckIntent.UpdateName -> handleUpdateName(intent.name)
            is EditDeckIntent.UpdateDescription -> handleUpdateDescription(intent.description)
            is EditDeckIntent.ClickBack -> postSideEffect(EditDeckSideEffect.NavigateBack)
            is EditDeckIntent.ClickConfirm -> handleConfirm()
        }
    }

    private fun handleUpdateName(name: String) {
        reduce { copy(name = name.take(DeckInputLimits.NAME_MAX)) }
    }

    private fun handleUpdateDescription(description: String) {
        reduce { copy(description = description.take(DeckInputLimits.DESC_MAX)) }
    }

    private fun handleConfirm() {
        if (currentState.isConfirmEnabled.not()) return
        reduce { copy(isLoading = true) }
        launch {
            suspendRunCatching {
                deckRepository.updateDeck(
                    deckId = deckId,
                    name = currentState.name,
                    description = currentState.description,
                )
            }.onSuccess {
                postSideEffect(EditDeckSideEffect.NavigateBack)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "Edit Deck failed" }
                reduce { copy(isLoading = false) }
                postSideEffect(EditDeckSideEffect.ShowError)
            }
        }
    }
}

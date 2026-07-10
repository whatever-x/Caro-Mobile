package com.whatever.caro.feature.deck.create

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.deck.DeckInputLimits
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.create.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.create.mvi.CreateDeckSideEffect
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import io.github.aakira.napier.Napier

class CreateDeckViewModel(
    private val deckRepository: DeckRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<CreateDeckState, CreateDeckIntent, CreateDeckSideEffect>(
        initialState = CreateDeckState(),
        exceptionFilter = exceptionFilter,
    ) {
    override suspend fun handleIntent(intent: CreateDeckIntent) {
        when (intent) {
            is CreateDeckIntent.UpdateName -> handleUpdateName(intent.name)
            is CreateDeckIntent.UpdateDescription -> handleUpdateDescription(intent.description)
            is CreateDeckIntent.ClickBack -> postSideEffect(CreateDeckSideEffect.NavigateBack)
            is CreateDeckIntent.ClickConfirm -> handleConfirm()
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
                deckRepository.createDeck(
                    name = currentState.name,
                    description = currentState.description,
                )
            }.onSuccess {
                postSideEffect(CreateDeckSideEffect.NavigateBack)
            }.onFailure { throwable ->
                Napier.e(throwable = throwable) { "createDeck failed" }
                reduce { copy(isLoading = false) }
                postSideEffect(CreateDeckSideEffect.ShowError)
            }
        }
    }
}

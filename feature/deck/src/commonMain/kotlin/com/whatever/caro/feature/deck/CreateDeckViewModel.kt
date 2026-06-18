package com.whatever.caro.feature.deck

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.deck.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.mvi.CreateDeckSideEffect
import com.whatever.caro.feature.deck.mvi.CreateDeckState

class CreateDeckViewModel(
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
        // TODO: 덱 생성 API 연동 (서버 스펙 확정 후 DeckRepository 호출)
        postSideEffect(CreateDeckSideEffect.NavigateBack)
    }
}

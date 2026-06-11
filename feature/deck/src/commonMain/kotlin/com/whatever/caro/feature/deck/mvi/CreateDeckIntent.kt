package com.whatever.caro.feature.deck.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CreateDeckIntent : UiIntent {
    data class UpdateName(
        val name: String,
    ) : CreateDeckIntent

    data class UpdateDescription(
        val description: String,
    ) : CreateDeckIntent

    data object ClickBack : CreateDeckIntent

    data object ClickConfirm : CreateDeckIntent
}

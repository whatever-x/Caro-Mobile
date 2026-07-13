package com.whatever.caro.feature.deck.edit.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface EditDeckIntent : UiIntent {
    data class UpdateName(
        val name: String,
    ) : EditDeckIntent

    data class UpdateDescription(
        val description: String,
    ) : EditDeckIntent

    data object ClickBack : EditDeckIntent

    data object ClickConfirm : EditDeckIntent
}

package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CreateCardIntent : UiIntent {
    data class UpdateFront(
        val front: String,
    ) : CreateCardIntent

    data class UpdateBack(
        val back: String,
    ) : CreateCardIntent

    data object ClickSwap : CreateCardIntent

    data object ClickAddCard : CreateCardIntent

    data class ClickRemoveCard(
        val id: Long,
    ) : CreateCardIntent

    data object ClickSave : CreateCardIntent

    data object ClickBack : CreateCardIntent

    data object ConfirmDiscard : CreateCardIntent

    data object DismissDiscardDialog : CreateCardIntent
}

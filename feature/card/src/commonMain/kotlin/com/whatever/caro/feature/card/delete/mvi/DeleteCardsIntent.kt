package com.whatever.caro.feature.card.delete.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface DeleteCardsIntent : UiIntent {
    data object Initialize : DeleteCardsIntent

    data object ClickBack : DeleteCardsIntent

    data class ClickCard(
        val cardId: Long,
    ) : DeleteCardsIntent

    data object ClickDeleteSelected : DeleteCardsIntent

    data object ClickDeleteConfirm : DeleteCardsIntent

    data object ClickDeleteCancel : DeleteCardsIntent

    data object ClickCancel : DeleteCardsIntent
}

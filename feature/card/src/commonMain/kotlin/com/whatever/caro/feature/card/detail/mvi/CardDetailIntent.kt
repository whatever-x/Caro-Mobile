package com.whatever.caro.feature.card.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CardDetailIntent : UiIntent {
    data object ClickBack : CardDetailIntent

    data object ClickEdit : CardDetailIntent

    data object ClickDelete : CardDetailIntent

    data object DismissDeleteDialog : CardDetailIntent

    data object ConfirmDelete : CardDetailIntent

    data object FlipCard : CardDetailIntent

    data class ChangeCard(
        val index: Int,
    ) : CardDetailIntent

    data object RefreshCards : CardDetailIntent
}

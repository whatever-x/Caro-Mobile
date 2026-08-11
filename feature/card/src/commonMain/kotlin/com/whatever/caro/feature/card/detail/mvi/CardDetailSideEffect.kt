package com.whatever.caro.feature.card.detail.mvi

import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface CardDetailSideEffect : UiSideEffect {
    data object NavigateBack : CardDetailSideEffect

    data class NavigateToEdit(
        val cardId: Long,
        val front: String,
        val back: String,
    ) : CardDetailSideEffect

    data object ShowLoadError : CardDetailSideEffect

    data object ShowRefreshError : CardDetailSideEffect

    data object ShowDeleteError : CardDetailSideEffect
}

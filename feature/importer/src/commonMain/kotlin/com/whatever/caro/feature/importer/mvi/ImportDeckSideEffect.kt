package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface ImportDeckSideEffect : UiSideEffect {
    data object OpenFilePicker : ImportDeckSideEffect

    data object NavigateBack : ImportDeckSideEffect

    data object NavigateHome : ImportDeckSideEffect

    /** [saved]가 [total]보다 작으면 멈춘 저장이고, 0이면 빈 덱이 남았다. */
    data class NavigateToDeckDetail(
        val deck: Deck,
        val saved: Int,
        val total: Int,
        val completed: Boolean,
    ) : ImportDeckSideEffect

    /** 서버가 이름이나 설명 값을 거절했다. */
    data object ShowDeckInputError : ImportDeckSideEffect

    data object ShowDeckCreateError : ImportDeckSideEffect
}

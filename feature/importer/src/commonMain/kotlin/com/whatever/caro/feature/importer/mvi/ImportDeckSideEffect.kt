package com.whatever.caro.feature.importer.mvi

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.viewmodel.contract.UiSideEffect

sealed interface ImportDeckSideEffect : UiSideEffect {
    data object OpenFilePicker : ImportDeckSideEffect

    data object NavigateBack : ImportDeckSideEffect

    data object NavigateHome : ImportDeckSideEffect

    /** [saved]는 확인한 장수다. [isSaveUnconfirmed]면 서버에 더 저장됐을 수 있어 결과 안내가 장수를 단정하면 안 된다. */
    data class NavigateToDeckDetail(
        val deck: Deck,
        val saved: Int,
        val total: Int,
        val completed: Boolean,
        val isSaveUnconfirmed: Boolean,
    ) : ImportDeckSideEffect

    /** 서버가 덱 이름이나 설명을 거절했다. 미리보기에 머물고 덱 정보 시트는 열지 않는다. */
    data object ShowDeckInputError : ImportDeckSideEffect
}

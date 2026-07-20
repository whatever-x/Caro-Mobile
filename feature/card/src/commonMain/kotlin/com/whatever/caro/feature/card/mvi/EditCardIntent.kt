package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface EditCardIntent : UiIntent {
    data class UpdateFront(
        val front: String,
    ) : EditCardIntent

    data class UpdateBack(
        val back: String,
    ) : EditCardIntent

    data object ClickSwap : EditCardIntent

    data object ClickSave : EditCardIntent

    data object ClickBack : EditCardIntent
}

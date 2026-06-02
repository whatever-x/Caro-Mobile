package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface HomeIntent : UiIntent {
    data object ClickSettingButton : HomeIntent

    data object ClickCreateDeckButton : HomeIntent

    data class ClickDeckButton(
        val deckId: Long,
    ) : HomeIntent
}

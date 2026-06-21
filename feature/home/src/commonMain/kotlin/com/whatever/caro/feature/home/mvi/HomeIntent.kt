package com.whatever.caro.feature.home.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface HomeIntent : UiIntent {
    data object ClickLogout : HomeIntent

    data object ClickSignUp : HomeIntent

    data object ClickProfile : HomeIntent

    data object ClickCreateDeck : HomeIntent

    data object ClickCreateCard : HomeIntent
}

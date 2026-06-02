package com.whatever.caro.feature.home

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState

class HomeViewModel :
    BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
    ) {
    override fun handleClientException(throwable: Throwable) {
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ClickCreateDeckButton -> TODO()
            is HomeIntent.ClickDeckButton -> TODO()
            HomeIntent.ClickSettingButton -> TODO()
            HomeIntent.ClickProfile -> postSideEffect(HomeSideEffect.NavigateToProfile)
            HomeIntent.ClickCreateDeck -> postSideEffect(HomeSideEffect.NavigateToCreateDeck)
        }
    }
}

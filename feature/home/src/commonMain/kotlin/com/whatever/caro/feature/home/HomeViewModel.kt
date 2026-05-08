package com.whatever.caro.feature.home

import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState

class HomeViewModel(
    private val navKey: HomeEntry,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
    ) {
    override fun handleClientException(throwable: Throwable) {
    }

    override suspend fun handleIntent(intent: HomeIntent) {
    }

    fun init() {
        reduce {
            copy(
                screenName = "HomeScreen",
                name = navKey.payload.name,
            )
        }
    }
}

package com.whatever.caro.feature.home

import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState

class HomeViewModel(
    private val navKey: HomeEntry,
    private val demoRepository: DemoRepository,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
    ) {
    override fun handleClientException(throwable: Throwable) {
        TODO()
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        TODO()
    }

    fun init() {
        launch {
            val userData = demoRepository.getData(id = navKey.payload.id.toLong())

            reduce {
                copy(
                    screenName = "HomeScreen",
                    name = userData.name,
                )
            }
        }
    }
}

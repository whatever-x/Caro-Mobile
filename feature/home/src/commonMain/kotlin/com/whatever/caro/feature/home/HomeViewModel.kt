package com.whatever.caro.feature.home

import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import io.github.aakira.napier.Napier

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val navKey: HomeEntry,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
    ) {
    override fun handleClientException(throwable: Throwable) {
        Napier.e { "exception: $throwable" }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ClickLogout -> testClickLogout()
            HomeIntent.ClickSignUp -> testClickSignUp()
            HomeIntent.ClickProfile -> postSideEffect(HomeSideEffect.NavigateToProfile)
            HomeIntent.ClickCreateDeck -> postSideEffect(HomeSideEffect.NavigateToCreateDeck)
        }
    }

    private suspend fun testClickLogout() {
        authRepository.logout()
    }

    private suspend fun testClickSignUp() {
        authRepository.completeRegistration(
            nickname = "Test",
            termsAgreed = true,
        )
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

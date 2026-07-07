package com.whatever.caro.feature.home

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import io.github.aakira.napier.Napier

class HomeViewModel(
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
        exceptionFilter = exceptionFilter,
    ) {
    override fun handleClientException(throwable: Throwable) {
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ClickCreateDeckButton -> {
                Napier.d { "intent: $intent" }
            }

            is HomeIntent.ClickDeckButton -> {
                postSideEffect(
                    HomeSideEffect.NavigateToDeckCards(
                        deckId = intent.deckId,
                        deckTitle = intent.deckTitle,
                    ),
                )
            }

            HomeIntent.ClickSettingButton -> {
                postSideEffect(HomeSideEffect.NavigateToSetting)
            }

            HomeIntent.ClickProfile -> {
                postSideEffect(HomeSideEffect.NavigateToProfile)
            }

            HomeIntent.ClickCreateDeck -> {
                postSideEffect(HomeSideEffect.NavigateToCreateDeck)
            }
        }
    }
}

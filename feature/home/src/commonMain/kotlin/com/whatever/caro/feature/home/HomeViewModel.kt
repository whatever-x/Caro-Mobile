package com.whatever.caro.feature.home

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import kotlinx.collections.immutable.toImmutableList

class HomeViewModel(
    private val deckRepository: DeckRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
        exceptionFilter = exceptionFilter,
    ) {
    override fun handleClientException(throwable: Throwable) {
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Initialize -> {
                initialize()
            }

            HomeIntent.ClickCreateDeckButton -> {
                postSideEffect(
                    HomeSideEffect.NavigateToCreateDeck,
                )
            }

            is HomeIntent.ClickDeckButton -> {
                postSideEffect(
                    HomeSideEffect.NavigateToDeckDetail(
                        deck = intent.deck,
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

    private suspend fun initialize() {
        val decks = deckRepository.getDecks()
        reduce { copy(decks = decks.toImmutableList()) }
    }
}

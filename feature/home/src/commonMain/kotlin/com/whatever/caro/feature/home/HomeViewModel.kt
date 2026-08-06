package com.whatever.caro.feature.home

import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.repository.streak.StreakRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.model.streak.Streak
import com.whatever.caro.core.model.streak.StreakStatus
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HomeViewModel(
    private val deckRepository: DeckRepository,
    private val streakRepository: StreakRepository,
    private val profileRepository: ProfileRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = HomeState(),
        exceptionFilter = exceptionFilter,
    ) {
    private var initializationGeneration = 0L

    override fun handleClientException(throwable: Throwable) {
        reduce { copy(isLoading = false) }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Initialize -> {
                initialize()
            }

            HomeIntent.ClickRetry -> {
                initialize()
            }

            is HomeIntent.ClickStartLearning -> {
                postSideEffect(HomeSideEffect.NavigateToDailyLearning(deckId = intent.deckId))
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

            HomeIntent.ClickCreateDeck -> {
                postSideEffect(HomeSideEffect.NavigateToCreateDeck)
            }
        }
    }

    private suspend fun initialize() =
        coroutineScope {
            val generation = ++initializationGeneration
            reduce {
                copy(
                    isLoading = true,
                    hasLoadError = false,
                )
            }

            val decksDeferred =
                async {
                    suspendRunCatching {
                        deckRepository.getDecks().toImmutableList()
                    }
                }
            val streakDeferred =
                async {
                    suspendRunCatching {
                        streakRepository.getStreak().toHomeStreakState()
                    }
                }
            val nicknameDeferred =
                async {
                    suspendRunCatching {
                        profileRepository.getMyNickname()
                    }
                }

            val decksResult = decksDeferred.await()
            val streakResult = streakDeferred.await()
            val nicknameResult = nicknameDeferred.await()
            val loadedDecks = decksResult.getOrNull()
            val loadedStreak = streakResult.getOrNull()
            val loadedNickname = nicknameResult.getOrNull()
            val exceptions =
                listOfNotNull(
                    decksResult.exceptionOrNull(),
                    streakResult.exceptionOrNull(),
                    nicknameResult.exceptionOrNull(),
                )

            if (generation != initializationGeneration) return@coroutineScope

            reduce {
                copy(
                    isLoading = false,
                    nickname = loadedNickname ?: nickname,
                    decks = loadedDecks ?: decks,
                    streakState = loadedStreak ?: streakState,
                    hasLoadError = exceptions.isNotEmpty(),
                )
            }

            if (decksResult.isFailure) {
                postSideEffect(HomeSideEffect.ShowDeckLoadError)
            }

            exceptions.firstOrNull()?.let { primary ->
                primary.addSuppressedExceptions(exceptions.drop(1))
                throw primary
            }
        }

    private fun Streak.toHomeStreakState(): HomeStreakState =
        when (status) {
            StreakStatus.NOT_STARTED -> HomeStreakState.NotStarted
            StreakStatus.ACTIVE -> HomeStreakState.Active(days = currentDays)
            StreakStatus.BROKEN -> HomeStreakState.Broken
        }
}

private fun Throwable.addSuppressedExceptions(exceptions: Iterable<Throwable>) {
    exceptions.forEach(::addSuppressed)
}

package com.whatever.caro.feature.home

import com.whatever.caro.core.data.repository.banner.HomeBannerRepository
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

class HomeViewModel(
    private val deckRepository: DeckRepository,
    private val streakRepository: StreakRepository,
    private val profileRepository: ProfileRepository,
    private val homeBannerRepository: HomeBannerRepository,
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
            is HomeIntent.ClickBanner -> {
                val banner =
                    state.value.banners.firstOrNull {
                        it.id == intent.bannerId && it.revision == intent.revision && it.expiresAt > Clock.System.now()
                    } ?: return
                postSideEffect(
                    banner.libraryDeckId?.let { HomeSideEffect.NavigateToLibraryPreview(it) }
                        ?: HomeSideEffect.NavigateToLibrary,
                )
            }

            HomeIntent.ClickLibrary -> {
                postSideEffect(HomeSideEffect.NavigateToLibrary)
            }

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
            val bannersDeferred =
                async {
                    suspendRunCatching { homeBannerRepository.list().toImmutableList() }
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

            // 배너 응답을 기다리는 동안에도 덱 조회와 학습 진입은 먼저 표시한다.
            val banners = bannersDeferred.await().getOrDefault(persistentListOf())
            if (generation == initializationGeneration) reduce { copy(banners = banners) }
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

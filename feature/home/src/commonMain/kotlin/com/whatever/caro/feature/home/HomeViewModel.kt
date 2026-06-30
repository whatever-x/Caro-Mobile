package com.whatever.caro.feature.home

import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.persistentListOf

class HomeViewModel(
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
        initialState = testableInitialState,
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

    private companion object {
        val testableInitialState =
            HomeState(
                nickname = "테스터",
                additionalDescription = "디바이스 테스트용 덱을 선택해보세요.",
                learningDays = 7,
                decks =
                    persistentListOf(
                        Deck(
                            id = 1L,
                            title = "영어 단어 1000개",
                            description = "일상에서 많이 쓰는 단어를 예문 중심으로 빠르게 익혀요.",
                            cardTotalCount = 40,
                            todayLearningCount = 40,
                            todayCompleteCount = 0,
                            state = DeckState.NOT_STARTED,
                        ),
                        Deck(
                            id = 2L,
                            title = "Android 면접 질문",
                            description = "Compose, Coroutine, Architecture 핵심 질문을 복습해요.",
                            cardTotalCount = 32,
                            todayLearningCount = 12,
                            todayCompleteCount = 6,
                            state = DeckState.LEARNING,
                        ),
                    ),
            )
    }
}

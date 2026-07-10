package com.whatever.caro.feature.deck.detail.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditDeckEntry
import com.whatever.caro.feature.deck.detail.DeckDetailScreen
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                DeckDetailSideEffect.NavigateBack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                is DeckDetailSideEffect.NavigateToCreateCard -> {
                    // TODO: 카드 생성 화면 구현 후 sideEffect.deckId 를 사용해 이동 로직을 연결합니다.
                }

                is DeckDetailSideEffect.NavigateToAllStudy -> {
                    // TODO: 전체 학습 화면 구현 후 sideEffect.deckId 를 사용해 이동 로직을 연결합니다.
                }

                is DeckDetailSideEffect.NavigateToDailyStudy -> {
                    // TODO: 일일 학습 화면 구현 후 sideEffect.deckId 를 사용해 이동 로직을 연결합니다.
                }

                is DeckDetailSideEffect.NavigateToEditCardList -> {
                    // TODO: 카드 목록 편집 화면 구현 후 sideEffect.deckId 를 사용해 이동 로직을 연결합니다.
                }

                is DeckDetailSideEffect.NavigateToEditDeck -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                EditDeckEntry(
                                    deckId = state.deck.id,
                                    deckName = state.deck.title,
                                    deckDescription = state.deck.description,
                                ),
                        ),
                    )
                }

                is DeckDetailSideEffect.NavigateToCardDetail -> {
                    // TODO: 카드 상세 화면 구현 후 sideEffect.cardId 를 사용해 이동 로직을 연결합니다.
                }
            }
        }
    }

    DeckDetailScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

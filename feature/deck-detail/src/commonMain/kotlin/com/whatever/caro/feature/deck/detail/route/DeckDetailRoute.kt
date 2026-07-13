package com.whatever.caro.feature.deck.detail.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.AllLearningEntry
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.DailyLearningEntry
import com.whatever.caro.core.navigator.entries.DeleteCardsEntry
import com.whatever.caro.core.navigator.entries.EditDeckEntry
import com.whatever.caro.feature.deck.detail.DeckDetailScreen
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.intent(DeckDetailIntent.Initialize)
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                DeckDetailSideEffect.NavigateBack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                is DeckDetailSideEffect.NavigateToCreateCard -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            CreateCardEntry(
                                CreateCardEntry.Payload(
                                    sideEffect.deckId,
                                ),
                            ),
                        ),
                    )
                }

                is DeckDetailSideEffect.NavigateToAllStudy -> {
                    navDispatcher.emit(NavCommand.To(AllLearningEntry(sideEffect.deckId)))
                }

                is DeckDetailSideEffect.NavigateToDailyStudy -> {
                    navDispatcher.emit(NavCommand.To(DailyLearningEntry(sideEffect.deckId)))
                }

                is DeckDetailSideEffect.NavigateToEditCardList -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                DeleteCardsEntry(
                                    payload =
                                        DeleteCardsEntry.Payload(
                                            deckId = sideEffect.deckId,
                                        ),
                                ),
                        ),
                    )
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

package com.whatever.caro.feature.deck.detail.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_list_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.deck.detail.DeckDetailScreen
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailIntent
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cardLoadErrorMessage = stringResource(Res.string.card_list_error)
    var hasResumed by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumed) {
            viewModel.intent(DeckDetailIntent.RefreshCards)
        } else {
            hasResumed = true
        }
    }

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
                    // TODO: 덱 수정 화면 구현 후 sideEffect.deckId 를 사용해 이동 로직을 연결합니다.
                }

                is DeckDetailSideEffect.NavigateToEditCard -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                EditCardEntry(
                                    payload =
                                        EditCardEntry.Payload(
                                            cardId = sideEffect.cardId,
                                            front = sideEffect.front,
                                            back = sideEffect.back,
                                        ),
                                ),
                        ),
                    )
                }

                DeckDetailSideEffect.ShowCardLoadError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = cardLoadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    DeckDetailScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

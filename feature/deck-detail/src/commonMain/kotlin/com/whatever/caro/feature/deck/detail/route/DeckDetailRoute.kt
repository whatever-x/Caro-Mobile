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
import com.whatever.caro.core.navigator.entries.CardDetailEntry
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.DeleteCardsEntry
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.navigator.entries.EditDeckEntry
import com.whatever.caro.core.navigator.entries.LearningEntry
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
                    navDispatcher.emit(
                        NavCommand.To(
                            CreateCardEntry(
                                deckId = sideEffect.deckId,
                            ),
                        ),
                    )
                }

                is DeckDetailSideEffect.NavigateToLearning -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            LearningEntry(
                                deckId = sideEffect.deckId,
                                mode = sideEffect.mode,
                            ),
                        ),
                    )
                }

                is DeckDetailSideEffect.NavigateToEditCardList -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                DeleteCardsEntry(
                                    deckId = sideEffect.deckId,
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
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                CardDetailEntry(
                                    deckId = sideEffect.deckId,
                                    cardId = sideEffect.cardId,
                                ),
                        ),
                    )
                }

                is DeckDetailSideEffect.NavigateToEditCard -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key =
                                EditCardEntry(
                                    cardId = sideEffect.cardId,
                                    front = sideEffect.front,
                                    back = sideEffect.back,
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

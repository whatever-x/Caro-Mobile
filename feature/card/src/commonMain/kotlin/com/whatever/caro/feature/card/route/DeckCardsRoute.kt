package com.whatever.caro.feature.card.route

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
import com.whatever.caro.core.navigator.entries.CreateCardEntry
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.DeckCardsScreen
import com.whatever.caro.feature.card.DeckCardsViewModel
import com.whatever.caro.feature.card.mvi.DeckCardsIntent
import com.whatever.caro.feature.card.mvi.DeckCardsSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeckCardsRoute(
    viewModel: DeckCardsViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loadErrorMessage = stringResource(Res.string.card_list_error)
    var hasResumed by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumed) {
            viewModel.intent(DeckCardsIntent.RefreshCards)
        } else {
            hasResumed = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                DeckCardsSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is DeckCardsSideEffect.NavigateToCreateCard -> {
                    navDispatcher.emit(
                        command =
                            NavCommand.To(
                                key =
                                    CreateCardEntry(
                                        payload =
                                            CreateCardEntry.Payload(
                                                deckId = sideEffect.deckId,
                                            ),
                                    ),
                            ),
                    )
                }

                is DeckCardsSideEffect.NavigateToEditCard -> {
                    navDispatcher.emit(
                        command =
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

                DeckCardsSideEffect.ShowLoadError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = loadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    DeckCardsScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

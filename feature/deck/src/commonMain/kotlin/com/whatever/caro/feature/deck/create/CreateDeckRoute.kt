package com.whatever.caro.feature.deck.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_snackbar_action_open
import caromobile.core.designsystem.generated.resources.deck_snackbar_create_error
import caromobile.core.designsystem.generated.resources.deck_snackbar_create_success
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.DeckDetailEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.deck.create.mvi.CreateDeckSideEffect
import com.whatever.caro.feature.deck.edit.CreateDeckScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateDeckRoute(
    viewModel: CreateDeckViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val createErrorMessage = stringResource(Res.string.deck_snackbar_create_error)
    val createSuccessMessage = stringResource(Res.string.deck_snackbar_create_success)
    val openDeckActionLabel = stringResource(Res.string.deck_snackbar_action_open)

    LaunchedEffect(
        viewModel,
        navDispatcher,
        snackbarController,
        createErrorMessage,
        createSuccessMessage,
        openDeckActionLabel,
    ) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateDeckSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is CreateDeckSideEffect.Created -> {
                    navDispatcher.emit(command = NavCommand.Back)
                    snackbarController.show(
                        createDeckCreatedSnackbar(
                            deck = sideEffect.deck,
                            message = createSuccessMessage,
                            actionLabel = openDeckActionLabel,
                            navDispatcher = navDispatcher,
                        ),
                    )
                }

                is CreateDeckSideEffect.ShowError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = createErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    CreateDeckScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

internal fun createDeckCreatedSnackbar(
    deck: Deck,
    message: String,
    actionLabel: String,
    navDispatcher: NavigationDispatcher,
): SnackBarMessage =
    SnackBarMessage(
        message = message,
        actionLabel = actionLabel,
        onAction = {
            navDispatcher.emit(
                command =
                    NavCommand.To(
                        key =
                            DeckDetailEntry(
                                payload = DeckDetailEntry.Payload(deck = deck),
                            ),
                    ),
            )
        },
    )

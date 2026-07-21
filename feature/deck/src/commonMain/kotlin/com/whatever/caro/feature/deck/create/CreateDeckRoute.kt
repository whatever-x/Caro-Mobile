package com.whatever.caro.feature.deck.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_snackbar_create_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
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

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateDeckSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is CreateDeckSideEffect.Created -> {
                    navDispatcher.emit(command = NavCommand.Back)
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

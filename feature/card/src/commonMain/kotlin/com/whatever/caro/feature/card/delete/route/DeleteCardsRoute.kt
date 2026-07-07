package com.whatever.caro.feature.card.delete.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_delete_error
import caromobile.core.designsystem.generated.resources.card_load_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.delete.DeleteCardsScreen
import com.whatever.caro.feature.card.delete.DeleteCardsViewModel
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsIntent
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteCardsRoute(
    viewModel: DeleteCardsViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loadErrorMessage = stringResource(Res.string.card_load_error)
    val deleteErrorMessage = stringResource(Res.string.card_delete_error)

    LaunchedEffect(Unit) {
        viewModel.intent(DeleteCardsIntent.Initialize)
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                DeleteCardsSideEffect.NavigateBack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                DeleteCardsSideEffect.ShowLoadError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = loadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                DeleteCardsSideEffect.ShowDeleteError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = deleteErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    DeleteCardsScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

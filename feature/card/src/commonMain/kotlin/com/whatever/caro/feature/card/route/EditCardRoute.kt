package com.whatever.caro.feature.card.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_update_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.EditCardScreen
import com.whatever.caro.feature.card.EditCardViewModel
import com.whatever.caro.feature.card.mvi.EditCardSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditCardRoute(
    viewModel: EditCardViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateErrorMessage = stringResource(Res.string.card_update_error)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                EditCardSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                EditCardSideEffect.ShowSaveError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = updateErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    EditCardScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

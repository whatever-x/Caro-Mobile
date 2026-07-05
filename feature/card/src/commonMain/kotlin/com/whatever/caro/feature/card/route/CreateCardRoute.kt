package com.whatever.caro.feature.card.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_save_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.CreateCardScreen
import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.mvi.CreateCardSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateCardRoute(
    viewModel: CreateCardViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveErrorMessage = stringResource(Res.string.card_save_error)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateCardSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is CreateCardSideEffect.ShowSaveError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = saveErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    CreateCardScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

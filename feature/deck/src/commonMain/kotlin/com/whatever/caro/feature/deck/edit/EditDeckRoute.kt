package com.whatever.caro.feature.deck.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_snackbar_edit_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.deck.edit.mvi.EditDeckSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditDeckRoute(
    viewModel: EditDeckViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editErrorMessage = stringResource(Res.string.deck_snackbar_edit_error)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is EditDeckSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is EditDeckSideEffect.ShowError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = editErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    EditDeckScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

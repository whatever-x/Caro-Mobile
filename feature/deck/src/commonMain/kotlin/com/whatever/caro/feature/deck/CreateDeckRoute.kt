package com.whatever.caro.feature.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_toast_create_error
import com.whatever.caro.core.designsystem.components.snackbar.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.toast.ToastController
import com.whatever.caro.core.ui.toast.ToastMessage
import com.whatever.caro.feature.deck.mvi.CreateDeckSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateDeckRoute(
    viewModel: CreateDeckViewModel,
    navDispatcher: NavigationDispatcher,
    toastController: ToastController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val createErrorMessage = stringResource(Res.string.deck_toast_create_error)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateDeckSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is CreateDeckSideEffect.ShowError -> {
                    toastController.show(
                        ToastMessage(
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

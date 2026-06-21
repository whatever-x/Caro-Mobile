package com.whatever.caro.feature.card.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_save_error
import com.whatever.caro.core.designsystem.components.LocalSnackbarHostState
import com.whatever.caro.core.designsystem.components.showSnackbarMessage
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.card.CreateCardScreen
import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.mvi.CreateCardSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateCardRoute(
    viewModel: CreateCardViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHostState.current
    val saveErrorMessage = stringResource(Res.string.card_save_error)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateCardSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                is CreateCardSideEffect.ShowSaveError -> {
                    showSnackbarMessage(
                        coroutineScope = this,
                        snackbarHostState = snackbarHost,
                        message = saveErrorMessage,
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

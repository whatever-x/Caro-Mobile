package com.whatever.caro.feature.card.detail.route

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
import caromobile.core.designsystem.generated.resources.card_detail_delete_error
import caromobile.core.designsystem.generated.resources.card_detail_load_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditCardEntry
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.detail.CardDetailScreen
import com.whatever.caro.feature.card.detail.CardDetailViewModel
import com.whatever.caro.feature.card.detail.mvi.CardDetailIntent
import com.whatever.caro.feature.card.detail.mvi.CardDetailSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardDetailRoute(
    viewModel: CardDetailViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loadErrorMessage = stringResource(Res.string.card_detail_load_error)
    val deleteErrorMessage = stringResource(Res.string.card_detail_delete_error)
    var hasResumed by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasResumed) {
            viewModel.intent(CardDetailIntent.RefreshCards)
        } else {
            hasResumed = true
        }
    }

    LaunchedEffect(
        viewModel,
        navDispatcher,
        snackbarController,
        loadErrorMessage,
        deleteErrorMessage,
    ) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                CardDetailSideEffect.NavigateBack -> {
                    navDispatcher.emit(NavCommand.Back)
                }

                is CardDetailSideEffect.NavigateToEdit -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            EditCardEntry(
                                cardId = sideEffect.cardId,
                                front = sideEffect.front,
                                back = sideEffect.back,
                            ),
                        ),
                    )
                }

                CardDetailSideEffect.ShowLoadError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = loadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                    navDispatcher.emit(NavCommand.Back)
                }

                CardDetailSideEffect.ShowRefreshError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = loadErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                CardDetailSideEffect.ShowDeleteError -> {
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

    CardDetailScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

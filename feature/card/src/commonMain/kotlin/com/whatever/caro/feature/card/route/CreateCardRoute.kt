package com.whatever.caro.feature.card.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_max_cards_reached
import caromobile.core.designsystem.generated.resources.card_save_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.card.CreateCardScreen
import com.whatever.caro.feature.card.CreateCardViewModel
import com.whatever.caro.feature.card.mvi.CreateCardIntent
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
    val maxCardsReachedMessage = stringResource(Res.string.card_max_cards_reached)
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    // 시스템 뒤로가기도 상단 화살표와 같은 경로를 타야 입력이 조용히 사라지지 않는다.
    NavigationBackHandler(
        state = backState,
        onBackCompleted = {
            if (state.isSaving.not()) {
                viewModel.intent(CreateCardIntent.ClickBack)
            }
        },
    )

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

                CreateCardSideEffect.ShowMaxCardsReached -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = maxCardsReachedMessage,
                            style = CaroSnackbarStyle.Info,
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

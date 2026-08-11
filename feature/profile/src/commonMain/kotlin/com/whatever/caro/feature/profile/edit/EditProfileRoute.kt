package com.whatever.caro.feature.profile.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.profile_snackbar_network_error
import caromobile.core.designsystem.generated.resources.profile_snackbar_nickname_check_error
import caromobile.core.designsystem.generated.resources.profile_snackbar_random_nickname_error
import caromobile.core.designsystem.generated.resources.profile_snackbar_update_nickname_error
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditProfileRoute(
    viewModel: EditProfileViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val nicknameCheckErrorMessage = stringResource(Res.string.profile_snackbar_nickname_check_error)
    val randomNicknameErrorMessage = stringResource(Res.string.profile_snackbar_random_nickname_error)
    val updateNicknameErrorMessage = stringResource(Res.string.profile_snackbar_update_nickname_error)
    val networkErrorMessage = stringResource(Res.string.profile_snackbar_network_error)

    NavigationBackHandler(
        state = backState,
        onBackCompleted = {
            if (state.isLoading.not()) {
                viewModel.intent(EditProfileIntent.ClickBack)
            }
        },
    )

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is EditProfileSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }

                EditProfileSideEffect.ShowNicknameCheckError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = nicknameCheckErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                EditProfileSideEffect.ShowRandomNicknameError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = randomNicknameErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                EditProfileSideEffect.ShowUpdateNicknameError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = updateNicknameErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }

                EditProfileSideEffect.ShowNetworkError -> {
                    snackbarController.show(
                        SnackBarMessage(
                            message = networkErrorMessage,
                            style = CaroSnackbarStyle.Error,
                        ),
                    )
                }
            }
        }
    }

    EditProfileScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

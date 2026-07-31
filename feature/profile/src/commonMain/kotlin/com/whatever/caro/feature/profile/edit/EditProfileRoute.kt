package com.whatever.caro.feature.profile.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.profile.edit.mvi.EditProfileIntent
import com.whatever.caro.feature.profile.edit.mvi.EditProfileSideEffect

@Composable
fun EditProfileRoute(
    viewModel: EditProfileViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

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
            }
        }
    }

    EditProfileScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

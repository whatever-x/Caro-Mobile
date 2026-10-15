package com.whatever.caro.feature.profile.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.feature.profile.create.mvi.CreateProfileIntent
import com.whatever.caro.feature.profile.create.mvi.CreateProfileSideEffect

@Composable
fun CreateProfileRoute(
    viewModel: CreateProfileViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    NavigationBackHandler(
        state = backState,
        onBackCompleted = {
            if (state.isLoading.not()) {
                viewModel.intent(CreateProfileIntent.ClickBack)
            }
        },
    )

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            navDispatcher.emit(command = createProfileNavigationCommand(sideEffect))
        }
    }

    CreateProfileScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

internal fun createProfileNavigationCommand(sideEffect: CreateProfileSideEffect): NavCommand =
    when (sideEffect) {
        CreateProfileSideEffect.NavigateLogin -> NavCommand.ResetTo(key = LoginEntry)
        CreateProfileSideEffect.NavigateHome -> NavCommand.ResetTo(key = HomeEntry)
    }

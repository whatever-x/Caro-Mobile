package com.whatever.caro.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.profile.mvi.CreateProfileSideEffect

@Composable
fun CreateProfileRoute(
    viewModel: CreateProfileViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateProfileSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }
            }
        }
    }

    CreateProfileScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

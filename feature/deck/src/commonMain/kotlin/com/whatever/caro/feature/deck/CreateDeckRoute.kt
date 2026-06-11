package com.whatever.caro.feature.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.deck.mvi.CreateDeckSideEffect

@Composable
fun CreateDeckRoute(
    viewModel: CreateDeckViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is CreateDeckSideEffect.NavigateBack -> {
                    navDispatcher.emit(command = NavCommand.Back)
                }
            }
        }
    }

    CreateDeckScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

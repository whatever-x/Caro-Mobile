package com.whatever.caro.feature.deck.detail.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.deck.detail.DeckDetailScreen
import com.whatever.caro.feature.deck.detail.DeckDetailViewModel
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailSideEffect

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                DeckDetailSideEffect.NavigateBack -> navDispatcher.emit(NavCommand.Back)
            }
        }
    }

    DeckDetailScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            TODO()
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

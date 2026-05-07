package com.whatever.caro.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.feature.home.HomeScreen
import com.whatever.caro.feature.home.HomeViewModel
import io.github.aakira.napier.Napier

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
            Napier.d { "sideEffect: $sideEffect" }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

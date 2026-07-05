package com.whatever.caro.feature.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand.To
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.CreateDeckEntry
import com.whatever.caro.core.navigator.entries.CreateProfileEntry
import com.whatever.caro.core.navigator.entries.SettingEntry
import com.whatever.caro.feature.home.HomeScreen
import com.whatever.caro.feature.home.HomeViewModel
import com.whatever.caro.feature.home.mvi.HomeSideEffect

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.NavigateToProfile -> {
                    navDispatcher.emit(command = To(key = CreateProfileEntry))
                }

                is HomeSideEffect.NavigateToCreateDeck -> {
                    navDispatcher.emit(command = To(key = CreateDeckEntry))
                }

                HomeSideEffect.NavigateToSetting -> {
                    navDispatcher.emit(command = To(key = SettingEntry))
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

package com.whatever.caro.feature.splash.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.feature.splash.SplashScreen
import com.whatever.caro.feature.splash.SplashViewModel
import com.whatever.caro.feature.splash.mvi.SplashIntent
import com.whatever.caro.feature.splash.mvi.SplashSideEffect

@Composable
fun SplashRoute(
    viewModel: SplashViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.intent(SplashIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                SplashSideEffect.NavigateLogin -> {
                    navDispatcher.emit(command = NavCommand.ResetTo(key = LoginEntry))
                }

                SplashSideEffect.NavigateHome -> {
                    navDispatcher.emit(
                        command =
                            NavCommand.ResetTo(key = HomeEntry),
                    )
                }
            }
        }
    }

    SplashScreen(state = state)
}

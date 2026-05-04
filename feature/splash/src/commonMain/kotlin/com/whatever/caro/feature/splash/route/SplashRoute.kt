package com.whatever.caro.feature.splash.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.feature.splash.SplashScreen
import com.whatever.caro.feature.splash.SplashViewModel
import com.whatever.caro.feature.splash.mvi.SplashSideEffect

@Composable
fun SplashRoute(
    viewModel: SplashViewModel,
    navDispatcher: NavigationDispatcher,
) {
    LaunchedEffect(Unit) {
        viewModel.start()
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is SplashSideEffect.NavigateLogin -> {
                    navDispatcher.emit(NavCommand.ResetTo(LoginEntry))
                }

                is SplashSideEffect.NavigateHome -> {
                    navDispatcher.emit(
                        NavCommand.ResetTo(
                            HomeEntry(
                                payload =
                                    Payload(
                                        id = 1,
                                        name = "push",
                                        deckId = sideEffect.deckId,
                                    ),
                            ),
                        ),
                    )
                }
            }
        }
    }

    SplashScreen()
}

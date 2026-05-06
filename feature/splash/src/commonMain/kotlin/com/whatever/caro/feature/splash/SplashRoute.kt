package com.whatever.caro.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.feature.splash.mvi.SplashSideEffect
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory

@Composable
fun SplashRoute(
    viewModel: SplashViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)

    LaunchedEffect(Unit) {
        viewModel.start(permissionsController)
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

                is SplashSideEffect.ShowNotificationPermissionDeniedDialog -> {
                    // TODO: 알림 권한 거부 안내 Dialog 노출
                }

                is SplashSideEffect.ShowNotificationPermissionDeniedAlwaysDialog -> {
                    // TODO: 설정 화면 이동 안내 Dialog 노출
                }
            }
        }
    }

    SplashScreen(state = state)
}

package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.navigator.entries.Payload
import com.whatever.caro.feature.login.mvi.LoginSideEffect

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is LoginSideEffect.NavigateHome -> {
                    navDispatcher.emit(
                        command =
                            NavCommand.To(
                                key =
                                    HomeEntry(
                                        payload =
                                            Payload(
                                                id = 1,
                                                name = "test",
                                            ),
                                    ),
                            ),
                    )
                }
            }
        }
    }

    LoginScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}

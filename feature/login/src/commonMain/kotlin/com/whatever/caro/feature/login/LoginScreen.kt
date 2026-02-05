package com.whatever.caro.feature.login

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginState

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
) {
    Button(
        onClick = { onIntent(LoginIntent.ClickLogin) },
    ) {
        Text(
            text = state.test,
        )
    }
}

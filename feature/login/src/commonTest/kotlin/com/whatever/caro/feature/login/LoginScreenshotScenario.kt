package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.login.mvi.LoginState

internal object LoginScreenshotScenario {
    const val ID = "login.login.initial"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            LoginScreen(state = LoginState(), onIntent = {})
        }
    }
}

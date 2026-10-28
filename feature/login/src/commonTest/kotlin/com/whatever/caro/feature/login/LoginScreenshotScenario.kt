package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.login.mvi.LoginState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object LoginScreenshotScenario {
    const val ID = "login.login.initial"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            LoginScreen(state = LoginState(), onIntent = {})
        }
    }
}

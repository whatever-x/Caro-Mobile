package com.whatever.caro.feature.splash

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState

internal object SplashScreenshotScenario {
    const val ID = "splash.splash.initial"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            SplashScreen(state = SplashState(isInitializing = true))
        }
    }
}

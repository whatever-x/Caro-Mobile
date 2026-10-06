package com.whatever.caro.feature.splash

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState

internal object SplashScreenshotScenario {
    const val ID = "splash.splash.initial"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            SplashScreen(state = SplashState(isInitializing = true))
        }
    }
}

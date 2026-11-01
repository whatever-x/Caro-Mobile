package com.whatever.caro.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.splash_content
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState

internal object SplashScreenshotScenario {
    const val ID = "splash.splash.initial"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.splash_content)),
                beforeCapture = { strings ->
                    onNodeWithText(
                        strings[Res.string.splash_content],
                    ).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            SplashScreen(state = SplashState(isInitializing = true))
        }
    }
}

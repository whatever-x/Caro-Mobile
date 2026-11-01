package com.whatever.caro.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.login_button_google
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.login.mvi.LoginState

internal object LoginScreenshotScenario {
    const val ID = "login.login.initial"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.login_button_google)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.login_button_google]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            LoginScreen(state = LoginState(), onIntent = {})
        }
    }
}

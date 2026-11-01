package com.whatever.caro.feature.profile.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.profile_title_create
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.ScreenshotScenario
import com.whatever.caro.feature.profile.ScreenshotString
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState

internal object CreateProfileScreenshotScenario {
    const val ID = "profile.create.initial"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.profile_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.profile_title_create]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            CreateProfileScreen(state = CreateProfileState(), onIntent = {})
        }
    }
}

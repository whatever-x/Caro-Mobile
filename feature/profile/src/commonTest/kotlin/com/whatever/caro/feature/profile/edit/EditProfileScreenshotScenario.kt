package com.whatever.caro.feature.profile.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.profile_title_edit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState

internal object EditProfileScreenshotScenario {
    const val ID = "profile.edit.existing"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.profile_title_edit)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.profile_title_edit]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            EditProfileScreen(state = EditProfileState(nickname = "승우"), onIntent = {})
        }
    }
}

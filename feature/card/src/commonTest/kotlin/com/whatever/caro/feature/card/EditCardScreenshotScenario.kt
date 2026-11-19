package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_title_edit_content
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.card.mvi.EditCardState

internal object EditCardScreenshotScenario {
    const val ID = "card.edit.existing"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.card_title_edit_content)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.card_title_edit_content]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            EditCardScreen(state = EditCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}

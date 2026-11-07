package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_title_create
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.card.mvi.CreateCardState

internal object CreateCardScreenshotScenario {
    const val ID = "card.create.draft"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.card_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.card_title_create]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            CreateCardScreen(state = CreateCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}

package com.whatever.caro.feature.deck.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_title_create
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import com.whatever.caro.feature.deck.edit.CreateDeckScreen

internal object CreateDeckScreenshotScenario {
    const val ID = "deck.create.initial"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.deck_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.deck_title_create]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            CreateDeckScreen(state = CreateDeckState(), onIntent = {})
        }
    }
}

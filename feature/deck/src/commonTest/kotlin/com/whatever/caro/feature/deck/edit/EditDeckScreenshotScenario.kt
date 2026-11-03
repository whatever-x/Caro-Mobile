package com.whatever.caro.feature.deck.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_edit_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.deck.edit.mvi.EditDeckState

internal object EditDeckScreenshotScenario {
    const val ID = "deck.edit.existing"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.deck_edit_title)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.deck_edit_title]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            EditDeckScreen(state = EditDeckState(name = "Android", description = "기초 학습"), onIntent = {})
        }
    }
}

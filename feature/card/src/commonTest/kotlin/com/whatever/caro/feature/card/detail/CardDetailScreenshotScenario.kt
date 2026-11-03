package com.whatever.caro.feature.card.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_detail_previous
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.card.detail.mvi.CardDetailState
import kotlinx.collections.immutable.persistentListOf

internal object CardDetailScreenshotScenario {
    const val ID = "card.detail.front"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.card_detail_previous)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.card_detail_previous]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            CardDetailScreen(
                state =
                    CardDetailState(
                        cards = persistentListOf(DeckCard(1, CardContent("Apple", "사과"), CardBadge.NEW, 0)),
                    ),
                onIntent = {
                },
            )
        }
    }
}

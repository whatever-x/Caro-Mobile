package com.whatever.caro.feature.card.delete

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.card_title_edit
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.ScreenshotScenario
import com.whatever.caro.feature.card.ScreenshotString
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object DeleteCardsScreenshotScenario {
    const val ID = "card.delete.selected"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.card_title_edit)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.card_title_edit]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            DeleteCardsScreen(
                state =
                    DeleteCardsState(
                        cards = persistentListOf(DeleteCardItem(DeckCard(1, CardContent("Apple", "사과"), CardBadge.NEW, 0))),
                        selectedCardIds = persistentSetOf(1L),
                    ),
                onIntent = {
                },
            )
        }
    }
}

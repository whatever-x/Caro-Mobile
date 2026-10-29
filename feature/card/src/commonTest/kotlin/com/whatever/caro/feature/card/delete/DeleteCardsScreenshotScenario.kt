package com.whatever.caro.feature.card.delete

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.ScreenshotScenario
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object DeleteCardsScreenshotScenario {
    const val ID = "card.delete.selected"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

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

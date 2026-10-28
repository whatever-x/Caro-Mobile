package com.whatever.caro.feature.card.detail

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.detail.mvi.CardDetailState
import com.whatever.caro.screenshot.ScreenshotScenario
import kotlinx.collections.immutable.persistentListOf

internal object CardDetailScreenshotScenario {
    const val ID = "card.detail.front"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

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

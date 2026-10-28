package com.whatever.caro.feature.deck.detail

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object DeckDetailScreenshotScenario {
    const val ID = "deck_detail.detail.empty"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            DeckDetailScreen(state = DeckDetailState(deck = Deck(1, "Android", "기초 학습", 0, 0, 0, DeckState.NOT_STARTED)), onIntent = {})
        }
    }
}
